package com.example.solarShop.data.room.tables.appInfo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAppInfo(appInfoEntity: AppInfoEntity= AppInfoEntity(id = 1)):Long

    @Delete
    suspend fun deleteAppInfo(appInfoEntity: AppInfoEntity): Int

    @Update
    suspend fun updateAppInfo(appInfoEntity: AppInfoEntity): Int

    @Query("UPDATE app_info SET currentUserId = :userId WHERE id = 1")
    suspend fun setCurrentUserId(userId: Int?)

    @Query("UPDATE app_info SET selectedClientId = :clientId WHERE id = 1")
    suspend fun setSelectedClientId(clientId: Int?)

    @Query("UPDATE app_info SET selectedOrderId = :orderId WHERE id = 1")
    suspend fun setSelectedOrderId(orderId: Int?)




    //Flow

    @Query("SELECT * FROM app_info WHERE id = 1")
    fun observeAppInfo() : Flow<AppInfoEntity?>


}