package com.example.solarShop.data.local.entity.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_categories",
    indices = [Index("uid", unique = true)]
)
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,

    val imageFileName: String? = null,

    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "")
    val uid: String? = null,

    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "NULL")
    val deletedAt: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false
)