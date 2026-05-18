package com.example.solarShop.data.room.tables.orderAll.orderTimelineItem

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity

@Entity(
    tableName = "time_line_item_entity",
    indices = [Index("orderId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimelineItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int?=null,
    val orderId:Int?=null,
    val title: String,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val completed: Boolean
    )


@Entity(
    tableName = "order_timeline_suggestions",
    indices = [Index(value = ["orderId"], unique = true)]
)
data class OrderTimelineSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val orderId: Int,
    val systemKey: String,           // به کدام مرحله پیشنهاد می‌دهد
    val message: String,             // متن اسنک‌بار
    val createdAt: Long = System.currentTimeMillis(),
    val consumed: Boolean = false,   // وقتی کاربر action/dismiss کرد true می‌شود

    // برای کاتالوگ/مواردی که وابسته به عدد است (اختیاری)
    val metaInt: Int? = null
)