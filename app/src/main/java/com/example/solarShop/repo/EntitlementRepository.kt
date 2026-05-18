package com.example.solarShop.repo


import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.network.remote.EntitlementApi
import com.example.solarShop.data.room.tables.premiumEntitlementCache.PremiumEntitlementCacheEntity
import com.example.solarShop.data.room.tables.premiumEntitlementCache.PremiumEntitlementDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface EntitlementRepository {
    /** سنک وضعیت پریمیوم کاربر جاری از سرور و ذخیره در Room + DataStore */
    suspend fun syncForCurrentUser(): Result<PremiumEntitlementCacheEntity?>

    /** مشاهدهٔ وضعیت پریمیوم یک userId */
    fun observe(userId: Int): Flow<PremiumEntitlementCacheEntity?>

    /** مشاهدهٔ وضعیت پریمیوم کاربر جاری */
    fun observeCurrent(): Flow<PremiumEntitlementCacheEntity?>
}

@Singleton
class EntitlementRepositoryImpl @Inject constructor(
    private val entitlementApi: EntitlementApi,
    private val entitlementDao: PremiumEntitlementDao,
    private val session: SessionDataStore
) : EntitlementRepository {

    override suspend fun syncForCurrentUser(): Result<PremiumEntitlementCacheEntity?> = runCatching {
        val uid = session.snapshot().currentUserId ?: return@runCatching null
        val dto = entitlementApi.get()
        val entity = dto.toEntity(uid)
        withContext(Dispatchers.IO) {
            entitlementDao.upsert(entity)
            session.setIsPremium(entity.isActive) // کش سریع برای گیت UI
        }
        entity
    }

    override fun observe(userId: Int): Flow<PremiumEntitlementCacheEntity?> =
        entitlementDao.observeForUser(userId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrent(): Flow<PremiumEntitlementCacheEntity?> =
        session.currentUserIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(null) else entitlementDao.observeForUser(id)
        }
}
