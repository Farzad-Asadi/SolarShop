package com.example.solarShop.data.backupRestore.v2

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

// ============================================================================
// مرحلهٔ ۵ — Orchestrator (BackupManagerV2 / RestoreManagerV2)
// فقط کدهای این مرحله. مراحل قبل را تکرار نمی‌کنیم.
// ----------------------------------------------------------------------------
// چه می‌کند؟
// - export(): از Providerهای انتخاب‌شده snapshot می‌گیرد، manifest می‌سازد و ZIP می‌نویسد.
// - previewManifest(): فقط manifest.json را از ZIP می‌خواند (برای UI پیش‌نمایش).
// - restoreSelected(): ZIP را موقتاً extract می‌کند، snapshot هر دسته را می‌سازد و به Provider مربوطه می‌دهد.
// نکات طراحی:
// - مسیرها طبق ZipLayout مرحلهٔ ۱ است.
// - counts در manifest فعلاً خالی می‌ماند؛ اگر خواستی، بعداً hook شمارش اضافه می‌کنیم.
// ============================================================================


class BackupManagerV2 @Inject constructor(
    private val providers: Map<BackupCategory,@JvmSuppressWildcards BackupProvider> // تزریق از بیرون
) {

    /**
     * ساخت ZIP روی outFile با توجه به برنامهٔ انتخاب کاربر (plan) و کانتکست
     */
    suspend fun export(plan: BackupPlan, ctx: BackupContext, outFile: File): File = withContext(
        Dispatchers.IO) {
        outFile.parentFile?.mkdirs()


        // 1) snapshot گرفتن از هر Provider انتخاب‌شده
        val selectedProviders = plan.categories.mapNotNull { providers[it] }
        val snapshots = selectedProviders.map { it.snapshot(ctx) }


        // 2) ساخت manifest
        val manifest = Manifest(
            version = 1,
            createdAt = System.currentTimeMillis(),
            sourceUserKey = ctx.currentUserKey,
            categories = snapshots.map { it.category }
        // counts: بعداً می‌توانیم با hook پر کنیم
        )
        val manifestJson = ctx.json.encodeToString(Manifest.serializer(), manifest)


        // 3) نوشتن ZIP
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
        // 3-1) manifest.json
            zos.putNextEntry(ZipEntry(ZipLayout.ROOT_MANIFEST))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()


             // 3-2) محتویات هر snapshot (data.json + extraFiles)
            snapshots.forEach { snap ->
                // data.json
                val dataPath = snap.zipFolder.trimEnd('/') + "/" + snap.jsonFileName
                zos.putNextEntry(ZipEntry(dataPath))
                zos.write(snap.jsonPayload.toByteArray(Charsets.UTF_8))
                zos.closeEntry()


                // فایل‌های جانبی
                snap.extraFiles.forEach { ref ->
                    val entryPath = snap.zipFolder.trimEnd('/') + "/" + ref.zipPath.trimStart('/')
                    zos.putNextEntry(ZipEntry(entryPath))
                    ref.file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        outFile
    }
}

class RestoreManagerV2 @Inject constructor(
    private val providers: Map<BackupCategory,@JvmSuppressWildcards BackupProvider>
) {
    /** فقط manifest.json را می‌خواند — برای نمایش پیش‌نمایش در UI */
    suspend fun previewManifest(zipFile: File, ctx: RestoreContext): Manifest = withContext(Dispatchers.IO) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == ZipLayout.ROOT_MANIFEST) {
                    val text = zis.readBytes().toString(Charsets.UTF_8)
                    return@withContext ctx.json.decodeFromString(Manifest.serializer(), text)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        error("manifest.json not found")
    }
            /**
             * ریستور تفکیکی: ZIP را به temp extract می‌کنیم، برای هر دسته ProviderSnapshot می‌سازیم
             * و به Provider مربوطه می‌دهیم. گزارش‌ها را برمی‌گرداند.
             */
            suspend fun restoreSelected(zipFile: File, options: RestoreOptions, ctx: RestoreContext): List<RestoreReport> = withContext(Dispatchers.IO) {
                val tempDir = File(ctx.appContext.cacheDir, "restore_v2_" + System.currentTimeMillis()).apply { mkdirs() }
                try {
                    // 1) استخراج کامل ZIP به temp
                    extractAll(zipFile, tempDir)


                    // 2) برای هر دستهٔ انتخاب‌شده، snapshot لازم را از روی فایل‌های temp بساز
                    val reports = mutableListOf<RestoreReport>()
                    options.selected.forEach { cat ->
                        val provider = providers[cat] ?: return@forEach
                        val snap = buildSnapshotForCategory(cat, tempDir)
                        val rep = ctx.db.withTransaction {
                            provider.restore(snap, options, ctx)
                        }

                        reports.add(rep)
                    }
                    reports
                } finally {
                    // 3) پاکسازی temp
                    tempDir.deleteRecursively()
                }
            }

            // ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ
            // Helpers
            // ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ

    private fun extractAll(zipFile: File, destDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = File(destDir, entry.name).apply { parentFile?.mkdirs() }
                    FileOutputStream(out).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    private fun buildSnapshotForCategory(category: BackupCategory, root: File): ProviderSnapshot {
        return when (category) {
            BackupCategory.QNA -> {
                val folder = File(root, ZipLayout.Qna.FOLDER)
                val data = File(folder, ZipLayout.Qna.DATA).readText()
                val imagesDir = File(folder, ZipLayout.Qna.IMAGES)
                val files = imagesDir.listFiles()?.filter { it.isFile }?.map { f ->
                    FileRef(f, zipPath = ZipLayout.Qna.IMAGES + f.name)
                } ?: emptyList()
                ProviderSnapshot(category, ZipLayout.Qna.FOLDER, ZipLayout.Qna.DATA, data, files)
            }
            BackupCategory.CONTRACTS -> {
                val folder = File(root, ZipLayout.Contracts.FOLDER)
                val data = File(folder, ZipLayout.Contracts.DATA).readText()
                val filesDir = File(folder, ZipLayout.Contracts.FILES)
                val files = filesDir.listFiles()?.filter { it.isFile }?.map { f ->
                    FileRef(f, zipPath = ZipLayout.Contracts.FILES + f.name)
                } ?: emptyList()
                ProviderSnapshot(category, ZipLayout.Contracts.FOLDER, ZipLayout.Contracts.DATA, data, files)
            }
            BackupCategory.CUSTOMERS_ACTIVE -> {
                val folder = File(root, ZipLayout.Customers.Active.FOLDER)
                val data = File(folder, ZipLayout.Customers.Active.DATA).readText()
                ProviderSnapshot(category, ZipLayout.Customers.Active.FOLDER, ZipLayout.Customers.Active.DATA, data, emptyList())
            }
            BackupCategory.CUSTOMERS_ARCHIVED -> {
                val folder = File(root, ZipLayout.Customers.Archived.FOLDER)
                val data = File(folder, ZipLayout.Customers.Archived.DATA).readText()
                ProviderSnapshot(category, ZipLayout.Customers.Archived.FOLDER, ZipLayout.Customers.Archived.DATA, data, emptyList())
            }
        }
    }
}