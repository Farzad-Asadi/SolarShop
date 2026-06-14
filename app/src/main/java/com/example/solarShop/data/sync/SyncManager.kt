package com.example.solarShop.data.sync

import android.util.Log
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.network.dto.sync.BrandSyncDto
import com.example.solarShop.data.network.dto.sync.CategorySyncDto
import com.example.solarShop.data.network.dto.sync.InventoryTransactionSyncDto
import com.example.solarShop.data.network.dto.sync.ProductImageSyncDto
import com.example.solarShop.data.network.dto.sync.ProductSyncDto
import com.example.solarShop.data.network.dto.sync.RegisterDeviceRequestDto
import com.example.solarShop.data.network.dto.sync.SyncStatusDto
import com.example.solarShop.data.network.remote.SyncApi
import com.example.solarShop.data.repository.file.FileSyncRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import com.example.solarShop.data.repository.sync.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val syncApi: SyncApi,
    private val productRepository: ProductRepository,
    private val fileSyncRepository: FileSyncRepository,
    private val productImageRepository: ProductImageRepository,
    private val inventoryRepository: InventoryRepository
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

        categories.forEach { category ->
            fileSyncRepository.uploadIfNeeded(category.imageFileName)
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
                    isActive = dto.deletedAt == null,
                    imageFileName = dto.imageFileName,
                    uid = dto.uid,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
            fileSyncRepository.downloadIfMissing(dto.imageFileName)
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
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
            fileSyncRepository.downloadIfMissing(dto.imageFileName)
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
                uid = brand.uid,
                name = brand.name,
                description = brand.description,
                imageFileName = brand.imageFileName,
                isActive = brand.isActive,
                updatedAt = brand.updatedAt,
                deletedAt = brand.deletedAt
            )
        }

        if (dtoList.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Brands have no valid uid")
            return true
        }

        brands.forEach { brand ->
            fileSyncRepository.uploadIfNeeded(brand.imageFileName)
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

    suspend fun pushAllBrands(): Boolean {
        val brands = productRepository
            .observeActiveBrands()
            .first()

        val dtoList = brands.mapNotNull { brand ->
            val uid = brand.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            BrandSyncDto(
                uid = uid,
                name = brand.name,
                description = brand.description,
                imageFileName = brand.imageFileName,
                isActive = brand.isActive,
                updatedAt = brand.updatedAt,
                deletedAt = brand.deletedAt
            )
        }

        val success = syncApi.pushBrands(dtoList)

        Log.d("SYNC_TEST", "Initial Upload Brands = ${dtoList.size}, success=$success")

        return success
    }

    suspend fun pushAllProducts(): Boolean {
        val products = productRepository
            .observeActiveProducts()
            .first()

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

        val success = syncApi.pushProducts(dtoList)

        Log.d("SYNC_TEST", "Initial Upload Products = ${dtoList.size}, success=$success")

        return success
    }

    suspend fun initialUploadAll(): Boolean {
        val registered = registerDevice()
        if (!registered) return false

        val categoriesOk = pushAllCategories()
        if (!categoriesOk) return false

        val brandsOk = pushAllBrands()
        if (!brandsOk) return false

        val productsOk = pushAllProducts()
        if (!productsOk) return false

        val status = syncApi.getStatus()
        syncRepository.updateLastSyncAt(status.serverTime)

        Log.d("SYNC_TEST", "Initial Upload Completed")

        return true
    }

    suspend fun pullProductImages(): Int {
        val lastSyncAt = syncRepository.getLastSyncAt()

        Log.d("SYNC_TEST", "Pull ProductImages Since = $lastSyncAt")

        val images = syncApi.pullProductImages(lastSyncAt)

        Log.d("SYNC_TEST", "Received ProductImages = ${images.size}")

        images.forEach { dto ->

            val product = productRepository.getProductByUid(dto.productUid)

            if (product == null) {
                Log.d("SYNC_TEST", "Skipped ProductImage, missing productUid = ${dto.productUid}")
                return@forEach
            }

            productImageRepository.upsertProductImageByUid(
                ProductImageEntity(
                    id = null,
                    uid = dto.uid,
                    productId = product.id ?: return@forEach,
                    fileName = dto.fileName,
                    sortOrder = dto.sortOrder,
                    createdAt = dto.updatedAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )

            fileSyncRepository.downloadIfMissing(dto.fileName)
        }

        return images.size
    }

    suspend fun pushUnsyncedProductImages(): Boolean {
        val images = productImageRepository.getUnsyncedProductImages()

        if (images.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced ProductImages = 0")
            return true
        }

        val dtoList = images.mapNotNull { image ->

            val uid = image.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val product = productRepository.getProductById(image.productId)
                ?: return@mapNotNull null

            val productUid = product.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            fileSyncRepository.uploadIfNeeded(image.fileName)

            ProductImageSyncDto(
                uid = uid,
                productUid = productUid,
                fileName = image.fileName,
                sortOrder = image.sortOrder,
                updatedAt = image.updatedAt,
                deletedAt = image.deletedAt
            )
        }

        if (dtoList.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced ProductImages have no valid productUid")
            return true
        }

        val success = syncApi.pushProductImages(dtoList)

        if (success) {
            productImageRepository.markProductImagesSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d("SYNC_TEST", "Pushed ProductImages = ${dtoList.size}, success=$success")

        return success
    }

    suspend fun pullInventoryTransactions(): Int {

        val lastSyncAt = syncRepository.getLastSyncAt()

        Log.d(
            "SYNC_TEST",
            "Pull Inventory Since = $lastSyncAt"
        )

        val items =
            syncApi.pullInventoryTransactions(lastSyncAt)

        Log.d(
            "SYNC_TEST",
            "Received Inventory = ${items.size}"
        )

        items.forEach { dto ->

            val product =
                productRepository.getProductByUid(
                    dto.productUid
                )

            if (product == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped Inventory, missing productUid = ${dto.productUid}"
                )
                return@forEach
            }

            inventoryRepository.upsertInventoryTransactionByUid(
                InventoryTransactionEntity(
                    id = null,
                    uid = dto.uid,
                    productId = product.id ?: return@forEach,
                    quantity = dto.quantity,
                    transactionType =
                    InventoryTransactionType.valueOf(
                        dto.transactionType
                    ),
                    note = dto.note,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return items.size
    }

    suspend fun pushUnsyncedInventoryTransactions(): Boolean {

        val items =
            inventoryRepository.getUnsyncedInventoryTransactions()

        if (items.isEmpty()) {
            Log.d(
                "SYNC_TEST",
                "Unsynced Inventory = 0"
            )
            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                val product =
                    productRepository.getProductById(
                        item.productId
                    )
                        ?: return@mapNotNull null

                val productUid =
                    product.uid.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                InventoryTransactionSyncDto(
                    uid = item.uid,
                    productUid = productUid,
                    quantity = item.quantity,
                    transactionType =
                    item.transactionType.name,
                    note = item.note,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt
                )
            }

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushInventoryTransactions(dtoList)

        if (success) {
            inventoryRepository.markInventoryTransactionsSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed Inventory = ${dtoList.size}, success=$success"
        )

        return success
    }






    suspend fun syncCategoriesOnce(): Boolean {
        val registered = registerDevice()
        if (!registered) return false



        pullCategories()
        pullBrands()
        pullProducts()
        pullProductImages()
        pullInventoryTransactions()


        val categoriesPushed = pushUnsyncedCategories()
        if (!categoriesPushed) return false

        val brandsPushed = pushUnsyncedBrands()
        if (!brandsPushed) return false

        val productsPushed = pushUnsyncedProducts()
        if (!productsPushed) return false

        val productImagesPushed = pushUnsyncedProductImages()
        if (!productImagesPushed) return false

        val inventoryPushed = pushUnsyncedInventoryTransactions()
        if (!inventoryPushed) return false




        val status = syncApi.getStatus()

        syncRepository.updateLastSyncAt(status.serverTime)

        return true
    }


}