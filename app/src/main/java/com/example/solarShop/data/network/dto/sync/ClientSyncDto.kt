package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class ClientSyncDto(
    val uid: String,

    val name: String,
    val mobilePhone: String,
    val landlinePhone: String,
    val nationalCode: String,
    val workshop: String,
    val address: String,
    val note: String? = null,

    val archive: Boolean = false,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    /*
     * در Push ممکن است null باشند.
     * سرور آن‌ها را از JWT تعیین می‌کند.
     */
    val createdByUserId: Int? = null,
    val updatedByUserId: Int? = null,

    /*
     * در اولین Push می‌تواند null باشد.
     * سرور فروشگاه کاربر را تعیین می‌کند.
     */
    val shopUid: String? = null
)