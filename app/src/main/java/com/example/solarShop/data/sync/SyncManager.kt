package com.example.solarShop.data.sync

import android.util.Log
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.network.dto.sync.BrandSyncDto
import com.example.solarShop.data.network.dto.sync.CategorySyncDto
import com.example.solarShop.data.network.dto.sync.ProductSyncDto
import com.example.solarShop.data.network.dto.sync.RegisterDeviceRequestDto
import com.example.solarShop.data.network.dto.sync.SyncStatusDto
import com.example.solarShop.data.network.remote.SyncApi
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.sync.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val syncApi: SyncApi,
    private val productRepository: ProductRepository
) {

    suspend fun getLastSyncAt(): Long {
        return syncRepository.getLastSyncAt()
    }

    suspend fun updateLastSyncAt(value: Long) {
        syncRepository.updateLastSyncAt(value)
    }

    suspend fun getDeviceId(): String {
        return deviceIdProvider.getOrCreateDeviceId()
    }

    suspend fun pingServer(): Boolean {
        return syncApi.ping()
    }

    suspend fun getSyncStatus(): SyncStatusDto {
        return syncApi.getStatus()
    }

    suspend fun registerDevice(): Boolean {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()

        val response = syncApi.registerDevice(
            RegisterDeviceRequestDto(
                deviceId = deviceId,
                appVersion = 1,
                platform = "android"
            )
        )

        return response.accepted
    }

    suspend fun pushAllCategories(): Boolean {
        val categories = productRepository
            .observeActiveCategories()
            .first()

        val dtoList = categories.map { category ->
            CategorySyncDto(
                uid = category.uid ?: return@map null,
                name = category.name,
                imageFileName = category.imageFileName,
                sortOrder = category.sortOrder,
                deletedAt = category.deletedAt,
                updatedAt = category.updatedAt
            )
        }.filterNotNull()

        return syncApi.pushCategories(dtoList)
    }

    suspend fun pushUnsyncedCategories(): Boolean {
        val categories = productRepository.getUnsyncedCategories()

        if (categories.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Categories = 0")
            return true
        }

        val dtoList = categories.mapNotNull { category ->
            val uid = category.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            CategorySyncDto(
                uid = uid,
                name = category.name,
                imageFileName = category.imageFileName,
                sortOrder = category.sortOrder,
                deletedAt = category.deletedAt,
                updatedAt = category.updatedAt
            )
        }

        if (dtoList.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Categories have no valid uid")
            return true
        }

        val success = syncApi.pushCategories(dtoList)

        if (success) {
            productRepository.markCategoriesSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed Categories = ${dtoList.size}, success=$success"
        )

        return success
    }

    suspend fun pullCategories(): Int {
        val lastSyncAt = syncRepository.getLastSyncAt()

        Log.d(
            "SYNC_TEST",
            "Pull Since = $lastSyncAt"
        )

        val categories = syncApi.pullCategories(lastSyncAt)

        Log.d(
            "SYNC_TEST",
            "Received Categories = ${categories.size}"
        )

        categories.forEach { dto ->
            productRepository.upsertCategoryByUid(
                ProductCategoryEntity(
                    id = null,
                    name = dto.name,
                    description = "",
                    sortOrder = dto.sortOrder,
                    isActive = true,
                    imageFileName = dto.imageFileName,
                    uid = dto.uid,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return categories.size
    }

    suspend fun pullBrands(): Int {
        val lastSyncAt = syncRepository.getLastSyncAt()

        Log.d("SYNC_TEST", "Pull Brands Since = $lastSyncAt")

        val brands = syncApi.pullBrands(lastSyncAt)

        Log.d("SYNC_TEST", "Received Brands = ${brands.size}")

        brands.forEach { dto ->
            productRepository.upsertBrandByUid(
                ProductBrandEntity(
                    id = null,
                    name = dto.name,
                    description = dto.description,
                    imageFileName = dto.imageFileName,
                    isActive = dto.isActive,
                    uid = dto.uid,
                    updatedAt = dto.updatedAt,
                    deletedAt = null,
                    isSynced = true
                )
            )
        }

        return brands.size
    }

    suspend fun pushUnsyncedBrands(): Boolean {
        val brands = productRepository.getUnsyncedBrands()

        if (brands.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Brands = 0")
            return true
        }

        val dtoList = brands.mapNotNull { brand ->
            val uid = brand.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            BrandSyncDto(
                uid = uid,
                name = brand.name,
                description = brand.description,
                imageFileName = brand.imageFileName,
                isActive = brand.isActive,
                updatedAt = brand.updatedAt
            )
        }

        if (dtoList.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Brands have no valid uid")
            return true
        }

        val success = syncApi.pushBrands(dtoList)

        if (success) {
            productRepository.markBrandsSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d("SYNC_TEST", "Pushed Brands = ${dtoList.size}, success=$success")

        return success
    }

    suspend fun pullProducts(): Int {
        val lastSyncAt = syncRepository.getLastSyncAt()

        Log.d("SYNC_TEST", "Pull Products Since = $lastSyncAt")

        val products = syncApi.pullProducts(lastSyncAt)

        Log.d("SYNC_TEST", "Received Products = ${products.size}")

        products.forEach { dto ->

            val category = dto.categoryUid
                ?.let { productRepository.getCategoryByUid(it) }

            val brand = dto.brandUid
                ?.let { productRepository.getBrandByUid(it) }

            if (category == null) {
                Log.d("SYNC_TEST", "Skipped Product, missing categoryUid = ${dto.categoryUid}")
                return@forEach
            }

            productRepository.upsertProductByUid(
                ProductEntity(
                    id = null,
                    uid = dto.uid,
                    categoryId = category.id ?: return@forEach,
                    brandId = brand?.id,
                    name = dto.name,
                    model = dto.model,
                    description = dto.description,
                    isArchived = dto.isArchived,
                    isDraft = false,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return products.size
    }

    suspend fun pushUnsyncedProducts(): Boolean {
        val products = productRepository.getUnsyncedProducts()

        if (products.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Products = 0")
            return true
        }

        val dtoList = products.mapNotNull { product ->

            val category = productRepository.getCategoryById(product.categoryId)
                ?: return@mapNotNull null

            val categoryUid = category.uid
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val brandUid = product.brandId
                ?.let { productRepository.getBrandById(it) }
                ?.uid
                ?.takeIf { it.isNotBlank() }

            ProductSyncDto(
                uid = product.uid,
                categoryUid = categoryUid,
                brandUid = brandUid,
                name = product.name,
                model = product.model,
                description = product.description,
                isArchived = product.isArchived,
                updatedAt = product.updatedAt,
                deletedAt = product.deletedAt
            )
        }

        if (dtoList.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Products have no valid categoryUid")
            return true
        }

        val success = syncApi.pushProducts(dtoList)

        if (success) {
            productRepository.markProductsSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d("SYNC_TEST", "Pushed Products = ${dtoList.size}, success=$success")

        return success
    }

    suspend fun syncCategoriesOnce(): Boolean {
        val registered = registerDevice()
        if (!registered) return false



        pullCategories()
        pullBrands()
        pullProducts()



        val categoriesPushed = pushUnsyncedCategories()
        if (!categoriesPushed) return false

        val brandsPushed = pushUnsyncedBrands()
        if (!brandsPushed) return false

        val productsPushed = pushUnsyncedProducts()
        if (!productsPushed) return false




        val status = syncApi.getStatus()

        syncRepository.updateLastSyncAt(status.serverTime)

        return true
    }
}