package com.example.solarShop.data.sync

import android.util.Log
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity
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
import com.example.solarShop.data.network.dto.sync.SyncStatusDto
import com.example.solarShop.data.network.remote.SyncApi
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.file.FileSyncRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
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
    private val inventoryRepository: InventoryRepository,
    private val pricingRepository: PricingRepository,
    private val attributeRepository: AttributeRepository,
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

    suspend fun pullPurchasePrices(): Int {

        val lastSyncAt = syncRepository.getLastSyncAt()

        val items =
            syncApi.pullPurchasePrices(lastSyncAt)

        items.forEach { dto ->

            val product =
                productRepository.getProductByUid(
                    dto.productUid
                )

            if (product == null) {
                return@forEach
            }

            pricingRepository.upsertPurchasePriceByUid(
                ProductPurchasePriceEntity(
                    id = null,
                    uid = dto.uid,
                    productId = product.id ?: return@forEach,

                    buyPriceDollar = dto.buyPriceDollar,
                    buyPriceToman = dto.buyPriceToman,
                    dollarRateToman = dto.dollarRateToman,

                    quantity = dto.quantity,
                    purchasedAt = dto.purchasedAt,
                    note = dto.note,

                    isActive = dto.isActive,

                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return items.size
    }

    suspend fun pushUnsyncedPurchasePrices(): Boolean {

        val items =
            pricingRepository.getUnsyncedPurchasePrices()

        Log.d(
            "SYNC_TEST",
            "Unsynced PurchasePrices = ${items.size}"
        )

        if (items.isEmpty()) {
            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                val product =
                    productRepository.getProductById(
                        item.productId
                    )
                        ?: return@mapNotNull null

                ProductPurchasePriceSyncDto(
                    uid = item.uid,
                    productUid = product.uid,

                    buyPriceDollar = item.buyPriceDollar,
                    buyPriceToman = item.buyPriceToman,
                    dollarRateToman = item.dollarRateToman,

                    quantity = item.quantity,
                    purchasedAt = item.purchasedAt,
                    note = item.note,

                    isActive = item.isActive,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt
                )
            }

        Log.d(
            "SYNC_TEST",
            "PurchasePrice dtoList = ${dtoList.size}"
        )

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushPurchasePrices(dtoList)

        if (success) {
            pricingRepository.markPurchasePricesSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed PurchasePrices = ${dtoList.size}, success=$success"
        )

        return success
    }

    suspend fun pullSalePrices(): Int {

        val lastSyncAt = syncRepository.getLastSyncAt()

        val items =
            syncApi.pullSalePrices(lastSyncAt)

        items.forEach { dto ->

            val product =
                productRepository.getProductByUid(
                    dto.productUid
                )

            if (product == null) {
                return@forEach
            }

            pricingRepository.upsertSalePriceByUid(
                ProductSalePriceEntity(
                    id = null,
                    uid = dto.uid,
                    productId = product.id ?: return@forEach,

                    priceType = dto.priceType,
                    salePriceToman = dto.salePriceToman,
                    profitPercent = dto.profitPercent,
                    baseDollarPrice = dto.baseDollarPrice,
                    dollarRateToman = dto.dollarRateToman,
                    basePurchasePriceToman =
                    dto.basePurchasePriceToman,
                    note = dto.note,

                    isActive = dto.isActive,

                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return items.size
    }

    suspend fun pushUnsyncedSalePrices(): Boolean {

        val items =
            pricingRepository.getUnsyncedSalePrices()

        if (items.isEmpty()) {
            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                val product =
                    productRepository.getProductById(
                        item.productId
                    )
                        ?: return@mapNotNull null

                ProductSalePriceSyncDto(
                    uid = item.uid,
                    productUid = product.uid,

                    priceType = item.priceType,
                    salePriceToman = item.salePriceToman,
                    profitPercent = item.profitPercent,
                    baseDollarPrice = item.baseDollarPrice,
                    dollarRateToman = item.dollarRateToman,
                    basePurchasePriceToman =
                    item.basePurchasePriceToman,
                    note = item.note,

                    isActive = item.isActive,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt
                )
            }

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushSalePrices(dtoList)

        if (success) {
            pricingRepository.markSalePricesSynced(
                dtoList.map { it.uid }
            )
        }

        return success
    }

    suspend fun pullCategoryAttributeDefinitions(): Int {

        val lastSyncAt = syncRepository.getLastSyncAt()

        val items =
            syncApi.pullCategoryAttributeDefinitions(lastSyncAt)

        items.forEach { dto ->

            val category =
                productRepository.getCategoryByUid(dto.categoryUid)

            if (category == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped AttributeDefinition, missing categoryUid = ${dto.categoryUid}"
                )
                return@forEach
            }

            attributeRepository.upsertAttributeDefinitionByUid(
                CategoryAttributeDefinitionEntity(
                    id = null,
                    uid = dto.uid,
                    categoryId = category.id ?: return@forEach,

                    title = dto.title,
                    key = dto.key,
                    description = dto.description,

                    valueType = dto.valueType,
                    unit = dto.unit,
                    isRequired = dto.isRequired,
                    sortOrder = dto.sortOrder,
                    enumOptions = dto.enumOptions,

                    isActive = dto.isActive,

                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return items.size
    }

    suspend fun pushUnsyncedCategoryAttributeDefinitions(): Boolean {

        val items =
            attributeRepository.getUnsyncedAttributeDefinitions()

        if (items.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced AttributeDefinitions = 0")
            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                val category =
                    productRepository.getCategoryById(item.categoryId)
                        ?: return@mapNotNull null

                val categoryUid =
                    category.uid?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                CategoryAttributeDefinitionSyncDto(
                    uid = item.uid,
                    categoryUid = categoryUid,

                    title = item.title,
                    key = item.key,
                    description = item.description,

                    valueType = item.valueType,
                    unit = item.unit,
                    isRequired = item.isRequired,
                    sortOrder = item.sortOrder,
                    enumOptions = item.enumOptions,

                    isActive = item.isActive,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt
                )
            }

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushCategoryAttributeDefinitions(dtoList)

        if (success) {
            attributeRepository.markAttributeDefinitionsSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed AttributeDefinitions = ${dtoList.size}, success=$success"
        )

        return success
    }

    suspend fun pullProductAttributeValues(): Int {

        val lastSyncAt = syncRepository.getLastSyncAt()

        val items =
            syncApi.pullProductAttributeValues(lastSyncAt)

        items.forEach { dto ->

            val product =
                productRepository.getProductByUid(dto.productUid)

            val definition =
                attributeRepository.getAttributeDefinitionByUid(
                    dto.attributeDefinitionUid
                )

            if (product == null || definition == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped ProductAttributeValue, productUid=${dto.productUid}, definitionUid=${dto.attributeDefinitionUid}"
                )
                return@forEach
            }

            attributeRepository.upsertProductAttributeValueByUid(
                ProductAttributeValueEntity(
                    id = null,
                    uid = dto.uid,
                    productId = product.id ?: return@forEach,
                    attributeDefinitionId = definition.id ?: return@forEach,

                    valueText = dto.valueText,

                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return items.size
    }

    suspend fun pushUnsyncedProductAttributeValues(): Boolean {

        val items =
            attributeRepository.getUnsyncedProductAttributeValues()

        if (items.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced ProductAttributeValues = 0")
            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                val product =
                    productRepository.getProductById(item.productId)
                        ?: return@mapNotNull null

                val productUid =
                    product.uid.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val definition =
                    attributeRepository.getAttributeDefinitionById(
                        item.attributeDefinitionId
                    )
                        ?: return@mapNotNull null

                val definitionUid =
                    definition.uid.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                ProductAttributeValueSyncDto(
                    uid = item.uid,
                    productUid = productUid,
                    attributeDefinitionUid = definitionUid,

                    valueText = item.valueText,

                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt
                )
            }

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushProductAttributeValues(dtoList)

        if (success) {
            attributeRepository.markProductAttributeValuesSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed ProductAttributeValues = ${dtoList.size}, success=$success"
        )

        return success
    }

    suspend fun pullUnits(): Int {
        val lastSyncAt = syncRepository.getLastSyncAt()

        val items =
            syncApi.pullUnits(lastSyncAt)

        items.forEach { dto ->
            productRepository.upsertUnitByUid(
                ProductUnitEntity(
                    id = null,
                    uid = dto.uid,
                    name = dto.name,
                    symbol = dto.symbol,
                    isActive = dto.isActive,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        return items.size
    }

    suspend fun pushUnsyncedUnits(): Boolean {
        val items =
            productRepository.getUnsyncedUnits()

        if (items.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Units = 0")
            return true
        }

        val dtoList =
            items.map { unit ->
                ProductUnitSyncDto(
                    uid = unit.uid,
                    name = unit.name,
                    symbol = unit.symbol,
                    isActive = unit.isActive,
                    createdAt = unit.createdAt,
                    updatedAt = unit.updatedAt,
                    deletedAt = unit.deletedAt
                )
            }

        val success =
            syncApi.pushUnits(dtoList)

        if (success) {
            productRepository.markUnitsSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed Units = ${dtoList.size}, success=$success"
        )

        return success
    }

    suspend fun pullCurrencyRates(): Int {

        val lastSyncAt =
            syncRepository.getLastSyncAt()

        val items =
            syncApi.pullCurrencyRates(lastSyncAt)

        items.forEach { dto ->
            pricingRepository.upsertCurrencyRateByUid(
                CurrencyRateEntity(
                    id = null,
                    uid = dto.uid,
                    currencyCode = dto.currencyCode,
                    rateToman = dto.rateToman,
                    source = dto.source,
                    note = dto.note,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pulled CurrencyRates = ${items.size}"
        )

        return items.size
    }

    suspend fun pushUnsyncedCurrencyRates(): Boolean {

        val items =
            pricingRepository.getUnsyncedCurrencyRates()

        Log.d(
            "SYNC_TEST",
            "Unsynced CurrencyRates = ${items.size}"
        )

        if (items.isEmpty()) {
            return true
        }

        val dtoList =
            items.map { item ->
                CurrencyRateSyncDto(
                    uid = item.uid,
                    currencyCode = item.currencyCode,
                    rateToman = item.rateToman,
                    source = item.source,
                    note = item.note,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt
                )
            }

        val success =
            syncApi.pushCurrencyRates(dtoList)

        if (success) {
            pricingRepository.markCurrencyRatesSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed CurrencyRates = ${dtoList.size}, success=$success"
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
        pullCurrencyRates()
        pullPurchasePrices()
        pullSalePrices()
        pullCategoryAttributeDefinitions()
        pullProductAttributeValues()
        pullUnits()


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

        val currencyRatesPushed =
            pushUnsyncedCurrencyRates()
        if (!currencyRatesPushed) return false

        val purchasePricesPushed =
            pushUnsyncedPurchasePrices()
        if (!purchasePricesPushed) return false

        val salePricesPushed =
            pushUnsyncedSalePrices()
        if (!salePricesPushed) return false

        val attributeDefinitionsPushed =
            pushUnsyncedCategoryAttributeDefinitions()
        if (!attributeDefinitionsPushed) return false

        val productAttributeValuesPushed =
            pushUnsyncedProductAttributeValues()
        if (!productAttributeValuesPushed) return false

        val unitsPushed =
            pushUnsyncedUnits()
        if (!unitsPushed) return false







        val status = syncApi.getStatus()

        syncRepository.updateLastSyncAt(status.serverTime)

        return true
    }




}