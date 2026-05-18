package com.example.solarShop.repo

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import com.example.solarShop.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")
private val KEY_APP_LANG = stringPreferencesKey("app_lang")



interface LanguageRepository {
    val languageFlow: Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)

    // ✅ جدید: فقط زبان ذخیره‌شده را برگردان (اگر هنوز ست نشده بود null)
    suspend fun getStoredLanguageOrNull(): AppLanguage?
}


@Singleton
class LanguageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : LanguageRepository {

    private object Keys {
        val appLanguage = stringPreferencesKey("app_language")
    }

    override val languageFlow: Flow<AppLanguage> =
        dataStore.data.map { prefs ->
            val stored = prefs[Keys.appLanguage]
            if (stored != null) {
                AppLanguage.fromTag(stored)
            } else {
                // زبان اولیه بر اساس زبان سیستم
                val sysLocale = context.resources.configuration.locales[0]
                val tag = sysLocale.language  // "fa" یا "en" و ...
                if (tag.startsWith("fa")) AppLanguage.FA else AppLanguage.EN
            }
        }

    override suspend fun setLanguage(language: AppLanguage) {
        // ۱) ذخیره در DataStore
        dataStore.edit { prefs ->
            prefs[Keys.appLanguage] = language.languageTag
        }
        // ۲) اعمال روی AppCompatDelegate  (per-app language)
        val locales = LocaleListCompat.forLanguageTags(language.languageTag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    override suspend fun getStoredLanguageOrNull(): AppLanguage? {
        val prefs = dataStore.data.first()
        val stored = prefs[Keys.appLanguage]
        return stored?.let { AppLanguage.fromTag(it) }
    }
}