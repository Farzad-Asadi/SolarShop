package com.example.solarShop.data.room.tables.orderAll.order

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.PENDING
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemEntity
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateEntity
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceEntity
import java.util.UUID

@Entity(
    tableName = "orders",
    indices = [
        Index("clientId"),

        Index(value = ["uid"], unique = true),
        Index("updatedAt"),
        Index("deletedAt"),
        Index("isSynced")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val clientId: Int,

    val name: String? = null,
    val note: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val status: String = PENDING,
    val archive: Boolean = false,

    // ---------- Sync ----------

    @ColumnInfo(defaultValue = "''")
    val uid: String = UUID.randomUUID().toString(),

    val deletedAt: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false,

    val createdByUserId: Int? = null,
    val updatedByUserId: Int? = null,

    val shopUid: String? = null
)


data class OrderWithPriceEstimate(
    @Embedded
    val orderEntity: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderId",
        entity = PriceEstimateEntity::class
    )
    val priceEstimates: PriceEstimateEntity?
)



data class OrderWithSelectedChoice(
    @Embedded
    val orderEntity: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderId",
        entity = SelectedChoiceEntity::class
    )
    val selectedChoice: SelectedChoiceEntity?
)


data class OrderWithTimelineItem(
    @Embedded
    val orderEntity: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderId",
    )
    val timelineItemEntity: List<TimelineItemEntity>
)
