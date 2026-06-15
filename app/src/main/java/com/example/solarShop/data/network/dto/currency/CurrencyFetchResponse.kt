package com.example.solarShop.data.network.dto.currency

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyFetchResponse(
    val ok: Boolean,
    val rateToman: Long? = null,
    val message: String? = null
)