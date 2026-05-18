package com.example.solarShop.data.room.tables.client

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.user.UserEntity

@Entity(
    tableName = "clients",
    indices = [Index("userKey"), Index("archive"), Index(value = ["userKey","archive"])],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userKey"],
            childColumns  = ["userKey"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int?=null,
    val userKey: String,
    val name: String="",
    val mobilePhone: String="",
    val landlinePhone: String="",
    val nationalCode: String="",
    val workshop: String="",
    val address: String="",
    val avatar: String="",
    val note: String? = null,
    val archive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)


data class ClientWithOrders(
    @Embedded val clientEntity: ClientEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "clientId"
    )val orders: List<OrderEntity>
)
