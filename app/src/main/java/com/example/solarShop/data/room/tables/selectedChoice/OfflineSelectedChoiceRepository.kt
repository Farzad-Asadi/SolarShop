package com.example.solarShop.data.room.tables.selectedChoice

import com.example.solarShop.data.room.tables.question_answers.answer.AnswerDao
import com.example.solarShop.ui.orderScreen.orderCatalog.AnswerItemUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File
import javax.inject.Inject


class OfflineSelectedChoiceRepository @Inject constructor(
    private val selectedChoiceDao: SelectedChoiceDao,
    private val answerDao : AnswerDao
) : SelectedChoiceRepository {


    override suspend fun insertSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity) =
        selectedChoiceDao.insertSelectedChoice(selectedChoiceEntity)


    override suspend fun deleteSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity) =
        selectedChoiceDao.deleteSelectedChoice(selectedChoiceEntity)


    override suspend fun updateSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity) =
        selectedChoiceDao.updateSelectedChoice(selectedChoiceEntity)


    override suspend fun getAllSelectedChoice(): List<SelectedChoiceEntity>? =
        selectedChoiceDao.getAllSelectedChoice()


    override suspend fun getAllSelectedChoiceByOrderParentId(orderParentId: Int): List<SelectedChoiceEntity>? =
        selectedChoiceDao.getAllSelectedChoiceByOrderParentId(orderParentId)

    override suspend fun clearSelections(orderId: Int, questionId: Int): Int =
        selectedChoiceDao.clearSelections(orderId, questionId)

    override suspend fun deleteSelection(orderId: Int, questionId: Int, answerId: Int): Int =
        selectedChoiceDao.deleteSelection(orderId, questionId, answerId)

    override suspend fun insertSelection(orderId: Int, questionId: Int, answerId: Int) =
        selectedChoiceDao.insertSelection(orderId, questionId, answerId)

    override suspend fun getSelectedAnswerId(orderId: Int, questionId: Int): Int? =
        selectedChoiceDao.getSelectedAnswerId(orderId,questionId)

    override suspend fun getPreviousQuestionId(orderId: Int, currentQuestionId: Int): Int? =
        selectedChoiceDao.getPreviousQuestionId(orderId, currentQuestionId)

    override suspend fun deleteSelectionsInDescendants(orderId: Int, fromQuestionId: Int): Int =
        selectedChoiceDao.deleteSelectionsInDescendants(orderId, fromQuestionId)

    override suspend fun getDirectChildren(questionId: Int): List<Int> =
        selectedChoiceDao.getDirectChildren(questionId)

    override suspend fun deleteSelectionsForQuestions(orderId: Int, questionIds: List<Int>): Int =
            selectedChoiceDao.deleteSelectionsForQuestions(orderId, questionIds)

    override suspend fun deleteSelectionByQuestion(orderId: Int, questionId: Int): Int =
        selectedChoiceDao.deleteSelectionByQuestion(orderId, questionId)

    override suspend fun updateNote(orderId: Int, questionId: Int, answerId: Int, note: String): Int =
        selectedChoiceDao.updateNote(orderId, questionId, answerId, note)

    override suspend fun insertChoice(entity: SelectedChoiceEntity): Long =
            selectedChoiceDao.insertChoice(entity)

    override fun currentAnswerItems(
        questionId: Int,
        orderId: Int,
        filesDir: File
    ): Flow<List<AnswerItemUnit>> =
        combine(
            answerDao.observeAnswersWithImages(questionId),                 // Flow<List<AnswerWithImages>>
            selectedChoiceDao.observeSelectedAnswerIds(orderId, questionId) // Flow<List<Int>>
        ) { awiList, selectedIds ->

            val selectedSet = selectedIds.toSet()

            awiList.mapNotNull { awi ->
                val id = awi.answer.id ?: return@mapNotNull null

                val sortedImages = awi.images.sortedBy { it.sortOrder } // ✅ ترتیب ویرایش

                AnswerItemUnit(
                    answerId = id,
                    title = awi.answer.title.orEmpty(),

                    // ✅ جدیدها
                    imageIds = sortedImages.mapNotNull { it.id }, // Int
                    imageUris = sortedImages.map { img ->
                        File(File(filesDir, "images"), img.fileName).toURI().toString()
                    },

                    selected = id in selectedSet,

                    liked = false,
                    note = "",

                    // ✅ جدید: نوت خود Answer (همون که تو فول‌اسکرین آخر می‌خوای نشون بدی)
                    answerNote = awi.answer.note.orEmpty()
                )
            }
        }


    override fun observeAnsweredOrder(orderId: Int, questionId: Int): Flow<Int?> =
        selectedChoiceDao.observeAnsweredOrder(orderId, questionId)

    override fun observeTotalAnswered(orderId: Int): Flow<Int?> =
            selectedChoiceDao.observeTotalAnswered(orderId)

    override fun observeChoice(orderId: Int, questionId: Int): Flow<SelectedChoiceEntity?> =
        selectedChoiceDao.observeChoice(orderId, questionId)

    override suspend fun upsertNote(orderId: Int, questionId: Int, answerId: Int, note: String) {
        val updated = selectedChoiceDao.updateNote(orderId, questionId, answerId, note)
        if (updated == 0) {
            selectedChoiceDao.insertChoice(
                SelectedChoiceEntity(
                    orderId = orderId,
                    questionId = questionId,
                    answerId = answerId,
                    choiceDescription = note
                )
            )
        }
    }


}