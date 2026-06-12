package com.example.solarShop.data.network.mock

import com.example.solarShop.data.network.dto.BasicOkResponse
import com.example.solarShop.data.network.dto.EntitlementDto
import com.example.solarShop.data.network.dto.RefreshRequest
import com.example.solarShop.data.network.dto.RequestOtpRequest
import com.example.solarShop.data.network.dto.TokenResponse
import com.example.solarShop.data.network.dto.UserDto
import com.example.solarShop.data.network.dto.VerifyOtpRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.absoluteValue

/**
 *  امضای درست: تابع با receiver از نوع MockRequestHandleScope
 *  تا بتوانیم از respond(...) استفاده کنیم.
 */
fun mockHandler(
    json: Json
): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = handler@ { request ->

    val path   = request.url.encodedPath
    val method = request.method

    val bodyText: String = when (val b = request.body) {
        is TextContent      -> b.text
        is ByteArrayContent -> b.bytes().decodeToString()
        else                -> ""
    }

    val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    fun ok(body: String)  = respond(body, status = HttpStatusCode.OK, headers = jsonHeaders)
    fun err(code: HttpStatusCode, body: String) = respond(body, status = code, headers = jsonHeaders)
    fun hasBearer(): Boolean = request.headers[HttpHeaders.Authorization]?.startsWith("Bearer ") == true

    // کل when به‌صورت expression تا نیازی به returnهای متعدد نباشد
    return@handler when {
        method == HttpMethod.Post && path == "/auth/request-otp" -> {
            val req = runCatching { json.decodeFromString<RequestOtpRequest>(bodyText) }.getOrNull()
            if (req?.phone?.startsWith("+") != true) {
                err(HttpStatusCode.BadRequest, json.encodeToString(BasicOkResponse(false, "invalid_phone")))
            } else {
                ok(json.encodeToString(BasicOkResponse(true)))
            }
        }

        method == HttpMethod.Post && path == "/auth/verify-otp" -> {
            val req = runCatching { json.decodeFromString<VerifyOtpRequest>(bodyText) }.getOrNull()
            if (req == null) {
                err(HttpStatusCode.BadRequest, json.encodeToString(BasicOkResponse(false, "bad_body")))
            } else if (req.code != "123456") {
                err(HttpStatusCode.Unauthorized, json.encodeToString(BasicOkResponse(false, "invalid_code")))
            } else {
                val now = System.currentTimeMillis()

                // 💡 userId را از phone بسازیم تا برای هر شماره ثابت بماند:
                val userId = req.phone.hashCode().absoluteValue

                // توکن را طوری بسازیم که phone داخلش باشد:
                val access  = "mock_access_${userId}_${req.phone}"
                val refresh = "mock_refresh_${userId}_${req.phone}"

                val user = UserDto(
                    id = userId,
                    phone = req.phone,
                    createdAt = now - 86_400_000
                )
                val ent  = EntitlementDto(isActive = true, plan = "pro-monthly", expiresAt = null)

                ok(json.encodeToString(TokenResponse(access, refresh, user, ent)))
            }
        }


        method == HttpMethod.Post && path == "/auth/refresh" -> {
            val req = runCatching { json.decodeFromString<RefreshRequest>(bodyText) }.getOrNull()
            val token = req?.refresh ?: ""

            if (!token.startsWith("mock_refresh_")) {
                err(HttpStatusCode.Unauthorized, """{"message":"invalid_refresh"}""")
            } else {
                val now = System.currentTimeMillis()

                // mock_refresh_<userId>_<phone>
                val raw = token.removePrefix("mock_refresh_")
                val parts = raw.split("_", limit = 2)
                val userId = parts.getOrNull(0)?.toIntOrNull() ?: 1
                val phone  = parts.getOrNull(1) ?: "+989121234567"

                val access  = "mock_access_${userId}_${phone}"
                val refresh = "mock_refresh_${userId}_${phone}"

                val user = UserDto(
                    id = userId,
                    phone = phone,
                    createdAt = now - 172_800_000
                )

                ok(json.encodeToString(TokenResponse(access, refresh, user, null)))
            }
        }


        method == HttpMethod.Get && path == "/me" -> {
            if (!hasBearer()) {
                err(HttpStatusCode.Unauthorized, """{"message":"Unauthorized"}""")
            } else {
                val auth = request.headers[HttpHeaders.Authorization] ?: ""
                val token = auth.removePrefix("Bearer ").trim()

                // انتظار: mock_access_<userId>_<phone>
                val raw = token.removePrefix("mock_access_")
                val parts = raw.split("_", limit = 2)
                val userId = parts.getOrNull(0)?.toIntOrNull() ?: 1
                val phone  = parts.getOrNull(1) ?: "+989121234567"

                val user = UserDto(
                    id = userId,
                    phone = phone,
                    createdAt = System.currentTimeMillis() - 172_800_000
                )
                ok(json.encodeToString(user))
            }
        }


        method == HttpMethod.Get && path == "/entitlements" -> {
            if (!hasBearer()) {
                err(HttpStatusCode.Unauthorized, """{"message":"Unauthorized"}""")
            } else {
                ok(json.encodeToString(EntitlementDto(isActive = true, plan = "pro-monthly", expiresAt = null)))
            }
        }

        method == HttpMethod.Get && path == "/sync/ping" -> {
            ok("""{"ok":true,"message":"pong"}""")
        }

        method == HttpMethod.Get && path == "/sync/status" -> {
            ok(
                """
        {
          "serverTime": ${System.currentTimeMillis()},
          "serverVersion": 1,
          "message": "mock sync server is ready"
        }
        """.trimIndent()
            )
        }

        method == HttpMethod.Post && path == "/sync/register-device" -> {
            ok(
                """
        {
          "accepted": true,
          "serverVersion": 1
        }
        """.trimIndent()
            )
        }

        else -> {
            err(HttpStatusCode.NotFound, json.encodeToString(mapOf("message" to "No mock for ${method.value} $path")))
        }
    }
}


/**
 *  🔹 خودِ Engine آماده (اگر جایی خواستی مستقیم Engine بسازی)
 */
fun mockPlainEngine(json: Json): MockEngine = MockEngine { req -> mockHandler(json)(req) }

fun mockAuthedEngine(json: Json) = mockPlainEngine(json)
