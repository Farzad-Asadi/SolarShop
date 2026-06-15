package com.example.solarShop.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ServerState {
    data object Connected : ServerState
    data object Unreachable : ServerState
    data object AuthExpired : ServerState
}

@Singleton
class ServerConnectionState @Inject constructor() {

    private val _state = MutableStateFlow<ServerState>(ServerState.Connected)
    val state: StateFlow<ServerState> = _state

    fun connected() {
        _state.value = ServerState.Connected
    }

    fun unreachable() {
        _state.value = ServerState.Unreachable
    }

    fun authExpired() {
        _state.value = ServerState.AuthExpired
    }
}