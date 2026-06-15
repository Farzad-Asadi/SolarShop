package com.example.solarShop.data.network

import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthConfig {
    var getAccessToken: suspend () -> String? = { null }
    var getRefreshToken: suspend () -> String? = { null }
    var refreshTokens: suspend (refreshToken: String) -> Boolean = { false }
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
        val firstCall = execute(request)

        if (
            firstCall.response.status != HttpStatusCode.Unauthorized ||
            request.headers["No-Auth"] == "true"
        ) {
            return@intercept firstCall
        }

        val refreshed = refreshMutex.withLock {
            val refreshToken = cfg.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                false
            } else {
                cfg.refreshTokens(refreshToken)
            }
        }

        if (!refreshed) {
            return@intercept firstCall
        }

        val newAccessToken = cfg.getAccessToken()
        if (!newAccessToken.isNullOrBlank()) {
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $newAccessToken")
        }

        execute(request)
    }
}


