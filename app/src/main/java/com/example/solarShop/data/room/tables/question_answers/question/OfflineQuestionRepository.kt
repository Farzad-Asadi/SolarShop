package com.example.solarShop.data.room.tables.question_answers.question

import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject


class OfflineQuestionRepository @Inject constructor(
    private val questionDao: QuestionDao
) : QuestionRepository {


    override suspend fun insertQuestion(questionEntity: QuestionEntity) =
        questionDao.insertQuestion(questionEntity)

    override suspend fun insertQuestions(list: List<QuestionEntity>) =
        questionDao.insertQuestions(list)

    override suspend fun deleteQuestion(questionEntity: QuestionEntity) =
        questionDao.deleteQuestion(questionEntity)

    override suspend fun updateQuestion(questionEntity: QuestionEntity) =
        questionDao.updateQuestion(questionEntity)

    override suspend fun updateQuestionTitleById(questionid: Int, title: String) =
        questionDao.updateQuestionTitleById(questionid, title)

    override suspend fun upsertAnswerNextQuestions(list: List<AnswerNextQuestionCrossRef>) =
        questionDao.upsertAnswerNextQuestions(list)

    override suspend fun upsertAnswerNextQuestion(answerNextQuestionCrossRef: AnswerNextQuestionCrossRef) =
        questionDao.upsertAnswerNextQuestion(answerNextQuestionCrossRef)

    override suspend fun deleteAnswerNextQuestion(answerNextQuestionCrossRef: AnswerNextQuestionCrossRef) =
        questionDao.deleteAnswerNextQuestion(answerNextQuestionCrossRef)

    override suspend fun getAllQuestions(): List<QuestionEntity> =
        questionDao.getAllQuestions()

    override suspend fun getAllAnswerNextQuestions(): List<AnswerNextQuestionCrossRef> =
            questionDao.getAllAnswerNextQuestions()

    override suspend fun clearQuestions() =
        questionDao.clearQuestions()

    override suspend fun insertRoutes(list: List<AnswerNextQuestionCrossRef>) =
        questionDao.insertRoutes(list)

    override suspend fun clearRoutes() =
        questionDao.clearRoutes()

    override suspend fun getById(id: Int): QuestionEntity? =
        questionDao.getById(id)

    override suspend fun getNextQuestionIdForAnswer(answerId: Int): Int? =
        questionDao.getNextQuestionIdForAnswer(answerId)

    override suspend fun getQuestionById(questionId: Int): QuestionEntity? =
        questionDao.getQuestionById(questionId)

    override fun observeQuestionById(questionId: Int): Flow<QuestionEntity?> =
        questionDao.observeQuestionById(questionId)


    //Flow

    override fun observeAllQuestionWithAnswers(): Flow<List<QuestionWithAnswers>> =
        questionDao.observeAllQuestionWithAnswers().distinctUntilChanged()

    override fun observeAllNextQuestionsForAnswer(): Flow<List<AnswerWithNextQuestions>> =
        questionDao.observeAllNextQuestionsForAnswer().distinctUntilChanged()

    override fun observeIncomingAnswersForQuestion(questionId: Int): Flow<List<AnswerWithParentQuestion>> =
        questionDao.observeIncomingAnswersForQuestion(questionId)

    override fun observeSingleIncomingAnswerForQuestion(questionId: Int): Flow<AnswerWithParentQuestion?> =
        questionDao.observeSingleIncomingAnswerForQuestion(questionId)

    override fun observeIncomingAnswers(questionId: Int): Flow<List<AnswerEntity>> =
        questionDao.observeIncomingAnswers(questionId)

    override fun observeSingleIncomingAnswer(questionId: Int): Flow<AnswerEntity?> =
        questionDao.observeSingleIncomingAnswer(questionId)

    override suspend fun getNextQuestionIdsForAnswer(answerId: Int): List<Int> =
        questionDao.getNextQuestionIdsForAnswer(answerId)

    override suspend fun getDistinctChildrenOfQuestion(questionId: Int): List<Int> =
        questionDao.getDistinctChildrenOfQuestion(questionId)

    override suspend fun getParentAnswersOfQuestion(questionId: Int): List<AnswerEntity> =
        questionDao.getParentAnswersOfQuestion(questionId)

    override suspend fun deleteAllIncomingEdgesToQuestion(questionId: Int) =
        questionDao.deleteAllIncomingEdgesToQuestion(questionId)

    override suspend fun insertEdge(edge: AnswerNextQuestionCrossRef): Long =
        questionDao.insertEdge(edge)

    override suspend fun getSingleIncomingAnswer(questionId: Int): AnswerEntity? =
        questionDao.getSingleIncomingAnswer(questionId)

    override suspend fun deleteEdge(answerId: Int, questionId: Int): Int =
            questionDao.deleteEdge(answerId, questionId)

    override fun observeChoicesForOrder(orderId: Int): Flow<List<ChoiceEdgeRow>> =
        questionDao.observeChoicesForOrder(orderId)

    override fun observeAllAnswerEdges(): Flow<List<AnswerNextQuestionCrossRef>> =
        questionDao.observeAllAnswerEdges()

    override suspend fun existsQuestion(qId: Int): Boolean =
        questionDao.existsQuestion(qId)

    override suspend fun setRootQuestion(rootId: Int) =
        questionDao.setRootQuestion(rootId)

    override suspend fun getMainRootIdOrNull(): Int? =
        questionDao.getMainRootIdOrNull()

    override suspend fun findAnyGraphRootQuestionIdOrNull(): Int? =
            questionDao.findAnyGraphRootQuestionIdOrNull()
}