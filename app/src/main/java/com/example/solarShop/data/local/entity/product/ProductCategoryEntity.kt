package com.example.solarShop.data.local.entity.product

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_categories",
    indices = [Index("name", unique = true)]
)
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,

    val imageFileName: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)