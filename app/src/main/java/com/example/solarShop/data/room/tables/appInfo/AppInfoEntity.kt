package com.example.solarShop.data.room.tables.appInfo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey
    val id : Int = 1,
    val currentUserId :Int?=null,
    val selectedClientId :Int?=null,
    val selectedOrderId :Int?=null,
    val selectedQuestionId :Int?=null,

    )
