package com.example.solarShop.data.room.tables.orderAll.orderTimelineItem

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTimelineItem(timelineItemEntity: TimelineItemEntity):Long

    @Delete
    suspend fun deleteTimelineItem(timelineItemEntity: TimelineItemEntity):Int

    @Update
    suspend fun updateTimelineItem(timelineItemEntity: TimelineItemEntity):Int


    //flow




    @Query("SELECT * FROM time_line_item_entity WHERE orderId =:orderId ")
    fun observeTimelineItemsByOrderId(orderId :Int) : Flow<List<TimelineItemEntity>>











    @Query("SELECT * FROM order_timeline_suggestions WHERE orderId=:orderId AND consumed=0 LIMIT 1")
    fun observePending(orderId: Int): Flow<OrderTimelineSuggestionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OrderTimelineSuggestionEntity)

    @Query("UPDATE order_timeline_suggestions SET consumed=1 WHERE id=:id")
    suspend fun consume(id: Int)

    @Query("DELETE FROM order_timeline_suggestions WHERE orderId=:orderId")
    suspend fun clear(orderId: Int)



}