package com.example.solarShop.data.local.entity.pricing

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID


@Entity(
    tableName = "profit_rules",
    indices = [
        Index("categoryId"),
        Index("isDefault")
    ]
)
data class ProfitRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    // null یعنی قانون عمومی
    val categoryId: Int? = null,

    val title: String = "پیش‌فرض",

    val profitPercent: Double = 0.0,
    val fixedProfitToman: Long = 0L,

    val isDefault: Boolean = false,
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)