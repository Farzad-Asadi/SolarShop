package com.example.solarShop.data.room.tables.client

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClient(client: ClientEntity):Long

    @Delete
    suspend fun deleteClient(client: ClientEntity): Int

    @Update
    suspend fun updateClient(client: ClientEntity): Int

    @Query("SELECT * FROM clients WHERE id=:clientId")
    suspend fun getClientById(clientId:Int) : ClientEntity?



    //Flow

    @Query("SELECT * FROM clients ")
    fun observeAllClients() : Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id=:clientId")
    fun observeClientById(clientId:Int) : Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE userKey=:userKey")
    fun observeAllClientOfUser(userKey:String) : Flow<List<ClientEntity>>


    //Relation

    @Transaction
    @Query("SELECT * FROM clients")
    fun observeClientWithOrders(): Flow<List<ClientWithOrders>>

    @Transaction
    @Query("SELECT * FROM clients WHERE userKey=:userKey ORDER BY createdAt DESC")
    fun observeClientsWithOrdersByUserId(userKey:String): Flow<List<ClientWithOrders>>




}