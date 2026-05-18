package com.example.solarShop.data.room.tables.question_answers.answer

import androidx.room.withTransaction
import com.example.solarShop.data.room.appDatabase.AppDatabase
import com.example.solarShop.data.room.tables.question_answers.question.AnswerWithNextQuestions
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class OfflineAnswerRepository @Inject constructor(
    private val answerDao: AnswerDao,
    private val db: AppDatabase
) : AnswerRepository {


    override suspend fun insertAnswer(answerEntity: AnswerEntity): Long =
        answerDao.insertAnswer(answerEntity)

    override suspend fun deleteAnswer(answerEntity: AnswerEntity): Int =
        answerDao.deleteAnswer(answerEntity)

    override suspend fun updateAnswer(answerEntity: AnswerEntity): Int =
        answerDao.updateAnswer(answerEntity)

    override suspend fun updateAnswerTitleById(answerId: Int, title: String) =
        answerDao.updateAnswerTitleById(answerId, title)

    override suspend fun getAllAnswers(): List<AnswerEntity> =
        answerDao.getAllAnswers()

    override suspend fun getAnswerById(answerById: Int): AnswerEntity? =
        answerDao.getAnswerById(answerById)

    override suspend fun getAllAnswerImageEntities(): List<AnswerImageEntity> =
        answerDao.getAllAnswerImageEntities()

    override suspend fun clearAnswers() =
        answerDao.clearAnswers()

    override suspend fun insertAnswers(list: List<AnswerEntity>) =
        answerDao.insertAnswers(list)

    override suspend fun clearAnswerImages() =
        answerDao.clearAnswerImages()

    override suspend fun insertAnswerImages(list: List<AnswerImageEntity>) =
        answerDao.insertAnswerImages(list)

    override suspend fun updateImageOrder(id: Int, order: Int) =
        answerDao.updateImageOrder(id, order)

    override suspend fun insertAnswerImageEntity(e: AnswerImageEntity): Long =
        answerDao.insertAnswerImageEntity(e)

    override suspend fun deleteAnswerImageEntityById(id: Int) =
        answerDao.deleteAnswerImageEntityById(id)

    override suspend fun getMaxSortOrderForAnswer(answerId: Int): Int? =
        answerDao.getMaxSortOrderForAnswer(answerId)

    override suspend fun getQuestionIdForAnswer(answerId: Int): Int? =
        answerDao.getQuestionIdForAnswer(answerId)

    override suspend fun countEdge(answerId: Int, questionId: Int): Int =
        answerDao.countEdge(answerId, questionId)

    override suspend fun getDirectChildrenIds(questionId: Int): List<Int> =
        answerDao.getDirectChildrenIds(questionId)

    override suspend fun countParents(questionId: Int): Int =
        answerDao.countParents(questionId)

    override suspend fun getChildIdsForAnswer(answerId: Int): List<Int> =
        answerDao.getChildIdsForAnswer(answerId)

    override suspend fun getIncomingAnswer(questionId: Int): AnswerEntity? =
        answerDao.getIncomingAnswer(questionId)

    override suspend fun getNextQuestionForAnswer(answerId: Int): QuestionEntity? =
        answerDao.getNextQuestionForAnswer(answerId)

    override suspend fun getImagesByAnswer(answerId: Int): List<AnswerImageEntity> =
        answerDao.getImagesByAnswer(answerId)

    override fun observeImagesByAnswer(answerId: Int): Flow<List<AnswerImageEntity>> =
        answerDao.observeImagesByAnswer(answerId)

    override fun observeAnswerById(answerId: Int): Flow<AnswerEntity?> =
        answerDao.observeAnswerById(answerId)

    override fun observeAnswersForQuestion(questionId: Int): Flow<List<AnswerEntity>> =
        answerDao.observeAnswersForQuestion(questionId)

    override fun observeImages(answerId: Int): Flow<List<AnswerImageEntity>> =
        answerDao.observeImagesByAnswerId(answerId)

    override fun observeImagesForAnswerIds(answerIds: List<Int>): Flow<List<AnswerImageEntity>> =
        answerDao.observeImagesForAnswerIds(answerIds)

    override fun observeAnswersWithNextQuestions(questionId: Int): Flow<List<AnswerWithNextQuestions>> =
        answerDao.observeAnswersWithNextQuestions(questionId)

    override fun observeAnswersWithNextQuestionsByIds(answerIds: List<Int>): Flow<List<AnswerWithNextQuestions>> =
        answerDao.observeAnswersWithNextQuestionsByIds(answerIds)

    override fun observeRootQuestion(): Flow<QuestionEntity?> =
        answerDao.observeRootQuestion()

    override suspend fun insertImage(entity: AnswerImageEntity): Long =
        answerDao.insertImage(entity)

    override suspend fun deleteImageById(imageId: Int) =
        answerDao.deleteImageById(imageId)

    override suspend fun updateAnswerNoteById(id: Int, note: String) =
        answerDao.updateAnswerNoteById(id,note)

    override suspend fun getMaxSortOrderForQuestion(questionId: Int): Int? =
        answerDao.getMaxSortOrderForQuestion(questionId)

    override suspend fun updateAnswerSortOrder(answerId: Int, sortOrder: Int) =
            answerDao.updateAnswerSortOrder(answerId, sortOrder)


    override suspend fun insertAnswerForQuestion(questionId: Int, title: String) {
        val max = answerDao.getMaxSortOrderForQuestion(questionId) ?: -1
        answerDao.insertAnswer(
            AnswerEntity(
                id = null,
                questionId = questionId,
                title = title,
                sortOrder = max + 1
            )
        )
    }

    override suspend fun updateAnswersOrder(newOrderIds: List<Int>) {
        db.withTransaction {
            newOrderIds.forEachIndexed { index, id ->
                answerDao.updateAnswerSortOrder(id, index)
            }
        }
    }

    override suspend fun getImagesForAnswerIds(answerIds: List<Int>): List<AnswerImageEntity> =
        answerDao.getImagesForAnswerIds(answerIds)

    override suspend fun existsAnswer(answerId: Int): Boolean =
        answerDao.existsAnswer(answerId)

    override fun photosForReviewFlow(orderId: Int, answerId: Int): Flow<List<AnswerImageEntity>> =
        answerDao.photosForReviewFlow(orderId, answerId)



    override suspend fun getAnswerImageById(id: Int): AnswerImageEntity? =
        answerDao.getAnswerImageById(id)

    override suspend fun getImageById(id: Int): AnswerImageEntity? =
        answerDao.getImageById(id)

    override suspend fun getImageFileNameById(imageId: Int): String? =
        answerDao.getImageFileNameById(imageId)

    override suspend fun getFirstImageIdForAnswer(answerId: Int): Int? =
        answerDao.getFirstImageIdForAnswer(answerId)


}