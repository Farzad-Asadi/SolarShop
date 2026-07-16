package com.example.solarShop.data.room.tables.orderAll.order

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.solarShop.ui.orderScreen.orderCosts.OrderMini
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    // =========================================================
    // Local CRUD
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrder(
        orderEntity: OrderEntity
    ): Long

    @Update
    suspend fun updateOrder(
        orderEntity: OrderEntity
    ): Int

    @Query(
        """
        UPDATE orders
        SET
            name = :name,
            updatedAt = :updatedAt,
            isSynced = 0
        WHERE id = :orderId
          AND deletedAt IS NULL
        """
    )
    suspend fun updateOrderName(
        orderId: Int,
        name: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE orders
        SET
            note = :note,
            updatedAt = :updatedAt,
            isSynced = 0
        WHERE id = :orderId
          AND deletedAt IS NULL
        """
    )
    suspend fun updateOrderNote(
        orderId: Int,
        note: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        SELECT *
        FROM orders
        WHERE id = :orderId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getOrderById(
        orderId: Int
    ): OrderEntity?

    // =========================================================
    // UID
    // =========================================================

    /*
     * tombstone نیز باید قابل دریافت باشد.
     */
    @Query(
        """
        SELECT *
        FROM orders
        WHERE uid = :uid
        LIMIT 1
        """
    )
    suspend fun getOrderByUid(
        uid: String
    ): OrderEntity?

    @Query(
        """
        SELECT id
        FROM orders
        WHERE uid = :uid
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getOrderIdByUid(
        uid: String
    ): Int?

    // =========================================================
    // UI Flows
    // =========================================================

    @Query(
        """
        SELECT *
        FROM orders
        WHERE deletedAt IS NULL
        ORDER BY updatedAt DESC, createdAt DESC
        """
    )
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query(
        """
        SELECT *
        FROM orders
        WHERE id = :orderId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun observeOrderById(
        orderId: Int
    ): Flow<OrderEntity?>

    @Query(
        """
        SELECT *
        FROM orders
        WHERE clientId = :clientId
          AND deletedAt IS NULL
        ORDER BY updatedAt DESC, createdAt DESC
        """
    )
    fun observeAllOrderOfClient(
        clientId: Int
    ): Flow<List<OrderEntity>>

    // =========================================================
    // Relations
    // =========================================================

    @Transaction
    @Query(
        """
        SELECT *
        FROM orders
        WHERE id = :orderId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun observeOrderWithPriceEstimates(
        orderId: Int
    ): Flow<OrderWithPriceEstimate?>

    @Transaction
    @Query(
        """
        SELECT *
        FROM orders
        WHERE id = :orderId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun observeOrderWithSelectedChoice(
        orderId: Int
    ): Flow<OrderWithSelectedChoice?>

    @Transaction
    @Query(
        """
        SELECT *
        FROM orders
        WHERE id = :orderId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun observeOrderWithTimelineItems(
        orderId: Int
    ): Flow<OrderWithTimelineItem?>

    @Query(
        """
        SELECT clientId
        FROM orders
        WHERE id = :orderId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun clientIdOfOrderOnce(
        orderId: Int
    ): Int

    @Query(
        """
        SELECT clientId
        FROM orders
        WHERE id = :orderId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun clientIdOfOrder(
        orderId: Int
    ): Flow<Int>

    @Query(
        """
        SELECT id, name
        FROM orders
        WHERE clientId = :clientId
          AND deletedAt IS NULL
        ORDER BY updatedAt DESC, id DESC
        """
    )
    fun ordersOfClientMini(
        clientId: Int
    ): Flow<List<OrderMini>>

    // =========================================================
    // Sync
    // =========================================================

    @Query(
        """
    SELECT *
    FROM orders
    WHERE id = :orderId
    LIMIT 1
    """
    )
    suspend fun getOrderByIdForSync(
        orderId: Int
    ): OrderEntity?

    @Query(
        """
    SELECT *
    FROM orders
    ORDER BY updatedAt ASC
    """
    )
    suspend fun getAllOrdersForSync():
            List<OrderEntity>

    @Query(
        """
        SELECT *
        FROM orders
        WHERE isSynced = 0
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getUnsyncedOrders(): List<OrderEntity>

    @Query(
        """
        UPDATE orders
        SET isSynced = 1
        WHERE uid IN (:uids)
        """
    )
    suspend fun markOrdersSynced(
        uids: List<String>
    )

    // =========================================================
    // Soft delete
    // =========================================================

    @Query(
        """
        UPDATE orders
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE id = :orderId
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteById(
        orderId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE orders
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE uid = :uid
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteByUid(
        uid: String,
        deletedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE orders
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE clientId = :clientId
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteByClientId(
        clientId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ): Int
}