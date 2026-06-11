package com.example.solarShop.data.sync

import com.example.solarShop.data.repository.sync.SyncRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend fun getOrCreateDeviceId(): String {
        val existing = syncRepository.getDeviceId()

        if (!existing.isNullOrBlank()) {
            return existing
        }

        val newDeviceId = UUID.randomUUID().toString()
        syncRepository.saveDeviceId(newDeviceId)

        return newDeviceId
    }
}