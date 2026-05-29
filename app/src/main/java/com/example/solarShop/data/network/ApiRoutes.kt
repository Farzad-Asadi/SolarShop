package com.example.solarShop.data.network

import com.example.solarShop.BuildConfig


object ApiRoutes {

    // ✅ منبع حقیقت (فقط path ها)
    object Auth {
        const val REQUEST_OTP = "/auth/request-otp"
        const val VERIFY_OTP  = "/auth/verify-otp"
        const val REFRESH     = "/auth/refresh"
    }

    object User {
        const val ME = "/me"
    }

    object Entitlement {
        const val GET = "/entitlements"
    }

    // ⛔️ BASE_URL دیگر اینجا استفاده نشود؛ از BuildConfig بخوان
    @Deprecated(
        message = "از BuildConfig.BASE_URL استفاده کن (NetworkModule → defaultRequest)",
        replaceWith = ReplaceWith("BuildConfig.BASE_URL")
    )
    const val BASE_URL: String = "https://api.example.com" // فقط برای سازگاری موقت

    // ✅ آلیاس‌های گذار (Deprecated) تا کدهای قدیمی نشکنند
    @Deprecated("Use ApiRoutes.Auth.REQUEST_OTP", ReplaceWith("Auth.REQUEST_OTP"))
    const val REQUEST_OTP: String = Auth.REQUEST_OTP

    @Deprecated("Use ApiRoutes.Auth.VERIFY_OTP", ReplaceWith("Auth.VERIFY_OTP"))
    const val VERIFY_OTP: String = Auth.VERIFY_OTP

    @Deprecated("Use ApiRoutes.Auth.REFRESH", ReplaceWith("Auth.REFRESH"))
    const val REFRESH: String = Auth.REFRESH

    @Deprecated("Use ApiRoutes.User.ME", ReplaceWith("User.ME"))
    const val ME: String = User.ME

    @Deprecated("Use ApiRoutes.Entitlement.GET", ReplaceWith("Entitlement.GET"))
    const val ENTITLEMENTS: String = Entitlement.GET
}

/**
 * (اختیاری) اگر جایی هنوز full URL لازم داشتی:
 */
fun fullUrl(path: String): String = BuildConfig.BASE_URL + path
