package com.example.solarShop.data.remote.currency

import kotlinx.serialization.Serializable

@Serializable
data class BrsMarketResponseDto(
    val gold: List<BrsCurrencyItemDto> = emptyList(),
    val currency: List<BrsCurrencyItemDto> = emptyList()
)

@Serializable
data class BrsCurrencyItemDto(
    val date: String? = null,
    val time: String? = null,
    val symbol: String? = null,
    val name: String? = null,
    val price: Long? = null,
    val unit: String? = null
)