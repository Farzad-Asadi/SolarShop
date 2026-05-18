package com.example.solarShop.data.backupRestore

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import com.example.solarShop.data.room.appDatabase.AppDatabase
import com.example.solarShop.data.room.tables.contract.ContractTemplateEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionEntity
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerImageEntity
import com.example.solarShop.data.room.tables.question_answers.question.AnswerNextQuestionCrossRef
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

class RestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }


    @WorkerThread
    suspend fun restoreFromZip(zipInputStream: InputStream): Int = withContext(Dispatchers.IO) {
// === [1] استخراج کل ZIP در پوشه‌ی موقت
        val tempDir =
            File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
        var dataJsonFile: File? = null
        var imagesCopied = 0
        var contractsCopied = 0

        ZipInputStream(zipInputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(tempDir, entry.name).apply { parentFile?.mkdirs() }
                    outFile.outputStream().use { out -> zis.copyTo(out) }
                    if (entry.name.equals("data.json", ignoreCase = true)) {
                        dataJsonFile = outFile
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

// === [2] data.json
        val dataFile = requireNotNull(dataJsonFile) { "data.json not found in zip" }
        val backup: BackupDTO = json.decodeFromString(dataFile.readText())

// === [3] اعتبار نسخه بک‌آپ
        if (backup.version !in listOf(1)) {
            throw IllegalArgumentException("Unsupported backup version: ${backup.version}")
        }

// === [4] آماده‌سازی کپی تصاویر (کل فایل‌های استخراج‌شده را ایندکس کن)
        val extractedFiles = tempDir.walkTopDown().filter { it.isFile }.toList()
        val byName: Map<String, File> = extractedFiles.associateBy { it.name }

// پوشه‌ی مقصد تصاویر داخل حافظه‌ی داخلی اپ
        val destImagesDir = File(context.filesDir, "images").apply { mkdirs() }

// نگاشت "نام اصلی در data.json" → "نام نهایی روی دیسک (با پسوند)"
        val imageNameMap = mutableMapOf<String, String>()

// تنها فایل‌هایی را کپی می‌کنیم که در data.json ذکر شده‌اند
        backup.answerImages.forEach { dto ->
            val original = dto.fileName

// اول با اسم کامل، بعد با nameWithoutExtension جستجو
            val src = byName[original]
                ?: byName.values.firstOrNull { it.nameWithoutExtension == original }

            if (src == null) {
                Log.w("RestoreManager", "Missing image in zip: $original")
                return@forEach
            }

            val finalName = ensureNameWithExt(src)
            val dst = File(destImagesDir, finalName)

            src.inputStream().use { input ->
                dst.outputStream().use { output -> input.copyTo(output) }
            }
            imagesCopied++
            imageNameMap[original] = finalName
        }

        Log.d("RestoreManager", "Images copied=$imagesCopied, mapped=${imageNameMap.size}")

// === [5] نگاشت DTO → Entity ها (سؤال/پاسخ/مسیرها/تصاویر)
        val questions: List<QuestionEntity> = backup.questions.map {
            QuestionEntity(
                id = it.id ?: 0,
                title = it.title,
                userKey = it.userKey
            )
        }

        val answers: List<AnswerEntity> = backup.answers.map {
            AnswerEntity(
                id = it.id ?: 0,
                questionId = it.questionId,
                title = it.title
            )
        }

        val routes: List<AnswerNextQuestionCrossRef> = backup.routes.map {
            AnswerNextQuestionCrossRef(
                answerId = it.answerId,
                nextQuestionId = it.nextQuestionId
            )
        }

// ⚠️ اینجاست که نام فایل تصویر را با «نام نهاییِ کپی‌شده» همسان می‌کنیم
        val answerImages: List<AnswerImageEntity> = backup.answerImages.map {
            val finalName = imageNameMap[it.fileName] ?: it.fileName
            AnswerImageEntity(
                answerId = it.answerId,
                fileName = finalName
            )
        }

// === [6] نگاشت قراردادها: ContractTemplateBundleDTO → جداول
        val contractTemplates: List<ContractTemplateEntity> =
            backup.contractTemplates.map { b ->
                ContractTemplateEntity(
                    id = b.template.id ?: 0,
                    userKey = b.template.userKey,
                    title = b.template.title,
                    description = b.template.description,
                    isProtected = b.template.isProtected ?: false,
                    createdAt = b.template.createdAt,
                    updatedAt = b.template.updatedAt
                )
            }

        val contractParties: List<ContractTemplatePartyEntity> =
            backup.contractTemplates.flatMap { b ->
                b.parties.map { p ->
                    ContractTemplatePartyEntity(
                        id = p.id ?: 0,
                        templateId = p.templateId,
                        role = p.role,
                        fullName = p.fullName,
                        fatherFullName = p.fatherFullName,
                        nationalId = p.nationalId,
                        companyName = p.companyName,
                        address = p.address,
                        phone = p.phone,
                        createdAt = p.createdAt,
                        updatedAt = p.updatedAt
                    )
                }
            }

        val contractSections: List<ContractTemplateSectionEntity> =
            backup.contractTemplates.flatMap { b ->
                b.sections.map { s ->
                    ContractTemplateSectionEntity(
                        id = s.id ?: 0,
                        templateId = s.templateId,
                        orderNo = s.orderNo,
                        title = s.title,
                        body = s.body,
                        isDefaultVisible = s.isDefaultVisible,
                        isRequired = s.isRequired
                    )
                }
            }

        val contractNotes: List<ContractTemplateNoteEntity> =
            backup.contractTemplates.flatMap { b ->
                b.notes.map { n ->
                    ContractTemplateNoteEntity(
                        id = n.id ?: 0,
                        sectionId = n.sectionId,
                        orderNo = n.orderNo,
                        title = n.title,
                        body = n.body,
                        createdAt = n.createdAt,
                        updatedAt = n.updatedAt
                    )
                }
            }

// === [7] درج دیتابیس در یک تراکنش (پاک‌سازی → درج)
        db.withTransaction {
// Q/A
            db.questionDao().clearRoutes()
            db.answerDao().clearAnswerImages()
            db.answerDao().clearAnswers()
            db.questionDao().clearQuestions()

            db.questionDao().insertQuestions(questions)
            db.answerDao().insertAnswers(answers)
            db.questionDao().insertRoutes(routes)
            db.answerDao().insertAnswerImages(answerImages)

// Contracts: اول وابسته‌ها پاک می‌شن، بعد Templateها
            db.contractTemplateNoteDao().clearNotes()
            db.contractTemplateSectionDao().clearSections()
            db.contractTemplatePartyDao().clearParties()
            db.contractTemplateDao().clearTemplates()

            db.contractTemplateDao().insertTemplates(contractTemplates)
            db.contractTemplatePartyDao().insertParties(contractParties)
            db.contractTemplateSectionDao().insertSections(contractSections)
            db.contractTemplateNoteDao().insertAll(contractNotes)
        }

// === [8] کپی فایل‌های قرارداد به files/contracts (اختیاری ولی مثل قبل)
        val destContractsDir = File(context.filesDir, "contracts").apply { mkdirs() }
        val srcContractsDir = File(tempDir, "contracts")
        if (srcContractsDir.exists()) {
            srcContractsDir.walkTopDown().filter { it.isFile }.forEach { src ->
                val dst = File(destContractsDir, src.name)
                src.inputStream().use { input ->
                    dst.outputStream().use { output -> input.copyTo(output) }
                }
                contractsCopied++
            }
        }
        Log.d("RestoreManager", "Contracts copied=$contractsCopied")

// === [9] پاکسازی پوشه‌ی موقت
        tempDir.deleteRecursively()

        imagesCopied
    }

    /**
     * بازگردانی از فایل انتخاب‌شده‌ی کاربر (SAF)
     */
    suspend fun restoreFromUri(uri: Uri): Int = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { ins ->
            restoreFromZip(ins)
        } ?: error("Cannot open selected file")
    }

// --- Helpers ---

    /**
     * اگر فایل پسوند ندارد، با خواندن سرآیند (header) نوع را حدس می‌زنیم
     * و یکی از jpg/png/webp را اضافه می‌کنیم. اگر نام پسوند دارد، همان را برمی‌گردانیم.
     */
    private fun ensureNameWithExt(src: File): String {
        if (src.name.contains('.')) return src.name
        return try {
            src.inputStream().use { ins ->
                val head = ByteArray(12)
                val n = ins.read(head)
                val isJpeg = n >= 3 && head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte()
                val isPng = n >= 8 && head[0] == 0x89.toByte() &&
                        head[1] == 0x50.toByte() && head[2] == 0x4E.toByte() &&
                        head[3] == 0x47.toByte() && head[4] == 0x0D.toByte() &&
                        head[5] == 0x0A.toByte() && head[6] == 0x1A.toByte() &&
                        head[7] == 0x0A.toByte()
                val isWebp = n >= 12 &&
                        String(head, 0, 4) == "RIFF" &&
                        String(head, 8, 4) == "WEBP"

                when {
                    isJpeg -> src.name + ".jpg"
                    isPng -> src.name + ".png"
                    isWebp -> src.name + ".webp"
                    else -> src.name + ".jpg" // پیش‌فرض امن
                }
            }
        } catch (_: Exception) {
            src.name + ".jpg"
        }
    }
}





