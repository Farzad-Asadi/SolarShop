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
}