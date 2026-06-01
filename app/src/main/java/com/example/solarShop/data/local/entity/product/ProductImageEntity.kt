package com.example.solarShop.data.local.entity.product

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_images",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productId")
    ]
)
data class ProductImageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val productId: Int,

    val fileName: String,

    val createdAt: Long,

    val sortOrder: Int
)