package com.example.solarShop.repo

import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.network.dto.VerifyOtpRequest
import com.example.solarShop.data.network.remote.AuthApi
import com.example.solarShop.data.room.tables.premiumEntitlementCache.PremiumEntitlementDao
import com.example.solarShop.data.room.tables.user.UserDao
import com.example.solarShop.data.room.tables.user.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun requestOtp(phoneE164: String): Result<Unit>
    suspend fun verifyOtp(phoneE164: String, code: String): Result<Unit>
    suspend fun signOut(clearLocal: Boolean = false): Result<Unit>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val entitlementDao: PremiumEntitlementDao,
    private val session: SessionDataStore,
    private val userRepo: UserRepository,
    private val entitlementRepo: EntitlementRepository
) : AuthRepository {

    override suspend fun requestOtp(phoneE164: String) =
        runCatching {
            authApi.requestOtp(phoneE164)
            Unit
        }

    override suspend fun verifyOtp(phone: String, code: String): Result<Unit> =
        runCatching {
            val res = authApi.verifyOtp(VerifyOtpRequest(phone=phone, code=code))   // TokenResponse
            val token = res                                           // اگر Ktor/Retrofit فرق دارد، تطبیق بده

            // ۱) ذخیره توکن‌ها و وضعیت پریمیوم
            session.setSession(
                accessToken   = token.access,
                refreshToken  = token.refresh,
                isPremium = token.entitlement?.isActive == true,
                userId = token.user.id
            )

            // ۲) آپسرت کاربر در Room
            userRepo.upsertFromRemote(token.user)

            // ۳) (اختیاری) sync entitlement لوکال
//            entitlementRepo.updateFromRemote(token.entitlement)
        }

    override suspend fun signOut(clearLocal: Boolean) =
        runCatching {
            session.clearSession()
            if (clearLocal) {
                userDao.clearAll()
                entitlementDao.clearAll()
            }
            Unit
        }

}
