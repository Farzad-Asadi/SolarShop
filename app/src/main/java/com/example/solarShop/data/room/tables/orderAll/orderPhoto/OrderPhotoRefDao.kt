package com.example.solarShop.data.room.tables.orderAll.orderPhoto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


@Dao
interface OrderPhotoRefDao {


    @Query("DELETE FROM order_photo_ref WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE order_photo_ref SET is_pinned = :isPinned, local_copy_path = :localCopyPath WHERE id = :id")
    suspend fun updatePin(id: Int, isPinned: Boolean, localCopyPath: String?)

    @Query("SELECT * FROM order_photo_ref WHERE order_id = :orderId ORDER BY created_at DESC")
    fun observeByOrder(orderId: Int): Flow<List<OrderPhotoRefEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: OrderPhotoRefEntity): Long

    @Query("DELETE FROM order_photo_ref WHERE id IN (:ids)")
    suspend fun deleteMany(ids: List<Int>)

    @Query("UPDATE order_photo_ref SET is_pinned = :isPinned, local_copy_path = :localCopyPath WHERE id IN (:ids)")
    suspend fun updatePinMany(ids: List<Int>, isPinned: Boolean, localCopyPath: String?)

    @Query("UPDATE order_photo_ref SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Int, note: String)

    @Query("SELECT local_copy_path FROM order_photo_ref WHERE id = :id LIMIT 1")
    suspend fun getLocalCopyPath(id: Int): String?

    @Query("SELECT id, local_copy_path FROM order_photo_ref WHERE id IN (:ids)")
    suspend fun getLocalCopyPaths(ids: List<Int>): List<IdPath>

    @Query("SELECT id FROM order_photo_ref WHERE order_id = :orderId")
    suspend fun photoIdsOnceForOrder(orderId: Int): List<Int>

    @Query("DELETE FROM order_photo_ref WHERE order_id = :orderId")
    suspend fun deletePhotosByOrderId(orderId: Int)


}


@Dao
interface OrderPhotoMetaDao {

    @Query("SELECT cover_photo_ref_id FROM order_photo_meta WHERE order_id = :orderId LIMIT 1")
    fun observeCover(orderId: Int): kotlinx.coroutines.flow.Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: OrderPhotoMetaEntity)

    @Query("UPDATE order_photo_meta SET cover_photo_ref_id = :photoId WHERE order_id = :orderId")
    suspend fun setCover(orderId: Int, photoId: Int?)

    // اختیاری: اگر ردیفی وجود نداشت، بساز
    @Transaction
    suspend fun ensureAndSetCover(orderId: Int, photoId: Int?) {
        // با REPLACE عملاً upsert می‌شود
        upsert(OrderPhotoMetaEntity(orderId = orderId, coverPhotoRefId = photoId))
    }
}




data class IdPath(
    val id: Int,
    val local_copy_path: String?
)
