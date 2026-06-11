package com.example.solarShop.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import com.example.solarShop.data.local.relation.product.ProductFullInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class ProductPdfExporter @Inject constructor(
    @ApplicationContext private val app: Context
) {
    fun exportProductDetail(
        product: ProductFullInfo,
        attributes: List<ProductAttributeDisplayInfo>,
        images: List<Uri>
    ): File {
        val file = File(
            app.cacheDir,
            "product_${product.product.id}_${System.currentTimeMillis()}.pdf"
        )

        val doc = PdfDocument()
        val pageW = 595
        val pageH = 842
        val margin = 24
        val contentW = pageW - margin * 2

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 180, 180)
            strokeWidth = 1f
        }

        fun drawRtlText(
            canvas: android.graphics.Canvas,
            text: String,
            x: Int,
            y: Int,
            width: Int,
            textSize: Float = 10f,
            bold: Boolean = false
        ): Int {
            if (text.isBlank()) return 0

            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                this.textSize = textSize
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            }

            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, tp, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setIncludePad(false)
                .build()

            canvas.save()
            canvas.translate(x.toFloat(), y.toFloat())
            layout.draw(canvas)
            canvas.restore()

            return layout.height
        }

        fun decodeBitmap(uri: Uri) =
            try {
                app.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (_: Throwable) {
                null
            }

        fun newPage(): Pair<PdfDocument.Page, android.graphics.Canvas> {
            val page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
            )
            return page to page.canvas
        }

        var pageAndCanvas = newPage()
        var page = pageAndCanvas.first
        var canvas = pageAndCanvas.second
        var y = margin

        fun finishPage() {
            doc.finishPage(page)
        }

        fun ensureSpace(needed: Int) {
            if (y + needed <= pageH - margin) return

            finishPage()
            pageAndCanvas = newPage()
            page = pageAndCanvas.first
            canvas = pageAndCanvas.second
            y = margin
        }

        y += drawRtlText(
            canvas = canvas,
            text = "مشخصات کالا",
            x = margin,
            y = y,
            width = contentW,
            textSize = 16f,
            bold = true
        ) + 16

        y += drawRtlText(canvas, "نام کالا: ${product.product.name}", margin, y, contentW) + 8
        y += drawRtlText(canvas, "برند: ${product.brand?.name ?: "-"}", margin, y, contentW) + 8
        y += drawRtlText(canvas, "مدل: ${product.product.model ?: "-"}", margin, y, contentW) + 16

        val imageGap = 12
        val imgW = (contentW - imageGap) / 2
        val imgH = 150

        images.take(6).chunked(2).forEach { rowImages ->
            ensureSpace(imgH + 14)

            rowImages.forEachIndexed { index, uri ->
                val bitmap = decodeBitmap(uri) ?: return@forEachIndexed

                val left = if (index == 0) {
                    pageW - margin - imgW
                } else {
                    margin
                }

                drawBitmapFitCenter(
                    canvas = canvas,
                    bitmap = bitmap,
                    box = android.graphics.Rect(left, y, left + imgW, y + imgH)
                )
            }

            y += imgH + 14
        }

        y += 12
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), linePaint)
        y += 18

        val headers = listOf(
            "ردیف",
            "نام مشخصه",
            "کلید داخلی",
            "مقدار و واحد",
            "توضیحات"
        )

        val colWidths = listOf(34, 90, 90, 115, contentW - 34 - 90 - 90 - 115)

        fun drawTableRow(values: List<String>, rowHeight: Int, isHeader: Boolean = false) {
            var right = pageW - margin

            values.forEachIndexed { index, value ->
                val w = colWidths[index]
                val left = right - w

                canvas.drawRect(
                    left.toFloat(),
                    y.toFloat(),
                    right.toFloat(),
                    (y + rowHeight).toFloat(),
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = Color.rgb(180, 180, 180)
                        strokeWidth = 1f
                    }
                )

                drawRtlText(
                    canvas = canvas,
                    text = value,
                    x = left + 4,
                    y = y + 5,
                    width = w - 8,
                    textSize = 8.5f,
                    bold = isHeader
                )

                right = left
            }

            y += rowHeight
        }

        ensureSpace(34)
        drawTableRow(headers, rowHeight = 20, isHeader = true)

        attributes.forEachIndexed { index, attr ->
            val valueWithUnit = buildString {
                append(attr.valueText.orEmpty())
                if (!attr.unit.isNullOrBlank()) {
                    append(" ")
                    append(attr.unit)
                }
            }

            val rowValues = listOf(
                (index + 1).toString(),
                attr.title,
                attr.key,
                valueWithUnit,
                attr.description
            )

            ensureSpace(30)
            drawTableRow(rowValues, rowHeight = 28)
        }

        finishPage()

        doc.writeTo(file.outputStream())
        doc.close()

        return file
    }
}

private fun drawBitmapFitCenter(
    canvas: android.graphics.Canvas,
    bitmap: android.graphics.Bitmap,
    box: android.graphics.Rect
) {
    val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val boxRatio = box.width().toFloat() / box.height().toFloat()

    val drawW: Int
    val drawH: Int

    if (bitmapRatio > boxRatio) {
        drawW = box.width()
        drawH = (drawW / bitmapRatio).toInt()
    } else {
        drawH = box.height()
        drawW = (drawH * bitmapRatio).toInt()
    }

    val left = box.left + (box.width() - drawW) / 2
    val top = box.top + (box.height() - drawH) / 2

    val dst = android.graphics.Rect(
        left,
        top,
        left + drawW,
        top + drawH
    )

    canvas.drawBitmap(bitmap, null, dst, null)
}