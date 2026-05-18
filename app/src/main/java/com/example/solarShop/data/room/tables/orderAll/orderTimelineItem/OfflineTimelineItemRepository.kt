package com.example.solarShop.data.room.tables.orderAll.orderTimelineItem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject


class OfflineTimelineItemRepository @Inject constructor(
    private val timelineItemDao: TimelineItemDao
) : TimelineItemRepository {
    override suspend fun insertTimelineItem(timelineItemEntity: TimelineItemEntity): Long =
        timelineItemDao.insertTimelineItem(timelineItemEntity)

    override suspend fun deleteTimelineItem(timelineItemEntity: TimelineItemEntity): Int =
        timelineItemDao.deleteTimelineItem(timelineItemEntity)

    override suspend fun updateTimelineItem(timelineItemEntity: TimelineItemEntity): Int =
        timelineItemDao.updateTimelineItem(timelineItemEntity)

    override fun observeTimelineItemsByOrderId(orderId: Int): Flow<List<TimelineItemEntity>> =
        timelineItemDao.observeTimelineItemsByOrderId(orderId).distinctUntilChanged()


    override fun observePending(orderId: Int): Flow<OrderTimelineSuggestionEntity?> =
        timelineItemDao.observePending(orderId)

    override suspend fun upsert(entity: OrderTimelineSuggestionEntity) =
        timelineItemDao.upsert(entity)

    override suspend fun consume(id: Int) =
        timelineItemDao.consume(id)

    override suspend fun clear(orderId: Int) =
        timelineItemDao.clear(orderId)

    override suspend fun postSuggestion(
        orderId: Int,
        systemKey: String,
        message: String,
        metaInt: Int?
    ) {
        timelineItemDao.upsert(
            OrderTimelineSuggestionEntity(
                orderId = orderId,
                systemKey = systemKey,
                message = message,
                metaInt = metaInt,
                consumed = false
            )
        )
    }


}