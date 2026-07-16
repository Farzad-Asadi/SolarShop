package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceItemSyncDto(
    val uid: String,

    /*
     * ارتباط با سند بر اساس UID پایدار.
     */
    val invoiceUid: String,

    val rowIndex: Int,

    val description: String,

    val quantity: Double,
    val unit: String? = null,

    val unitPrice: Long,

    val rowDiscount: Long,

    val rowSubtotal: Long,

    val taxPercent: Float? = null,
    val taxAmount: Long,

    val rowTotal: Long,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    val createdByUserId: Int? = null,
    val updatedByUserId: Int? = null,

    val shopUid: String? = null
)