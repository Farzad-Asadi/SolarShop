package com.example.solarShop.data.network.dto.file

import kotlinx.serialization.Serializable

@Serializable
data class FileUploadResponseDto(
    val fileName: String
)
