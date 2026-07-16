package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceDocumentSyncDto(
    val uid: String,

    /*
     * ارتباط با سفارش بر اساس UID پایدار.
     */
    val orderUid: String,

    /*
     * PROFORMA یا INVOICE
     */
    val type: String,

    val number: String,

    val createdAt: Long,
    val updatedAt: Long,

    /*
     * DRAFT یا سایر وضعیت‌های فعلی پروژه
     */
    val status: String,

    // ---------- Snapshot فروشنده ----------

    val sellerLabel: String? = null,
    val sellerName: String,
    val sellerPhone: String? = null,
    val sellerAddress: String? = null,
    val sellerNationalId: String? = null,
    val sellerEconomicCode: String? = null,

    // ---------- Snapshot خریدار ----------

    val buyerLabel: String? = null,
    val buyerName: String,
    val buyerPhone: String? = null,
    val buyerAddress: String? = null,
    val buyerNationalId: String? = null,

    // ---------- مبالغ سند ----------

    val subtotalBeforeDiscount: Long,
    val totalDiscount: Long,
    val totalBeforeTax: Long,
    val totalTax: Long,
    val totalFinal: Long,

    val notes: String? = null,

    val deletedAt: Long? = null,

    val createdByUserId: Int? = null,
    val updatedByUserId: Int? = null,

    val shopUid: String? = null
)