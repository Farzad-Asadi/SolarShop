package com.example.solarShop.data.room.tables.user.userData.userMarketPrices

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMarketPricesDao {

    @Query("SELECT * FROM user_market_prices WHERE userKey = :userKey LIMIT 1")
    fun observeUserMarketPrices(userKey: String): Flow<UserMarketPricesEntity?>

    @Query("SELECT * FROM user_market_prices WHERE userKey = :userKey LIMIT 1")
    suspend fun getUserMarketPrices(userKey: String): UserMarketPricesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserMarketPrices(entity: UserMarketPricesEntity): Long
}