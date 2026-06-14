package com.example.solarShop.data.local.entity.attribute

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_attribute_values",
    indices = [
        Index("productId"),
        Index("attributeDefinitionId"),
        Index(value = ["productId", "attributeDefinitionId"], unique = true)
    ]
)
data class ProductAttributeValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,
    val attributeDefinitionId: Int,

    // همه‌چیز فعلاً به صورت String ذخیره می‌شود
    val valueText: String = "",

    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isSynced: Boolean = false
)
