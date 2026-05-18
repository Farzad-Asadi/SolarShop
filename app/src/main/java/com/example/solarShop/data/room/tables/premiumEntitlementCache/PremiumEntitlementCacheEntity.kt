package com.example.solarShop.data.room.tables.premiumEntitlementCache

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.solarShop.data.room.tables.user.UserEntity

@Entity(
    tableName = "premium_entitlement_cache",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class PremiumEntitlementCacheEntity(
    @PrimaryKey val userId: Int,
    val isActive: Boolean,
    val plan: String?,       // مثل: "pro-monthly" یا null
    val expiresAt: Long?     // millis یا null (برای پلن‌های بدون انقضا)
)