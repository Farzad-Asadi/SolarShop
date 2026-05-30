package com.example.solarShop.data.room.tables.selectedChoice

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedChoiceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity)

    @Delete
    suspend fun deleteSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity)

    @Update
    suspend fun updateSelectedChoice(selectedChoiceEntity: SelectedChoiceEntity)

    @Query("SELECT * FROM selected_choices ")
    suspend fun getAllSelectedChoice() : List<SelectedChoiceEntity>

    @Query("SELECT * FROM selected_choices WHERE orderId =:orderParentId")
    suspend fun getAllSelectedChoiceByOrderParentId(orderParentId:Int) : List<SelectedChoiceEntity>


    @Query("""
        UPDATE selected_choices
        SET choiceDescription = :note, answerId = :answerId
        WHERE orderId = :orderId AND questionId = :questionId
    """)
    suspend fun updateNote(orderId: Int, questionId: Int, answerId: Int, note: String): Int // تعداد ردیف‌های آپدیت‌شده

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChoice(entity: SelectedChoiceEntity): Long


    // 2) حذف یک انتخاب مشخص
    @Query(
        """
        DELETE FROM selected_choices
        WHERE orderId = :orderId AND questionId = :questionId AND answerId = :answerId
    """
    )
    suspend fun deleteSelection(orderId: Int, questionId: Int, answerId: Int): Int



    @Transaction
    suspend fun insertSelection(orderId: Int, questionId: Int, answerId: Int) {
        // createdAt را همین‌جا ست می‌کنیم
        val row = SelectedChoiceEntity(
            id = null,
            orderId = orderId,
            questionId = questionId,
            answerId = answerId,
            choiceDescription = ""
        )
        // اگر Upsert داری:
        // upsert(row)
        // در غیر این صورت:
        insertOrReplace(row)
    }





    // جایگزین انتخابِ همین سؤال برای همین سفارش
    @Query("""
        DELETE FROM selected_choices
        WHERE orderId = :orderId AND questionId = :questionId
    """)
    suspend fun clearSelections(orderId: Int, questionId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: SelectedChoiceEntity): Long



    // برای onClickPrevious
    @Query(
        """
        SELECT sc.questionId
        FROM selected_choices sc
        JOIN answer_next_questions r ON r.answerId = sc.answerId
        WHERE sc.orderId = :orderId AND r.nextQuestionId = :currentQuestionId
        LIMIT 1
    """
    )
    suspend fun getPreviousQuestionId(orderId: Int, currentQuestionId: Int): Int?


    @Query("""
        SELECT DISTINCT r.nextQuestionId
        FROM answer_next_questions r
        JOIN answers a ON a.id = r.answerId
        WHERE a.questionId = :questionId
    """)
    suspend fun getDirectChildren(questionId: Int): List<Int>

    @Query("""
        DELETE FROM selected_choices
        WHERE orderId = :orderId AND questionId IN (:questionIds)
    """)
    suspend fun deleteSelectionsForQuestions(orderId: Int, questionIds: List<Int>): Int






    @Query(
        """
        SELECT answerId FROM selected_choices
        WHERE orderId = :orderId AND questionId = :questionId
        LIMIT 1
    """
    )
    suspend fun getSelectedAnswerId(orderId: Int, questionId: Int): Int?

    // جایگزین‌کردن انتخاب همین سؤال: اول پاکِ همین سؤال، بعد insert
    @Query("DELETE FROM selected_choices WHERE orderId = :orderId AND questionId = :questionId")
    suspend fun deleteSelectionByQuestion(orderId: Int, questionId: Int): Int

    @Insert
    suspend fun insert(entity: SelectedChoiceEntity): Long

    // پاک کردن انتخاب‌های همهٔ سؤال‌های «بعد از» این سؤال (خودِ fromQuestionId حذف نمی‌شود)
    @Query("""
        WITH RECURSIVE qtree(id) AS (
            SELECT :fromQuestionId
            UNION
            SELECT r.nextQuestionId
            FROM answer_next_questions r
            JOIN answers a ON a.id = r.answerId
            JOIN qtree t ON a.questionId = t.id
        )
        DELETE FROM selected_choices
        WHERE orderId = :orderId
          AND questionId IN (SELECT id FROM qtree WHERE id != :fromQuestionId)
    """)
    suspend fun deleteSelectionsInDescendants(orderId: Int, fromQuestionId: Int): Int








    //Flow

    @Query(
        """
        SELECT answerId FROM selected_choices
        WHERE orderId = :orderId AND questionId = :questionId
    """
    )
    fun observeSelectedAnswerIds(orderId: Int, questionId: Int): Flow<List<Int>>



     //شمارهٔ ترتیبی پاسخ‌دهی این سؤال در همان order
     @Query("""
SELECT CASE
  WHEN EXISTS (
    SELECT 1 FROM selected_choices
    WHERE orderId = :orderId AND questionId = :questionId
  )
  THEN 1 + (
    SELECT COUNT(DISTINCT sc.questionId)
    FROM selected_choices sc
    WHERE sc.orderId = :orderId
      AND sc.createdAt < (
        SELECT MIN(createdAt)
        FROM selected_choices
        WHERE orderId = :orderId AND questionId = :questionId
      )
  )
  ELSE (
    SELECT COUNT(DISTINCT questionId)
    FROM selected_choices
    WHERE orderId = :orderId
  ) + 1
END
""")
     fun observeAnsweredOrder(orderId: Int, questionId: Int): Flow<Int?>

    // تعداد کل سؤال‌های پاسخ‌داده‌شده در همان order
    @Query("""
        SELECT COUNT(DISTINCT questionId)
        FROM selected_choices
        WHERE orderId = :orderId
    """)
    fun observeTotalAnswered(orderId: Int): Flow<Int?>

    @Query("""
    SELECT * FROM selected_choices
    WHERE orderId = :orderId AND questionId = :questionId
    LIMIT 1
""")
    fun observeChoice(orderId: Int, questionId: Int): Flow<SelectedChoiceEntity?>




}