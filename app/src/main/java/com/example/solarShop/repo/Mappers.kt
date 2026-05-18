package com.example.solarShop.repo

import com.example.solarShop.data.network.dto.EntitlementDto
import com.example.solarShop.data.room.tables.premiumEntitlementCache.PremiumEntitlementCacheEntity


//fun UserDto.toEntity(): UserEntity =
//    UserEntity(id = id, mobilePhone = phone, createdAt = createdAt)

fun EntitlementDto?.toEntity(userId: Int): PremiumEntitlementCacheEntity =
    PremiumEntitlementCacheEntity(
        userId = userId,
        isActive = this?.isActive == true,
        plan = this?.plan,
        expiresAt = this?.expiresAt
    )
