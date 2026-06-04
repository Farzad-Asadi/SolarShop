package com.example.solarShop.data.local.entity.product

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "products",
    indices = [
        Index("categoryId"),
        Index("brandId"),
        Index("unitId"),
        Index("name"),
        Index("model"),
        Index("isArchived"),
        Index("isDraft")
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val categoryId: Int,
    val brandId: Int? = null,
    val unitId: Int? = null,

    val name: String,
    val model: String = "",
    val description: String = "",

    val isArchived: Boolean = false,
    val isDraft: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)