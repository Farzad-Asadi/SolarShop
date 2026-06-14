package com.example.solarShop.data.repository.file

interface FileSyncRepository {

    suspend fun uploadIfNeeded(fileName: String?): Boolean

    suspend fun downloadIfMissing(fileName: String?): Boolean
}