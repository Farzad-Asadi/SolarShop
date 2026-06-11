package com.example.solarShop.data.repository.sync

import com.example.solarShop.data.local.dao.sync.SyncMetadataDao
import com.example.solarShop.data.local.entity.sync.SyncMetadataEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) : SyncRepository {

    override suspend fun getLastSyncAt(): Long {
        return syncMetadataDao.getValue(KEY_LAST_SYNC_AT)?.toLongOrNull() ?: 0L
    }

    override suspend fun updateLastSyncAt(value: Long) {
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                key = KEY_LAST_SYNC_AT,
                value = value.toString()
            )
        )
    }

    override suspend fun getDeviceId(): String? {
        return syncMetadataDao.getValue(KEY_DEVICE_ID)
    }

    override suspend fun saveDeviceId(deviceId: String) {
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                key = KEY_DEVICE_ID,
                value = deviceId
            )
        )
    }

    private companion object {
        const val KEY_LAST_SYNC_AT = "lastSyncAt"
        const val KEY_DEVICE_ID = "deviceId"
    }
}