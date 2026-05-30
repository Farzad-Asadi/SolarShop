package com.example.solarShop.data.local.entity.product

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_images",
    indices = [
        Index("productId"),
        Index(value = ["productId", "sortOrder"])
    ]
)
data class ProductImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    val relativePath: String,
    val thumbnailRelativePath: String? = null,

    val sortOrder: Int = 0,

    val createdAt: Long = System.currentTimeMillis()
)