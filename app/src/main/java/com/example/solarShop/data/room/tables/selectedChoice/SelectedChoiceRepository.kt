package com.example.solarShop.data.room.tables.selectedChoice


import com.example.solarShop.ui.orderScreen.orderCatalog.AnswerItemUnit
import kotlinx.coroutines.flow.Flow
import java.io.File


interface SelectedChoiceRepository {

    suspend fun insertSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity)
    suspend fun deleteSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity)
    suspend fun updateSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity)
    suspend fun getAllSelectedChoice() : List<SelectedChoiceEntity>?
    suspend fun getAllSelectedChoiceByOrderParentId(orderParentId:Int) : List<SelectedChoiceEntity>?

    suspend fun clearSelections(orderId: Int, questionId: Int): Int
    suspend fun deleteSelection(orderId: Int, questionId: Int, answerId: Int): Int
    suspend fun insertSelection(orderId: Int, questionId: Int, answerId: Int)

    suspend fun getSelectedAnswerId(orderId: Int, questionId: Int): Int?
    suspend fun getPreviousQuestionId(orderId: Int, currentQuestionId: Int): Int?
    suspend fun deleteSelectionsInDescendants(orderId: Int, fromQuestionId: Int): Int

    suspend fun getDirectChildren(questionId: Int): List<Int>
    suspend fun deleteSelectionsForQuestions(orderId: Int, questionIds: List<Int>): Int
    suspend fun deleteSelectionByQuestion(orderId: Int, questionId: Int): Int

    suspend fun updateNote(orderId: Int, questionId: Int, answerId: Int, note: String): Int
    suspend fun insertChoice(entity: SelectedChoiceEntity): Long


    //Flow
    fun currentAnswerItems(
        questionId: Int,
        orderId: Int,
        filesDir: File
    ): Flow<List<AnswerItemUnit>>


    fun observeAnsweredOrder(orderId: Int, questionId: Int): Flow<Int?>
    fun observeTotalAnswered(orderId: Int): Flow<Int?>

    fun observeChoice(orderId: Int, questionId: Int): Flow<SelectedChoiceEntity?>

    suspend fun upsertNote(orderId: Int, questionId: Int, answerId: Int, note: String)

}

