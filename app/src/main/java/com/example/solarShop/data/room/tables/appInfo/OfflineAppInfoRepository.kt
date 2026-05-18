package com.example.solarShop.data.room.tables.appInfo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject


class OfflineAppInfoRepository @Inject constructor(
    private val appInfoDao: AppInfoDao
) : AppInfoRepository {


    override suspend fun insertAppInfo(appInfoEntity: AppInfoEntity):Long =
        appInfoDao.insertAppInfo(appInfoEntity)
    override suspend fun deleteAppInfo(appInfoEntity: AppInfoEntity):Int =
        appInfoDao.deleteAppInfo(appInfoEntity)
    override suspend fun updateAppInfo(appInfoEntity: AppInfoEntity):Int =
        appInfoDao.updateAppInfo(appInfoEntity)
    override suspend fun setCurrentUserId(userId: Int?)
    = appInfoDao.setCurrentUserId(userId)
    override suspend fun setSelectedClientId(clientId: Int?)
    = appInfoDao.setSelectedClientId(clientId)
    override suspend fun setSelectedOrderId(orderId: Int?)
    = appInfoDao.setSelectedOrderId(orderId)


    //Flow
    override fun observeAppInfo() : Flow<AppInfoEntity?> =
        appInfoDao.observeAppInfo().distinctUntilChanged()

}