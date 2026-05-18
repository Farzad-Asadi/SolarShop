package com.example.solarShop.data.network.remote

import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.network.ApiRoutes
import com.example.solarShop.data.network.dto.UserDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders

class UserApi(
    private val client: HttpClient,
    private val session: SessionDataStore
) {
    suspend fun me(): UserDto = client.get(ApiRoutes.User.ME) {
        session.snapshot().accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }.body()
}
