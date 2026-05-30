package com.example.solarShop.data.room.tables.question_answers

import android.content.Context
import androidx.core.content.FileProvider
import androidx.room.Dao
import androidx.room.Query
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerDao
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerImageEntity
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceDao
import com.example.solarShop.ui.orderScreen.orderCatalog.AnswerItemUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File
import javax.inject.Inject





interface CatalogRepository {
    fun currentAnswerItems(
        questionId: Int,
        orderId: Int,
        filesDir: File
    ): Flow<List<AnswerItemUnit>>
}


class CatalogRepositoryImpl @Inject constructor(
    @ApplicationContext private val app: Context,
    private val answerDao: AnswerDao,
    private val selectedChoiceDao: SelectedChoiceDao,

// این‌ها را بعداً برای انتخاب‌ها/لایک/یادداشت‌ها اضافه کن:
// private val selectedChoiceDao: SelectedChoiceDao,
) : CatalogRepository {

    private fun AnswerImageEntity.toUriString(filesDir: File): String =
        File(File(filesDir, "images"), fileName).toURI().toString()   // -> file:///data/...



    override fun currentAnswerItems(
        questionId: Int,
        orderId: Int,
        filesDir: File
    ): Flow<List<AnswerItemUnit>> =
        combine(
            answerDao.observeAnswersWithImages(questionId),                 // Flow<List<AnswerWithImages>>
            selectedChoiceDao.observeSelectedAnswerIds(orderId, questionId)       // Flow<List<Int>>
        ) { awiList, selectedIds ->
            val selectedSet = selectedIds.toSet()
            awiList.mapNotNull { awi ->
                val id = awi.answer.id ?: return@mapNotNull null
                val sortedImages = awi.images.sortedBy { it.sortOrder } // ✅ ترتیب ویرایش
                AnswerItemUnit(
                    answerId = id,
                    title = awi.answer.title.orEmpty(),
                    imageIds = sortedImages.mapNotNull { it.id },
                    imageUris = awi.images
                        .sortedBy { it.createdAt }
                        .map { img ->
                            val f = File(File(app.filesDir, "images"), img.fileName)
                            FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", f)
                                .toString()
                        },

                    selected   = id in selectedSet,
                    liked      = false,

                    // 👇 فعلاً note سفارش رو خالی می‌ذاریم، ویومدل خودش پرش می‌کند
                    note       = "",

                    // 👈 این مهمه: توضیح خود Answer از جدول answers
                    answerNote = awi.answer.note.orEmpty()
                )

            }
        }


}