package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class ProductAttributeValueSyncDto(
    val uid: String,
    val productUid: String,
    val attributeDefinitionUid: String,

    val valueText: String,

    val updatedAt: Long,
    val deletedAt: Long?
)