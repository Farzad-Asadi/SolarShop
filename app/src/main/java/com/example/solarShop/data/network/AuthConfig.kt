package com.example.solarShop.data.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.*

// خودِ HttpSend

// بقیه‌ی نیازها


/**
 * تنظیمات پلاگین احراز هویت برای Ktor.
 * این مقادیر باید هنگام install(AuthPlugin) ست شوند.
 */
class AuthConfig {

    /**
     * دسترسی لحظه‌ای به accessToken فعلی (مثال: از SessionDataStore.snapshot()).
     */
    var getAccessToken: suspend () -> String? = {
        throw IllegalStateException("AuthConfig.getAccessToken تنظیم نشده است.")
    }

    /**
     * دسترسی لحظه‌ای به refreshToken فعلی.
     */
    var getRefreshToken: suspend () -> String? = {
        throw IllegalStateException("AuthConfig.getRefreshToken تنظیم نشده است.")
    }

    /**
     * تلاش برای نوسازی توکن‌ها. در صورت موفقیت true برگردان.
     * (مثال: AuthApi.refresh(...) -> SessionDataStore.setTokens(...))
     */
    var refreshTokens: suspend (refreshToken: String) -> Boolean = {
        throw IllegalStateException("AuthConfig.refreshTokens تنظیم نشده است.")
    }
}

class AuthHeaderConfig {
    var getAccessToken: suspend () -> String? = {
        error("AuthHeaderConfig.getAccessToken تنظیم نشده است.")
    }
}

val AuthHeaderPlugin = createClientPlugin("AuthHeaderPlugin", ::AuthHeaderConfig) {
    val cfg = pluginConfig
    onRequest { request, _ ->
        if (request.headers["No-Auth"] == "true") return@onRequest
        val token = cfg.getAccessToken()
        if (!token.isNullOrBlank()) {
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}