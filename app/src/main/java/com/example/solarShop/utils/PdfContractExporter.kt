package com.example.solarShop.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.solarShop.data.room.tables.contract.ContractInstanceFull
import com.example.solarShop.data.room.tables.contract.ContractTemplateAggregate
import com.example.solarShop.data.room.tables.contract.ContractTemplateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val FOOTER_H = 22  // ارتفاع تقریبی شماره صفحه

class PdfContractExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val templateRepo: ContractTemplateRepository,
) {

    private fun contentBottomY() =contentBottom


    private val pageWidth = 595; private val pageHeight = 842
    private val margin = 40; private val lineGap = 8

    private val titlePaint = TextPaint().apply {
        isAntiAlias = true; typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textSize = 18f; color = Color.BLACK
    }
    private val headerPaint = TextPaint().apply {
        isAntiAlias = true; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        textSize = 14f; color = Color.BLACK
    }
    private val bodyPaint = TextPaint().apply { isAntiAlias = true; textSize = 12f; color = Color.BLACK }
    private val grayPaint = TextPaint().apply { isAntiAlias = true; textSize = 11f; color = Color.BLACK}

    private val NBSP: Char = '\u00A0'
    private val SEP: String = "       "   // جداکننده بین آیتم‌ها (می‌توانی " | " بگذاری)

    private val dots = "." // جایگزین موقت
    // ابعاد صفحه و حواشی
    private val outerMargin = 24              // حاشیهٔ بیرونی صفحه
    private val framePadding = 14             // پدینگ داخل کادر
    private val FOOTER_H = 22                 // ارتفاع فوتر (شماره صفحه)

    // مختصات ناحیۀ محتوا؛ هر صفحه آپدیت می‌شوند
    private var contentLeft = 0
    private var contentTop = 0
    private var contentRight = 0
    private var contentBottom = 0

    private val dotsM = "............................."      // متوسط
    private val dotsL = "............................................................................................" // بزرگ (برای نشانی)

    //هِلپر برای مقدار یا نقطه‌چین بر اساس سایز
    private fun valueOrDots(v: String?, large: Boolean): String =
        v?.takeIf { it.isNotBlank() } ?: if (large) dotsL else dotsM


    // دسترسی سریع
    private fun contentWidth() = (contentRight - contentLeft).coerceAtLeast(10)

    private fun drawInlineKV(
        c: Canvas,
        text: String, // مثلا [ "نام:" to "علی", "نام پدر:" to "محمد", ... ]
        y: Int,
        tp: TextPaint = bodyPaint
    ): Int {

        // یک پاراگراف واحد، که خودش هرجا لازم شد می‌شکند
        return drawStaticLayout(
            c = c,
            text = text,
            top = y,
            x = contentLeft,
            right =contentRight,
            tp = tp
        ) + (lineGap / 2)
    }


    private fun drawInlineKVs(
        c: Canvas,
        pairs: List<Pair<String, String?>>, // مثلا [ "نام:" to "علی", "نام پدر:" to "محمد", ... ]
        y: Int,
        tp: TextPaint = bodyPaint
    ): Int {
        val text = pairs.joinToString(SEP) { (label, v) ->
            val value = v?.takeIf { it.isNotBlank() } ?: dots
            // «نام:‌علی» — NBSP بین کلید و مقدار
            "${label.trimEnd()}$NBSP$value"
        }
        // یک پاراگراف واحد، که خودش هرجا لازم شد می‌شکند
        return drawStaticLayout(
            c = c,
            text = text,
            top = y,
            x = contentLeft,
            right =contentRight,
            tp = tp
        ) + (lineGap / 2)
    }

    private fun drawInlineKVsSized(
        c: Canvas,
        items: List<Triple<String, String?, Boolean>>, // label, value, isLarge
        y: Int,
        tp: TextPaint = bodyPaint
    ): Int {
        val text = items.joinToString(SEP) { (label, v, large) ->
            "${label.trimEnd()}$NBSP${valueOrDots(v, large)}"
        }
        return drawStaticLayout(
            c = c,
            text = text,
            top = y,
            x = contentLeft,
            right = contentRight,
            tp = tp
        ) + (lineGap / 2)
    }



    private fun drawPartyCard(
        c: Canvas,
        yStart: Int,
        indexLabel: String,
        fullName: String?,
        workShop: String?,
        nationalId: String?,
        phone: String?,
        address: String?
    ): Int {
        var y = yStart
        y = drawSubHeader(c, "$indexLabel:", y)

        y = drawInlineKVsSized(
            c, listOf(
                Triple("نام:",       fullName,   false), // متوسط
                Triple("کد ملی:",    nationalId,false),  // متوسط
                Triple("کارگاه/شرکت:",workShop, false), // متوسط
                Triple("نشانی:",     address,   true),   // بزرگ
                Triple("تلفن:",      phone,     false)   // متوسط
            ),
            y
        )
        return y + (lineGap / 2)
    }


    private fun drawCheckbox(c: Canvas, x: Float, yCenter: Float, size: Float = 10f) {
        val p = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.BLACK
            isAntiAlias = true
        }
        val half = size / 2f
        c.drawRect(x - half, yCenter - half, x + half, yCenter + half, p)
    }

    private fun drawPaymentOptionsRow(
        c: Canvas,
        yStart: Int
    ): Int {
        // «مبلغ … تومان بصورت  [ ] نقد   [ ] انتقال به کارت   [ ] چک  در تاریخ … به عنوان پیش‌پرداخت تحویل متخصص گردید.»
        var y = yStart

        // خطِ “مبلغ …… تومان بصورت”
        y = drawInlineKV(c, "مبلغ:........................ تومان به صورت :", y) // “مبلغ: ........”
        // همان خط ادامه پیدا کند:


        // ردیف سه گزینه با چک‌باکس (یک خط)
        val baseline = y + bodyPaint.textSize
        val left = contentLeft.toFloat()
        var cursorX = left

        fun drawOpt(label: String) {
            drawCheckbox(c, cursorX + 6f, baseline - bodyPaint.textSize * 0.4f)
            cursorX += 16f
            c.drawText(label, cursorX, baseline, bodyPaint)
            cursorX += (bodyPaint.measureText(label) + 24f)
        }

        drawOpt("نقد")
        drawOpt("انتقال به کارت")
        drawOpt("چک")

        y = (baseline + lineGap).toInt()

        // ادامه‌ی جمله: «در تاریخ …… به عنوان پیش‌پرداخت تحویل متخصص گردید.»
        y = drawInlineKVs(
            c,
            listOf(
                "در تاریخ:.............." to dots,
                "به عنوان پیش‌ پرداخت تحویل پیمانکار گردید." to ""
            ),
            y
        )
        return y
    }

    private fun drawFormLine(c: Canvas, label: String, y: Int): Int {
        // «برچسب:  ............» در یک پاراگراف (اگر طولانی شد می‌شکند)
        return drawInlineKV(c, label, y)
    }

    private fun drawClosingBlock(
        doc: PdfDocument,
        state: PageState,
        sectionsCount: Int,
        notesCount: Int
    ): PageState {
        var st = state

        // 2.1) ردیف‌های پرداخت
        run {
            val s = ensureSpace(doc, st.page, st.canvas, st.y, st.pageNum, need = 120)
            st = st.copy(page = s.page, canvas = s.canvas, y = s.y, pageNum = s.pageNum)

            st.y = drawSectionHeader(st.canvas, "خلاصهٔ پرداخت", st.y)
            st.y = drawPaymentOptionsRow(st.canvas, st.y)

            // اگر با چک پرداخت شد
            st.y = drawParagraphs(st.canvas, "***در صورت پرداخت به وسیلهٔ چک مشخصات چک در ذیل وارد گردد:", st.y)
            st.y = drawParagraphs(st.canvas, "چک به شماره.................... به تاریخ ........................ از شعبه.............. بانک ............ با کد شعبه .............. به مبلغ .......................", st.y)

        }

        // 2.2) جملهٔ شمارشی (ماده/تبصره/صفحه)
        run {
            val s = ensureSpace(doc, st.page, st.canvas, st.y, st.pageNum, need = 60)
            st = st.copy(page = s.page, canvas = s.canvas, y = s.y, pageNum = s.pageNum)

            // دقت: تعداد صفحه را بعد از رسم تمام این بلوک می‌خواهیم، اما همینجا هم قابل قبول است،
            // چون ممکن است همین بلوک خودش صفحه‌ اضافه کند. پس بعد از تکمیل بلوک، عدد واقعی را جایگزین می‌کنیم.
            val tempPages = st.pageNum // مقدار موقت
            val summary = "این قرارداد شامل  ${sectionsCount}  ماده و ${notesCount}  تبصره و ${tempPages}  صفحه در دو نسخه تهیه شده و پس از امضا هر نسخه حکم واحد دارد."
            st.y = drawParagraphs(st.canvas, summary, st.y)
        }

        // 2.3) امضاها (بالای فوتر/داخل کادر)
        run {
            val s = ensureSpace(doc, st.page, st.canvas, st.y, st.pageNum, need = 140)
            st = st.copy(page = s.page, canvas = s.canvas, y = s.y, pageNum = s.pageNum)

            st.y += lineGap

            st.y = drawParagraphs(st.canvas, "نام و نام خانوادگی و امضای کارفرما:", st.y)
            st.y = drawParagraphs(st.canvas, "نام و نام خانوادگی و مهر/امضای پیمانکار:", st.y)
            st.y = drawParagraphs(st.canvas, "نام و نام خانوادگی، کد ملی و امضای شاهد اول:", st.y)
            st.y = drawParagraphs(st.canvas, "نام و نام خانوادگی، کد ملی و امضای شاهد دوم:", st.y)

            st.y += lineGap
        }

        return st
    }

    // یک پاراگراف را داخل ناحیه‌ی محتوا (contentLeft..contentRight) رسم می‌کند
// و y جدید را برمی‌گرداند. برای متن‌های کوتاه/میانه مناسب است.
    private fun drawParagraphs(
        c: Canvas,
        text: String,
        y: Int,
        tp: TextPaint = bodyPaint
    ): Int {
        if (text.isBlank()) return y
        val width = (contentRight - contentLeft).coerceAtLeast(10)

        val layout =
            StaticLayout.Builder
                .obtain(text, 0, text.length, tp, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()

        c.save()
        c.translate(contentLeft.toFloat(), y.toFloat())
        layout.draw(c)
        c.restore()

        return y + layout.height + lineGap
    }

    suspend fun exportTemplate(templateId: Int): File {
        val agg = templateRepo.getTemplateAggregate(templateId)
            ?: error("Template not found: $templateId")

        val doc = PdfDocument()
        var pageNum = 1
        var page = newPage(doc, pageNum)
        var c = page.canvas
        var y = contentTop


        // هدر
        y = drawCentered(c, agg.template.title, y, titlePaint) + lineGap


        //طرفین قرارداد
        y = drawSectionHeader(c, "طرفین قرارداد ", y)

        val parties = agg.parties // همان لیست DAO
        parties.forEachIndexed { idx, p ->
            // قبل از چاپ هر کارت مطمئن شو جا داریم:
            ensureSpace(doc, page, c, y, pageNum, need = 160).also {
                page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
            }

            val label = when {
                (p.role.isNotBlank()) -> p.role
                (p.role.isNotBlank()) -> p.role
                else -> "طرف ${idx + 1}"
            }

            y = drawPartyCard(
                c = c,
                yStart = y,
                indexLabel = label,
                fullName  = p.fullName,      // اگر نام/سمت را یکی نگه می‌داری، همین کافی‌ست
                workShop  = p.companyName,
                nationalId = p.nationalId,   // اگر فیلد نامش فرق دارد، اینجا تطبیق بده
                phone = p.phone,
                address = p.address
            )
        }


        // مواد و تبصره‌ها
        val sections = agg.sections.sortedBy { it.orderNo }
        if (sections.isNotEmpty()) {
            ensureSpace(doc, page, c, y, pageNum, need = 50).also {
                page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
            }
            y = drawSectionHeader(c, "مواد قرارداد", y)
        }
        sections.forEach { s ->
            ensureSpace(doc, page, c, y, pageNum, need = 50).also {
                page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
            }
            val title = "ماده ${s.orderNo} - ${s.title.orEmpty()}"
            y = drawSubHeader(c, title, y)
            if (s.body.isNotBlank()) {
                var st = PageState(page, c, y, pageNum)
                st = drawParagraphsPaged(doc, st, s.body)
                page = st.page; c = st.canvas; y = st.y; pageNum = st.pageNum

            }

            val notes = agg.notes.filter { it.sectionId == s.id }.sortedBy { it.orderNo }
            notes.forEach { n ->
                ensureSpace(doc, page, c, y, pageNum, need = 50).also {
                    page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
                }
                val bullet = "تبصره ${s.orderNo}.${n.orderNo}"
                y = drawBullet(c, bullet, dots ?: dots, y)
                if (n.body.isNotBlank()) {
                    var st = PageState(page, c, y, pageNum)
                    st = drawParagraphsPaged(doc, st, n.body)
                    page = st.page; c = st.canvas; y = st.y; pageNum = st.pageNum

                }

            }
        }


        //توضیحات

        y = drawSectionHeader(c, "توضیحات : ", y)
        ensureSpace(doc, page, c, y, pageNum, need = 50).also {
            page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
        }
        y = drawParagraphs(c,"..........................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................",y )


        // === پس از تمام شدن مواد و تبصره‌ها ===
        val sectionsCount = sections.size
        val notesCount    = agg.notes.size

        var st2 = PageState(page, c, y, pageNum)
        st2 = drawClosingBlock(doc, st2, sectionsCount, notesCount)
        page = st2.page; c = st2.canvas; y = st2.y; pageNum = st2.pageNum




        drawFooterPageNumber(c, pageNum)
        doc.finishPage(page)
        val out = File(File(context.cacheDir, "contract_previews").apply { mkdirs() },
            "ContractTemplate-$templateId-${System.currentTimeMillis()}.pdf"
        )
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }

    suspend fun exportInstance(full: ContractInstanceFull): File {
        val model = mapInstanceFullToPrintable(full)
        return renderPdf(model, "ContractInstance-${full.instance.id}")
    }



    private fun newPage(doc: PdfDocument, pageNum: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        // کادر: از حاشیهٔ بیرونی تا بالای فوتر
        val frameRect = Rect(
            outerMargin,
            outerMargin,
            pageWidth - outerMargin,
            pageHeight - outerMargin - FOOTER_H
        )

        // مستطیل یا گردگوشه؛ هرکدام را می‌خواهی انتخاب کن
        val framePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.parseColor("#C8C8C8")
            isAntiAlias = true
        }
        // مستطیل ساده:
        c.drawRect(frameRect, framePaint)
        // یا گردگوشه:
        // c.drawRoundRect(frameRect.left.toFloat(), frameRect.top.toFloat(),
        //    frameRect.right.toFloat(), frameRect.bottom.toFloat(), 10f, 10f, framePaint)

        // پدینگ داخل کادر = ناحیهٔ محتوای قابل نوشتن
        contentLeft   = frameRect.left   + framePadding
        contentTop    = frameRect.top    + framePadding
        contentRight  = frameRect.right  + framePadding * -1
        contentBottom = frameRect.bottom + framePadding * -1

        return page
    }

    private fun drawFooterPageNumber(c: Canvas, pageNum: Int) {
        val employerTxt   = "امضای کارفرما"
        val contractorTxt = "امضای پیمانکار"
        val pageTxt       = "صفحه $pageNum"

        // ارتفاع نوشته‌ی فوتر (زیر کادر)
        val y = pageHeight - outerMargin - 10f

        // برای اینکه وسط هر نیمه را کمی از خط میانی فاصله بدهیم
        val halfGap = 16f

        // نیمه‌ی چپ: [outerMargin .. mid - gap]
        val leftStart = outerMargin.toFloat()
        val leftEnd   = pageWidth / 2f - halfGap
        val leftX = (leftStart + leftEnd - bodyPaint.measureText(employerTxt)) / 2f

        // وسط صفحه برای شمارهٔ صفحه
        val centerX = (pageWidth - bodyPaint.measureText(pageTxt)) / 2f

        // نیمه‌ی راست: [mid + gap .. pageWidth - outerMargin]
        val rightStart = pageWidth / 2f + halfGap
        val rightEnd   = (pageWidth - outerMargin).toFloat()
        val rightX = (rightStart + rightEnd - bodyPaint.measureText(contractorTxt)) / 2f

        c.drawText(employerTxt,   leftX,   y, bodyPaint)
        c.drawText(pageTxt,       centerX, y, bodyPaint)
        c.drawText(contractorTxt, rightX,  y, bodyPaint)
    }


    private fun drawSectionHeader(c: Canvas, text: String, y: Int): Int {
        val h = 24
        val rect = Rect(contentLeft, y, contentRight, y + h)
        val bg = Paint().apply { color = Color.parseColor("#EEEEEE") }
        c.drawRect(rect, bg)
        c.drawText(text, (contentRight - headerPaint.measureText(text)), (y + 17f), headerPaint)
        return rect.bottom + lineGap
    }

    private fun drawSubHeader(c: Canvas, text: String, y: Int): Int {
        val rect = Rect(margin, y, pageWidth - margin, y + 22)
        val bg = Paint().apply { color =Color.WHITE }
        c.drawRect(rect, bg)
        c.drawText(text, (pageWidth - margin - headerPaint.measureText(text)), (y + 16f), headerPaint)
        return rect.bottom + lineGap
    }
    private fun drawCentered(c: Canvas, text: String, y: Int, tp: TextPaint): Int {
        val w = tp.measureText(text)
        c.drawText(text, (pageWidth - w) / 2f, y + tp.textSize, tp)
        return (y + tp.textSize + 4).toInt()
    }
    private fun drawKeyValue(c: Canvas, key: String, value: String, y: Int): Int {
        val keyW = bodyPaint.measureText("$key: ")
        val lineY = y + bodyPaint.textSize.toInt()
        c.drawText("$key: ", (pageWidth - margin - keyW), lineY.toFloat(), bodyPaint)
        return drawStaticLayout(
            c,
            value,
            lineY - (bodyPaint.textSize * 0.8f).toInt(),
            x = contentLeft  ,
            right = (contentRight  - keyW.toInt() - 8 ) ,
            tp = bodyPaint) + lineGap
    }
    private fun drawMultiline(c: Canvas, label: String, value: String, y: Int): Int {
        val lineY = y + bodyPaint.textSize.toInt()
        c.drawText("$label:", (pageWidth - margin - bodyPaint.measureText("$label:")),
            lineY.toFloat(), bodyPaint)
        return drawStaticLayout(
            c,
            value,
            lineY + 2,
            x = contentLeft,
            right = contentRight,
            tp = bodyPaint) + lineGap
    }
    // پاراگراف را به اندازه‌ی فضای هر صفحه می‌شکند و ادامه را صفحه‌ی بعد می‌کشد
    private fun drawParagraphsPaged(
        doc: PdfDocument,
        state: PageState,
        text: String
    ): PageState {
        if (text.isBlank()) return state

        var st = state                 // ✅ کپی قابل‌تغییر محلی
        var remainingStart = 0
        val width = contentWidth()

        while (remainingStart < text.length) {
            val limit = contentBottomY()
            if (st.y + bodyPaint.textSize.toInt() > limit) {
                val s = ensureSpace(doc, st.page, st.canvas, st.y, st.pageNum, need = 30)
                st = st.copy(page = s.page, canvas = s.canvas, y = s.y, pageNum = s.pageNum) // ✅
            }

            val layout =
                StaticLayout.Builder
                    .obtain(text, remainingStart, text.length, bodyPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)
                    .build()

            val available = (limit - st.y).coerceAtLeast(0)
            var fitLines = layout.lineCount
            for (i in 0 until layout.lineCount) {
                if (layout.getLineBottom(i) > available) { fitLines = i; break }
            }

            if (fitLines <= 0) {
                val s = ensureSpace(doc, st.page, st.canvas, st.y, st.pageNum, need = 30)
                st = st.copy(page = s.page, canvas = s.canvas, y = s.y, pageNum = s.pageNum) // ✅
                continue
            }

            val pageStartIdx = remainingStart
            val pageEndIdx = remainingStart + (layout.getLineEnd(fitLines - 1) - layout.getLineStart(0))

            val pageLayout =
                StaticLayout.Builder
                    .obtain(text, pageStartIdx, pageEndIdx, bodyPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)
                    .build()

            st.canvas.save()
            st.canvas.translate(contentLeft.toFloat(), st.y.toFloat())
            pageLayout.draw(st.canvas)
            st.canvas.restore()

            st.y += pageLayout.height + lineGap
            remainingStart = pageEndIdx
        }

        return st                      // ✅ خروجیِ وضعیت به‌روز
    }


    private fun drawBullet(c: Canvas, bullet: String, text: String, y: Int): Int {
        val bW = bodyPaint.measureText("$bullet: ")
        val lineY = y + bodyPaint.textSize.toInt()
        c.drawText("$bullet: ", (pageWidth - margin - bW), lineY.toFloat(), bodyPaint)
        return drawStaticLayout(c, text, lineY - (bodyPaint.textSize * 0.8f).toInt(),
            x = contentLeft, right = (contentRight - bW.toInt() - 8), tp = bodyPaint) + (lineGap / 2)
    }

    private fun drawStaticLayout(c: Canvas, text: String, top: Int, x: Int, right: Int, tp: TextPaint): Int {
        if (text.isBlank()) return top
        val width = (right - x).coerceAtLeast(10)
        val layout =
            if (Build.VERSION.SDK_INT >= 23) {
                StaticLayout.Builder.obtain(text, 0, text.length, tp, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, tp, width, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false)
            }
        c.save()
        c.translate(x.toFloat(), top.toFloat())
        layout.draw(c)
        c.restore()
        return top + layout.height
    }

    private fun ensureSpace(
        doc: PdfDocument,
        page: PdfDocument.Page,
        canvas: Canvas,
        y: Int,
        pageNum: Int,
        need: Int
    ): SpaceResult {
        val limit = contentBottomY()
        if (y + need < limit) return SpaceResult(page, canvas, y, pageNum)

        // صفحه فعلی را با فوتر (زیر کادر) ببند
        drawFooterPageNumber(canvas, pageNum)
        doc.finishPage(page)

        val nextNum = pageNum + 1
        val newPage = newPage(doc, nextNum)   // ✅ کادر + content* دوباره ست می‌شود
        return SpaceResult(newPage, newPage.canvas, contentTop, nextNum)
    }



    // نگه‌دارندهٔ موقت برای ensureSpace (ساده نگه داشتیم؛ می‌تونی ساختار کد رو بهتر کنی)
    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null


    // از TemplateAggregate به PrintableContract
    private fun mapTemplateAggregateToPrintable(agg: ContractTemplateAggregate): PrintableContract {
        val sections = agg.sections.map {
            PrintableSection(
                id = it.id,
                orderNo = it.orderNo,
                title = it.title,
                body = it.body
            )
        }
        val notes = agg.notes.map {
            PrintableNote(
                sectionId = it.sectionId,
                orderNo = it.orderNo,
                title = it.title,
                body = it.body
            )
        }
        val parties = agg.parties.map {
            PrintableParty(
                role = it.role,
                fullName = it.fullName,
                workshop = it.companyName,
                nationalId = it.nationalId,
                phone = it.phone,
                address = it.address
            )
        }
        return PrintableContract(
            title = agg.template.title,
            parties = parties,
            sections = sections,
            notes = notes
        )
    }

    // از InstanceFull به PrintableContract
    private fun mapInstanceFullToPrintable(full: ContractInstanceFull): PrintableContract {
        val sections = full.sectionsWithNotes.map { it.section }.map {
            PrintableSection(
                id = it.id,
                orderNo = it.orderNo,
                title = it.title,
                body = it.body
            )
        }
        val notes = full.sectionsWithNotes.flatMap { swn ->
            swn.notes.map { n ->
                PrintableNote(
                    sectionId = swn.section.id,
                    orderNo = n.orderNo,
                    title = n.title,
                    body = n.body
                )
            }
        }
        val parties = full.parties.map {
            PrintableParty(
                role = it.role,
                fullName = it.fullName,
                workshop = it.companyName,
                nationalId = it.nationalId,
                phone = it.phone,
                address = it.address
            )
        }
        return PrintableContract(
            title = full.instance.title,
            parties = parties,
            sections = sections,
            notes = notes
        )
    }

    private fun renderPdf(model: PrintableContract, filePrefix: String): File {
        val doc = PdfDocument()
        var pageNum = 1
        var page = newPage(doc, pageNum)
        var c = page.canvas
        var y = contentTop

        // عنوان
        y = drawCentered(c, model.title, y, titlePaint) + lineGap

        // طرفین
        y = drawSectionHeader(c, "طرفین قرارداد", y)
        model.parties.forEachIndexed { idx, p ->
            ensureSpace(doc, page, c, y, pageNum, need = 160).also {
                page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
            }
            val label = if (p.role.isNotBlank()) p.role else "طرف ${idx + 1}"
            y = drawPartyCard(
                c = c,
                yStart = y,
                indexLabel = label,
                fullName = p.fullName,
                workShop = p.workshop,
                nationalId = p.nationalId,
                phone = p.phone,
                address = p.address
            )
        }

        // مواد
        val sections = model.sections.sortedBy { it.orderNo }
        if (sections.isNotEmpty()) {
            ensureSpace(doc, page, c, y, pageNum, need = 50).also {
                page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
            }
            y = drawSectionHeader(c, "مواد قرارداد", y)
        }

        sections.forEach { s ->
            ensureSpace(doc, page, c, y, pageNum, need = 50).also {
                page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
            }
            val title = "ماده ${s.orderNo} - ${s.title}"
            y = drawSubHeader(c, title, y)
            if (s.body.isNotBlank()) {
                var st = PageState(page, c, y, pageNum)
                st = drawParagraphsPaged(doc, st, s.body)
                page = st.page; c = st.canvas; y = st.y; pageNum = st.pageNum
            }

            val notes = model.notes.filter { it.sectionId == s.id }.sortedBy { it.orderNo }
            notes.forEach { n ->
                ensureSpace(doc, page, c, y, pageNum, need = 50).also {
                    page = it.page; c = it.canvas; y = it.y; pageNum = it.pageNum
                }
                val bullet = "تبصره ${s.orderNo}.${n.orderNo}"
                y = drawBullet(c, bullet, n.title ?: "", y)
                if (n.body.isNotBlank()) {
                    var st = PageState(page, c, y, pageNum)
                    st = drawParagraphsPaged(doc, st, n.body)
                    page = st.page; c = st.canvas; y = st.y; pageNum = st.pageNum
                }
            }
        }

        // (بخش توضیحات و بلوک پایانی همانند قبل؛ می‌تونی همان کدهای فعلی را بگذاری)
        val sectionsCount = sections.size
        val notesCount    = model.notes.size
        var st2 = PageState(page, c, y, pageNum)
        st2 = drawClosingBlock(doc, st2, sectionsCount, notesCount)
        page = st2.page; c = st2.canvas; y = st2.y; pageNum = st2.pageNum

        drawFooterPageNumber(c, pageNum)
        doc.finishPage(page)

        val out = File(File(context.cacheDir, "contract_previews").apply { mkdirs() },
            "$filePrefix-${System.currentTimeMillis()}.pdf"
        )
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }


}


private data class SpaceResult(
    val page: PdfDocument.Page,
    val canvas: Canvas,
    val y: Int,
    val pageNum: Int
)

private data class PageState(
    var page: PdfDocument.Page,
    var canvas: Canvas,
    var y: Int,
    var pageNum: Int
)









data class PrintableContract(
    val title: String,
    val parties: List<PrintableParty>,
    val sections: List<PrintableSection>,
    val notes: List<PrintableNote> // همه نوت‌ها یکجا؛ با sectionId ربط می‌خورند
)

data class PrintableParty(
    val role: String,
    val fullName: String?,
    val workshop: String?,
    val nationalId: String?,
    val phone: String?,
    val address: String?
)

data class PrintableSection(
    val id: Int,
    val orderNo: Int,
    val title: String,
    val body: String
)

data class PrintableNote(
    val sectionId: Int,
    val orderNo: Int,
    val title: String?,
    val body: String
)

