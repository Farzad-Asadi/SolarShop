package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class ProductUnitSyncDto(
    val uid: String,
    val name: String,
    val symbol: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)