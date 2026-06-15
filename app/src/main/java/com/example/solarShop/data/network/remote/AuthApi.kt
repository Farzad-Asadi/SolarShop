package com.example.solarShop.data.network.remote

import com.example.solarShop.data.network.ApiRoutes
import com.example.solarShop.data.network.dto.BasicOkResponse
import com.example.solarShop.data.network.dto.RefreshRequest
import com.example.solarShop.data.network.dto.RequestOtpRequest
import com.example.solarShop.data.network.dto.TokenResponse
import com.example.solarShop.data.network.dto.VerifyOtpRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(private val client: HttpClient) {
    suspend fun requestOtp(phone: String): BasicOkResponse =
        client.post(ApiRoutes.REQUEST_OTP) {
            contentType(ContentType.Application.Json)
            setBody(RequestOtpRequest(phone))
            header("No-Auth", "true") // اگر جایی Bearer اتومات داری
        }.body()

    suspend fun verifyOtp(verifyOtpRequest :VerifyOtpRequest ): TokenResponse =
        client.post(ApiRoutes.VERIFY_OTP) {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(verifyOtpRequest.phone, verifyOtpRequest.code))
            header("No-Auth", "true")
        }.body()

    suspend fun refresh(refresh: String): TokenResponse =
        client.post(ApiRoutes.REFRESH) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refresh))
            header("No-Auth", "true")
        }.body()

    suspend fun refreshRaw(refresh: String): HttpResponse =
        client.post(ApiRoutes.REFRESH) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refresh))
            header("No-Auth", "true")
        }
}
