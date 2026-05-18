package com.example.solarShop.data.room.tables.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity):Long

    @Delete
    suspend fun deleteUser(userEntity: UserEntity): Int

    @Update
    suspend fun updateUser(userEntity: UserEntity): Int

    @Query("SELECT * FROM users WHERE id=:userId LIMIT 1")
    suspend fun getUserById(userId:Int) : UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): UserEntity?


    @Query("SELECT * FROM users WHERE mobilePhone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearAll()



    //Flow

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers() : Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id=:userId")
    fun observeUserById(userId:Int) : Flow<UserEntity?>

    @Transaction
    @Query("SELECT * FROM users")
    fun observeUserWithClients(): Flow<List<UserWithClients>>

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserWithClientsByUserId(userId: Int): Flow<UserWithClients?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: Int): Flow<UserEntity?>


}