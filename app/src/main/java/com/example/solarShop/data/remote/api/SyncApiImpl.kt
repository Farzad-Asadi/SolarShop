package com.example.solarShop.data.remote.api

import io.ktor.client.HttpClient
import javax.inject.Inject

class SyncApiImpl @Inject constructor(
    private val httpClient: HttpClient
) : SyncApi {

    override suspend fun ping(): Boolean {
        TODO("مرحله بعد")
    }
}