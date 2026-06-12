package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class CategorySyncDto(
    val uid: String,
    val name: String,
    val imageFileName: String?,
    val sortOrder: Int,
    val updatedAt: Long
)