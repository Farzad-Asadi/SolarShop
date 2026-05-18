package com.example.solarShop.data.modules



import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf

private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

/** Engine موک برای endpointهای بدون احراز (OTP/Refresh) */
//fun mockPlainEngine(json: Json): MockEngine = MockEngine { request ->
//    when (request.url.fullPath) {
//        ApiRoutes.Auth.REQUEST_OTP -> respondOk()
//        ApiRoutes.Auth.VERIFY_OTP  -> {
//            val body = TokenResponse(
//                access = "acc_123",
//                refresh = "ref_123",
//                user = UserDto(id = 1, phone = "+989121234567", createdAt = System.currentTimeMillis()),
//                entitlement = EntitlementDto(isActive = true, plan = "pro-monthly")
//            )
//            respond(json.encodeToString(body), headers = jsonHeaders())
//        }
//        ApiRoutes.Auth.REFRESH -> {
//            val body = TokenResponse(
//                access = "acc_refreshed",
//                refresh = "ref_refreshed",
//                user = UserDto(id = 1, phone = "+989121234567", createdAt = System.currentTimeMillis())
//            )
//            respond(json.encodeToString(body), headers = jsonHeaders())
//        }
//        else -> respond("Not Found", HttpStatusCode.NotFound)
//    }
//}
//
///** Engine موک برای endpointهای نیازمند Bearer (ME/Entitlements) */
//fun mockAuthedEngine(json: Json): MockEngine = MockEngine { request ->
//    val auth = request.headers[HttpHeaders.Authorization]
//    val isAuthed = !auth.isNullOrBlank()
//
//    when (request.url.fullPath) {
//        ApiRoutes.User.ME -> {
//            if (!isAuthed) return@MockEngine respond("Unauthorized", HttpStatusCode.Unauthorized)
//            val body = UserDto(id = 1, phone = "+989121234567", createdAt = System.currentTimeMillis())
//            respond(json.encodeToString(body), headers = jsonHeaders())
//        }
//        ApiRoutes.Entitlement.GET -> {
//            if (!isAuthed) return@MockEngine respond("Unauthorized", HttpStatusCode.Unauthorized)
//            val body = EntitlementDto(isActive = true, plan = "pro-monthly")
//            respond(json.encodeToString(body), headers = jsonHeaders())
//        }
//        else -> respond("Not Found", HttpStatusCode.NotFound)
//    }
//}
