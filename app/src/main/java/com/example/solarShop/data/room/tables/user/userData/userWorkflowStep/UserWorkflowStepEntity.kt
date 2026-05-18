package com.example.solarShop.data.room.tables.user.userData.userWorkflowStep

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.userData.userMarketPrices.UserMarketPricesEntity

@Entity(
    tableName = "user_workflow_steps",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userKey"],
            childColumns = ["userKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userKey"),
        Index(value = ["systemKey"], unique = true) // ✅ یکتا برای systemKey
    ]
)
data class UserWorkflowStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val userKey: String?,

    val title: String,

    val weightPercent: Int,
    val sortOrder: Int = 0,
    val isLocked: Boolean = false,
    val systemKey: String? = null
)







data class UserWithWorkflowTemplate(
    @Embedded
    val user: UserEntity,

    @Relation(
        parentColumn = "userKey",
        entityColumn = "userKey",
        entity = UserWorkflowStepEntity::class
    )
    val workflowTemplate: List<UserWorkflowStepEntity>,

    @Relation(
        parentColumn = "userKey",
        entityColumn = "userKey",
        entity = UserMarketPricesEntity::class
    )
    val marketPrices: UserMarketPricesEntity?
)