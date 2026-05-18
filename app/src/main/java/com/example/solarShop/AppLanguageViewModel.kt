package com.example.solarShop

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.repo.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLanguageViewModel @Inject constructor(
    private val langRepo: LanguageRepository
) : ViewModel() {

    val appLanguage: StateFlow<AppLanguage> =
        langRepo.languageFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppLanguage.FA // ✅ بهتر برای بازار فارسی
            )

    init {
        // ✅ اگر اولین اجراست و چیزی ذخیره نشده، یک بار فارسی را ذخیره کن
        viewModelScope.launch {
            val stored = langRepo.getStoredLanguageOrNull()
            if (stored == null) {
                langRepo.setLanguage(AppLanguage.FA)
            }
        }

        // مثل قبل: هر تغییری در زبان → اعمال locales
        viewModelScope.launch {
            langRepo.languageFlow.collect { lang ->
                val locales = LocaleListCompat.forLanguageTags(lang.languageTag)
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { langRepo.setLanguage(language) }
    }
}

