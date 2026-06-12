package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncStatusDto(
    val serverTime: Long,
    val serverVersion: Int,
    val message: String
)