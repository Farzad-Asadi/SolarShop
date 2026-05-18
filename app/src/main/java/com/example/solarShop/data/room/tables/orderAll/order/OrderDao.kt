package com.example.solarShop.data.room.tables.orderAll.order

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.solarShop.ui.orderScreen.orderCosts.OrderMini
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrder(orderEntity: OrderEntity):Long

    @Delete
    suspend fun deleteOrder(orderEntity: OrderEntity): Int

    @Update
    suspend fun updateOrder(orderEntity: OrderEntity): Int

    @Query("UPDATE orders SET name = :name WHERE id = :orderId")
    suspend fun updateOrderName(orderId: Int, name: String): Int


    @Query("SELECT * FROM `orders` WHERE id=:orderId")
    suspend fun getOrderById(orderId:Int) : OrderEntity?


    //Flow

    @Query("SELECT * FROM `orders`")
    fun observeAllOrders() : Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id=:orderId")
    fun observeOrderById(orderId:Int) : Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE clientId=:clientId ORDER BY updatedAt DESC")
    fun observeAllOrderOfClient(clientId:Int) : Flow<List<OrderEntity>>




    //Relation

    @Transaction
    @Query("SELECT * FROM `orders` WHERE id=:orderId")
    fun observeOrderWithPriceEstimates(orderId:Int): Flow<OrderWithPriceEstimate?>


    @Transaction
    @Query("SELECT * FROM `orders` WHERE id=:orderId")
    fun observeOrderWithSelectedChoice(orderId:Int): Flow<OrderWithSelectedChoice?>

    @Transaction
    @Query("SELECT * FROM `orders` WHERE id=:orderId")
    fun observeOrderWithTimelineItems(orderId:Int): Flow<OrderWithTimelineItem?>

    @Query("SELECT clientId FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun clientIdOfOrderOnce(orderId: Int): Int


    //for orderCosts
    @Query("SELECT clientId FROM orders WHERE id = :orderId LIMIT 1")
    fun clientIdOfOrder(orderId: Int): Flow<Int>

    @Query("""
    SELECT id, name
    FROM orders
    WHERE clientId = :clientId
    ORDER BY id DESC
""")
    fun ordersOfClientMini(clientId: Int): Flow<List<OrderMini>>

    @Query("UPDATE orders SET note = :note WHERE id = :orderId")
    suspend fun updateOrderNote(orderId: Int, note: String)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Int)


}