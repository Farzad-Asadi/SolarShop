package com.example.solarShop.data.room.tables.orderAll.order

import com.example.solarShop.ui.orderScreen.orderCosts.OrderMini
import kotlinx.coroutines.flow.Flow


interface OrderRepository {

    suspend fun insertOrder(orderEntity: OrderEntity):Long
    suspend fun deleteOrder(orderEntity: OrderEntity): Int
    suspend fun updateOrder(orderEntity: OrderEntity): Int
    suspend fun updateOrderName(orderId: Int, name: String): Int
    suspend fun getOrderById(orderId:Int) : OrderEntity?



    //Flow

    fun observeAllOrders() : Flow<List<OrderEntity>>
    fun observeOrderById(orderId:Int) : Flow<OrderEntity?>
    fun observeAllOrderOfClient(clientId:Int) : Flow<List<OrderEntity>>


    //Relation

    fun getOrderWithPriceEstimates(orderId:Int): Flow<OrderWithPriceEstimate?>
    fun getOrderWithSelectedChoice(orderId:Int): Flow<OrderWithSelectedChoice?>
    fun observeOrderWithTimelineItem(orderId:Int): Flow<OrderWithTimelineItem?>


    suspend fun clientIdOfOrderOnce(orderId: Int): Int

    fun clientIdOfOrder(orderId: Int): Flow<Int>
    fun ordersOfClientMini(clientId: Int): Flow<List<OrderMini>>

    suspend fun updateOrderNote(orderId: Int, note: String)


    suspend fun deleteOrderWithChildren(orderId: Int)
}