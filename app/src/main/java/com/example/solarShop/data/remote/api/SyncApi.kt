package com.example.solarShop.data.remote.api

interface SyncApi {

    suspend fun ping(): Boolean
}