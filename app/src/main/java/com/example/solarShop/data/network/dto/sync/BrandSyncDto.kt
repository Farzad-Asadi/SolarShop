package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class BrandSyncDto(
    val uid: String,
    val name: String,
    val description: String,
    val imageFileName: String?,
    val isActive: Boolean,
    val updatedAt: Long,
    val deletedAt: Long?
)