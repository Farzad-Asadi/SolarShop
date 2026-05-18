package com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderAnswerSelectedPhotoDao {

    @Upsert
    suspend fun upsert(entity: OrderAnswerSelectedPhotoEntity)

    @Query("""
        SELECT selectedPhotoId 
        FROM order_answer_selected_photo
        WHERE orderId = :orderId AND answerId = :answerId
        LIMIT 1
    """)
    fun selectedPhotoIdFlow(orderId: Int, answerId: Int): Flow<Int?>

    @Query("""
        DELETE FROM order_answer_selected_photo
        WHERE orderId = :orderId AND answerId = :answerId
    """)
    suspend fun clearSelection(orderId: Int, answerId: Int)

    @Query("""
    SELECT * FROM order_answer_selected_photo
    WHERE orderId = :orderId
""")
    fun observeAllForOrder(orderId: Int): Flow<List<OrderAnswerSelectedPhotoEntity>>

}
