package com.example.solarShop.data.room.tables.question_answers.answer


import com.example.solarShop.data.room.tables.question_answers.question.AnswerWithNextQuestions
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import kotlinx.coroutines.flow.Flow


interface AnswerRepository {

    suspend fun insertAnswer(answerEntity: AnswerEntity):Long
    suspend fun deleteAnswer(answerEntity: AnswerEntity): Int
    suspend fun updateAnswer(answerEntity: AnswerEntity): Int
    suspend fun updateAnswerTitleById(answerId: Int , title:String)
    suspend fun getAllAnswers() : List<AnswerEntity>
    suspend fun getAnswerById(answerById :Int) : AnswerEntity?
    suspend fun getAllAnswerImageEntities(): List<AnswerImageEntity>
    suspend fun clearAnswers()
    suspend fun insertAnswers(list: List<AnswerEntity>)
    suspend fun clearAnswerImages()
    suspend fun insertAnswerImages(list: List<AnswerImageEntity>)

    suspend fun updateImageOrder(id: Int, order: Int)

    suspend fun insertAnswerImageEntity(e: AnswerImageEntity): Long
    suspend fun deleteAnswerImageEntityById(id: Int)

    suspend fun getMaxSortOrderForAnswer(answerId: Int): Int?

    suspend fun getQuestionIdForAnswer(answerId: Int): Int?
    suspend fun countEdge(answerId: Int, questionId: Int): Int
    suspend fun getDirectChildrenIds(questionId: Int): List<Int>

    suspend fun countParents(questionId: Int): Int
    suspend fun getChildIdsForAnswer(answerId: Int): List<Int>
    suspend fun getIncomingAnswer(questionId: Int): AnswerEntity?

    suspend fun getNextQuestionForAnswer(answerId: Int): QuestionEntity?
    suspend fun getImagesByAnswer(answerId: Int): List<AnswerImageEntity>

    //Flow
    fun observeImagesByAnswer(answerId: Int): Flow<List<AnswerImageEntity>>

    fun observeAnswerById(answerId :Int): Flow<AnswerEntity?>
    fun observeAnswersForQuestion(questionId: Int): Flow<List<AnswerEntity>>

    fun observeImages(answerId: Int): Flow<List<AnswerImageEntity>>
    fun observeImagesForAnswerIds(answerIds: List<Int>): Flow<List<AnswerImageEntity>>

    fun observeAnswersWithNextQuestions(questionId: Int): Flow<List<AnswerWithNextQuestions>>
    fun observeAnswersWithNextQuestionsByIds(answerIds: List<Int>): Flow<List<AnswerWithNextQuestions>>

    fun observeRootQuestion(): Flow<QuestionEntity?>

    suspend fun insertImage(entity: AnswerImageEntity): Long

    suspend fun deleteImageById(imageId: Int)


    suspend fun updateAnswerNoteById(id: Int, note: String)


    suspend fun getMaxSortOrderForQuestion(questionId: Int): Int?

    suspend fun updateAnswerSortOrder(answerId: Int, sortOrder: Int)

    suspend fun insertAnswerForQuestion(questionId: Int, title: String)

    suspend fun updateAnswersOrder(newOrderIds: List<Int>)

    suspend fun getImagesForAnswerIds(answerIds: List<Int>): List<AnswerImageEntity>

    suspend fun existsAnswer(answerId: Int): Boolean

    fun photosForReviewFlow(orderId: Int, answerId: Int): Flow<List<AnswerImageEntity>>


    suspend fun getAnswerImageById(id: Int): AnswerImageEntity?

    suspend fun getImageById(id: Int): AnswerImageEntity?

    suspend fun getImageFileNameById(imageId: Int): String?

    suspend fun getFirstImageIdForAnswer(answerId: Int): Int?


}