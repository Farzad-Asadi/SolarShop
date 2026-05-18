package com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "order_answer_selected_photo",
    primaryKeys = ["orderId", "answerId"],
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["answerId"]),
        Index(value = ["selectedPhotoId"])
    ]
)
data class OrderAnswerSelectedPhotoEntity(
    val orderId: Int,
    val questionId: Int,     // برای دیباگ/گزارش و راحتی
    val answerId: Int,
    val selectedPhotoId: Int, // photoId (ترجیحاً stable)
    val updatedAt: Long = System.currentTimeMillis()
)
