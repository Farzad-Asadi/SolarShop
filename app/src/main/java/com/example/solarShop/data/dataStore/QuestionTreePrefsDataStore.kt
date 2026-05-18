package com.example.solarShop.data.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// اگه قبلاً DataStore تعریف نکردی:
private val Context.questionTreeDataStore by preferencesDataStore(
    name = "question_tree_prefs"
)

private object QuestionTreePrefsKeys {
    val SCALE = floatPreferencesKey("question_tree_scale")
    val OFFSET_X = floatPreferencesKey("question_tree_offset_x")
    val OFFSET_Y = floatPreferencesKey("question_tree_offset_y")
}

// مدل ترجیحی
data class QuestionTreeViewPrefs(
    val scale: Float = 0.5f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

class QuestionTreePrefsDataStore(
    private val context: Context
) {

    val prefsFlow: Flow<QuestionTreeViewPrefs> =
        context.questionTreeDataStore.data.map { prefs ->
            QuestionTreeViewPrefs(
                scale = prefs[QuestionTreePrefsKeys.SCALE] ?: 0.5f,
                offsetX = prefs[QuestionTreePrefsKeys.OFFSET_X] ?: 0f,
                offsetY = prefs[QuestionTreePrefsKeys.OFFSET_Y] ?: 0f,
            )
        }

    suspend fun updateView(scale: Float, offsetX: Float, offsetY: Float) {
        context.questionTreeDataStore.edit { prefs ->
            prefs[QuestionTreePrefsKeys.SCALE] = scale
            prefs[QuestionTreePrefsKeys.OFFSET_X] = offsetX
            prefs[QuestionTreePrefsKeys.OFFSET_Y] = offsetY
        }
    }
}
