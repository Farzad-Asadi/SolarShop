package com.example.solarShop.data.room.tables.selectedChoice

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "selected_choices",
    indices = [
        Index(value = ["orderId", "questionId"], unique = true) // ✅ نه فقط orderId
    ]
)
data class SelectedChoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int?=null,
    val orderId:Int?=null,
    val questionId:Int?=null,
    val answerId:Int?=null,
    val choiceDescription:String?=null,
    val createdAt: Long = System.currentTimeMillis()



    )