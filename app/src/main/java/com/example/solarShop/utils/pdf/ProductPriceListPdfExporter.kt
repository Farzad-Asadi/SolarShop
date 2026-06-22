package com.example.solarShop.utils.pdf

import android.content.Context
import android.graphics.Bitmap
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
import com.example.solarShop.utils.formatPersianDateTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.random.Random


data class ProductPriceReportRow(
    val categoryName: String,
    val coverUri: Uri?,
    val name: String,
    val model: String,
    val brand: String,
    val consumerPriceToman: Long?,
    val colleaguePriceToman: Long?
)

class ProductPriceListPdfExporter @Inject constructor(
    @ApplicationContext private val app: Context
) {


    private fun drawBitmapFitCenter(
        canvas: android.graphics.Canvas,
        bitmap: Bitmap,
        box: android.graphics.Rect
    ) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()

        if (srcW <= 0f || srcH <= 0f) return

        val boxW = box.width().toFloat()
        val boxH = box.height().toFloat()

        val scale = minOf(
            boxW / srcW,
            boxH / srcH
        )

        val drawW = srcW * scale
        val drawH = srcH * scale

        val left = box.left + (boxW - drawW) / 2f
        val top = box.top + (boxH - drawH) / 2f

        val dst = android.graphics.RectF(
            left,
            top,
            left + drawW,
            top + drawH
        )

        canvas.drawBitmap(
            bitmap,
            null,
            dst,
            Paint(Paint.ANTI_ALIAS_FLAG)
        )
    }


    fun exportProductsWithPrices(
        rows: List<ProductPriceReportRow>
    ): File {
        val persianDateForFileName =
            formatPersianDateTime(
                millis = System.currentTimeMillis(),
                noClock = true
            ).replace("/", "／")

        val randomNumber =
            Random.nextInt(1000, 9999)

        val file = File(
            app.cacheDir,
            "لیست محصولات _${persianDateForFileName}_$randomNumber.pdf"
        )


        val doc = PdfDocument()

        val pageW = 595
        val pageH = 842
        val margin = 24
        val contentW = pageW - margin * 2

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 180, 180)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(235, 235, 235)
            style = Paint.Style.FILL
        }

        fun drawRtlText(
            canvas: android.graphics.Canvas,
            text: String,
            x: Int,
            y: Int,
            width: Int,
            textSize: Float = 8.5f,
            bold: Boolean = false
        ): Int {
            if (text.isBlank()) return 0

            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                this.textSize = textSize
                typeface = if (bold) {
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    Typeface.DEFAULT
                }
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

        fun decodeBitmap(uri: Uri?): Bitmap? {
            if (uri == null) return null

            return try {
                val maxSize = 96

                val original =
                    app.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return null

                val scale =
                    minOf(
                        maxSize.toFloat() / original.width.toFloat(),
                        maxSize.toFloat() / original.height.toFloat(),
                        1f
                    )

                val targetW =
                    (original.width * scale).toInt().coerceAtLeast(1)

                val targetH =
                    (original.height * scale).toInt().coerceAtLeast(1)

                val scaled =
                    Bitmap.createScaledBitmap(
                        original,
                        targetW,
                        targetH,
                        true
                    )

                if (scaled !== original) {
                    original.recycle()
                }

                val out = ByteArrayOutputStream()

                scaled.compress(
                    Bitmap.CompressFormat.JPEG,
                    45,
                    out
                )

                val compressedBytes =
                    out.toByteArray()

                scaled.recycle()

                BitmapFactory.decodeByteArray(
                    compressedBytes,
                    0,
                    compressedBytes.size
                )
            } catch (_: Throwable) {
                null
            }
        }

        fun formatPrice(value: Long?): String {
            if (value == null || value <= 0L) return "-"

            return "%,d تومان".format(value)
        }

        fun newPage(pageIndex: Int): Pair<PdfDocument.Page, android.graphics.Canvas> {
            val page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex).create()
            )
            return page to page.canvas
        }

        var pageIndex = 1
        var pageAndCanvas = newPage(pageIndex)
        var page = pageAndCanvas.first
        var canvas = pageAndCanvas.second
        var y = margin

        fun finishPage() {
            doc.finishPage(page)
        }

        fun startNewPage() {
            finishPage()
            pageIndex++
            pageAndCanvas = newPage(pageIndex)
            page = pageAndCanvas.first
            canvas = pageAndCanvas.second
            y = margin
        }

        fun ensureSpace(needed: Int) {
            if (y + needed <= pageH - margin) return
            startNewPage()
        }

        fun drawTitle() {
            y += drawRtlText(
                canvas = canvas,
                text = "لیست کالاها با قیمت",
                x = margin,
                y = y,
                width = contentW,
                textSize = 16f,
                bold = true
            ) + 18

            y += drawRtlText(
                canvas = canvas,
                text = "تعداد کالاها: ${rows.size}",
                x = margin,
                y = y,
                width = contentW,
                textSize = 10f,
                bold = false
            ) + 14
        }

        val colImage = 52
        val colName = 128
        val colModel = 82
        val colBrand = 78
        val colConsumer = 104
        val colColleague = contentW - colImage - colName - colModel - colBrand - colConsumer

        val colWidths = listOf(
            colImage,
            colName,
            colModel,
            colBrand,
            colConsumer,
            colColleague
        )

        fun drawTableHeader() {
            val rowH = 28
            var right = pageW - margin

            val headers = listOf(
                "عکس",
                "نام",
                "مدل",
                "برند",
                "مصرف‌کننده",
                "همکار"
            )

            headers.forEachIndexed { index, title ->
                val w = colWidths[index]
                val left = right - w

                canvas.drawRect(
                    left.toFloat(),
                    y.toFloat(),
                    right.toFloat(),
                    (y + rowH).toFloat(),
                    headerBgPaint
                )

                canvas.drawRect(
                    left.toFloat(),
                    y.toFloat(),
                    right.toFloat(),
                    (y + rowH).toFloat(),
                    linePaint
                )

                drawRtlText(
                    canvas = canvas,
                    text = title,
                    x = left + 4,
                    y = y + 8,
                    width = w - 8,
                    textSize = 8.5f,
                    bold = true
                )

                right = left
            }

            y += rowH
        }

        fun drawRow(row: ProductPriceReportRow) {
            val rowH = 58
            ensureSpace(rowH + 4)

            var right = pageW - margin

            val values = listOf(
                "",
                row.name,
                row.model,
                row.brand,
                formatPrice(row.consumerPriceToman),
                formatPrice(row.colleaguePriceToman)
            )

            values.forEachIndexed { index, value ->
                val w = colWidths[index]
                val left = right - w

                canvas.drawRect(
                    left.toFloat(),
                    y.toFloat(),
                    right.toFloat(),
                    (y + rowH).toFloat(),
                    linePaint
                )

                if (index == 0) {
                    val bitmap = decodeBitmap(row.coverUri)
                    if (bitmap != null) {
                        drawBitmapFitCenter(
                            canvas = canvas,
                            bitmap = bitmap,
                            box = android.graphics.Rect(
                                left + 6,
                                y + 6,
                                right - 6,
                                y + rowH - 6
                            )
                        )
                    }
                } else {
                    drawRtlText(
                        canvas = canvas,
                        text = value,
                        x = left + 4,
                        y = y + 8,
                        width = w - 8,
                        textSize = 8f
                    )
                }

                right = left
            }

            y += rowH
        }

        drawTitle()

        val groupedRows =
            rows
                .groupBy { it.categoryName }
                .toSortedMap()

        groupedRows.forEach { (categoryName, categoryRows) ->

            ensureSpace(70)

            y += 10

            y += drawRtlText(
                canvas = canvas,
                text = categoryName,
                x = margin,
                y = y,
                width = contentW,
                textSize = 12f,
                bold = true
            ) + 10

            drawTableHeader()

            categoryRows.forEach { row ->
                if (y + 58 > pageH - margin) {
                    startNewPage()

                    y += drawRtlText(
                        canvas = canvas,
                        text = categoryName,
                        x = margin,
                        y = y,
                        width = contentW,
                        textSize = 12f,
                        bold = true
                    ) + 10

                    drawTableHeader()
                }

                drawRow(row)
            }
        }

        finishPage()

        doc.writeTo(file.outputStream())
        doc.close()

        return file
    }
}