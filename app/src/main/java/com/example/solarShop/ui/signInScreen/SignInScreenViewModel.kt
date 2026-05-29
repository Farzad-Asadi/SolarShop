package com.example.solarShop.ui.signInScreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.AppLanguage
import com.example.solarShop.R
import com.example.solarShop.data.backupRestore.SeedDumper
import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.user.UserRepository
import com.example.solarShop.repo.AuthRepository
import com.example.solarShop.repo.EntitlementRepository
import com.example.solarShop.repo.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appInfoRepo: AppInfoRepository,
    private val dumper: SeedDumper,
    private val entitlementRepository: EntitlementRepository,
    private val session: SessionDataStore,
    private val langRepo: LanguageRepository,
    @ApplicationContext private val app: Context,
) : ViewModel() {



    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _events = Channel<SignInUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var timerJob: Job? = null

    // زبان جاری (برای UI سوییچر)
    val appLanguage: StateFlow<AppLanguage> =
        langRepo.languageFlow.stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.FA)




    fun onPickLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            langRepo.setLanguage(lang)        // دیتااستور + setApplicationLocales
            // همین کافی است؛ نیازی به collect دائمی نیست
        }
    }


    fun onCodeChange(input: String): String {
        val sanitized = input.filter(Char::isDigit).take(MAX_CODE_LEN)
        if (sanitized != _state.value.code) {
            _state.update { it.copy(code = sanitized, codeError = null) } // ← پاک‌کردن خطا
        }
        return sanitized
    }

    private fun currentE164(): String = "+98${state.value.phoneNational}"

    fun onClickSendOtp() {
//        val phone = normalizeE164(_state.value.phone)
//        if (phone == null) {
//            _state.update { it.copy(phoneError = "شماره را به فرمت E.164 مثل +98912… وارد کن") }
//            return
//        }
        viewModelScope.launch {
            val national = state.value.phoneNational
            if (national.length != 10) {
                _state.update { it.copy(phoneError = app.getString(R.string.sign_in_error_phone_length)) }
                return@launch
            }
            val e164 = currentE164()
            setLoading(true)
            val result = authRepository.requestOtp(e164)
            setLoading(false)
            result.onSuccess {
                _state.update { it.copy(step = SignInStep.EnterCode) }
                startResendTimer(RESEND_SECONDS)
            }.onFailure {
                _events.send(SignInUiEvent.Message(it.message ?:  app.getString(R.string.sign_in_error_send_code)))
            }
        }
    }

    fun onClickVerify() = viewModelScope.launch {
        val national = state.value.phoneNational
        if (national.length != 10) {
            _state.update { it.copy(phoneError = app.getString(R.string.sign_in_error_phone_length)) }
            return@launch
        }
        val e164 = currentE164()
        setLoading(true)
        val verify = authRepository.verifyOtp(e164, _state.value.code)
        setLoading(false)

        if (verify.isSuccess) {
//            userRepository.syncMe().onFailure { }
            entitlementRepository.syncForCurrentUser().onFailure { }

            // ⬇️ ۱) گرفتن یوزر فعلی از Room
            val user = userRepository.observeCurrentUser()
                .filterNotNull()
                .first()

            val id = user.id // یا یک خطای منطقی

            session.setCurrentUserId(id)
            session.setPrivacyAccepted(true)

            // ⬇️ ۲) فرستادن event با userId

            _events.send(SignInUiEvent.NavigateHome)

        } else {
            // ❌ بدون Snackbar؛ خطا را کنار OTP نشان بده
            _state.update { it.copy(codeError = app.getString(R.string.sign_in_error_code_incorrect)) }
            // اگر می‌خواهی کد را هم خالی کنی:
            // _state.update { it.copy(code = "", codeError = "کد وارد شده نادرست است") }
        }
    }

    fun onClickResend() {
        if (_state.value.secondsToResend > 0) return
        onClickSendOtp()
    }

    fun onClickEditPhone() {
        timerJob?.cancel()
        _state.update { it.copy(step = SignInStep.EnterPhone, code = "", secondsToResend = 0) }
    }

    private fun startResendTimer(total: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var left = total
            while (left >= 0) {
                _state.update { it.copy(secondsToResend = left) }
                delay(1000)
                left--
            }
        }
    }

    private fun setLoading(value: Boolean) {
        _state.update { it.copy(isLoading = value) }
    }

    companion object {
        private const val MAX_CODE_LEN = 6
        private const val RESEND_SECONDS = 60
        /**
         * نرمال‌سازی ساده به E.164 (حداقلی). در عمل بهتر است از کتابخانهٔ libphonenumber استفاده شود.
         */
        fun normalizeE164(input: String): String? {
            val trimmed = input.trim().replace(" ", "")
            if (!trimmed.startsWith("+")) return null
            if (trimmed.length < 10) return null
            if (trimmed.drop(1).any { !it.isDigit() }) return null
            return trimmed
        }
    }

    fun onPhoneNationalChange(input: String): String {
        // فقط رقم
        val digits = input.filter(Char::isDigit)

        // اگر کاربر "09..." می‌زند، صفر اول را حذف کن
        val noLeadingZero = if (digits.startsWith("0")) digits.drop(1) else digits

        // محدود به 10 رقم
        val limited = noLeadingZero.take(10)

        _state.update { it.copy(phoneNational = limited, phoneError = null) }

        return limited // ← خروجی تمیزشده
    }

    //create seed
    private val _path = MutableStateFlow<String?>(null)
    val path = _path.asStateFlow()
    fun onClickCreateDumpSeed()= viewModelScope.launch {          // dbیک دکمه/اکشن موقتی برای گرفتن خروجی
        val f = dumper.dumpSeedDb()
        _path.value = f.absolutePath
    }


}







enum class SignInStep { EnterPhone, EnterCode }

data class SignInUiState(
    val step: SignInStep = SignInStep.EnterPhone,
    val phoneNational: String = "",   // فقط ۱۰ رقم بعد از +98 (بدون صفر)
    val phoneError: String? = null,
    val code: String = "",
    val codeError: String? = null,
    val isLoading: Boolean = false,
    val secondsToResend: Int = 0
)

sealed interface SignInUiEvent {
    data object NavigateHome : SignInUiEvent
    data class Message(val text: String) : SignInUiEvent
}
