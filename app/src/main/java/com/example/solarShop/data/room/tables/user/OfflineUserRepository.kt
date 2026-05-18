package com.example.solarShop.data.room.tables.user

import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.network.dto.UserDto
import com.example.solarShop.data.network.remote.UserApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import javax.inject.Inject


class OfflineUserRepository @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val session: SessionDataStore
) : UserRepository {


    override suspend fun insertUser(userEntity: UserEntity):Long =
        userDao.insertUser(userEntity)
    override suspend fun deleteUser(userEntity: UserEntity):Int=
        userDao.deleteUser(userEntity)
    override suspend fun updateUser(userEntity: UserEntity):Int =
        userDao.updateUser(userEntity)
    override suspend fun getUserById(userId: Int): UserEntity? =
        userDao.getUserById(userId)

    override suspend fun upsert(user: UserEntity) =
        userDao.upsert(user)

    override suspend fun upsertAll(users: List<UserEntity>) =
            userDao.upsertAll(users)

    override suspend fun getById(id: Int): UserEntity? =
                userDao.getById(id)



    override suspend fun getByPhone(phone: String): UserEntity? =
            userDao.getByPhone(phone)

    override suspend fun clearAll() =
                userDao.clearAll()


    override suspend fun upsertFromRemote(dto: UserDto) {
        val now = System.currentTimeMillis()
        val serverId = dto.id          // همون idی که موک می‌دهد

        // ۱) ببین چنین یوزری قبلاً هست یا نه
        val existing = userDao.getById(serverId)

        val entity = // کاربر قدیمی → userKey قبلی را نگه دار
            existing?.copy(
                mobilePhone    = dto.phone,
                updatedAt = now
            )
                ?: // کاربر جدید → userKey تازه
                UserEntity(
                    id       = serverId,
                    mobilePhone    = dto.phone,
                    name     = dto.name ?:"",        // اگر داری
                    userKey  = UUID.randomUUID().toString(),
                    createdAt = dto.createdAt ?: now,
                    updatedAt = now
                )

        if (existing == null) {
            userDao.insertUser(entity)
        } else {
            userDao.updateUser(entity)
        }
    }



    override suspend fun syncMe(): Result<Unit> = runCatching {
//        val dto = api.getMe()   // UserDto از سرور
//
//        // اینجا فرض می‌کنیم dto.id = id سرور
//        val serverId = dto.id
//
//        val now = System.currentTimeMillis()
//
//        // ⬅️ ۱) از دیتابیس ببین قبلاً چنین یوزری داشتیم یا نه
//        val existing = userDao.getById(serverId)
//
//        val entity = if (existing == null) {
//            // ⬅️ ۲) کاربر جدید → userKey تازه بساز
//            UserEntity(
//                id = serverId,
//                mobilePhone = dto.phone,
//                name = dto.name,
//                userKey = UUID.randomUUID().toString(),  // فقط اینجا
//                createdAt = dto.createdAt ?: now,
//                updatedAt = now
//            )
//        } else {
//            // ⬅️ ۳) کاربر موجود → userKey قبلی رو نگه دار، بقیه رو آپدیت کن
//            existing.copy(
//                mobilePhone = dto.phone,
//                name = dto.name,
//                updatedAt = now
//                // createdAt و userKey رو دست نمی‌زنیم
//            )
//        }
//
//        if (existing == null) {
//            userDao.insertUser(entity)
//        } else {
//            userDao.updateUser(entity)
//        }

        // (اختیاری) اگر هنوز از AppInfo استفاده می‌کنی:
        // appInfoDao.setCurrentUserId(serverId)
    }

    override fun observeUser(userId: Int): Flow<UserEntity?> =
        userDao.observeById(userId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<UserEntity?> =
        session.currentUserIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(null) else userDao.observeById(id)
        }



    //Flow
    override fun observeAllUsers(): Flow<List<UserEntity>> =
        userDao.observeAllUsers().distinctUntilChanged()
    override fun observeUserById(userId: Int): Flow<UserEntity?> =
        userDao.observeUserById(userId).distinctUntilChanged()
    override fun observeUserWithClients(): Flow<List<UserWithClients>> =
        userDao.observeUserWithClients().distinctUntilChanged()
    override fun observeUserWithClientsByUserId(userId: Int): Flow<UserWithClients?> =
        userDao.observeUserWithClientsByUserId(userId).distinctUntilChanged()
    override fun observeById(id: Int): Flow<UserEntity?> =
        userDao.observeById(id)


}