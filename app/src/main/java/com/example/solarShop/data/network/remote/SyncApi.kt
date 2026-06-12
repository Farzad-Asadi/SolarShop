package com.example.solarShop.data.network.remote

import com.example.solarShop.data.network.dto.sync.BrandSyncDto
import com.example.solarShop.data.network.dto.sync.CategorySyncDto
import com.example.solarShop.data.network.dto.sync.ProductSyncDto
import com.example.solarShop.data.network.dto.sync.RegisterDeviceRequestDto
import com.example.solarShop.data.network.dto.sync.RegisterDeviceResponseDto
import com.example.solarShop.data.network.dto.sync.SyncStatusDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

class SyncApi(
    private val client: HttpClient
) {

    suspend fun ping(): Boolean {
        val response = client.get("sync/ping")

        return response.status.isSuccess()
    }

    suspend fun getStatus(): SyncStatusDto {
        return client.get("sync/status").body()
    }

    suspend fun registerDevice(
        request: RegisterDeviceRequestDto
    ): RegisterDeviceResponseDto {
        return client.post("sync/register-device") {
            setBody(request)
        }.body()
    }

    suspend fun pushCategories(
        categories: List<CategorySyncDto>
    ): Boolean {
        val response = client.post("sync/categories") {
            setBody(categories)
        }

        return response.status.isSuccess()
    }

    suspend fun pullCategories(
        since: Long = 0L
    ): List<CategorySyncDto> {
        return client.get("sync/categories") {
            url {
                parameters.append("since", since.toString())
            }
        }.body()
    }

    suspend fun pushBrands(
        brands: List<BrandSyncDto>
    ): Boolean {
        val response = client.post("sync/brands") {
            setBody(brands)
        }

        return response.status.isSuccess()
    }

    suspend fun pullBrands(
        since: Long = 0L
    ): List<BrandSyncDto> {
        return client.get("sync/brands") {
            url {
                parameters.append("since", since.toString())
            }
        }.body()
    }

    suspend fun pushProducts(
        products: List<ProductSyncDto>
    ): Boolean {
        val response = client.post("sync/products") {
            setBody(products)
        }

        return response.status.isSuccess()
    }

    suspend fun pullProducts(
        since: Long = 0L
    ): List<ProductSyncDto> {
        return client.get("sync/products") {
            url {
                parameters.append("since", since.toString())
            }
        }.body()
    }


}