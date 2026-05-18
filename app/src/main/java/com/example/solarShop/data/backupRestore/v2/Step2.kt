package com.example.solarShop.data.backupRestore.v2


// مرحلهٔ ۲ — QnaBackupProvider (استخراج از کد فعلی و هماهنگ با V2)
// فایل: qna/QnaBackupProvider.kt
// ----------------------------------------------------------------------------
// نکته‌ها:
//  - snapshot(): خروجی JSON + جمع‌آوری فایل‌های تصویر پاسخ‌ها در مسیر qna/images/*
//  - restore(): فعلاً سیاست Overwrite را پیاده‌سازی می‌کند (پاک‌سازی و درج مجدد)
//  - در مرحلهٔ ۴، سیاست‌های Skip/Merge را اضافه می‌کنیم
//  - فرض: orchestrator فایل ZIP را در temp استخراج می‌کند و extraFiles را به Provider می‌دهد
// ============================================================================


import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerImageEntity
import com.example.solarShop.data.room.tables.question_answers.question.AnswerNextQuestionCrossRef
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject

/* -------------------------------
 * DTO های مستقل QNA (فقط مخصوص qna/data.json)
 * - عمداً از DTO های قدیمی جداست تا V2 ایزوله و قابل مهاجرت باشد.
 * ------------------------------- */
@Serializable
data class QnaDTO(
    val version: Int = 1,
    val questions: List<QnaQuestionDTO>,
    val answers: List<QnaAnswerDTO>,
    val routes: List<QnaRouteDTO>,
    val answerImages: List<QnaAnswerImageDTO>
)

@Serializable
data class QnaQuestionDTO(
    val id: Int?,
//    val userKey: String? = null,
    val title: String,
    val isHidden: Boolean = false,
    val isRoot: Boolean = false,
    val createdAt: Long = 0L
)

@Serializable
data class QnaAnswerDTO(
    val id: Int?,
    val questionId: Int,
    val title: String,
    val note: String = "",
    val isLiked: Boolean = false,
    val isHidden: Boolean = false,
    val createdAt: Long = 0L,
    val sortOrder: Int = 0
)

@Serializable
data class QnaRouteDTO(
    val answerId: Int,
    val nextQuestionId: Int
)

@Serializable
data class QnaAnswerImageDTO(
//    val id: Int? = null,
    val answerId: Int,
    val fileName: String,
    val createdAt: Long = 0L,
    val sortOrder: Int = 0
)

/* -------------------------------
 * QnaBackupProvider — پیاده‌سازی Provider برای دستهٔ QNA
 * ------------------------------- */
class QnaBackupProvider @Inject constructor() : BackupProvider {
    override val category: BackupCategory = BackupCategory.QNA

    override suspend fun snapshot(ctx: BackupContext): ProviderSnapshot {
        // 1) خواندن داده‌ها از DB — همانی که قبلاً در exportAllToZip استفاده می‌کردی
        val questions = ctx.db.questionDao().getAllQuestions()
        val answers = ctx.db.answerDao().getAllAnswers()
        val routes = ctx.db.questionDao().getAllAnswerNextQuestions()
        val images = ctx.db.answerDao().getAllAnswerImageEntities()

        // 2) نگاشت به DTO های V2 (واضح و Stable)
        val dto = QnaDTO(
            version = 1,
            questions = questions.map { it.toDto() },
            answers = answers.map { it.toDto() },
            routes = routes.map { it.toDto() },
            answerImages = images.map { it.toDto() }
        )

        // 3) سریالایز JSON
        val jsonStr = ctx.json.encodeToString(QnaDTO.serializer(), dto)

        // 4) جمع‌آوری فایل‌های تصویر پاسخ‌ها برای قرار گرفتن در ZIP زیر qna/images/*
        val imagesRoot = File(ctx.appContext.filesDir, "images")
        val fileRefs: List<FileRef> = images.mapNotNull { ai ->
            val f = File(imagesRoot, ai.fileName)
            if (f.exists()) FileRef(f, zipPath = ZipLayout.Qna.IMAGES + f.name) else null
        }

        // 5) ساخت ProviderSnapshot
        return ProviderSnapshot(
            category = category,
            zipFolder = ZipLayout.Qna.FOLDER,
            jsonFileName = ZipLayout.Qna.DATA,
            jsonPayload = jsonStr,
            extraFiles = fileRefs
        )
    }

    override suspend fun restore(
        snapshot: ProviderSnapshot,
        options: RestoreOptions,
        ctx: RestoreContext
    ): RestoreReport {
        // ایمنی: فقط وقتی این Provider انتخاب شده که دستهٔ آن در options آمده باشد
        if (!options.selected.contains(category)) {
            return RestoreReport(category, inserted = 0, updated = 0, skipped = 0)
        }

        // 1) پارس JSON ماژول QNA
        val data = ctx.json.decodeFromString(QnaDTO.serializer(), snapshot.jsonPayload)

        // 2) سیاست فعلی: Overwrite (ساده و شفاف)
        // در مرحلهٔ ۴، Skip/Merge را اضافه می‌کنیم.
        when (options.conflictPolicy) {
            ConflictPolicy.Overwrite -> {
                ctx.db.questionDao().clearRoutes()
                ctx.db.answerDao().clearAnswerImages()
                ctx.db.answerDao().clearAnswers()
                ctx.db.questionDao().clearQuestions()
            }
            ConflictPolicy.Skip, ConflictPolicy.Merge -> {
                // TODO(step4): پیاده‌سازی Upsert هوشمند
                // فعلاً مانند Overwrite عمل می‌کنیم تا رفتار پیش‌بینی‌پذیر باشد
                ctx.db.questionDao().clearRoutes()
                ctx.db.answerDao().clearAnswerImages()
                ctx.db.answerDao().clearAnswers()
                ctx.db.questionDao().clearQuestions()
            }
        }

        // 3) درج مجدد رکوردها (Int IDs)
        val questions: List<QuestionEntity> = data.questions.map { it.toEntity() }
        val answers: List<AnswerEntity>   = data.answers.map { it.toEntity() }
        val routes: List<AnswerNextQuestionCrossRef> = data.routes.map { it.toEntity() }

        ctx.db.questionDao().insertQuestions(questions)
        ctx.db.answerDao().insertAnswers(answers)
        ctx.db.questionDao().insertRoutes(routes)

        // 4) کپی تصاویر کنار اپ: محتویات extraFiles باید توسط orchestrator از temp به ما داده شود
        val destImagesDir = File(ctx.appContext.filesDir, "images").apply { mkdirs() }
        var copied = 0
        snapshot.extraFiles.forEach { ref ->
            val dst = File(destImagesDir, File(ref.zipPath).name)
            ref.file.inputStream().use { ins -> dst.outputStream().use { outs -> ins.copyTo(outs) } }
            copied++
        }

        // 5) درج نگاشت AnswerImage ها با نام فایل‌های کپی‌شده
//        val answerImages: List<AnswerImageEntity> = data.answerImages.map { dto ->
//            AnswerImageEntity(answerId = dto.answerId, fileName = File(dto.fileName).name)
//        }

        val answerImages = data.answerImages.map { it.toEntity() }
        ctx.db.answerDao().insertAnswerImages(answerImages)

        return RestoreReport(
            category = category,
            inserted = questions.size + answers.size + routes.size + answerImages.size,
            updated = 0, // در Overwrite، همه درج تازه‌اند
            skipped = 0,
            errors = emptyList()
        )
    }
}

/* -------------------------------
 * مبدل‌های ساده DTO ↔ Entity (و برعکس)
 * — کامنت‌ها دقیقاً می‌گویند چرا این نگاشت انتخاب شده است
 * ------------------------------- */

private fun QuestionEntity.toDto() = QnaQuestionDTO(
    id = id,
//    userKey = userKey,
    title = title,
    isHidden = isHidden,
    isRoot = isRoot,
    createdAt = createdAt
)

private fun AnswerEntity.toDto() = QnaAnswerDTO(
    id = id,
    questionId = questionId,
    title = title,
    note = note,
    isLiked = isLiked,
    isHidden = isHidden,
    createdAt = createdAt,
    sortOrder = sortOrder
)

private fun AnswerNextQuestionCrossRef.toDto() = QnaRouteDTO(
    answerId = answerId,
    nextQuestionId = nextQuestionId
)

private fun AnswerImageEntity.toDto() = QnaAnswerImageDTO(
//    id = id,
    answerId = answerId,
    fileName = fileName,
    createdAt = createdAt,
    sortOrder = sortOrder
)







private fun QnaQuestionDTO.toEntity() = QuestionEntity(
    id = this.id ?: 0,
//    userKey = targetUserKey,
    title = title,
    isHidden = isHidden,
    isRoot = isRoot,
    createdAt = createdAt
)

private fun QnaAnswerDTO.toEntity() = AnswerEntity(
    id = this.id ?: 0,
    questionId = questionId,
    title = title,
    note = note,
    isLiked = isLiked,
    isHidden = isHidden,
    createdAt = createdAt,
    sortOrder = sortOrder
)

private fun QnaRouteDTO.toEntity() = AnswerNextQuestionCrossRef(
    answerId = answerId,
    nextQuestionId = nextQuestionId
)

private fun QnaAnswerImageDTO.toEntity() = AnswerImageEntity(
//    id = this.id ?: 0,
    answerId = answerId,
    fileName = java.io.File(fileName).name,
    createdAt = createdAt,
    sortOrder = sortOrder
)

