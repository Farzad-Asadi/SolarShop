package com.example.solarShop.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import com.example.solarShop.InvoiceType
import com.example.solarShop.R
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

class PdfInvoiceExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val invoiceDao: InvoiceDocumentDao
) {

    // اندازهٔ ساده برای A4 در 72dpi
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 32

    private val titlePaint = Paint().apply {
        isAntiAlias = true
        textSize = 18f
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }
    private val headerPaint = Paint().apply {
        isAntiAlias = true
        textSize = 12f
        color = Color.BLACK
        textAlign = Paint.Align.RIGHT   // 👈 راست‌چین
    }
    private val bodyPaint = Paint().apply {
        isAntiAlias = true
        textSize = 11f
        color = Color.BLACK
        textAlign = Paint.Align.RIGHT   // 👈 راست‌چین
    }



    suspend fun exportInvoice(
        invoiceId: Int,
        showAsToman: Boolean = true
    ): File {
        val agg = invoiceDao.getInvoiceWithItems(invoiceId)
            ?: throw IllegalStateException("Invoice not found invoiceId=$invoiceId")

        val invoice = agg.invoice
        val items = agg.items

        fun formatAmount(v: Long): String = "%,d".format(v)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        val rowsPerPage = 6
        val totalItems = items.size
        val totalPages = if (totalItems <= 0) 1 else ((totalItems - 1) / rowsPerPage) + 1

        var globalRowNumber = 1
        var itemIndex = 0

        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        for (pageIndex in 0 until totalPages) {
            val page = doc.startPage(pageInfo)
            val c: Canvas = page.canvas

            val contentLeft = margin.toFloat()
            val contentRight = (pageWidth - margin).toFloat()

            var y = margin.toFloat()

            // ---------- 1) هدر بالایی ----------
            val headerTop = y
            val headerHeight = 72f
            val headerBottom = headerTop + headerHeight

            // قاب کلی هدر
            c.drawRect(
                contentLeft,
                headerTop,
                contentRight,
                headerBottom,
                linePaint
            )

            // لوگوی پنل‌کده در سمت راست هدر
            drawLogo(
                canvas = c,
                left = contentRight - 58f,
                top = headerTop + 8f,
                size = 48f
            )

            val shopHeaderPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.RIGHT
                textSize = 10f
                isFakeBoldText = true
            }

            c.drawText(
                "پنل‌کده",
                contentRight - 66f,
                headerTop + 24f,
                shopHeaderPaint
            )

            val shopSubHeaderPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.RIGHT
                textSize = 8.5f
            }

            c.drawText(
                "فروش، نصب و مشاوره تجهیزات خورشیدی",
                contentRight - 66f,
                headerTop + 40f,
                shopSubHeaderPaint
            )

            c.drawText(
                "سقز، خیابان ملت، مقابل برق آوازه",
                contentRight - 66f,
                headerTop + 56f,
                shopSubHeaderPaint
            )

            // عنوان وسط هدر: پیش فاکتور / فاکتور
            val titleText = when (invoice.type) {
                InvoiceType.PROFORMA -> "پیش فاکتور"
                InvoiceType.INVOICE -> "فاکتور فروش"
            }

            val titlePaintLocal = Paint(titlePaint).apply {
                textSize = if (invoice.type == InvoiceType.PROFORMA) 22f else 18f
                textAlign = Paint.Align.CENTER
            }

            val headerCenterY = headerTop + headerHeight / 2f
            c.drawText(
                titleText,
                (pageWidth / 2f),
                headerCenterY,
                titlePaintLocal
            )

            // تاریخ و شماره سمت چپ هدر
            val headerTextPaint = Paint(headerPaint).apply {
                textAlign = Paint.Align.RIGHT
                textSize = 11f
            }

            val headerLeftX = contentLeft + 112f  // ناحیه سمت چپ هدر
            val headerTextTop = headerTop + 25f
            c.drawText(
                "تاریخ: ${formatPersianDateTime(invoice.createdAt, true)}",
                headerLeftX,
                headerTextTop,
                headerTextPaint
            )
            c.drawText(
                "شماره: ${invoice.number}",
                headerLeftX,
                headerTextTop + 20f,
                headerTextPaint
            )

            y = headerBottom + 6f

            // ---------- 2) اطلاعات فروشنده ----------
            val partyBoxHeight = 60f
            val sellerTop = y
            val sellerBottom = sellerTop + partyBoxHeight

            // قاب فروشنده
            c.drawRect(
                contentLeft,
                sellerTop,
                contentRight,
                sellerBottom,
                linePaint
            )

            c.drawLine(
                contentRight - 26f,
                sellerTop,
                contentRight - 26f,
                sellerBottom,
                linePaint
            )




            // مرکز ستون عمودی
            val columnCenterX = contentRight - 13f            // وسط ستون 26 پیکسلی
            val columnCenterY = (sellerTop + sellerBottom) / 2f

            val sellerLabelPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 11f
            }

            // متن برچسب
            val sellerLabel = "فروشنده"

            // ذخیره وضعیت فعلی کانواس
            c.save()

            // چرخاندن ۹۰- درجه حول مرکز ستون
            c.rotate(-90f, columnCenterX, columnCenterY)

            // چون متن افقی است ولی کانواس چرخیده، اینجا افقی می‌نویسیم اما در خروجی عمودی دیده می‌شود
            c.drawText(
                sellerLabel,
                columnCenterX,            // محور افقی (الان عمودی دیده می‌شود)
                columnCenterY + 4f,       // کمی جابه‌جایی برای تنظیم خط پایه (چشمی تنظیمش کن)
                sellerLabelPaint
            )

            // برگرداندن کانواس به حالت عادی
            c.restore()







            val partyLabelPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.RIGHT
                textSize = 11f
            }

            var py = sellerTop + 20f

            c.drawText(
                "${invoice.sellerLabel ?: "نام"}: ${invoice.sellerName}",
                contentRight - 34f,
                py,
                partyLabelPaint
            )

            c.drawText(
                "تلفن: ${formatIranMobileForDisplay(invoice.sellerPhone.orEmpty())}",
                (pageWidth / 2f),
                py,
                partyLabelPaint
            )
            py += 20f
            c.drawText(
                "آدرس: ${invoice.sellerAddress.orEmpty()}",
                contentRight - 34f,
                py,
                partyLabelPaint
            )

            y = sellerBottom

            y = sellerBottom + 6f

            // ---------- 3) اطلاعات خریدار ----------
            val buyerTop = y
            val buyerBottom = buyerTop + partyBoxHeight

            c.drawRect(
                contentLeft,
                buyerTop,
                contentRight,
                buyerBottom,
                linePaint
            )



            c.drawLine(
                contentRight - 26f,
                buyerTop,
                contentRight - 26f,
                buyerBottom,
                linePaint
            )





            val columnCenterYBuyer = (buyerTop + buyerBottom) / 2f

            val buyerLabelPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 11f
            }

            // متن برچسب
            val buyerLabel = "خریدار"

            // ذخیره وضعیت فعلی کانواس
            c.save()

            // چرخاندن ۹۰- درجه حول مرکز ستون
            c.rotate(-90f, columnCenterX, columnCenterYBuyer)

            // چون متن افقی است ولی کانواس چرخیده، اینجا افقی می‌نویسیم اما در خروجی عمودی دیده می‌شود
            c.drawText(
                buyerLabel,
                columnCenterX,            // محور افقی (الان عمودی دیده می‌شود)
                columnCenterYBuyer + 4f,       // کمی جابه‌جایی برای تنظیم خط پایه (چشمی تنظیمش کن)
                buyerLabelPaint
            )

            // برگرداندن کانواس به حالت عادی
            c.restore()





            py = buyerTop + 20f

            c.drawText(
                "${invoice.buyerLabel ?: "نام"}: ${invoice.buyerName}",
                contentRight - 34f,
                py,
                partyLabelPaint
            )
            c.drawText(
                "تلفن: ${formatIranMobileForDisplay(invoice.buyerPhone.orEmpty())}",
                (pageWidth / 2f),
                py,
                partyLabelPaint
            )
            py += 16f
            c.drawText(
                "آدرس: ${invoice.buyerAddress.orEmpty()}",
                contentRight - 34f,
                py,
                partyLabelPaint
            )

            y = buyerBottom + 12f

            // ---------- 4) تیتر «محصولات» ----------
            val productsHeaderHeight = 24f
            val productsHeaderTop = y
            val productsHeaderBottom = productsHeaderTop + productsHeaderHeight

            c.drawRect(
                contentLeft,
                productsHeaderTop,
                contentRight,
                productsHeaderBottom,
                linePaint
            )

            val productsTitlePaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 12f
            }

            c.drawText(
                "محصولات/خدمات",
                (contentLeft + contentRight) / 2f,
                productsHeaderTop + 16f,
                productsTitlePaint
            )

            y = productsHeaderBottom

            // ---------- 5) جدول اقلام ----------

            val tableHeaderHeight = 22f
            val rowHeight = 44f

            // ستون‌ها (از راست به چپ)
            val colRight = contentRight

            // خطوط عمودی سرستون
            val edgeRowLeft = colRight - 26f          // بعد از ردیف
            val edgeDescLeft = colRight - 240f        // بعد از شرح کالا
            val edgeUnitLeft = colRight - 276f        // بعد از واحد
            val edgeQtyLeft = colRight - 306f         // بعد از تعداد
            val edgeUnitPriceLeft = colRight - 366f   // بعد از فی
            val edgeDiscountLeft = colRight - 446f    // بعد از تخفیف
            val edgeAmountLeft = contentLeft          // لبه چپ جدول

            val myVerticals = listOf(
                colRight,
                edgeRowLeft,
                edgeDescLeft,
                edgeUnitLeft,
                edgeQtyLeft,
                edgeUnitPriceLeft,
                edgeDiscountLeft,
                edgeAmountLeft
            )

            // مختصات متن هدر (مرکز هر ستون)
            fun colCenter(right: Float, left: Float) = (right + left) / 2f

            val xRowHeader       = colCenter(colRight, edgeRowLeft)          // ردیف
            val xDescHeader      = colCenter(edgeRowLeft, edgeDescLeft)      // شرح
            val xUnitHeader      = colCenter(edgeDescLeft, edgeUnitLeft)     // واحد
            val xQtyHeader       = colCenter(edgeUnitLeft, edgeQtyLeft)      // تعداد
            val xUnitPriceHeader = colCenter(edgeQtyLeft, edgeUnitPriceLeft) // فی
            val xDiscountHeader  = colCenter(edgeUnitPriceLeft, edgeDiscountLeft) // تخفیف
            val xAmountHeader    = colCenter(edgeDiscountLeft, edgeAmountLeft)    // مبلغ

            // مختصات متن سطرها (راست‌چین، نزدیک لبه راست هر ستون)
            val padding = 4f
            val xRowValue       = colRight - padding
            val xDescValue      = edgeRowLeft - padding
            val xUnitValue      = edgeDescLeft - padding
            val xQtyValue       = edgeUnitLeft - padding
            val xUnitPriceValue = edgeQtyLeft - padding
            val xDiscountValue  = edgeUnitPriceLeft - padding
            val xAmountValue    = edgeDiscountLeft - padding

            // سرستون جدول
            val headerRowTop = y
            val headerRowBottom = headerRowTop + tableHeaderHeight

            c.drawLine(contentLeft, headerRowTop, colRight, headerRowTop, linePaint)
            c.drawLine(contentLeft, headerRowBottom, colRight, headerRowBottom, linePaint)

            val tableHeaderPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 11f
            }

            val moneyUnitLabel = currencyLabel(showAsToman)
            val hy = headerRowTop + 14f

            c.drawText("ردیف", xRowHeader, hy, tableHeaderPaint)
            c.drawText("شرح کالا", xDescHeader, hy, tableHeaderPaint)
            c.drawText("واحد", xUnitHeader, hy, tableHeaderPaint)
            c.drawText("تعداد", xQtyHeader, hy, tableHeaderPaint)
            c.drawText("فی ($moneyUnitLabel)", xUnitPriceHeader, hy, tableHeaderPaint)
            c.drawText("تخفیف ($moneyUnitLabel)", xDiscountHeader, hy, tableHeaderPaint)
            c.drawText("مبلغ ($moneyUnitLabel)", xAmountHeader, hy, tableHeaderPaint)

            // خطوط عمودی
            myVerticals.forEach { x ->
                c.drawLine(x, headerRowTop, x, headerRowBottom, linePaint)
            }

            y = headerRowBottom

            // ردیف‌های جدول
            val rowsThisPage = rowsPerPage
            val rowTextPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.RIGHT
                textSize = 11f
            }

            repeat(rowsThisPage) { _ ->
                val rowTop = y
                val rowBottom = rowTop + rowHeight

                c.drawLine(contentLeft, rowTop, colRight, rowTop, linePaint)

                if (itemIndex < totalItems) {
                    val item = items[itemIndex]

                    val qtyStr = item.quantity.toString()
                    val unitPriceStr = formatMoneyFromToman(item.unitPrice, showAsToman)
                    val discountStr = if (item.rowDiscount != 0L)
                        formatMoneyFromToman(item.rowDiscount, showAsToman)
                    else ""
                    val amountStr = formatMoneyFromToman(item.rowTotal, showAsToman)
                    val unitStr = item.unit ?: ""

                    val textY = rowTop + 26f

                    c.drawText(amountStr, xAmountValue, textY, rowTextPaint)
                    c.drawText(discountStr, xDiscountValue, textY, rowTextPaint)
                    c.drawText(unitStr, xUnitValue, textY, rowTextPaint)
                    c.drawText(qtyStr, xQtyValue, textY, rowTextPaint)
                    c.drawText(unitPriceStr, xUnitPriceValue, textY, rowTextPaint)
                    drawRtlMultilineText(
                        canvas = c,
                        text = item.description,
                        left = edgeDescLeft + 4f,
                        top = rowTop + 5f,
                        right = edgeRowLeft - 4f,
                        bottom = rowBottom - 5f,
                        paint = rowTextPaint,
                        maxLines = 3
                    )
                    c.drawText(globalRowNumber.toString(), xRowValue, textY, rowTextPaint)

                    itemIndex++
                    globalRowNumber++
                }

                c.drawLine(contentLeft, rowBottom, colRight, rowBottom, linePaint)
                y = rowBottom
            }

            val tableBottom = y

            // خطوط عمودی کل جدول
            myVerticals.forEach { x ->
                c.drawLine(x, headerRowTop, x, tableBottom, linePaint)
            }

            // ---------- 6) جمع کل + توضیحات + امضا (فقط صفحه آخر) ----------
            if (pageIndex == totalPages - 1) {
                y = tableBottom + 12f

                // سطر جمع کل
                val totalRowTop = y
                val totalRowBottom = totalRowTop + 24f

                c.drawRect(
                    contentLeft,
                    totalRowTop,
                    colRight,
                    totalRowBottom,
                    linePaint
                )

                val totalTitlePaint = Paint(bodyPaint).apply {
                    textAlign = Paint.Align.RIGHT
                    textSize = 11f
                }

                val totalTextY = totalRowTop + 16f
                val totalFinalStr = formatMoneyFromToman(invoice.totalFinal, showAsToman)
                val totalUnit = currencyLabel(showAsToman)

                c.drawText(
                    "$totalFinalStr $totalUnit",
                    colRight - 448f,
                    totalTextY,
                    totalTitlePaint
                )
                c.drawLine(
                    (colRight - 446f),
                    totalRowTop,
                    (colRight - 446f),
                    totalRowBottom,
                    linePaint
                )
                c.drawText(
                    " جمع کل",
                    colRight - 400,
                    totalTextY,
                    totalTitlePaint
                )

                y = totalRowBottom + 16f

                // کادر توضیحات
                val notesBoxTop = y
                val notesBoxHeight = 80f
                val notesBoxBottom = notesBoxTop + notesBoxHeight

                c.drawRect(
                    contentLeft,
                    notesBoxTop,
                    colRight,
                    notesBoxBottom,
                    linePaint
                )

                val notesTitlePaint = Paint(bodyPaint).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = 11f
                }

                c.drawText(
                    "توضیحات",
                    (contentLeft + colRight) / 2f,
                    notesBoxTop + 16f,
                    notesTitlePaint
                )

                // متن توضیحات چندخطی
                invoice.notes?.takeIf { it.isNotBlank() }?.let { notesText ->
                    val notesPaint = Paint(bodyPaint).apply {
                        textSize = 10f
                        textAlign = Paint.Align.RIGHT
                    }

                    drawRtlMultilineText(
                        canvas = c,
                        text = notesText,
                        left = contentLeft + 8f,
                        top = notesBoxTop + 30f,
                        right = colRight - 8f,
                        bottom = notesBoxBottom - 8f,
                        paint = notesPaint,
                        maxLines = 4
                    )
                }

                y = notesBoxBottom + 24f

                // جدول امضا: فروشنده / خریدار / شماره حساب
                val signTop = y
                val signBottom = signTop + 60f

                val signColWidth = (colRight - contentLeft) / 3f
                val signCol2Left = contentLeft + signColWidth
                val signCol3Left = contentLeft + 2f * signColWidth

                // خطوط بیرونی
                c.drawRect(
                    contentLeft,
                    signTop,
                    colRight,
                    signBottom,
                    linePaint
                )

                // خطوط عمودی بین ستون‌ها
                c.drawLine(signCol2Left, signTop, signCol2Left, signBottom, linePaint)
                c.drawLine(signCol3Left, signTop, signCol3Left, signBottom, linePaint)

                val signTitlePaint = Paint(bodyPaint).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = 11f
                }

                val signTitleY = signTop + 18f

                c.drawText(
                    "فروشنده",
                    contentLeft + signColWidth / 2f,
                    signTitleY,
                    signTitlePaint
                )

                c.drawText(
                    "خریدار",
                    signCol2Left + signColWidth / 2f,
                    signTitleY,
                    signTitlePaint
                )

                c.drawText(
                    "شماره حساب",
                    signCol3Left + signColWidth / 2f,
                    signTitleY,
                    signTitlePaint
                )
            }

            doc.finishPage(page)
        }

        // ---------- نوشتن روی فایل ----------
        val outDir = File(context.cacheDir, "invoice_previews").apply { mkdirs() }
        val outFile = File(outDir, "Invoice-$invoiceId-${System.currentTimeMillis()}.pdf")

        outFile.outputStream().use { os ->
            doc.writeTo(os)
        }
        doc.close()

        return outFile
    }





    // کانُن: مبلغ بر حسب "تومان" است.
// این تابع فقط تصمیم می‌گیرد در PDF به تومان نشان بدهد یا ریال.
    private fun formatMoneyFromToman(
        amountToman: Long,
        showAsToman: Boolean
    ): String {
        val display = if (showAsToman) amountToman else amountToman * 10L

        return String
            .format(Locale.US, "%,d", display)
            .replace('٬', ',')
    }

    private fun currencyLabel(showAsToman: Boolean): String =
        if (showAsToman) "تومان" else "ریال"


    private fun drawLogo(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float
    ) {
        val bitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.panel_kadeh_logo
        ) ?: return

        val rect = RectF(
            left,
            top,
            left + size,
            top + size
        )

        canvas.drawBitmap(bitmap, null, rect, null)
    }

    private fun drawRtlMultilineText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
        maxLines: Int = 3
    ) {
        if (text.isBlank()) return

        val safeText = makeMixedFaEnTextSafe(text)

        val width = (right - left).toInt().coerceAtLeast(1)
        val height = (bottom - top).toInt().coerceAtLeast(1)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            textSize = paint.textSize
            typeface = paint.typeface

            // خیلی مهم:
            // برای StaticLayout نباید RIGHT بگذاری.
            // راست‌چین شدن را خود StaticLayout با direction/alignment انجام می‌دهد.
            textAlign = Paint.Align.LEFT
        }

        val lineHeight =
            (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent)
                .toInt()
                .coerceAtLeast(1)

        val availableLines =
            (height / lineHeight)
                .coerceAtLeast(1)
                .coerceAtMost(maxLines)

        val layout = StaticLayout.Builder
            .obtain(safeText, 0, safeText.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .setMaxLines(availableLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.clipRect(left, top, right, bottom)
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun makeMixedFaEnTextSafe(input: String): String {
        if (input.isBlank()) return input

        val rlm = "\u200F"
        val lrm = "\u200E"

        val ltrTokenRegex = Regex(
            """([A-Za-z]+[A-Za-z0-9\-_./:+()%#]*(?:\s+[A-Za-z0-9]+[A-Za-z0-9\-_./:+()%#]*)*|\d+(?:[./:-]\d+)*[A-Za-z%]*)"""
        )

        val protectedText = input.replace(ltrTokenRegex) { match ->
            "$lrm${match.value}$lrm"
        }

        return "$rlm$protectedText$rlm"
    }

}
