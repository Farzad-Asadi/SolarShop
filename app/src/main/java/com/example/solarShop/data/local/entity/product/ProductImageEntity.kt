package com.example.solarShop.data.local.entity.product

import androidx.room.ColumnInfo
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


    @ColumnInfo(defaultValue = "")
    val uid: String? = null,

    val productId: Int,

    val fileName: String,

    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),

    val sortOrder: Int,

    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "NULL")
    val deletedAt: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false
)