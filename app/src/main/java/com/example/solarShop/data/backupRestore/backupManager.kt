package com.example.solarShop.data.backupRestore

import android.content.Context
import com.example.solarShop.data.local.database.AppDatabase
import com.example.solarShop.data.room.tables.contract.ContractTemplateFullDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val contractTemplateFullDao: ContractTemplateFullDao
) {

    private val kxJson = Json { prettyPrint = true; encodeDefaults = true }

    // * خروجی: ZIP واحد که شامل:
    // *  - data.json                                  (Q/A/R/Images mapping)
    // *  - images/*                                   (فایلهای واقعی عکس)
    // *  - contracts/contracts_backup.json            (قالب قرارداد + طرفین + بخش‌ها)
    // *  - contracts/*                                (اگر فایل فیزیکی قرارداد داری)
    // *
    //  مسیر ذخیره: sdcard/Android/data/<pkg>/files/backups/bambo_backup.zip


    suspend fun exportAllToZip(overwriteSingleFile: Boolean = true): File =
        withContext(Dispatchers.IO) {
            // A) Q/A
            val questions = db.questionDao().getAllQuestions()
            val answers = db.answerDao().getAllAnswers()
            val routes = db.questionDao().getAllAnswerNextQuestions()
            val images = db.answerDao().getAllAnswerImageEntities()

            // B) Contracts (full)
            val fullTemplates = contractTemplateFullDao.getAllFull()
            val contractBundles: List<ContractTemplateBundleDTO> = fullTemplates.map { full ->
                ContractTemplateBundleDTO(
                    template = ContractTemplateDTO(
                        id = full.template.id,
                        userKey = full.template.userKey,
                        title = full.template.title,
                        description = full.template.description,
                        createdAt = full.template.createdAt,
                        updatedAt = full.template.updatedAt
                    ),
                    parties = full.parties.map { p ->
                        ContractTemplatePartyDTO(
                            id = p.id,
                            templateId = p.templateId,
                            role = p.role,
                            fullName = p.fullName,
                            fatherFullName=p.fatherFullName,
                            nationalId = p.nationalId,
                            companyName = p.companyName,
                            address = p.address,
                            phone = p.phone,
                            createdAt=p.createdAt,
                            updatedAt=p.updatedAt,

                            )
                    },
                    sections = full.sectionsWithNotes.map { swn ->
                        ContractTemplateSectionDTO(
                            id = swn.section.id,
                            templateId = swn.section.templateId,
                            orderNo =swn.section.orderNo,
                            title =swn.section.title,
                            body =swn.section.body,
                            isDefaultVisible =swn.section.isDefaultVisible,
                            isRequired =swn.section.isRequired,
                            createdAt=swn.section.createdAt,
                            updatedAt=swn.section.updatedAt,
                        )
                    },
                    notes = full.sectionsWithNotes.flatMap  { swn ->
                        swn.notes.map { n ->
                            ContractTemplateNoteDTO(
                                id = n.id,
                                sectionId = n.sectionId,                 // ✅ FK به همان سکشن
                                orderNo = n.orderNo,
                                title = n.title,                  // ممکن است null باشد
                                body = n.body,
                                createdAt = n.createdAt,
                                updatedAt = n.updatedAt
                            )
                        }
                    }
                )
            }

            // C) ساخت BackupDTO یکپارچه
            val backup = BackupDTO(
                version = 1,
                questions = questions.map { QuestionDTO(it.id, it.title,it.userKey) },
                answers = answers.map { AnswerDTO(it.id, it.questionId, it.title) },
                routes = routes.map { RouteDTO(it.answerId, it.nextQuestionId) },
                answerImages = images.map { AnswerImageDTO(it.answerId, it.fileName) },
                contractTemplates = contractBundles
            )
            val jsonStr = kxJson.encodeToString(backup)

            // D) مسیر خروجی
            val outDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
            val outZip = if (overwriteSingleFile) File(outDir, "bambo_backup.zip")
            else File(outDir, "bambo_backup_${System.currentTimeMillis()}.zip")

            // E) نوشتن ZIP: فقط یک data.json + پوشه‌های فایل‌ها
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outZip))).use { zos ->
                // E-1) data.json
                zos.putNextEntry(ZipEntry("data.json"))
                zos.write(jsonStr.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // E-2) images/
                val imagesDir = File(context.filesDir, "images")
                imagesDir.listFiles()?.filter { it.isFile }?.forEach { f ->
                    zos.putNextEntry(ZipEntry("images/${f.name}"))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                // (اختیاری) اگر فایل فیزیکی قرارداد هم داری:
                val contractsDir = File(context.filesDir, "contracts")
                contractsDir.listFiles()?.filter { it.isFile }?.forEach { f ->
                    zos.putNextEntry(ZipEntry("contracts/${f.name}"))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            outZip
        }
}