package com.example.solarShop.data.repository.sync

interface SyncRepository {

    suspend fun getLastSyncAt(): Long

    suspend fun updateLastSyncAt(value: Long)

    /*
 * Cursor مستقل برای هر موجودیت.
 * نبودن کلید یعنی اولین Pull باید از صفر انجام شود.
 */
    suspend fun getEntityLastSyncAt(
        entityKey: String
    ): Long

    suspend fun updateEntityLastSyncAt(
        entityKey: String,
        value: Long
    )

    suspend fun getDeviceId(): String?

    suspend fun saveDeviceId(deviceId: String)
}