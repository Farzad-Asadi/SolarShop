package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class OrderSyncDto(
    val uid: String,

    /*
     * ارتباط با مشتری بر اساس UID پایدار،
     * نه clientId محلی دستگاه.
     */
    val clientUid: String,

    val name: String? = null,
    val note: String? = null,

    val status: String,
    val archive: Boolean = false,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    val createdByUserId: Int? = null,
    val updatedByUserId: Int? = null,

    val shopUid: String? = null
)