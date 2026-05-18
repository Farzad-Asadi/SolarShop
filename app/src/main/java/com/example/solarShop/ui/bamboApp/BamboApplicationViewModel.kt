package com.example.solarShop.ui.bamboApp

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.entitlement.EntitlementCenter
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BamboAppViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val appInfoRepo: AppInfoRepository,
    private val session: SessionDataStore,
    private val entitlement: EntitlementCenter,
) : ViewModel() {


    init {
        // وقتی لاگین شدیم، سنک پریمیوم
        session.isLoggedInFlow
            .filter { it }
            .distinctUntilChanged()
            .onEach { viewModelScope.launch { entitlement.refresh() } }
            .launchIn(viewModelScope)
    }


    // --- 1) AppInfo از Room ---
    private val appInfoFlow: Flow<AppInfoEntity?> = appInfoRepo.observeAppInfo()

    // --- 2) کاربر جاری از Room (بر اساس currentUserId در AppInfo) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentUserFlow: Flow<UserEntity?> =
        appInfoFlow.flatMapLatest { appInfo ->
            val id = appInfo?.currentUserId
            if (id != null) userRepo.observeUserById(id) else flowOf(null)
        }





    // --- 5) UiState نهایی ---
    val uiState: StateFlow<BamboAppUiState> =
        combine(currentUserFlow, appInfoFlow) { currentUser, appInfo ->
            BamboAppUiState(
                currentUserEntity = currentUser,
                appInfoEntity = appInfo,
                isDataLoaded = true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // initialValue فوری از SharedPreferences تا صفحه سفید نشود
            initialValue = BamboAppUiState()
        )








}

/* ---------------- UiState ---------------- */

data class BamboAppUiState(
    val currentUserEntity: UserEntity? = null,
    val appInfoEntity: AppInfoEntity? = null,
    val isDataLoaded: Boolean = false
)

/* ---------------- Helpers ---------------- */

private fun SharedPreferences.booleanFlow(
    key: String,
    default: Boolean
): Flow<Boolean> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (changedKey == key) trySend(getBoolean(key, default))
    }
    registerOnSharedPreferenceChangeListener(listener)
    // مقدار اولیه
    trySend(getBoolean(key, default))
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.conflate()
