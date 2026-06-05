package com.example.solarShop.data.remote.currency



import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CurrencyRemoteDataSource @Inject constructor(
    @Named("external") private val client: HttpClient
) {
    suspend fun fetchUsdRateToman(): Long? {
        val response: BrsMarketResponseDto =
            client.get("https://Api.BrsApi.ir/Market/Gold_Currency.php") {
                parameter("key", "Bz9nMu2pDRNFukqA4Tfa6P61vKBiTkiP")
            }.body()

        val usdItem =
            response.currency.firstOrNull {
                it.symbol.equals("USD", ignoreCase = true)
            } ?: response.currency.firstOrNull {
                it.name?.contains("دلار") == true
            }

        return usdItem?.price
    }
}

