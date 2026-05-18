package com.example.solarShop.data.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.LengthUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class DisplayPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "display_prefs")

    private object Keys {
        val LENGTH_UNIT = stringPreferencesKey("length_unit")
        val CURRENCY_UNIT = stringPreferencesKey("currency_unit")
    }

    val prefsFlow: Flow<DisplayPreferences> =
        context.dataStore.data.map { prefs ->
            val length = when (prefs[Keys.LENGTH_UNIT]) {
                "CENTIMETER" -> LengthUnit.CENTIMETER
                else -> LengthUnit.METER
            }
            val currency = when (prefs[Keys.CURRENCY_UNIT]) {
                "RIAL" -> CurrencyUnit.RIAL
                else -> CurrencyUnit.TOMAN
            }
            DisplayPreferences(length, currency)
        }

    suspend fun setLengthUnit(unit: LengthUnit) {
        context.dataStore.edit { it[Keys.LENGTH_UNIT] = unit.name }
    }

    suspend fun setCurrencyUnit(unit: CurrencyUnit) {
        context.dataStore.edit { it[Keys.CURRENCY_UNIT] = unit.name }
    }
}




data class DisplayPreferences(
    val lengthUnit: LengthUnit = LengthUnit.METER,
    val currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN
)