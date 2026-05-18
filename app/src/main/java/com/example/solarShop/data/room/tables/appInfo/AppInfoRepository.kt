package com.example.solarShop.data.room.tables.appInfo

import kotlinx.coroutines.flow.Flow


interface AppInfoRepository {

    suspend fun insertAppInfo(appInfoEntity: AppInfoEntity):Long
    suspend fun deleteAppInfo(appInfoEntity: AppInfoEntity): Int
    suspend fun updateAppInfo(appInfoEntity: AppInfoEntity): Int

    suspend fun setCurrentUserId(userId: Int?)
    suspend fun setSelectedClientId(clientId: Int?)
    suspend fun setSelectedOrderId(orderId: Int?)

    //Flow
    fun observeAppInfo() : Flow<AppInfoEntity?>




}