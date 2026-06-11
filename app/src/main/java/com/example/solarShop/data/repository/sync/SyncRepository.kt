package com.example.solarShop.data.repository.sync

interface SyncRepository {

    suspend fun getLastSyncAt(): Long

    suspend fun updateLastSyncAt(value: Long)

    suspend fun getDeviceId(): String?

    suspend fun saveDeviceId(deviceId: String)
}