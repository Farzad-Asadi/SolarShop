package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequestDto(
    val deviceId: String,
    val appVersion: Int,
    val platform: String = "android"
)

@Serializable
data class RegisterDeviceResponseDto(
    val accepted: Boolean,
    val serverVersion: Int
)