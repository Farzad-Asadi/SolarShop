package com.example.solarShop.data.room.tables.question_answers.question

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestion(questionEntity: QuestionEntity):Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(list: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(questionEntity: QuestionEntity): Int

    @Query("UPDATE questions SET title = :title WHERE id = :questionid")
    suspend fun updateQuestionTitleById(questionid: Int , title:String)

    @Delete
    suspend fun deleteQuestion(questionEntity: QuestionEntity): Int

    @Upsert
    suspend fun upsertAnswerNextQuestions(list: List<AnswerNextQuestionCrossRef>)

    @Query("DELETE FROM questions")
    suspend fun clearQuestions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(list: List<AnswerNextQuestionCrossRef>)

    @Query("DELETE FROM answer_next_questions")
    suspend fun clearRoutes()

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): QuestionEntity?


    @Upsert
    suspend fun upsertAnswerNextQuestion(answerNextQuestionCrossRef: AnswerNextQuestionCrossRef)

    @Delete
    suspend fun deleteAnswerNextQuestion(answerNextQuestionCrossRef: AnswerNextQuestionCrossRef)

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM answer_next_questions") // ← اگر اسم جدولت فرق می‌کنه، همین‌جا عوض کن
    suspend fun getAllAnswerNextQuestions(): List<AnswerNextQuestionCrossRef>

    @Query("""
    SELECT nextQuestionId FROM answer_next_questions
    WHERE answerId = :answerId
    LIMIT 1
""")
    suspend fun getNextQuestionIdForAnswer(answerId: Int): Int?

    @Query("SELECT * FROM questions WHERE id=:questionId ")
    suspend fun getQuestionById(questionId :Int): QuestionEntity?


    //Flow
    @Query("SELECT * FROM questions WHERE id=:questionId ")
    fun observeQuestionById(questionId :Int): Flow<QuestionEntity?>


    @Transaction
    @Query("SELECT * FROM questions ")
    fun observeAllQuestionWithAnswers(): Flow<List<QuestionWithAnswers>>


    @Transaction
    @Query("""
        SELECT a.*
        FROM answers a
        INNER JOIN answer_next_questions anq ON anq.answerId = a.id
        WHERE anq.nextQuestionId = :questionId
    """)
    fun observeAnswersOfQuestionByQuestionId(questionId:Int): Flow<List<AnswerEntity>>


    @Transaction
    @Query("SELECT * FROM answers ")
    fun observeAllNextQuestionsForAnswer(): Flow<List<AnswerWithNextQuestions>>


    /**
     * همه‌ی جواب‌هایی که منجر به سؤال [questionId] شده‌اند را برمی‌گرداند
     * (ممکن است بیش از یکی باشند).
     */
    @Transaction
    @Query("""
        SELECT * FROM answers
        WHERE id IN (
            SELECT answerId
            FROM answer_next_questions
            WHERE nextQuestionId = :questionId
        )
    """)
    fun observeIncomingAnswersForQuestion(
        questionId: Int
    ): Flow<List<AnswerWithParentQuestion>>

    /**
     * اگر مطمئنی فقط یک جواب باید وجود داشته باشد (یا می‌خواهی یکی را انتخاب کنی)،
     * یک مورد را برمی‌گرداند (بر اساس کمترین id — در صورت داشتن createdAt بهتر است بر اساس آن مرتب کنی).
     */
    @Transaction
    @Query("""
        SELECT * FROM answers
        WHERE id = (
            SELECT answerId
            FROM answer_next_questions
            WHERE nextQuestionId = :questionId
            ORDER BY answerId ASC
            LIMIT 1
        )
    """)
    fun observeSingleIncomingAnswerForQuestion(
        questionId: Int
    ): Flow<AnswerWithParentQuestion?>

    @Query("""
        SELECT a.*
        FROM answers a
        INNER JOIN answer_next_questions anq ON anq.answerId = a.id
        WHERE anq.nextQuestionId = :questionId
    """)
    fun observeIncomingAnswers(
        questionId: Int
    ): Flow<List<AnswerEntity>>



    @Query("""
    SELECT a.*
    FROM answers AS a
    INNER JOIN answer_next_questions AS anq
        ON anq.answerId = a.id
    WHERE anq.nextQuestionId = :questionId
    LIMIT 1
""")
    fun observeSingleIncomingAnswer(
        questionId: Int
    ): Flow<AnswerEntity?>



    @Query("SELECT nextQuestionId FROM answer_next_questions WHERE answerId = :answerId")
    suspend fun getNextQuestionIdsForAnswer(answerId: Int): List<Int>





    //For deleteQuestionSmart



    // لیست یکتای nextQuestionIdهایی که از پاسخ‌های این سؤال بیرون می‌آید
    @Query("""
        SELECT DISTINCT anq.nextQuestionId
        FROM answer_next_questions anq
        INNER JOIN answers a ON a.id = anq.answerId
        WHERE a.questionId = :questionId
    """)
    suspend fun getDistinctChildrenOfQuestion(questionId: Int): List<Int>

    // پاسخ(های) والدی که این سؤال را فراخوانی کرده‌اند (incoming)
    @Query("""
        SELECT a.*
        FROM answers a
        INNER JOIN answer_next_questions anq ON anq.answerId = a.id
        WHERE anq.nextQuestionId = :questionId
    """)
    suspend fun getParentAnswersOfQuestion(questionId: Int): List<AnswerEntity>

    // حذف تمام یال‌های ورودی به این سؤال (answer -> this question)
    @Query("DELETE FROM answer_next_questions WHERE nextQuestionId = :questionId")
    suspend fun deleteAllIncomingEdgesToQuestion(questionId: Int)

    // اتصال یک پاسخ به یک سؤال (اگر وجود داشت، نادیده بگیر)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEdge(edge: AnswerNextQuestionCrossRef): Long




    //for rewireIncomingAnswerTo

    // پاسخِ والدی که به این سؤال ختم می‌شود (تکی)
    @Query("""
        SELECT a.* FROM answers a
        INNER JOIN answer_next_questions anq ON anq.answerId = a.id
        WHERE anq.nextQuestionId = :questionId
        LIMIT 1
    """)
    suspend fun getSingleIncomingAnswer(questionId: Int): AnswerEntity?

    // حذف یال answer -> question
    @Query("""
        DELETE FROM answer_next_questions
        WHERE answerId = :answerId AND nextQuestionId = :questionId
    """)
    suspend fun deleteEdge(answerId: Int, questionId: Int): Int


    // انتخاب‌های کاربر برای یک سفارش (برای ساخت مسیر)
    @Query("SELECT questionId, answerId,choiceDescription FROM selected_choices WHERE orderId = :orderId")
    fun observeChoicesForOrder(orderId: Int): Flow<List<ChoiceEdgeRow>>


    // یال‌های گراف: هر Answer → nextQuestionId
    @Query("SELECT answerId, nextQuestionId FROM answer_next_questions")
    fun observeAllAnswerEdges(): Flow<List<AnswerNextQuestionCrossRef>>





    @Query("DELETE FROM answer_next_questions WHERE nextQuestionId = :questionId")
    suspend fun deleteIncomingEdgesOfQuestion(questionId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(edge: AnswerNextQuestionCrossRef)

    @Transaction
    suspend fun replaceParentAnswer(questionId: Int, newAnswerId: Int) {
        deleteIncomingEdgesOfQuestion(questionId)      // ✅ پاک کردن والد(ها)
        upsert(AnswerNextQuestionCrossRef(newAnswerId, questionId)) // ✅ ساخت والد جدید
    }

    @Query("SELECT EXISTS(SELECT 1 FROM questions WHERE id = :qId)")
    suspend fun existsQuestion(qId: Int): Boolean



    @Transaction
    suspend fun setRootQuestion(rootId: Int) {
        clearRootFlag()
        markAsRoot(rootId)
    }

    @Query("UPDATE questions SET isRoot = 0")
    suspend fun clearRootFlag()

    @Query("UPDATE questions SET isRoot = 1 WHERE id = :rootId")
    suspend fun markAsRoot(rootId: Int)


    @Query("SELECT id FROM questions WHERE isRoot = 1 LIMIT 1")
    suspend fun getMainRootIdOrNull(): Int?

    @Query("""
    SELECT q.id
    FROM questions q
    WHERE q.id NOT IN (
        SELECT anq.nextQuestionId
        FROM answer_next_questions anq
    )
    ORDER BY q.id ASC
    LIMIT 1
""")
    suspend fun findAnyGraphRootQuestionIdOrNull(): Int?


}