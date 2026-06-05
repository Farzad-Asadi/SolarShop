package com.example.solarShop.data.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dollarRateDataStore by preferencesDataStore(
    name = "dollar_rate_preferences"
)

@Singleton
class DollarRatePreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manualDollarRateKey =
        longPreferencesKey("manual_dollar_rate_toman")

    val manualDollarRateFlow =
        context.dollarRateDataStore.data.map { prefs ->
            prefs[manualDollarRateKey]
        }

    suspend fun setManualDollarRate(value: Long?) {
        context.dollarRateDataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(manualDollarRateKey)
            } else {
                prefs[manualDollarRateKey] = value
            }
        }
    }
}