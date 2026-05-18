package com.example.solarShop.data.room.tables.user

import com.example.solarShop.data.network.dto.UserDto
import kotlinx.coroutines.flow.Flow


interface UserRepository {

    suspend fun insertUser(userEntity: UserEntity):Long
    suspend fun deleteUser(userEntity: UserEntity): Int
    suspend fun updateUser(userEntity: UserEntity): Int
    suspend fun getUserById(userId:Int) : UserEntity?
    suspend fun upsert(user: UserEntity)
    suspend fun upsertAll(users: List<UserEntity>)
    suspend fun getById(id: Int): UserEntity?
    suspend fun getByPhone(phone: String): UserEntity?
    suspend fun clearAll()


    /** سنک پروفایل کاربر جاری از سرور و ذخیره در Room */
    suspend fun syncMe(): Result<Unit> = Result.success(Unit)

    /** مشاهدهٔ کاربر با id مشخص */
    fun observeUser(userId: Int): Flow<UserEntity?>

    /** مشاهدهٔ کاربرِ جاری (بر اساس currentUserIdFlow) */
    fun observeCurrentUser(): Flow<UserEntity?>

    /**
     * آپسرت کاربر بر اساس دادهٔ دریافتی از سرور (UserDto).
     * اگر قبلاً وجود داشته → بروزرسانی؛
     * اگر نه → درج با userKey جدید.
     */
    suspend fun upsertFromRemote(dto: UserDto)


    //Flow
    fun observeAllUsers() : Flow<List<UserEntity>>
    fun observeUserById(userId:Int) : Flow<UserEntity?>
    fun observeUserWithClients(): Flow<List<UserWithClients>>
    fun observeUserWithClientsByUserId(userId: Int): Flow<UserWithClients?>
    fun observeById(id: Int): Flow<UserEntity?>


}