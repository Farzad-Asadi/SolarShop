package com.example.solarShop.data.room.tables.client

import kotlinx.coroutines.flow.Flow


interface ClientRepository {

    suspend fun insertClient(clientEntity: ClientEntity):Long
    suspend fun deleteClient(clientEntity: ClientEntity):Int
    suspend fun updateClient(clientEntity: ClientEntity):Int
    suspend fun getClientById(clientId:Int) : ClientEntity?


    //Flow

    fun observeAllClients() : Flow<List<ClientEntity>>
    fun observeClientById(clientId:Int) : Flow<ClientEntity?>
    fun observeAllClientOfUser(userKey:String) : Flow<List<ClientEntity>>

    fun observeClientWithOrders(): Flow<List<ClientWithOrders>>
    fun observeClientsWithOrdersByUserId(userKey:String): Flow<List<ClientWithOrders>>


}