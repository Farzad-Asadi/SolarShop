package com.example.solarShop.ui.ui_02_splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.ui.solarShopNav.SolarRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val session: SessionDataStore
) : ViewModel() {

    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination

    init {
        viewModelScope.launch {
            val snap = session.snapshot()

            _destination.value = when {
                snap.accessToken.isNullOrBlank() -> SolarRoute.SignIn.name
                snap.currentUserId == null       -> SolarRoute.SignIn.name
                else                             -> SolarRoute.Profile.name
            }


        }
    }
}