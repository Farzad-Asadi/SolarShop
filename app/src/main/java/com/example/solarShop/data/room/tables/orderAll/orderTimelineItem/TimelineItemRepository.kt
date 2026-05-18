package com.example.solarShop.data.room.tables.orderAll.orderTimelineItem

import kotlinx.coroutines.flow.Flow


interface TimelineItemRepository {

    suspend fun insertTimelineItem(timelineItemEntity: TimelineItemEntity):Long
    suspend fun deleteTimelineItem(timelineItemEntity: TimelineItemEntity):Int
    suspend fun updateTimelineItem(timelineItemEntity: TimelineItemEntity):Int


    //flow

    fun observeTimelineItemsByOrderId(orderId :Int) : Flow<List<TimelineItemEntity>>



    fun observePending(orderId: Int): Flow<OrderTimelineSuggestionEntity?>
    suspend fun upsert(entity: OrderTimelineSuggestionEntity)
    suspend fun consume(id: Int)
    suspend fun clear(orderId: Int)

    suspend fun postSuggestion(orderId: Int, systemKey: String, message: String, metaInt: Int? = null)

}