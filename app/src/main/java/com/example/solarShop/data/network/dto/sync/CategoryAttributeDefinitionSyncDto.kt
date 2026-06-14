package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class CategoryAttributeDefinitionSyncDto(
    val uid: String,
    val categoryUid: String,

    val title: String,
    val key: String,
    val description: String,

    val valueType: String,
    val unit: String?,
    val isRequired: Boolean,
    val sortOrder: Int,
    val enumOptions: String?,

    val isActive: Boolean,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)