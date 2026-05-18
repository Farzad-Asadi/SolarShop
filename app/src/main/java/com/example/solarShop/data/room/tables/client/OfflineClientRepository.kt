package com.example.solarShop.data.room.tables.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject


class OfflineClientRepository @Inject constructor(
    private val clientDao: ClientDao
) : ClientRepository {


    override suspend fun insertClient(clientEntity: ClientEntity):Long =
        clientDao.insertClient(clientEntity)
    override suspend fun deleteClient(clientEntity: ClientEntity):Int =
        clientDao.deleteClient(clientEntity)
    override suspend fun updateClient(clientEntity: ClientEntity):Int =
        clientDao.updateClient(clientEntity)

    override suspend fun getClientById(clientId: Int): ClientEntity? =
        clientDao.getClientById(clientId)

    override fun observeAllClients(): Flow<List<ClientEntity>> =
        clientDao.observeAllClients().distinctUntilChanged()

    override fun observeClientById(clientId: Int): Flow<ClientEntity?> =
        clientDao.observeClientById(clientId).distinctUntilChanged()

    override fun observeAllClientOfUser(userKey:String): Flow<List<ClientEntity>> =
        clientDao.observeAllClientOfUser(userKey).distinctUntilChanged()

    override fun observeClientWithOrders(): Flow<List<ClientWithOrders>> =
        clientDao.observeClientWithOrders().distinctUntilChanged()

    override fun observeClientsWithOrdersByUserId(userKey:String): Flow<List<ClientWithOrders>> =
        clientDao.observeClientsWithOrdersByUserId(userKey)


}