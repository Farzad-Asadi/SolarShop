package com.example.solarShop.data.room.tables.market_prices

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface MarketPricesDao {
    @Query("SELECT * FROM market_prices WHERE id = 1 LIMIT 1")
    fun observe(): Flow<MarketPricesEntity?>


    @Query("SELECT * FROM market_prices WHERE id = 1 LIMIT 1")
    suspend fun getOnce(): MarketPricesEntity?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MarketPricesEntity)


    @Update
    suspend fun update(entity: MarketPricesEntity)
}


@Dao
interface ClosetMarketDefaultsDao {

    @Query("SELECT * FROM closet_market_defaults WHERE id = 1 LIMIT 1")
    suspend fun get(): ClosetMarketDefaultsEntity?

    @Query("SELECT * FROM closet_market_defaults WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ClosetMarketDefaultsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClosetMarketDefaultsEntity)
}