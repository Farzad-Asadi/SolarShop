package com.example.solarShop.data.network.remote
import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.network.ApiRoutes
import com.example.solarShop.data.network.dto.EntitlementDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders

class EntitlementApi(
    private val client: HttpClient,
    private val session: SessionDataStore
) {
    suspend fun get(): EntitlementDto = client.get(ApiRoutes.ENTITLEMENTS) {
        session.snapshot().accessToken?.let { token ->
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }.body()
}
