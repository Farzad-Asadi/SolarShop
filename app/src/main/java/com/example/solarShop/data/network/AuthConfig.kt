package com.example.solarShop.data.network

import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthConfig {
    var getAccessToken: suspend () -> String? = { null }
    var getRefreshToken: suspend () -> String? = { null }

    var refreshTokens: suspend (refreshToken: String) -> RefreshResult =
        { RefreshResult.NetworkError }

    var onConnected: suspend () -> Unit = {}
    var onServerUnreachable: suspend () -> Unit = {}
    var onAuthExpired: suspend () -> Unit = {}
}

sealed interface RefreshResult {
    data object Success : RefreshResult
    data object InvalidRefreshToken : RefreshResult
    data object NetworkError : RefreshResult
}

val AuthPlugin = createClientPlugin("AuthPlugin", ::AuthConfig) {
    val cfg = pluginConfig
    val refreshMutex = Mutex()

    onRequest { request, _ ->
        if (request.headers["No-Auth"] == "true") return@onRequest

        val token = cfg.getAccessToken()
        if (!token.isNullOrBlank()) {
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    client.plugin(HttpSend).intercept { request ->
        val firstCall = try {
            execute(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cfg.onServerUnreachable()
            throw e
        }

        if (firstCall.response.status != HttpStatusCode.Unauthorized) {
            cfg.onConnected()
            return@intercept firstCall
        }

        if (firstCall.response.status != HttpStatusCode.Unauthorized) {
            cfg.onConnected()
            return@intercept firstCall
        }

        if (request.headers["No-Auth"] == "true") {
            return@intercept firstCall
        }

        val refreshResult = refreshMutex.withLock {
            val refreshToken = cfg.getRefreshToken()

            if (refreshToken.isNullOrBlank()) {
                RefreshResult.InvalidRefreshToken
            } else {
                cfg.refreshTokens(refreshToken)
            }
        }

        when (refreshResult) {
            RefreshResult.Success -> {
                val newToken = cfg.getAccessToken()

                if (!newToken.isNullOrBlank()) {
                    request.headers.remove(HttpHeaders.Authorization)
                    request.headers.append(HttpHeaders.Authorization, "Bearer $newToken")
                }

                val secondCall = execute(request)

                if (secondCall.response.status == HttpStatusCode.Unauthorized) {
                    cfg.onAuthExpired()
                } else {
                    cfg.onConnected()
                }

                secondCall
            }

            RefreshResult.InvalidRefreshToken -> {
                cfg.onAuthExpired()
                firstCall
            }

            RefreshResult.NetworkError -> {
                cfg.onServerUnreachable()
                firstCall
            }
        }
    }
}
