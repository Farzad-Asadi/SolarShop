package com.example.solarShop.data.room.tables.user.userData.userMarketPrices

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.data.room.tables.user.UserEntity

@Entity(
    tableName = "user_market_prices",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userKey"],   // فرض کردم userKey در UserEntity یونیکه
            childColumns = ["userKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userKey", unique = true)   // هر کاربر حداکثر یک رکورد تنظیم قیمت
    ]
)
data class UserMarketPricesEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,                 // شناسه‌ی داخلی جدول (Int)

    val userKey: String,                 // کلید کاربر (همونی که در پروفایل داری)

    val pricePerMeter: Long = 0L,        // قیمت هر متر کابینت
    val platePrice: Long = 0L,           // قیمت صفحه هر متر
    val installationFeePerMeter: Long = 0L, // اجرت نصب هر متر
    val transportationSuggestion: Long = 0L, // حمل‌ونقل پیشنهادی پیش‌فرض
    val updatedAt: Long = System.currentTimeMillis()
)












