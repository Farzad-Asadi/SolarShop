package com.example.solarShop.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerRepository
import com.example.solarShop.ui.orderScreen.orderCatalog.PdfExportData
import com.example.solarShop.ui.orderScreen.orderCatalog.PdfExportRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun buildPdfFile(
    context: Context,
    answerRepo: AnswerRepository,
    file: File,
    data: PdfExportData
) {
    val doc = PdfDocument()

    // A4 در 72dpi تقریباً: 595x842
    val pageW = 595
    val pageH = 842

    val margin = 24
    val contentW = pageW - margin * 2

    // ستون‌ها (جمع = contentW)
    val colNo = 34
    val colQ = 100
    val colA = 90
    val colImg = 90
    val colNote = contentW - (colNo + colQ + colA + colImg)

    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10f
    }
    val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 180, 180)
        strokeWidth = 1f
    }

    fun wrapHeightRight(text: String, width: Int, textSize: Float): Int {
        if (text.isBlank()) return 0
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = Color.BLACK
        }
        val sl = StaticLayout.Builder
            .obtain(text, 0, text.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL) // ✅ در RTL میشه راست‌چین
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL) // ✅ بهتر برای متن ترکیبی
            .setIncludePad(false)
            .build()
        return sl.height
    }

    fun drawWrappedRight(
        canvas: Canvas,
        text: String,
        x: Int,
        y: Int,
        width: Int,
        textSize: Float
    ) {
        if (text.isBlank()) return
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = Color.BLACK
        }
        val sl = StaticLayout.Builder
            .obtain(text, 0, text.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL) // ✅ در RTL میشه راست‌چین
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL) // ✅ بهتر برای متن ترکیبی
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        sl.draw(canvas)
        canvas.restore()
    }



    fun wrapHeight(text: String, width: Int, textSize: Float): Int {
        if (text.isBlank()) return 0
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = Color.BLACK
        }
        val sl = StaticLayout.Builder
            .obtain(text, 0, text.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        return sl.height
    }

    fun drawWrapped(canvas: Canvas, text: String, x: Int, y: Int, width: Int, textSize: Float) {
        if (text.isBlank()) return
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = Color.BLACK
        }
        val sl = StaticLayout.Builder
            .obtain(text, 0, text.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        sl.draw(canvas)
        canvas.restore()
    }

    // تصویر: فعلاً از photoId استفاده می‌کنیم اگر بتوانیم فایلش را پیدا کنیم.
    // اگر در پروژه‌ات متد دقیق داری (مثلاً answerRepo.getImageById)، همینجا وصلش کن.
    suspend fun loadSelectedPhotoBitmap(selectedPhotoId: Int?): Bitmap? {
        if (selectedPhotoId == null) return null

        // ✅ این بخش وابسته به ساختار پروژه توست:
        // پیشنهاد: یک متد در AnswerRepository داشته باشی:
        // fun getImageById(id:Int): AnswerImageEntity?
        // و AnswerImageEntity.fileName داخل /files/images/ باشد.

        return try {
            val img = answerRepo.getImageById(selectedPhotoId) ?: return null
            val imagesDir = File(context.filesDir, "images")
            val f = File(imagesDir, img.fileName)
            if (!f.exists()) return null
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (_: Exception) {
            null
        }
    }
    fun loadBitmapFromFile(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try { BitmapFactory.decodeFile(path) } catch (_: Throwable) { null }
    }

    fun drawCenteredText(
        canvas: Canvas,
        text: String,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        paint: Paint
    ) {
        val fm = paint.fontMetrics
        val textWidth = paint.measureText(text)
        val x = left + (width - textWidth) / 2f
        val y = top + (height / 2f) - ((fm.ascent + fm.descent) / 2f)
        canvas.drawText(text, x, y, paint)
    }


    fun drawHeader(canvas: Canvas, yStart: Int): Int {
        var y = yStart

        val rightEdge = pageW - margin

        run {
            val text = "خروجی انتخاب‌های سفارش"
            val x = rightEdge - paintBold.measureText(text)
            canvas.drawText(text, x, y.toFloat(), paintBold)
            y += 18
        }

        run {
            val text = "مشتری: ${data.header.clientName}"
            val x = rightEdge - paintText.measureText(text)
            canvas.drawText(text, x, y.toFloat(), paintText)
            y += 14
        }

        run {
            val text = "سفارش: ${data.header.orderTitle}  |  #${data.header.orderId}"
            val x = rightEdge - paintText.measureText(text)
            canvas.drawText(text, x, y.toFloat(), paintText)
            y += 18
        }


        // خط جداکننده
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), paintLine)
        y += 10

        // تیتر جدول (مثل یک ردیف واقعی)
        val x0 = margin
        val x1 = x0 + colNo
        val x2 = x1 + colQ
        val x3 = x2 + colA
        val x4 = x3 + colImg
        val x5 = margin + contentW

        val headerRowH = 28
        val headerTop = y
        val headerBottom = y + headerRowH

        // خط بالا و پایین هدر
        canvas.drawLine(x0.toFloat(), headerTop.toFloat(), x5.toFloat(), headerTop.toFloat(), paintLine)
        canvas.drawLine(x0.toFloat(), headerBottom.toFloat(), x5.toFloat(), headerBottom.toFloat(), paintLine)

        // ✅ خطوط عمودی هدر (هم مرزها هم جداکننده‌ها)
        canvas.drawLine(x0.toFloat(), headerTop.toFloat(), x0.toFloat(), headerBottom.toFloat(), paintLine)
        canvas.drawLine(x1.toFloat(), headerTop.toFloat(), x1.toFloat(), headerBottom.toFloat(), paintLine)
        canvas.drawLine(x2.toFloat(), headerTop.toFloat(), x2.toFloat(), headerBottom.toFloat(), paintLine)
        canvas.drawLine(x3.toFloat(), headerTop.toFloat(), x3.toFloat(), headerBottom.toFloat(), paintLine)
        canvas.drawLine(x4.toFloat(), headerTop.toFloat(), x4.toFloat(), headerBottom.toFloat(), paintLine)
        canvas.drawLine(x5.toFloat(), headerTop.toFloat(), x5.toFloat(), headerBottom.toFloat(), paintLine)

        // ✅ متن‌ها وسطِ ستون‌ها (افقی و عمودی)
        drawCenteredText(canvas, "#",     x0, headerTop, colNo,  headerRowH, paintBold)
        drawCenteredText(canvas, "سؤال",  x1, headerTop, colQ,   headerRowH, paintBold)
        drawCenteredText(canvas, "پاسخ",  x2, headerTop, colA,   headerRowH, paintBold)
        drawCenteredText(canvas, "عکس",   x3, headerTop, colImg, headerRowH, paintBold)
        drawCenteredText(canvas, "یادداشت", x4, headerTop, colNote, headerRowH, paintBold)

        y = headerBottom + 8
        return y

    }

    fun drawFooter(
        canvas: Canvas,
        pageIndex: Int,
        totalPages: Int
    ) {
        val text = "صفحه $pageIndex از $totalPages"

        val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 120)
            textSize = 9f
        }

        val textWidth = paintFooter.measureText(text)
        val x = (pageW - textWidth) / 2f
        val y = pageH - 12f   // کمی بالاتر از لبه پایین

        canvas.drawText(text, x, y, paintFooter)
    }


    fun centerCropSrcRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        val srcAspect = srcW.toFloat() / srcH.toFloat()
        val dstAspect = dstW.toFloat() / dstH.toFloat()

        return if (srcAspect > dstAspect) {
            // تصویر عریض‌تر از باکس است → از چپ/راست برش می‌زنیم
            val newW = (srcH * dstAspect).toInt()
            val x0 = ((srcW - newW) / 2).coerceAtLeast(0)
            Rect(x0, 0, x0 + newW, srcH)
        } else {
            // تصویر بلندتر از باکس است → از بالا/پایین برش می‌زنیم
            val newH = (srcW / dstAspect).toInt()
            val y0 = ((srcH - newH) / 2).coerceAtLeast(0)
            Rect(0, y0, srcW, y0 + newH)
        }
    }

    val paintImgPlaceholderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 245, 245) // خاکستری خیلی روشن
        style = Paint.Style.FILL
    }

    val paintImgPlaceholderBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(200, 200, 200)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    val paintImgPlaceholderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(150, 150, 150)
        textSize = 9f
        textAlign = Paint.Align.CENTER
    }

    fun drawImagePlaceholder(
        canvas: Canvas,
        rect: Rect,
        text: String = "بدون عکس"
    ) {
        // بک‌گراند
        canvas.drawRect(rect, paintImgPlaceholderBg)

        // کادر
        canvas.drawRect(rect, paintImgPlaceholderBorder)

        // متن وسط‌چین
        val cx = rect.centerX().toFloat()
        val cy = rect.centerY().toFloat()

        // تنظیم baseline برای وسط‌چین عمودی
        val fm = paintImgPlaceholderText.fontMetrics
        val textY = cy - (fm.ascent + fm.descent) / 2

        canvas.drawText(text, cx, textY, paintImgPlaceholderText)
    }



    suspend fun drawRow(canvas: Canvas, row: PdfExportRow, yTop: Int, rowH: Int) {
        val x0 = margin
        val x1 = x0 + colNo
        val x2 = x1 + colQ
        val x3 = x2 + colA
        val x4 = x3 + colImg
        val x5 = margin + contentW

        // خطوط عمودی (مرزهای جدول)
        canvas.drawLine(x0.toFloat(), yTop.toFloat(), x0.toFloat(), (yTop + rowH).toFloat(), paintLine)
        canvas.drawLine(x5.toFloat(), yTop.toFloat(), x5.toFloat(), (yTop + rowH).toFloat(), paintLine)


        // خطوط دور ردیف
        canvas.drawLine(x0.toFloat(), yTop.toFloat(), x5.toFloat(), yTop.toFloat(), paintLine)
        canvas.drawLine(x0.toFloat(), (yTop + rowH).toFloat(), x5.toFloat(), (yTop + rowH).toFloat(), paintLine)

        // خطوط عمودی
        canvas.drawLine(x1.toFloat(), yTop.toFloat(), x1.toFloat(), (yTop + rowH).toFloat(), paintLine)
        canvas.drawLine(x2.toFloat(), yTop.toFloat(), x2.toFloat(), (yTop + rowH).toFloat(), paintLine)
        canvas.drawLine(x3.toFloat(), yTop.toFloat(), x3.toFloat(), (yTop + rowH).toFloat(), paintLine)
        canvas.drawLine(x4.toFloat(), yTop.toFloat(), x4.toFloat(), (yTop + rowH).toFloat(), paintLine)

        val pad = 6
        val textY = yTop + 8

        // شماره
        canvas.drawText(row.index.toString(), (x0 + pad).toFloat(), (textY + 10).toFloat(), paintText)

        // سؤال (یک خط)
        drawWrappedRight(canvas, row.questionTitle, x1 + pad, textY, colQ - pad * 2, 10f)

        // پاسخ (یک خط)
        drawWrappedRight(canvas, row.answerTitle, x2 + pad, textY, colA - pad * 2, 10f)





        val imgBoxH = 56          // ارتفاع ثابت عکس داخل سلول
        val imgBoxW = colImg - 12 // پهنای ثابت عکس داخل سلول (pad*2)

        val boxW = colImg - pad * 2
        val boxH = imgBoxH

        val top = yTop + ((rowH - boxH) / 2).coerceAtLeast(pad)
        val rect = Rect(
            x3 + pad,
            top,
            x3 + pad + boxW,
            top + boxH
        )

        val bmp = loadBitmapFromFile(row.selectedPhotoAbsPath)
        if (bmp != null) {
            val src = centerCropSrcRect(bmp.width, bmp.height, boxW, boxH)
            canvas.drawBitmap(bmp, src, rect, null)
        } else {
            drawImagePlaceholder(canvas, rect)
        }



        // یادداشت (ممکن است چند خط شود)
        drawWrappedRight(canvas, row.note, x4 + pad, textY, colNote - pad * 2, 10f)
    }

    var pageIndex = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex).create())
    var canvas = page.canvas

    var y = margin + 18
    y = drawHeader(canvas, y)




    val minRowH = 80   // ✅ طوری که حداقل ~8 ردیف جا شود (842 - header - margin)
    val bottomLimit = pageH - margin

    for (row in data.rows) {
        // ارتفاع ردیف بر اساس note
        val noteH = wrapHeightRight(row.note, colNote - 12, 10f)
        val qH = wrapHeightRight(row.questionTitle, colQ - 12, 10f)
        val aH = wrapHeightRight(row.answerTitle, colA - 12, 10f)


        val contentH = maxOf(noteH, qH, aH)
        val dynamicH = maxOf(minRowH, contentH + 18)  // padding + متن

        // اگر جا نشد، صفحه جدید
        if (y + dynamicH > bottomLimit) {
            doc.finishPage(page)

            pageIndex += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex).create())
            canvas = page.canvas
            y = margin + 18
            y = drawHeader(canvas, y)
        }

        drawRow(canvas, row, y, dynamicH)
        y += dynamicH
    }

    doc.finishPage(page)

    withContext(Dispatchers.IO) {
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
    }
    doc.close()
}
