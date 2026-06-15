package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyRateSyncDto(
    val uid: String,
    val currencyCode: String,
    val rateToman: Long,
    val source: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)