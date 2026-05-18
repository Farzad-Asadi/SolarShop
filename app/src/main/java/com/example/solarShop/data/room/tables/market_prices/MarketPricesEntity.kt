package com.example.solarShop.data.room.tables.market_prices

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "market_prices")
data class MarketPricesEntity(
    @PrimaryKey val id: Int = 1,
    val pricePerMeter: Long = 0L, // قیمت هر متر کابینت
    val platePrice: Long = 0L, // قیمت صفحه هر متر
    val installationFeePerMeter: Long = 0L, // اجرت نصب هر متر
    val transportationSuggestion: Long = 0L, // حمل‌ونقل پیشنهادی پیش‌فرض
    val updatedAt: Long = System.currentTimeMillis()
)



@Entity(tableName = "closet_market_defaults")
data class ClosetMarketDefaultsEntity(
    @PrimaryKey val id: Int = 1,           // تک‌ردیفی (singleton)
    val pricePerM2: Long = 0L,             // تومان
    val installFeePerM2: Long = 0L,        // تومان
    val transportation: Long = 0L,         // تومان
    val updatedAt: Long = System.currentTimeMillis()
)