package com.example.solarShop.data.backupRestore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseAttachmentEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostRepository
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderExpenseEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderReceiptEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ReceiptAttachmentEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AttachmentController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: OrderCostRepository
) {

    // -------- Public API --------

    /** افزودن تصویر پیوست برای یک دریافتی */
    suspend fun addReceiptImage(
        orderId: Int,
        receiptId: Int,
        source: Uri
    ): Unit = withContext(Dispatchers.IO) {
        val dir = receiptsDir(orderId, receiptId).apply { mkdirs() }
        val res = processAndSave(source, dir)
        val entity = ReceiptAttachmentEntity(
            receiptId = receiptId,
            fileName = toRelativePath(res.file),
            thumbName = res.thumb?.let { toRelativePath(it) },
            mimeType = "image/jpeg",
            sizeBytes = res.sizeBytes,
            width = res.size.width,
            height = res.size.height,
            sha256 = res.sha256,
            createdEpoch = System.currentTimeMillis()
        )
        repo.addReceiptAttachment(entity)
    }

    /** افزودن تصویر پیوست برای یک هزینه */
    suspend fun addExpenseImage(
        orderId: Int,
        expenseId: Int,
        source: Uri
    ): Unit = withContext(Dispatchers.IO) {
        val dir = expensesDir(orderId, expenseId).apply { mkdirs() }
        val res = processAndSave(source, dir)
        val entity = ExpenseAttachmentEntity(
            expenseId = expenseId,
            fileName = toRelativePath(res.file),
            thumbName = res.thumb?.let { toRelativePath(it) },
            mimeType = "image/jpeg",
            sizeBytes = res.sizeBytes,
            width = res.size.width,
            height = res.size.height,
            sha256 = res.sha256,
            createdEpoch = System.currentTimeMillis()
        )
        repo.addExpenseAttachment(entity)
    }

    /** حذف پیوست دریافتی (فایل‌ها + DB) */
    suspend fun deleteReceiptAttachment(entity: ReceiptAttachmentEntity): Unit =
        withContext(Dispatchers.IO) {
            deletePhysicalFiles(entity.fileName, entity.thumbName)
            repo.deleteReceiptAttachment(entity)
        }

    /** حذف پیوست هزینه (فایل‌ها + DB) */
    suspend fun deleteExpenseAttachment(entity: ExpenseAttachmentEntity): Unit =
        withContext(Dispatchers.IO) {
            deletePhysicalFiles(entity.fileName, entity.thumbName)
            repo.deleteExpenseAttachment(entity)
        }

    suspend fun deleteReceiptWithAttachments(receipt: OrderReceiptEntity): Unit =
        withContext(Dispatchers.IO) {
            // 1) همه پیوست‌ها را از DB بگیر
            val attachments = repo.receiptAttachmentsOnce(receipt.id)  // ← مرحله ۲-ب
            // 2) فایل‌های فیزیکی را پاک کن + رکورد پیوست را حذف کن
            attachments.forEach { att ->
                deletePhysicalFiles(att.fileName, att.thumbName)
                repo.deleteReceiptAttachment(att)
            }
            // 3) خود رسید را پاک کن
            repo.deleteReceipt(receipt)
        }

    suspend fun deleteExpenseWithAttachments(expense: OrderExpenseEntity): Unit =
        withContext(Dispatchers.IO) {
            val attachments = repo.expenseAttachmentsOnce(expense.id)  // ← مرحله ۲-ب
            attachments.forEach { att ->
                deletePhysicalFiles(att.fileName, att.thumbName)
                repo.deleteExpenseAttachment(att)
            }
            repo.deleteExpense(expense)
        }



    // -------- Internal impl --------

    private data class Dim(val width: Int, val height: Int)
    private data class SaveResult(
        val file: File,
        val thumb: File?,
        val sizeBytes: Long,
        val size: Dim,
        val sha256: String
    )

    private val dateFmt = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)

    private val maxSide = 2000           // px
    private val quality = 85             // %
    private val thumbSide = 320          // px
    private val thumbQuality = 70        // %

    private fun mediaRoot(): File = File(context.filesDir, "media")
    private fun receiptsDir(orderId: Int, receiptId: Int) =
        File(mediaRoot(), "orders/$orderId/receipts/$receiptId")
    private fun expensesDir(orderId: Int, expenseId: Int) =
        File(mediaRoot(), "orders/$orderId/expenses/$expenseId")


    private fun toRelativePath(file: File): String =
        file.relativeTo(mediaRoot()).invariantSeparatorsPath

    private suspend fun deletePhysicalFiles(mainRel: String?, thumbRel: String?) {
        if (mainRel != null) File(mediaRoot(), mainRel).delete()
        if (thumbRel != null) File(mediaRoot(), thumbRel).delete()
    }


    private suspend fun processAndSave(
        source: Uri,
        targetDir: File
    ): SaveResult = withContext(Dispatchers.IO) {

        // 1) کپی ورودی Uri به فایل موقت
        val tmpIn = File.createTempFile("in_", ".tmp", context.cacheDir)
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(tmpIn).use { output -> input.copyTo(output) }
        } ?: throw IOException("Cannot open input stream for $source")

        // 2) sha256 روی فایل ورودی (مثل قبل)
        val sha256 = run {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(tmpIn).use { fis ->
                DigestInputStream(fis, md).use { dis -> dis.copyTo(FileOutputStream(File.createTempFile("hash_", ".tmp", context.cacheDir))) }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }

        // 3) کمپرس با Zelory (default constraints)
        // نکته: خروجی معمولاً یک فایل جدید در cache برمی‌گردونه
        val compressedFile: File = Compressor.compress(context, tmpIn) {
            default() // 👈 همون چیزی که گفتی: خودش تصمیم بگیره
        }

        // 4) اسم و مسیر خروجی نهایی در targetDir
        val stamp = dateFmt.format(Date())
        val baseName = "${stamp}_${sha256.take(8)}"
        val outFile = File(targetDir, "$baseName.jpg")

        // 5) انتقال فایل کمپرس شده به مسیر نهایی
        compressedFile.copyTo(outFile, overwrite = true)

        // 6) ابعاد واقعی خروجی
        val (w, h) = decodeBounds(outFile)

        // 7) thumbnail مثل قبل (با bitmap خروجی)
        val fullBmp = BitmapFactory.decodeFile(outFile.absolutePath) ?: throw IOException("Decode failed")
        val thumbBmp = createSquareThumb(fullBmp, thumbSide)
        val thumbFile = File(targetDir, "${baseName}_thumb.jpg")
        FileOutputStream(thumbFile).use { fos ->
            thumbBmp.compress(Bitmap.CompressFormat.JPEG, thumbQuality, fos)
        }
        fullBmp.recycle()
        thumbBmp.recycle()

        // پاکسازی
        runCatching { tmpIn.delete() }
        // compressedFile ممکنه همون tmpIn نباشه
        runCatching { if (compressedFile.absolutePath != tmpIn.absolutePath) compressedFile.delete() }

        SaveResult(
            file = outFile,
            thumb = thumbFile,
            sizeBytes = outFile.length(),
            size = Dim(w, h),
            sha256 = sha256
        )
    }

    // کمکی برای خواندن bounds بدون decode کامل
    private fun decodeBounds(file: File): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return opts.outWidth to opts.outHeight
    }


    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun exifRotationDegrees(exif: ExifInterface): Int = when (exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private fun rotateBitmap(bmp: Bitmap, degrees: Int): Bitmap {
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun scaleDownIfNeeded(bmp: Bitmap, maxSide: Int): Bitmap {
        val w = bmp.width; val h = bmp.height
        val maxCurrent = maxOf(w, h)
        if (maxCurrent <= maxSide) return bmp
        val ratio = maxSide.toFloat() / maxCurrent.toFloat()
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bmp, nw, nh, true)
    }

    /** inSampleSize تقریبی برای بارگذاری */
    private fun computeInSampleSize(w: Int, h: Int, maxSide: Int): Int {
        var sample = 1
        var maxCur = maxOf(w, h)
        while (maxCur / (sample * 2) >= maxSide) sample *= 2
        return sample.coerceAtLeast(1)
    }

    /** thumbnail مربعی (letterbox حداقل برش) */
    private fun createSquareThumb(src: Bitmap, side: Int): Bitmap {
        val scale = side / minOf(src.width, src.height).toFloat()
        val w = (src.width * scale).toInt()
        val h = (src.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        val x = (w - side) / 2
        val y = (h - side) / 2
        val out = Bitmap.createBitmap(scaled, x.coerceAtLeast(0), y.coerceAtLeast(0),
            side.coerceAtMost(scaled.width - x), side.coerceAtMost(scaled.height - y))
        if (scaled !== src) scaled.recycle()
        return out
    }

    suspend fun deletePhysicalOnly(mainRel: String?, thumbRel: String?) {
        deletePhysicalFiles(mainRel, thumbRel)
    }


    fun cleanupEmptyDirsForOrder(orderId: Int) {
        val orderDir = File(mediaRoot(), "orders/$orderId")
        deleteEmptyDirsRecursively(orderDir)

        // اگر خود orders هم خالی شد و دوست داشتی پاکش کنی:
        val ordersRoot = File(mediaRoot(), "orders")
        deleteEmptyDirsRecursively(ordersRoot)
    }

    private fun deleteEmptyDirsRecursively(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return

        // اول بچه‌ها
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) deleteEmptyDirsRecursively(child)
        }

        // بعد خود dir اگر خالی شد
        val files = dir.listFiles()
        if (files == null || files.isEmpty()) {
            runCatching { dir.delete() }
        }
    }


}
