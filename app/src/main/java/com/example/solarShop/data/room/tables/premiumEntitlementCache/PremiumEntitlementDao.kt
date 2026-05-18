package com.example.solarShop.data.room.tables.premiumEntitlementCache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PremiumEntitlementDao {

    @Upsert
    suspend fun upsert(cache: PremiumEntitlementCacheEntity)

    @Query("SELECT * FROM premium_entitlement_cache WHERE userId = :userId LIMIT 1")
    suspend fun getForUser(userId: Int): PremiumEntitlementCacheEntity?

    @Query("SELECT * FROM premium_entitlement_cache WHERE userId = :userId LIMIT 1")
    fun observeForUser(userId: Int): Flow<PremiumEntitlementCacheEntity?>

    @Query("DELETE FROM premium_entitlement_cache WHERE userId = :userId")
    suspend fun deleteForUser(userId: Int)

    @Query("DELETE FROM premium_entitlement_cache")
    suspend fun clearAll()
}