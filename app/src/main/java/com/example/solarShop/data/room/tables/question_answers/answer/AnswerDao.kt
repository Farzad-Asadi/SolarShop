package com.example.solarShop.data.room.tables.question_answers.answer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.solarShop.data.room.tables.question_answers.question.AnswerWithNextQuestions
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnswer(answerEntity: AnswerEntity):Long

    @Delete
    suspend fun deleteAnswer(answerEntity: AnswerEntity): Int

    @Update
    suspend fun updateAnswer(answerEntity: AnswerEntity): Int

    @Query("UPDATE answers SET title = :title WHERE id = :answerId")
    suspend fun updateAnswerTitleById(answerId: Int , title:String)

    @Query("SELECT * FROM answers ")
    suspend fun getAllAnswers() : List<AnswerEntity>

    @Query("SELECT * FROM answers WHERE id = :answerById ")
    suspend fun getAnswerById(answerById :Int) : AnswerEntity?

    @Query("DELETE FROM answers")
    suspend fun clearAnswers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(list: List<AnswerEntity>)

    @Query("DELETE FROM answer_images")
    suspend fun clearAnswerImages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswerImages(list: List<AnswerImageEntity>)



    @Query("SELECT * FROM answer_images")
    suspend fun getAllAnswerImageEntities(): List<AnswerImageEntity>


    @Query("UPDATE answer_images SET sortOrder = :order WHERE id = :id")
    suspend fun updateImageOrder(id: Int, order: Int)


    @Query("SELECT MAX(sortOrder) FROM answer_images WHERE answerId = :answerId")
    suspend fun getMaxSortOrderForAnswer(answerId: Int): Int?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswerImageEntity(e: AnswerImageEntity): Long

    @Query("DELETE FROM answer_images WHERE id = :id")
    suspend fun deleteAnswerImageEntityById(id: Int)


    @Query("SELECT questionId FROM answers WHERE id = :answerId")
    suspend fun getQuestionIdForAnswer(answerId: Int): Int?

    @Query("""
        SELECT COUNT(*) FROM answer_next_questions 
        WHERE answerId = :answerId AND nextQuestionId = :questionId
    """)
    suspend fun countEdge(answerId: Int, questionId: Int): Int


    @Query("""
        SELECT COUNT(*) FROM answer_next_questions
        WHERE nextQuestionId = :questionId
    """)
    suspend fun countParents(questionId: Int): Int

    @Query("SELECT nextQuestionId FROM answer_next_questions WHERE answerId = :answerId")
    suspend fun getChildIdsForAnswer(answerId: Int): List<Int>


    @Query("""
    SELECT answers.* FROM answers
    INNER JOIN answer_next_questions anq
    ON answers.id = anq.answerId
    WHERE anq.nextQuestionId = :questionId
    LIMIT 1
""")
    suspend fun getIncomingAnswer(questionId: Int): AnswerEntity?




    // همهٔ فرزندان مستقیمِ یک سوال (از طریق پاسخ‌هایش)
    @Query("""
        SELECT anq.nextQuestionId
        FROM answer_next_questions AS anq
        WHERE anq.answerId IN (SELECT id FROM answers WHERE questionId = :questionId)
    """)
    suspend fun getDirectChildrenIds(questionId: Int): List<Int>

    // گرفتن یک‌باره لیست تصاویر پاسخ
    @Query("""
        SELECT * 
        FROM answer_images 
        WHERE answerId = :answerId 
        ORDER BY sortOrder ASC
    """)
    suspend fun getImagesByAnswer(answerId: Int): List<AnswerImageEntity>

    // (اختیاری) استریم زنده با Flow
    @Query("""
        SELECT * 
        FROM answer_images 
        WHERE answerId = :answerId 
        ORDER BY sortOrder ASC
    """)
    fun observeImagesByAnswer(answerId: Int): Flow<List<AnswerImageEntity>>


    //Flow
    @Query("SELECT * FROM answers WHERE id=:answerId ")
    fun observeAnswerById(answerId :Int): Flow<AnswerEntity?>

    @Query("""
    SELECT * FROM answers
    WHERE questionId = :questionId
    ORDER BY sortOrder ASC, id ASC
""")
    fun observeAnswersForQuestion(questionId: Int): Flow<List<AnswerEntity>>



    @Query("SELECT * FROM answer_images WHERE answerId = :answerId ORDER BY sortOrder ASC")
    fun observeImagesByAnswerId(answerId: Int): Flow<List<AnswerImageEntity>>


    @Query(" SELECT * FROM answer_images WHERE answerId IN (:answerIds) ORDER BY sortOrder ASC")
    fun observeImagesForAnswerIds(answerIds: List<Int>): Flow<List<AnswerImageEntity>>



    // همهٔ جواب‌های یک سوال + فرزندان هر جواب (ممکنه خالی باشه)
    @Transaction
    @Query("""
    SELECT * FROM answers
    WHERE questionId = :questionId
    ORDER BY sortOrder ASC, id ASC
""")
    fun observeAnswersWithNextQuestions(questionId: Int): Flow<List<AnswerWithNextQuestions>>


    // همین، ولی فیلتر بر اساس لیست id جواب‌ها
    @Transaction
    @Query("""
    SELECT * FROM answers
    WHERE id IN (:answerIds)
    ORDER BY sortOrder ASC, id ASC
""")
    fun observeAnswersWithNextQuestionsByIds(answerIds: List<Int>): Flow<List<AnswerWithNextQuestions>>


    @Transaction
    @Query("""
    SELECT * FROM answers
    WHERE questionId = :questionId AND isHidden = 0
    ORDER BY sortOrder ASC, id ASC
""")
    fun observeAnswersWithImages(questionId: Int): Flow<List<AnswerWithImages>>



    @Query("""
    SELECT *
    FROM questions q
    WHERE
        q.isRoot = 1
        OR (
            NOT EXISTS (
                SELECT 1
                FROM answer_next_questions r
                WHERE r.nextQuestionId = q.id
            )
        )
    ORDER BY 
        CASE WHEN q.isRoot = 1 THEN 0 ELSE 1 END,
        q.createdAt ASC,
        q.id ASC
    LIMIT 1
""")
    fun observeRootQuestion(): Flow<QuestionEntity?>



    // برای مرحله‌های بعدی (پیدا کردن سؤال بعدی بر اساس پاسخ انتخابی)
    @Query(
        """
SELECT q.* FROM questions q
INNER JOIN answer_next_questions r ON r.nextQuestionId = q.id
WHERE r.answerId = :answerId
LIMIT 1
"""
    )
    suspend fun getNextQuestionForAnswer(answerId: Int): QuestionEntity?


    // درج تصویرِ پاسخ؛ id تولیدشده را برمی‌گرداند (rowId -> Int)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertImage(entity: AnswerImageEntity): Long

    // حذف بر اساس id
    @Query("DELETE FROM answer_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Int)


    @Query("UPDATE answers SET note = :note WHERE id = :id")
    suspend fun updateAnswerNoteById(id: Int, note: String)


    @Query("SELECT MAX(sortOrder) FROM answers WHERE questionId = :questionId")
    suspend fun getMaxSortOrderForQuestion(questionId: Int): Int?

    @Query("UPDATE answers SET sortOrder = :sortOrder WHERE id = :answerId")
    suspend fun updateAnswerSortOrder(answerId: Int, sortOrder: Int)


    @Query("""
    SELECT * FROM answer_images
    WHERE answerId IN (:answerIds)
    ORDER BY answerId ASC, sortOrder ASC, id ASC
""")
    suspend fun getImagesForAnswerIds(answerIds: List<Int>): List<AnswerImageEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM answers WHERE id = :answerId)")
    suspend fun existsAnswer(answerId: Int): Boolean


    @Query("""
        SELECT p.*
        FROM answer_images p
        LEFT JOIN order_answer_selected_photo s
            ON s.orderId = :orderId AND s.answerId = p.answerId
        WHERE p.answerId = :answerId
        ORDER BY 
            CASE WHEN p.id = s.selectedPhotoId THEN 0 ELSE 1 END,
            p.sortOrder ASC
    """)
    fun photosForReviewFlow(orderId: Int, answerId: Int): Flow<List<AnswerImageEntity>>





    @Query("SELECT * FROM answer_images WHERE id = :id LIMIT 1")
    suspend fun getAnswerImageById(id: Int): AnswerImageEntity?

    @Query("SELECT * FROM answer_images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: Int): AnswerImageEntity?

    @Query("SELECT fileName FROM answer_images WHERE id = :imageId ")
    suspend fun getImageFileNameById(imageId: Int): String?


    @Query("""
    SELECT id FROM answer_images
    WHERE answerId = :answerId
    ORDER BY sortOrder ASC
    LIMIT 1
""")
    suspend fun getFirstImageIdForAnswer(answerId: Int): Int?




}