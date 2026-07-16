package com.example.solarShop.data.room.tables.client

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    // =========================================================
    // Local CRUD
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClient(
        client: ClientEntity
    ): Long

    @Update
    suspend fun updateClient(
        client: ClientEntity
    ): Int

    @Query(
        """
        SELECT *
        FROM clients
        WHERE id = :clientId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getClientById(
        clientId: Int
    ): ClientEntity?

    // =========================================================
    // UID
    // =========================================================

    /*
     * عمداً deletedAt فیلتر نشده؛ هنگام Sync باید tombstone را هم پیدا کنیم.
     */
    @Query(
        """
        SELECT *
        FROM clients
        WHERE uid = :uid
        LIMIT 1
        """
    )
    suspend fun getClientByUid(
        uid: String
    ): ClientEntity?

    @Query(
        """
        SELECT id
        FROM clients
        WHERE uid = :uid
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getClientIdByUid(
        uid: String
    ): Int?

    // =========================================================
    // UI Flows
    // =========================================================

    @Query(
        """
        SELECT *
        FROM clients
        WHERE deletedAt IS NULL
        ORDER BY updatedAt DESC, createdAt DESC
        """
    )
    fun observeAllClients(): Flow<List<ClientEntity>>

    @Query(
        """
        SELECT *
        FROM clients
        WHERE id = :clientId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun observeClientById(
        clientId: Int
    ): Flow<ClientEntity?>

    /*
     * فعلاً برای سازگاری نگه داشته می‌شود.
     * Repository برای اشتراکی‌شدن مشتری‌ها از observeAllClients استفاده می‌کند.
     */
    @Query(
        """
        SELECT *
        FROM clients
        WHERE userKey = :userKey
          AND deletedAt IS NULL
        ORDER BY updatedAt DESC, createdAt DESC
        """
    )
    fun observeAllClientOfUser(
        userKey: String
    ): Flow<List<ClientEntity>>

    // =========================================================
    // Sync
    // =========================================================

    /*
     * رکورد حذف‌شده هم باید ارسال شود.
     */
    @Query(
        """
        SELECT *
        FROM clients
        WHERE isSynced = 0
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getUnsyncedClients(): List<ClientEntity>

    @Query(
        """
        UPDATE clients
        SET isSynced = 1
        WHERE uid IN (:uids)
        """
    )
    suspend fun markClientsSynced(
        uids: List<String>
    )

    @Query(
        """
    SELECT *
    FROM clients
    WHERE id = :clientId
    LIMIT 1
    """
    )
    suspend fun getClientByIdForSync(
        clientId: Int
    ): ClientEntity?

    @Query(
        """
    SELECT *
    FROM clients
    ORDER BY updatedAt ASC
    """
    )
    suspend fun getAllClientsForSync():
            List<ClientEntity>

    // =========================================================
    // Soft delete
    // =========================================================

    @Query(
        """
        UPDATE clients
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE id = :clientId
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteById(
        clientId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE clients
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE uid = :uid
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteByUid(
        uid: String,
        deletedAt: Long = System.currentTimeMillis()
    ): Int
}