package com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep


import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.UserWorkflowStepEntity

@Entity(
    tableName = "order_workflow_steps",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserWorkflowStepEntity::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["orderId", "stepId"], unique = true), // 👈 هر (order, step) فقط یک ردیف
        Index("stepId")
    ]
)
data class OrderWorkflowStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val orderId: Int,
    val stepId: Int,
    val completed: Boolean = false,
    val completedAt: Long? = null
)




data class OrderWithWorkflowSteps(
    @Embedded
    val order: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderId",
        entity = OrderWorkflowStepEntity::class
    )
    val workflowSteps: List<OrderWorkflowStepEntity>
)
