package com.example.solarShop.repo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ImageRepository @Inject constructor(
    @ApplicationContext private val app: Context
) {
    private fun imagesDir(): File = File(app.filesDir, "images").apply { mkdirs() }
    private fun cameraTmpDir(): File = File(app.cacheDir, "camera_tmp").apply { mkdirs() }

    /** Uri برای گرفتن عکس با دوربین (TakePicture) */
    fun createCameraTempUri(): Pair<File, Uri> {
        val tmpFile = File(cameraTmpDir(), "cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", tmpFile)
        return tmpFile to uri
    }

    /** بعد از TakePicture موفق: فایل temp را با Zelory کمپرس کن و در images/ ذخیره کن */
    suspend fun compressCameraTempToInternal(
        tempFile: File,
        targetName: String = "img_${System.currentTimeMillis()}.jpg"
    ): Pair<File, Uri> = withContext(Dispatchers.IO) {

        val compressed = Compressor.compress(app, tempFile) { default() }

        val dest = File(imagesDir(), targetName)
        compressed.copyTo(dest, overwrite = true)

        runCatching { tempFile.delete() }
        runCatching { compressed.delete() }

        val contentUri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", dest)
        dest to contentUri
    }


    suspend fun saveCompressedToInternal(
        src: Uri,
        targetName: String = "img_${System.currentTimeMillis()}.jpg",
    ): Pair<File, Uri> = withContext(Dispatchers.IO) {

        // 1) اول uri را داخل cache به یک فایل واقعی تبدیل کن (Compressor با File کار می‌کند)
        val inputFile = copyUriToCacheFile(src)

        // 2) کمپرس با دیفالت خود کتابخانه (بدون دستکاری)
        val compressed: File = Compressor.compress(app, inputFile) {
            default()
        }

        // 3) انتقال خروجی به مسیر نهایی images/
        val dest = File(imagesDir(), targetName)
        compressed.copyTo(dest, overwrite = true)

        // تمیزکاری
        runCatching { inputFile.delete() }
        runCatching { compressed.delete() } // خروجی موقت کمپرسور

        val contentUri = FileProvider.getUriForFile(
            app, "${app.packageName}.fileprovider", dest
        )
        dest to contentUri
    }


    fun deleteInternalImage(fileName: String): Boolean {
        return runCatching { File(imagesDir(), fileName).delete() }.getOrDefault(false)
    }

    private fun copyUriToCacheFile(src: Uri): File {
        val tmp = File.createTempFile("src_", ".tmp", app.cacheDir)
        app.contentResolver.openInputStream(src).use { ins ->
            requireNotNull(ins) { "Cannot open src stream" }
            tmp.outputStream().use { outs -> ins.copyTo(outs) }
        }
        return tmp
    }
}
