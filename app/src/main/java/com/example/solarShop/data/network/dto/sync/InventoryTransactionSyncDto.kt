package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class InventoryTransactionSyncDto(
    val uid: String,
    val productUid: String,
    val quantity: Double,
    val transactionType: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)