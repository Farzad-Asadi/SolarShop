package com.example.solarShop.data.local.dao.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.solarShop.data.local.entity.sync.SyncMetadataEntity

@Dao
interface SyncMetadataDao {

    @Query("SELECT value FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun upsert(item: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE `key` = :key")
    suspend fun delete(key: String)
}