package com.example.solarShop.data.network.remote

import com.example.solarShop.data.network.dto.sync.BrandSyncDto
import com.example.solarShop.data.network.dto.sync.CategoryAttributeDefinitionSyncDto
import com.example.solarShop.data.network.dto.sync.CategorySyncDto
import com.example.solarShop.data.network.dto.sync.CurrencyRateSyncDto
import com.example.solarShop.data.network.dto.sync.InventoryTransactionSyncDto
import com.example.solarShop.data.network.dto.sync.ProductAttributeValueSyncDto
import com.example.solarShop.data.network.dto.sync.ProductImageSyncDto
import com.example.solarShop.data.network.dto.sync.ProductPurchasePriceSyncDto
import com.example.solarShop.data.network.dto.sync.ProductSalePriceSyncDto
import com.example.solarShop.data.network.dto.sync.ProductSyncDto
import com.example.solarShop.data.network.dto.sync.ProductUnitSyncDto
import com.example.solarShop.data.network.dto.sync.RegisterDeviceRequestDto
import com.example.solarShop.data.network.dto.sync.RegisterDeviceResponseDto
import com.example.solarShop.data.network.dto.sync.SyncStatusDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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

    suspend fun pullProductImages(
        since: Long
    ): List<ProductImageSyncDto> {
        return client.get("sync/product-images") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushProductImages(
        images: List<ProductImageSyncDto>
    ): Boolean {
        val response = client.post("sync/product-images") {
            setBody(images)
        }

        return response.status.isSuccess()
    }

    suspend fun pullInventoryTransactions(
        since: Long
    ): List<InventoryTransactionSyncDto> {
        return client.get("sync/inventory-transactions") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushInventoryTransactions(
        items: List<InventoryTransactionSyncDto>
    ): Boolean {
        val response = client.post("sync/inventory-transactions") {
            setBody(items)
        }

        return response.status.isSuccess()
    }


    suspend fun pullPurchasePrices(
        since: Long
    ): List<ProductPurchasePriceSyncDto> {
        return client.get("sync/purchase-prices") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushPurchasePrices(
        items: List<ProductPurchasePriceSyncDto>
    ): Boolean {

        val response =
            client.post("sync/purchase-prices") {
                setBody(items)
            }

        return response.status.isSuccess()
    }

    suspend fun pullSalePrices(
        since: Long
    ): List<ProductSalePriceSyncDto> {
        return client.get("sync/sale-prices") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushSalePrices(
        items: List<ProductSalePriceSyncDto>
    ): Boolean {
        val response = client.post("sync/sale-prices") {
            setBody(items)
        }

        return response.status.isSuccess()
    }

    suspend fun pullCategoryAttributeDefinitions(
        since: Long
    ): List<CategoryAttributeDefinitionSyncDto> {
        return client.get("sync/category-attribute-definitions") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushCategoryAttributeDefinitions(
        items: List<CategoryAttributeDefinitionSyncDto>
    ): Boolean {
        val response = client.post("sync/category-attribute-definitions") {
            setBody(items)
        }

        return response.status.isSuccess()
    }

    suspend fun pullProductAttributeValues(
        since: Long
    ): List<ProductAttributeValueSyncDto> {
        return client.get("sync/product-attribute-values") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushProductAttributeValues(
        items: List<ProductAttributeValueSyncDto>
    ): Boolean {
        val response = client.post("sync/product-attribute-values") {
            setBody(items)
        }

        return response.status.isSuccess()
    }

    suspend fun pullUnits(
        since: Long
    ): List<ProductUnitSyncDto> {
        return client.get("sync/units") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushUnits(
        items: List<ProductUnitSyncDto>
    ): Boolean {
        val response = client.post("sync/units") {
            setBody(items)
        }

        return response.status.isSuccess()
    }

    suspend fun pullCurrencyRates(
        since: Long
    ): List<CurrencyRateSyncDto> {
        return client.get("sync/currency-rates") {
            parameter("since", since)
        }.body()
    }

    suspend fun pushCurrencyRates(
        items: List<CurrencyRateSyncDto>
    ): Boolean {
        val response = client.post("sync/currency-rates") {
            setBody(items)
        }

        return response.status.isSuccess()
    }

}