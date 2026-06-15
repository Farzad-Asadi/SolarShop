package com.example.solarShop.data.remote.currency

import com.example.solarShop.data.network.dto.currency.CurrencyFetchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CurrencyRemoteDataSource @Inject constructor(
    @Named("plain") private val client: HttpClient
) {
    suspend fun fetchUsdRateToman(): Long? {
        val response: CurrencyFetchResponse =
            client.post("currency/fetch-usd-now")
                .body()

        return response.rateToman
    }
}

