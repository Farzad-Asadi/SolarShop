package com.example.solarShop.data.room.tables.client

import kotlinx.coroutines.flow.Flow

interface ClientRepository {

    // =========================================================
    // Local CRUD
    // =========================================================

    suspend fun insertClient(
        clientEntity: ClientEntity
    ): Long

    /*
     * نام متد برای سازگاری UI فعلی حفظ شده،
     * ولی از این به بعد soft delete انجام می‌دهد.
     */
    suspend fun deleteClient(
        clientEntity: ClientEntity
    ): Int

    suspend fun updateClient(
        clientEntity: ClientEntity
    ): Int

    suspend fun getClientById(
        clientId: Int
    ): ClientEntity?

    // =========================================================
    // UI Flows
    // =========================================================

    fun observeAllClients(): Flow<List<ClientEntity>>

    fun observeClientById(
        clientId: Int
    ): Flow<ClientEntity?>

    fun observeAllClientOfUser(
        userKey: String
    ): Flow<List<ClientEntity>>

    fun observeClientWithOrders(): Flow<List<ClientWithOrders>>

    fun observeClientsWithOrdersByUserId(
        userKey: String
    ): Flow<List<ClientWithOrders>>

    // =========================================================
    // Sync
    // =========================================================

    suspend fun getClientByUid(
        uid: String
    ): ClientEntity?

    suspend fun getClientIdByUid(
        uid: String
    ): Int?

    suspend fun getUnsyncedClients(): List<ClientEntity>

    suspend fun markClientsSynced(
        uids: List<String>
    )

    suspend fun upsertClientByUid(
        clientEntity: ClientEntity
    ): Long

    suspend fun softDeleteByUid(
        uid: String
    ): Int
}