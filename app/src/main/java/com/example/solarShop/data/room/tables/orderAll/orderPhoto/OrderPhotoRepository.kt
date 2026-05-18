package com.example.solarShop.data.room.tables.orderAll.orderPhoto

import android.content.Context
import android.net.Uri
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class OrderPhotoRepository @Inject constructor(
    private val dao: OrderPhotoRefDao,
) {
    fun observe(orderId: Int) = dao.observeByOrder(orderId)

    suspend fun addFromGallery(orderId: Int, uri: Uri) {
        val now = System.currentTimeMillis()
        val e = OrderPhotoRefEntity(
            orderId = orderId,
            contentUri = uri.toString(),
            sourceType = PhotoSourceType.GALLERY,
            createdAt = now,
        )
        dao.insert(e)
    }

    suspend fun addFromCamera(orderId: Int, uri: Uri) {
        dao.insert(
            OrderPhotoRefEntity(
                orderId = orderId,
                contentUri = uri.toString(),
                sourceType = PhotoSourceType.CAMERA,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

}



class OrderPhotoMetaRepository @Inject constructor(
    private val dao: OrderPhotoRefDao,
    private val metaDao: OrderPhotoMetaDao
) {

    fun observe(orderId: Int) = dao.observeByOrder(orderId)


    suspend fun removeMany(ids: List<Int>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext

        // اگر پین بوده‌اند، فایل‌ها را پاک کن
        val rows = dao.getLocalCopyPaths(ids)
        rows.forEach { row ->
            val path = row.local_copy_path
            if (!path.isNullOrBlank()) runCatching { File(path).delete() }
        }

        dao.deleteMany(ids)
    }


    suspend fun setPinnedMany(
        ids: List<Int>,
        pinned: Boolean,
        localCopyPathForAll: String? = null
    ) = dao.updatePinMany(ids, pinned, localCopyPathForAll)


    suspend fun pinOneToLocal(
        context: Context,
        item: OrderPhotoRefEntity
    ): String = withContext(Dispatchers.IO) {

        val uri = Uri.parse(item.contentUri)
        val dir = File(context.filesDir, "pinned_photos").apply { mkdirs() }
        val file = File(dir, "p_${item.id}.jpg")

        pinWithZelory(context, uri, file)
    }


    suspend fun unpinOneAndDeleteLocalFile(id: Int) = withContext(Dispatchers.IO) {
        val path = dao.getLocalCopyPath(id)
        if (!path.isNullOrBlank()) runCatching { File(path).delete() }
        dao.updatePin(id, false, null)
    }

    suspend fun unpinManyAndDeleteLocalFiles(ids: List<Int>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext

        val rows = dao.getLocalCopyPaths(ids)
        rows.forEach { row ->
            val path = row.local_copy_path
            if (!path.isNullOrBlank()) runCatching { File(path).delete() }
        }
        dao.updatePinMany(ids, false, null)
    }


    private suspend fun pinWithZelory(
        context: Context,
        contentUri: Uri,
        outFile: File
    ): String = withContext(Dispatchers.IO) {

        // 1) کپی URI به فایل موقت (Compressor با File کار می‌کند)
        val tmpIn = File.createTempFile("pin_in_", ".tmp", context.cacheDir)
        context.contentResolver.openInputStream(contentUri)?.use { input ->
            FileOutputStream(tmpIn).use { output -> input.copyTo(output) }
        } ?: error("Cannot open input stream for $contentUri")

        // 2) کمپرس با default
        val compressed: File = Compressor.compress(context, tmpIn) { default() }

        // 3) انتقال به مسیر نهایی
        outFile.parentFile?.mkdirs()
        compressed.copyTo(outFile, overwrite = true)

        // 4) پاکسازی
        runCatching { tmpIn.delete() }
        runCatching { if (compressed.absolutePath != tmpIn.absolutePath) compressed.delete() }

        outFile.absolutePath
    }


}
