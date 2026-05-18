package com.example.solarShop.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    object Keys {
        val accessToken   = stringPreferencesKey("access_token")
        val refreshToken  = stringPreferencesKey("refresh_token")
        val currentUserId = intPreferencesKey("current_user_id")
        val userId = intPreferencesKey("user_id")
        val isPremium     = booleanPreferencesKey("is_premium")
        val privacyAccepted = booleanPreferencesKey("privacy_accepted")
    }

    // --- Flowها برای UI/VM ---
    val accessTokenFlow: Flow<String?> = dataStore.data.map { it[Keys.accessToken] }
    val refreshTokenFlow: Flow<String?> = dataStore.data.map { it[Keys.refreshToken] }
    val currentUserIdFlow: Flow<Int?> = dataStore.data.map { it[Keys.currentUserId] }
    val isPremiumFlow: Flow<Boolean> = dataStore.data.map { it[Keys.isPremium] ?: false }
    val isLoggedInFlow: Flow<Boolean> = accessTokenFlow.map { !it.isNullOrBlank() }

    val privacyAcceptedFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.privacyAccepted] ?: false }

    suspend fun setPrivacyAccepted(value: Boolean) {
        dataStore.edit { it[Keys.privacyAccepted] = value }}

    // --- Helperهای نوشتن/خواندن ---
    suspend fun setSession(
        accessToken: String,
        refreshToken: String,
        userId: Int,
        isPremium: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.accessToken] = accessToken
            prefs[Keys.refreshToken] = refreshToken
            prefs[Keys.currentUserId] = userId
            prefs[Keys.isPremium] = isPremium
        }
    }


    suspend fun setIsPremium(value: Boolean) {
        dataStore.edit { it[Keys.isPremium] = value }
    }

    suspend fun clearSession() {
        dataStore.edit {
            it.remove(Keys.accessToken)
            it.remove(Keys.refreshToken)
            it.remove(Keys.currentUserId)
            it[Keys.isPremium] = false
        }
    }

    suspend fun snapshot(): SessionSnapshot {
        val p = dataStore.data.first()
        return SessionSnapshot(
            accessToken   = p[Keys.accessToken],
            refreshToken  = p[Keys.refreshToken],
            currentUserId = p[Keys.currentUserId],
            isPremium     = p[Keys.isPremium] ?: false
        )
    }

    suspend fun setTokens(access: String?, refresh: String?) {
        dataStore.edit { prefs ->
            if (access != null) prefs[Keys.accessToken] = access else prefs.remove(Keys.accessToken)
            if (refresh != null) prefs[Keys.refreshToken] = refresh else prefs.remove(Keys.refreshToken)
        }
    }

    suspend fun setCurrentUserId(id: Int?) {
        dataStore.edit { prefs ->
            if (id != null) prefs[Keys.currentUserId] = id else prefs.remove(Keys.currentUserId)
        }
    }


}

data class SessionSnapshot(
    val accessToken: String?,
    val refreshToken: String?,
    val currentUserId: Int?,
    val isPremium: Boolean
)
