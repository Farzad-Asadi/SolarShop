package com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderWorkflowStepDao {

    @Query("""
        SELECT * FROM order_workflow_steps
        WHERE orderId = :orderId
    """)
    fun observeOrderSteps(orderId: Int): Flow<List<OrderWorkflowStepEntity>>

    @Query("""
        SELECT * FROM order_workflow_steps
        WHERE orderId = :orderId
    """)
    suspend fun getOrderSteps(orderId: Int): List<OrderWorkflowStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrderStep(entity: OrderWorkflowStepEntity): Long
    // به‌خاطر unique index روی (orderId, stepId)، این عملاً upsert است

    @Query("""
        DELETE FROM order_workflow_steps
        WHERE orderId = :orderId AND stepId = :stepId
    """)
    suspend fun deleteOrderStep(orderId: Int, stepId: Int)

    @Query("""
        DELETE FROM order_workflow_steps
        WHERE orderId = :orderId
    """)
    suspend fun deleteAllForOrder(orderId: Int)
}
