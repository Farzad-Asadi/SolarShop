package com.example.solarShop.data.room.tables.question_answers.question


import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import kotlinx.coroutines.flow.Flow


interface QuestionRepository {

    suspend fun insertQuestion(questionEntity: QuestionEntity):Long
    suspend fun insertQuestions(list: List<QuestionEntity>)
    suspend fun deleteQuestion(questionEntity: QuestionEntity): Int
    suspend fun updateQuestion(questionEntity: QuestionEntity): Int
    suspend fun updateQuestionTitleById(questionid: Int , title:String)
    suspend fun upsertAnswerNextQuestions(list: List<AnswerNextQuestionCrossRef>)
    suspend fun upsertAnswerNextQuestion(answerNextQuestionCrossRef: AnswerNextQuestionCrossRef)
    suspend fun deleteAnswerNextQuestion(answerNextQuestionCrossRef: AnswerNextQuestionCrossRef)
    suspend fun getAllQuestions(): List<QuestionEntity>
    suspend fun getAllAnswerNextQuestions(): List<AnswerNextQuestionCrossRef>
    suspend fun clearQuestions()
    suspend fun insertRoutes(list: List<AnswerNextQuestionCrossRef>)
    suspend fun clearRoutes()
    suspend fun getById(id: Int): QuestionEntity?
    suspend fun getNextQuestionIdForAnswer(answerId: Int): Int?
    suspend fun getQuestionById(questionId :Int): QuestionEntity?


    //Flow
    fun observeQuestionById(questionId :Int): Flow<QuestionEntity?>
    fun observeAllQuestionWithAnswers(): Flow<List<QuestionWithAnswers>>
    fun observeAllNextQuestionsForAnswer(): Flow<List<AnswerWithNextQuestions>>
    fun observeIncomingAnswersForQuestion(
        questionId: Int
    ): Flow<List<AnswerWithParentQuestion>>
    fun observeSingleIncomingAnswerForQuestion(
        questionId: Int
    ): Flow<AnswerWithParentQuestion?>
    fun observeIncomingAnswers(
        questionId: Int
    ): Flow<List<AnswerEntity>>
    fun observeSingleIncomingAnswer(
        questionId: Int
    ): Flow<AnswerEntity?>
    suspend fun getNextQuestionIdsForAnswer(answerId: Int): List<Int>



    //For deleteQuestionSmart

    suspend fun getDistinctChildrenOfQuestion(questionId: Int): List<Int>

    suspend fun getParentAnswersOfQuestion(questionId: Int): List<AnswerEntity>

    suspend fun deleteAllIncomingEdgesToQuestion(questionId: Int)

    suspend fun insertEdge(edge: AnswerNextQuestionCrossRef): Long



    //for rewireIncomingAnswerTo

    suspend fun getSingleIncomingAnswer(questionId: Int): AnswerEntity?

    suspend fun deleteEdge(answerId: Int, questionId: Int): Int

    fun observeChoicesForOrder(orderId: Int): Flow<List<ChoiceEdgeRow>>
    fun observeAllAnswerEdges(): Flow<List<AnswerNextQuestionCrossRef>>



    suspend fun existsQuestion(qId: Int): Boolean

    suspend fun setRootQuestion(rootId: Int)

    suspend fun getMainRootIdOrNull(): Int?

    suspend fun findAnyGraphRootQuestionIdOrNull(): Int?

}