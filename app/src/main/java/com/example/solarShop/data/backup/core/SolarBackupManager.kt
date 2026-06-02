package com.example.solarShop.data.backup.core

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class SolarBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providers: Set<@JvmSuppressWildcards BackupModuleProvider>,
    private val gson: Gson
) {

    suspend fun createBackup(): File = withContext(Dispatchers.IO) {

        val backupFile = File(
            context.cacheDir,
            "solar_backup_${System.currentTimeMillis()}.zip"
        )

        val enabledProviders = providers.toList()

        val manifest = BackupManifest(
            modules = enabledProviders.map { it.moduleName }
        )

        ZipOutputStream(backupFile.outputStream()).use { zip ->

            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(gson.toJson(manifest).toByteArray())
            zip.closeEntry()

            enabledProviders.forEach { provider ->
                val json = provider.exportData()

                zip.putNextEntry(
                    ZipEntry("${provider.moduleName}.json")
                )
                zip.write(json.toByteArray())
                zip.closeEntry()
            }

            val imagesDir = File(context.filesDir, "images")

            if (imagesDir.exists()) {
                imagesDir.listFiles()?.forEach { imageFile ->

                    if (imageFile.isFile) {
                        zip.putNextEntry(
                            ZipEntry("images/${imageFile.name}")
                        )

                        imageFile.inputStream().use { input ->
                            input.copyTo(zip)
                        }

                        zip.closeEntry()
                    }
                }
            }
        }

        backupFile
    }

    suspend fun restoreBackup(
        backupFile: File
    ) = withContext(Dispatchers.IO) {

        java.util.zip.ZipFile(backupFile).use { zip ->

            // 1) خواندن manifest
            val manifestEntry = zip.getEntry("manifest.json")
                ?: error("manifest.json پیدا نشد")

            val manifestJson = zip.getInputStream(manifestEntry)
                .bufferedReader()
                .use { it.readText() }

            val manifest = gson.fromJson(
                manifestJson,
                BackupManifest::class.java
            )

            if (manifest.version > CURRENT_BACKUP_VERSION) {
                error(
                    "این فایل بکاپ با نسخه جدیدتری از اپ ساخته شده و قابل بازیابی نیست."
                )
            }

            // 2) ریستور ماژول‌ها
            val providerMap = providers.associateBy { it.moduleName }

            manifest.modules.forEach { moduleName ->

                val provider = providerMap[moduleName]
                    ?: return@forEach

                val entry = zip.getEntry("$moduleName.json")
                    ?: return@forEach

                val json = zip.getInputStream(entry)
                    .bufferedReader()
                    .use { it.readText() }

                provider.importData(json)
            }

            // 3) کپی images
            val imagesDir = File(context.filesDir, "images").apply {
                mkdirs()
            }

            zip.entries().asSequence()
                .filter { entry ->
                    !entry.isDirectory &&
                            entry.name.startsWith("images/")
                }
                .forEach { entry ->

                    val fileName = entry.name.substringAfter("images/")
                    if (fileName.isBlank()) return@forEach

                    val dest = File(imagesDir, fileName)

                    zip.getInputStream(entry).use { input ->
                        dest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
        }
    }

    suspend fun previewBackup(
        backupFile: File
    ): BackupPreviewInfo = withContext(Dispatchers.IO) {

        java.util.zip.ZipFile(backupFile).use { zip ->

            val manifestEntry = zip.getEntry("manifest.json")
                ?: error("manifest.json پیدا نشد")

            val manifestJson = zip.getInputStream(manifestEntry)
                .bufferedReader()
                .use { it.readText() }

            val manifest = gson.fromJson(
                manifestJson,
                BackupManifest::class.java
            )

            if (manifest.version > CURRENT_BACKUP_VERSION) {
                error("این فایل بکاپ با نسخه جدیدتری از اپ ساخته شده است.")
            }

            val productEntry = zip.getEntry("product.json")

            val productData =
                if (productEntry != null) {
                    val productJson = zip.getInputStream(productEntry)
                        .bufferedReader()
                        .use { it.readText() }

                    gson.fromJson(
                        productJson,
                        com.example.solarShop.data.backup.product.ProductBackupData::class.java
                    )
                } else null

            BackupPreviewInfo(
                version = manifest.version,
                createdAt = manifest.createdAt,
                modules = manifest.modules,

                categoryCount = productData?.categories?.size ?: 0,
                brandCount = productData?.brands?.size ?: 0,
                productCount = productData?.products?.size ?: 0,
                imageCount = productData?.productImages?.size ?: 0
            )
        }
    }

}