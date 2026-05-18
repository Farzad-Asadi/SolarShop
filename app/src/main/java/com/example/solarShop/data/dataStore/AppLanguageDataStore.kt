package com.example.solarShop.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.solarShop.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLanguageDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val appLanguage = stringPreferencesKey("app_language")
    }

    /**
     * جریانِ زبان فعلی اپ؛
     * اگر چیزی ذخیره نشده باشد EN برمی‌گردد (پیش‌فرض)
     */
    val appLanguageFlow: Flow<AppLanguage> =
        dataStore.data.map { prefs ->
            val stored = prefs[Keys.appLanguage]
            AppLanguage.fromTag(stored)
        }

    /**
     * ذخیره‌کردن زبان انتخابی کاربر
     */
    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[Keys.appLanguage] = language.languageTag
        }
    }
}
