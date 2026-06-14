package com.example.solarShop.data.local.entity.attribute

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID


@Entity(
    tableName = "category_attribute_definitions",
    indices = [
        Index("categoryId"),
        Index(value = ["categoryId", "key"], unique = true)
    ]
)
data class CategoryAttributeDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val categoryId: Int,

    val title: String,
    val key: String,
    val description: String = "",

    // text, number, boolean, enum
    val valueType: String = "text",

    val unit: String? = null,
    val isRequired: Boolean = false,
    val sortOrder: Int = 0,

    // مثلا: "تک فاز,سه فاز"
    val enumOptions: String? = null,

    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isSynced: Boolean = false
)

