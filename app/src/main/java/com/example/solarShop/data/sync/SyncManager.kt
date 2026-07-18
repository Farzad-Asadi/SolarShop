package com.example.solarShop.data.sync

import android.util.Log
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.InvoiceStatus
import com.example.solarShop.InvoiceType
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
import com.example.solarShop.data.local.entity.sales.ProductSaleTransactionEntity
import com.example.solarShop.data.network.dto.sync.BrandSyncDto
import com.example.solarShop.data.network.dto.sync.CategoryAttributeDefinitionSyncDto
import com.example.solarShop.data.network.dto.sync.CategorySyncDto
import com.example.solarShop.data.network.dto.sync.ClientSyncDto
import com.example.solarShop.data.network.dto.sync.CurrencyRateSyncDto
import com.example.solarShop.data.network.dto.sync.InventoryTransactionSyncDto
import com.example.solarShop.data.network.dto.sync.InvoiceDocumentSyncDto
import com.example.solarShop.data.network.dto.sync.InvoiceItemSyncDto
import com.example.solarShop.data.network.dto.sync.OrderSyncDto
import com.example.solarShop.data.network.dto.sync.ProductAttributeValueSyncDto
import com.example.solarShop.data.network.dto.sync.ProductImageSyncDto
import com.example.solarShop.data.network.dto.sync.ProductPurchasePriceSyncDto
import com.example.solarShop.data.network.dto.sync.ProductSalePriceSyncDto
import com.example.solarShop.data.network.dto.sync.ProductSaleTransactionSyncDto
import com.example.solarShop.data.network.dto.sync.ProductSyncDto
import com.example.solarShop.data.network.dto.sync.ProductUnitSyncDto
import com.example.solarShop.data.network.dto.sync.RegisterDeviceRequestDto
import com.example.solarShop.data.network.remote.SyncApi
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.file.FileSyncRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import com.example.solarShop.data.repository.sales.ProductSaleTransactionRepository
import com.example.solarShop.data.repository.sync.SyncRepository
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentDao
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceItemEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton


data class SyncProgress(
    val step: Int,
    val totalSteps: Int,
    val title: String,
    val count: Int = 0,
    val percent: Float = 0f,
    val log: String = "",
    val imageIndex: Int = 0,
    val imageTotal: Int = 0
)

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
    private val productSaleTransactionRepository: ProductSaleTransactionRepository,
    private val clientRepository: ClientRepository,
    private val orderRepository: OrderRepository,
    private val invoiceDao: InvoiceDocumentDao,
    private val userRepository: UserRepository,
) {


    private val syncMutex = Mutex()
    private var lastAutoSyncStartedAt: Long = 0L

    private suspend fun requireCurrentUserKey(): String {
        return userRepository
            .observeCurrentUser()
            .first()
            ?.userKey
            ?.takeIf { it.isNotBlank() }
            ?: error(
                "Current local user is not available for client sync"
            )
    }

    private data class PullWindow(
        val since: Long,
        val upperBound: Long
    )

    private suspend fun createPullWindow(
        entityKey: String
    ): PullWindow {

        /*
         * زمان سقف را قبل از GET می‌گیریم.
         * هر تغییری که بعد از این زمان روی سرور رخ دهد،
         * در Sync بعدی دوباره قابل دریافت خواهد بود.
         */
        val upperBound =
            syncApi.getStatus().serverTime

        val cursor =
            syncRepository.getEntityLastSyncAt(
                entityKey
            )

        return PullWindow(
            since = (
                    cursor -
                            5_000L
                    ).coerceAtLeast(0L),

            upperBound = upperBound
        )
    }

    private suspend fun finishPullWindow(
        entityKey: String,
        window: PullWindow,
        fullyApplied: Boolean
    ) {
        if (!fullyApplied) {
            Log.w(
                "SYNC_TEST",
                "Cursor not advanced for $entityKey because some rows were skipped"
            )

            return
        }

        syncRepository.updateEntityLastSyncAt(
            entityKey = entityKey,
            value = window.upperBound
        )
    }

    private companion object {

        const val CURSOR_CLIENTS =
            "clients"

        const val CURSOR_ORDERS =
            "orders"

        const val CURSOR_INVOICE_DOCUMENTS =
            "invoice_documents"

        const val CURSOR_INVOICE_ITEMS =
            "invoice_items"
    }

    private suspend fun registerDevice(): Boolean {
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

    private suspend fun pushUnsyncedCategories(): Boolean {
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

    private suspend fun pullCategories(): Int {
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

    private suspend fun pullBrands(): Int {
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

    private suspend fun pushUnsyncedBrands(): Boolean {
        val brands = productRepository.getUnsyncedBrands()

        if (brands.isEmpty()) {
            Log.d("SYNC_TEST", "Unsynced Brands = 0")
            return true
        }

        val dtoList = brands.mapNotNull { brand ->
            brand.uid?.takeIf { it.isNotBlank() }
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

    private suspend fun pullProducts(): Int {
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

    private suspend fun pushUnsyncedProducts(): Boolean {
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

    private suspend fun pullProductImages(): Int {
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

    private suspend fun pullProductImagesWithProgress(
        step: Int,
        totalSteps: Int,
        onProgress: suspend (SyncProgress) -> Unit
    ): Int {
        val lastSyncAt = syncRepository.getLastSyncAt()
        val images = syncApi.pullProductImages(lastSyncAt)

        onProgress(
            SyncProgress(
                step = step,
                totalSteps = totalSteps,
                title = "دریافت تصاویر کالاها",
                count = images.size,
                percent = (step - 1) / totalSteps.toFloat(),
                log = "تعداد تصاویر پیدا شده: ${images.size}",
                imageIndex = 0,
                imageTotal = images.size
            )
        )

        var downloaded = 0

        images.forEachIndexed { index, dto ->
            val product = productRepository.getProductByUid(dto.productUid)

            if (product == null) {
                onProgress(
                    SyncProgress(
                        step = step,
                        totalSteps = totalSteps,
                        title = "دریافت تصاویر کالاها",
                        count = images.size,
                        percent = ((step - 1) + ((index + 1f) / images.size.coerceAtLeast(1))) / totalSteps,
                        log = "تصویر رد شد؛ کالا پیدا نشد",
                        imageIndex = index + 1,
                        imageTotal = images.size
                    )
                )
                return@forEachIndexed
            }

            productImageRepository.upsertProductImageByUid(
                ProductImageEntity(
                    id = null,
                    uid = dto.uid,
                    productId = product.id ?: return@forEachIndexed,
                    fileName = dto.fileName,
                    sortOrder = dto.sortOrder,
                    createdAt = dto.updatedAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true
                )
            )

            val ok = fileSyncRepository.downloadIfMissing(dto.fileName)
            if (ok) downloaded++

            onProgress(
                SyncProgress(
                    step = step,
                    totalSteps = totalSteps,
                    title = "دریافت تصاویر کالاها",
                    count = images.size,
                    percent = ((step - 1) + ((index + 1f) / images.size.coerceAtLeast(1))) / totalSteps,
                    log = "تصویر ${index + 1} از ${images.size} دریافت شد",
                    imageIndex = index + 1,
                    imageTotal = images.size
                )
            )
        }

        return downloaded
    }

    private suspend fun pushUnsyncedProductImages(): Boolean {
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

            val productUid = product.uid.takeIf { it.isNotBlank() }
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

    private suspend fun pullInventoryTransactions(): Int {

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

    private suspend fun pushUnsyncedInventoryTransactions(): Boolean {

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

    private suspend fun pullPurchasePrices(): Int {

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

    private suspend fun pushUnsyncedPurchasePrices(): Boolean {

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

    private suspend fun pullSalePrices(): Int {

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

    private suspend fun pushUnsyncedSalePrices(): Boolean {

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

    private suspend fun pullProductSaleTransactions(): Int {

        val lastSyncAt =
            syncRepository.getLastSyncAt()

        Log.d(
            "SYNC_TEST",
            "Pull ProductSaleTransactions Since = $lastSyncAt"
        )

        val items =
            syncApi.pullProductSaleTransactions(lastSyncAt)

        Log.d(
            "SYNC_TEST",
            "Received ProductSaleTransactions = ${items.size}"
        )

        items.forEach { dto ->

            val product =
                productRepository.getProductByUid(
                    dto.productUid
                )

            if (product == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped ProductSaleTransaction, missing productUid = ${dto.productUid}"
                )
                return@forEach
            }

            productSaleTransactionRepository.upsertSaleTransactionByUid(
                ProductSaleTransactionEntity(
                    id = null,
                    uid = dto.uid,

                    productId = product.id ?: return@forEach,

                    inventoryTransactionUid = dto.inventoryTransactionUid,

                    quantity = dto.quantity,

                    priceType = dto.priceType,

                    unitSalePriceToman = dto.unitSalePriceToman,
                    totalSalePriceToman = dto.totalSalePriceToman,

                    saleDollarRateToman = dto.saleDollarRateToman,

                    purchasePriceUid = dto.purchasePriceUid,
                    salePriceUid = dto.salePriceUid,

                    buyPriceDollar = dto.buyPriceDollar,
                    buyPriceToman = dto.buyPriceToman,
                    purchaseDollarRateToman = dto.purchaseDollarRateToman,

                    unitSalePriceDollar = dto.unitSalePriceDollar,

                    unitProfitToman = dto.unitProfitToman,
                    totalProfitToman = dto.totalProfitToman,

                    unitProfitDollar = dto.unitProfitDollar,
                    totalProfitDollar = dto.totalProfitDollar,

                    profitPercentByToman = dto.profitPercentByToman,
                    profitPercentByDollar = dto.profitPercentByDollar,

                    soldAt = dto.soldAt,

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

    private suspend fun pushUnsyncedProductSaleTransactions(): Boolean {

        val items =
            productSaleTransactionRepository.getUnsyncedSaleTransactions()

        if (items.isEmpty()) {
            Log.d(
                "SYNC_TEST",
                "Unsynced ProductSaleTransactions = 0"
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
                    product.uid
                        .takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                ProductSaleTransactionSyncDto(
                    uid = item.uid,

                    productUid = productUid,

                    inventoryTransactionUid = item.inventoryTransactionUid,

                    quantity = item.quantity,

                    priceType = item.priceType,

                    unitSalePriceToman = item.unitSalePriceToman,
                    totalSalePriceToman = item.totalSalePriceToman,

                    saleDollarRateToman = item.saleDollarRateToman,

                    purchasePriceUid = item.purchasePriceUid,
                    salePriceUid = item.salePriceUid,

                    buyPriceDollar = item.buyPriceDollar,
                    buyPriceToman = item.buyPriceToman,
                    purchaseDollarRateToman = item.purchaseDollarRateToman,

                    unitSalePriceDollar = item.unitSalePriceDollar,

                    unitProfitToman = item.unitProfitToman,
                    totalProfitToman = item.totalProfitToman,

                    unitProfitDollar = item.unitProfitDollar,
                    totalProfitDollar = item.totalProfitDollar,

                    profitPercentByToman = item.profitPercentByToman,
                    profitPercentByDollar = item.profitPercentByDollar,

                    soldAt = item.soldAt,

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
            syncApi.pushProductSaleTransactions(dtoList)

        if (success) {
            productSaleTransactionRepository.markSaleTransactionsSynced(
                dtoList.map { it.uid }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed ProductSaleTransactions = ${dtoList.size}, success=$success"
        )

        return success
    }

    private suspend fun pullCategoryAttributeDefinitions(): Int {

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

    private suspend fun pushUnsyncedCategoryAttributeDefinitions(): Boolean {

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

    private suspend fun pullProductAttributeValues(): Int {

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

    private suspend fun pushUnsyncedProductAttributeValues(): Boolean {

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

    private suspend fun pullUnits(): Int {
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

    private suspend fun pushUnsyncedUnits(): Boolean {
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

    private suspend fun pushUnsyncedCurrencyRates(): Boolean {

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

    private suspend fun pushUnsyncedClients(): Boolean {

        val items =
            clientRepository.getUnsyncedClients()

        if (items.isEmpty()) {
            Log.d(
                "SYNC_TEST",
                "Unsynced Clients = 0"
            )

            return true
        }

        val dtoList =
            items.map { item ->

                ClientSyncDto(
                    uid = item.uid,

                    name = item.name,
                    mobilePhone = item.mobilePhone,
                    landlinePhone = item.landlinePhone,
                    nationalCode = item.nationalCode,
                    workshop = item.workshop,
                    address = item.address,
                    note = item.note,

                    archive = item.archive,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        val success =
            syncApi.pushClients(dtoList)

        if (success) {
            clientRepository.markClientsSynced(
                dtoList.map { dto ->
                    dto.uid
                }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed Clients = ${dtoList.size}, success=$success"
        )

        return success
    }

    private suspend fun pullClients(): Int {

        val window =
            createPullWindow(
                CURSOR_CLIENTS
            )

        val localUserKey =
            requireCurrentUserKey()

        val items =
            syncApi.pullClients(
                window.since
            )

        items.forEach { dto ->

            clientRepository.upsertClientByUid(
                ClientEntity(
                    id = null,

                    /*
                     * فقط وابستگی محلی Room است.
                     */
                    userKey = localUserKey,

                    name = dto.name,
                    mobilePhone = dto.mobilePhone,
                    landlinePhone = dto.landlinePhone,
                    nationalCode = dto.nationalCode,
                    workshop = dto.workshop,
                    address = dto.address,

                    /*
                     * تصویر مشتری هنوز Sync نمی‌شود.
                     */
                    avatar = "",

                    note = dto.note,
                    archive = dto.archive,

                    createdAt = dto.createdAt,

                    uid = dto.uid,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true,

                    createdByUserId =
                    dto.createdByUserId,

                    updatedByUserId =
                    dto.updatedByUserId,

                    shopUid = dto.shopUid
                )
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pulled Clients = ${items.size}"
        )

        finishPullWindow(
            entityKey = CURSOR_CLIENTS,
            window = window,
            fullyApplied = true
        )

        return items.size
    }

    private suspend fun pushUnsyncedOrders(): Boolean {

        val items =
            orderRepository.getUnsyncedOrders()

        if (items.isEmpty()) {
            Log.d(
                "SYNC_TEST",
                "Unsynced Orders = 0"
            )

            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                /*
                 * شامل مشتری soft-delete شده نیز می‌شود.
                 */
                val client =
                    clientRepository.getClientByIdForSync(
                        item.clientId
                    )

                if (client == null) {
                    Log.d(
                        "SYNC_TEST",
                        "Skipped Order, missing local clientId=${item.clientId}"
                    )

                    return@mapNotNull null
                }

                OrderSyncDto(
                    uid = item.uid,
                    clientUid = client.uid,

                    name = item.name,
                    note = item.note,

                    status = item.status,
                    archive = item.archive,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushOrders(dtoList)

        if (success) {
            orderRepository.markOrdersSynced(
                dtoList.map { dto ->
                    dto.uid
                }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed Orders = ${dtoList.size}, success=$success"
        )

        return success
    }

    private suspend fun pullOrders(): Int {

        val window =
            createPullWindow(
                CURSOR_ORDERS
            )

        val items =
            syncApi.pullOrders(
                window.since
            )

        var appliedCount =
            0

        items.forEach { dto ->

            /*
             * getClientByUid رکورد tombstone را هم پیدا می‌کند.
             */
            val client =
                clientRepository.getClientByUid(
                    dto.clientUid
                )

            val clientId =
                client?.id

            if (clientId == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped Order, missing clientUid=${dto.clientUid}"
                )

                return@forEach
            }

            orderRepository.upsertOrderByUid(
                OrderEntity(
                    id = null,
                    clientId = clientId,

                    name = dto.name,
                    note = dto.note,

                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,

                    status = dto.status,
                    archive = dto.archive,

                    uid = dto.uid,
                    deletedAt = dto.deletedAt,
                    isSynced = true,

                    createdByUserId =
                    dto.createdByUserId,

                    updatedByUserId =
                    dto.updatedByUserId,

                    shopUid = dto.shopUid
                )
            )

            appliedCount++
        }

        Log.d(
            "SYNC_TEST",
            "Pulled Orders = $appliedCount/${items.size}"
        )

        finishPullWindow(
            entityKey = CURSOR_ORDERS,
            window = window,
            fullyApplied =
            appliedCount == items.size
        )

        return appliedCount
    }

    private suspend fun pushUnsyncedInvoiceDocuments():
            Boolean {

        val items =
            invoiceDao.getUnsyncedInvoices()

        if (items.isEmpty()) {
            Log.d(
                "SYNC_TEST",
                "Unsynced InvoiceDocuments = 0"
            )

            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                val order =
                    orderRepository.getOrderByIdForSync(
                        item.orderId
                    )

                if (order == null) {
                    Log.d(
                        "SYNC_TEST",
                        "Skipped InvoiceDocument, missing orderId=${item.orderId}"
                    )

                    return@mapNotNull null
                }

                InvoiceDocumentSyncDto(
                    uid = item.uid,
                    orderUid = order.uid,

                    type = item.type.name,
                    number = item.number,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,

                    status = item.status.name,

                    sellerLabel = item.sellerLabel,
                    sellerName = item.sellerName,
                    sellerPhone = item.sellerPhone,
                    sellerAddress = item.sellerAddress,
                    sellerNationalId =
                    item.sellerNationalId,

                    sellerEconomicCode =
                    item.sellerEconomicCode,

                    buyerLabel = item.buyerLabel,
                    buyerName = item.buyerName,
                    buyerPhone = item.buyerPhone,
                    buyerAddress = item.buyerAddress,
                    buyerNationalId =
                    item.buyerNationalId,

                    subtotalBeforeDiscount =
                    item.subtotalBeforeDiscount,

                    totalDiscount =
                    item.totalDiscount,

                    totalBeforeTax =
                    item.totalBeforeTax,

                    totalTax = item.totalTax,
                    totalFinal = item.totalFinal,

                    notes = item.notes,
                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushInvoiceDocuments(dtoList)

        if (success) {
            invoiceDao.markInvoicesSynced(
                dtoList.map { dto ->
                    dto.uid
                }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed InvoiceDocuments = ${dtoList.size}, success=$success"
        )

        return success
    }

    private suspend fun pullInvoiceDocuments(): Int {

        val window =
            createPullWindow(
                CURSOR_INVOICE_DOCUMENTS
            )

        val items =
            syncApi.pullInvoiceDocuments(
                window.since
            )

        var appliedCount =
            0

        items.forEach { dto ->

            val order =
                orderRepository.getOrderByUid(
                    dto.orderUid
                )

            val orderId =
                order?.id

            if (orderId == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped InvoiceDocument, missing orderUid=${dto.orderUid}"
                )

                return@forEach
            }

            val type =
                runCatching {
                    InvoiceType.valueOf(dto.type)
                }.getOrNull()

            if (type == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped InvoiceDocument, invalid type=${dto.type}"
                )

                return@forEach
            }

            val status =
                runCatching {
                    InvoiceStatus.valueOf(dto.status)
                }.getOrDefault(
                    InvoiceStatus.DRAFT
                )

            val existing =
                invoiceDao.getInvoiceByUid(dto.uid)

            /*
             * tombstone محلی با نسخه زنده قدیمی احیا نمی‌شود.
             */
            if (
                existing?.deletedAt != null &&
                dto.deletedAt == null
            ) {
                return@forEach
            }

            /*
             * تغییر محلی جدیدتر حفظ می‌شود.
             */
            if (
                existing != null &&
                !existing.isSynced &&
                existing.updatedAt > dto.updatedAt
            ) {
                return@forEach
            }

            val remoteEntity =
                InvoiceDocumentEntity(
                    id = existing?.id ?: 0,
                    orderId = orderId,

                    /*
                     * تمپلیت محلی در Pull حفظ می‌شود.
                     */
                    templateId =
                    existing?.templateId,

                    type = type,
                    number = dto.number,

                    /*
                     * این createdAt تاریخ خود سند است
                     * و باید از سرور دریافت شود.
                     */
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,

                    status = status,

                    sellerLabel = dto.sellerLabel,
                    sellerName = dto.sellerName,
                    sellerPhone = dto.sellerPhone,
                    sellerAddress = dto.sellerAddress,
                    sellerNationalId =
                    dto.sellerNationalId,

                    sellerEconomicCode =
                    dto.sellerEconomicCode,

                    buyerLabel = dto.buyerLabel,
                    buyerName = dto.buyerName,
                    buyerPhone = dto.buyerPhone,
                    buyerAddress = dto.buyerAddress,
                    buyerNationalId =
                    dto.buyerNationalId,

                    subtotalBeforeDiscount =
                    dto.subtotalBeforeDiscount,

                    totalDiscount =
                    dto.totalDiscount,

                    totalBeforeTax =
                    dto.totalBeforeTax,

                    totalTax = dto.totalTax,
                    totalFinal = dto.totalFinal,

                    notes = dto.notes,

                    /*
                     * فایل PDF محلی است.
                     */
                    pdfAttachmentId =
                    existing?.pdfAttachmentId,

                    uid = dto.uid,
                    deletedAt = dto.deletedAt,
                    isSynced = true,

                    createdByUserId =
                    dto.createdByUserId,

                    updatedByUserId =
                    dto.updatedByUserId,

                    shopUid = dto.shopUid
                )

            if (existing == null) {
                invoiceDao.insertInvoice(
                    remoteEntity
                )
            } else {
                invoiceDao.updateInvoice(
                    remoteEntity
                )
            }

            appliedCount++
        }

        Log.d(
            "SYNC_TEST",
            "Pulled InvoiceDocuments = $appliedCount/${items.size}"
        )

        finishPullWindow(
            entityKey = CURSOR_INVOICE_DOCUMENTS,
            window = window,
            fullyApplied =
            appliedCount == items.size
        )

        return appliedCount
    }

    private suspend fun pushUnsyncedInvoiceItems():
            Boolean {

        val items =
            invoiceDao.getUnsyncedInvoiceItems()

        if (items.isEmpty()) {
            Log.d(
                "SYNC_TEST",
                "Unsynced InvoiceItems = 0"
            )

            return true
        }

        val dtoList =
            items.mapNotNull { item ->

                val invoice =
                    invoiceDao.getInvoiceByIdForSync(
                        item.invoiceId
                    )

                if (invoice == null) {
                    Log.d(
                        "SYNC_TEST",
                        "Skipped InvoiceItem, missing invoiceId=${item.invoiceId}"
                    )

                    return@mapNotNull null
                }

                InvoiceItemSyncDto(
                    uid = item.uid,
                    invoiceUid = invoice.uid,

                    rowIndex = item.rowIndex,
                    description = item.description,

                    quantity = item.quantity,
                    unit = item.unit,

                    unitPrice = item.unitPrice,

                    rowDiscount =
                    item.rowDiscount,

                    rowSubtotal =
                    item.rowSubtotal,

                    taxPercent =
                    item.taxPercent,

                    taxAmount =
                    item.taxAmount,

                    rowTotal =
                    item.rowTotal,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        if (dtoList.isEmpty()) {
            return true
        }

        val success =
            syncApi.pushInvoiceItems(dtoList)

        if (success) {
            invoiceDao.markInvoiceItemsSynced(
                dtoList.map { dto ->
                    dto.uid
                }
            )
        }

        Log.d(
            "SYNC_TEST",
            "Pushed InvoiceItems = ${dtoList.size}, success=$success"
        )

        return success
    }

    private suspend fun pullInvoiceItems(): Int {

        val window =
            createPullWindow(
                CURSOR_INVOICE_ITEMS
            )

        val items =
            syncApi.pullInvoiceItems(
                window.since
            )

        var appliedCount =
            0

        items.forEach { dto ->

            /*
             * getInvoiceByUid رکورد حذف‌شده را هم پیدا می‌کند.
             */
            val invoice =
                invoiceDao.getInvoiceByUid(
                    dto.invoiceUid
                )

            val invoiceId =
                invoice?.id

            if (invoiceId == null) {
                Log.d(
                    "SYNC_TEST",
                    "Skipped InvoiceItem, missing invoiceUid=${dto.invoiceUid}"
                )

                return@forEach
            }

            val existing =
                invoiceDao.getItemByUid(dto.uid)

            if (
                existing?.deletedAt != null &&
                dto.deletedAt == null
            ) {
                return@forEach
            }

            if (
                existing != null &&
                !existing.isSynced &&
                existing.updatedAt > dto.updatedAt
            ) {
                return@forEach
            }

            val remoteEntity =
                InvoiceItemEntity(
                    id = existing?.id ?: 0,

                    /*
                     * بسیار مهم:
                     * هویت ردیف دریافتی باید دقیقاً از سرور حفظ شود.
                     */
                    uid = dto.uid,

                    invoiceId = invoiceId,

                    rowIndex = dto.rowIndex,
                    description = dto.description,

                    quantity = dto.quantity,
                    unit = dto.unit,

                    unitPrice = dto.unitPrice,

                    rowDiscount =
                    dto.rowDiscount,

                    rowSubtotal =
                    dto.rowSubtotal,

                    taxPercent =
                    dto.taxPercent,

                    taxAmount =
                    dto.taxAmount,

                    rowTotal =
                    dto.rowTotal,

                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    isSynced = true,

                    createdByUserId =
                    dto.createdByUserId,

                    updatedByUserId =
                    dto.updatedByUserId,

                    shopUid = dto.shopUid
                )

            if (existing == null) {
                invoiceDao.insertItems(
                    listOf(remoteEntity)
                )
            } else {
                invoiceDao.updateItem(
                    remoteEntity
                )
            }

            appliedCount++
        }

        Log.d(
            "SYNC_TEST",
            "Pulled InvoiceItems = $appliedCount/${items.size}"
        )

        finishPullWindow(
            entityKey = CURSOR_INVOICE_ITEMS,
            window = window,
            fullyApplied =
            appliedCount == items.size
        )

        return appliedCount
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
        pullProductSaleTransactions()
        pullCategoryAttributeDefinitions()
        pullProductAttributeValues()
        pullUnits()

        // داده‌های فروشگاه؛ والدها قبل از فرزندها
        pullClients()
        pullOrders()
        pullInvoiceDocuments()
        pullInvoiceItems()


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

        val productSaleTransactionsPushed =
            pushUnsyncedProductSaleTransactions()
        if (!productSaleTransactionsPushed) return false

        val attributeDefinitionsPushed =
            pushUnsyncedCategoryAttributeDefinitions()
        if (!attributeDefinitionsPushed) return false

        val productAttributeValuesPushed =
            pushUnsyncedProductAttributeValues()
        if (!productAttributeValuesPushed) return false

        val unitsPushed =
            pushUnsyncedUnits()
        if (!unitsPushed) return false


        val clientsPushed =
            pushUnsyncedClients()

        if (!clientsPushed) {
            Log.e(
                "SYNC_TEST",
                "Clients push failed"
            )

            return false
        }

        val ordersPushed =
            pushUnsyncedOrders()

        if (!ordersPushed) {
            Log.e(
                "SYNC_TEST",
                "Orders push failed"
            )

            return false
        }

        val invoiceDocumentsPushed =
            pushUnsyncedInvoiceDocuments()

        if (!invoiceDocumentsPushed) {
            Log.e(
                "SYNC_TEST",
                "Invoice documents push failed"
            )

            return false
        }

        val invoiceItemsPushed =
            pushUnsyncedInvoiceItems()

        if (!invoiceItemsPushed) {
            Log.e(
                "SYNC_TEST",
                "Invoice items push failed"
            )

            return false
        }


        val status = syncApi.getStatus()

        syncRepository.updateLastSyncAt(status.serverTime)

        return true
    }

    suspend fun fullUploadAllToServer(): Boolean {
        val registered = registerDevice()
        if (!registered) return false

        // 1) والدها اول
        val categories = productRepository.getAllCategoriesForBackup()
        val categoryDtos = categories.mapNotNull { category ->
            val uid = category.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            fileSyncRepository.uploadIfNeeded(category.imageFileName)

            CategorySyncDto(
                uid = uid,
                name = category.name,
                imageFileName = category.imageFileName,
                sortOrder = category.sortOrder,
                deletedAt = category.deletedAt,
                updatedAt = category.updatedAt
            )
        }

        if (!syncApi.pushCategories(categoryDtos)) return false
        productRepository.markCategoriesSynced(categoryDtos.map { it.uid })

        val brands = productRepository.getAllBrandsForBackup()
        val brandDtos = brands.mapNotNull { brand ->
            val uid = brand.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            fileSyncRepository.uploadIfNeeded(brand.imageFileName)

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

        if (!syncApi.pushBrands(brandDtos)) return false
        productRepository.markBrandsSynced(brandDtos.map { it.uid })

        val units = productRepository.getAllUnitsForBackup()
        val unitDtos = units.map { unit ->
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

        if (!syncApi.pushUnits(unitDtos)) return false
        productRepository.markUnitsSynced(unitDtos.map { it.uid })

        // 2) محصولات بعد از category/brand
        val products = productRepository.getAllProductsForBackup()
        val productDtos = products.mapNotNull { product ->
            val category = productRepository.getCategoryById(product.categoryId)
                ?: return@mapNotNull null

            val categoryUid = category.uid?.takeIf { it.isNotBlank() }
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

        if (!syncApi.pushProducts(productDtos)) return false
        productRepository.markProductsSynced(productDtos.map { it.uid })


        // 3) Attribute Definitions بعد از Category
        val attributeDefinitions =
            attributeRepository.getAllAttributeDefinitionsForBackup()

        val attributeDefinitionDtos =
            attributeDefinitions.mapNotNull { item ->

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

        if (!syncApi.pushCategoryAttributeDefinitions(attributeDefinitionDtos)) return false

        attributeRepository.markAttributeDefinitionsSynced(
            attributeDefinitionDtos.map { it.uid }
        )


// 4) Product Attribute Values بعد از Product و AttributeDefinition
        val attributeValues =
            attributeRepository.getAllAttributeValuesForBackup()

        val attributeValueDtos =
            attributeValues.mapNotNull { item ->

                val product =
                    productRepository.getProductById(item.productId)
                        ?: return@mapNotNull null

                val productUid =
                    product.uid.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val definition =
                    attributeRepository.getAttributeDefinitionById(
                        item.attributeDefinitionId
                    ) ?: return@mapNotNull null

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

        if (!syncApi.pushProductAttributeValues(attributeValueDtos)) return false

        attributeRepository.markProductAttributeValuesSynced(
            attributeValueDtos.map { it.uid }
        )

// 5) Purchase Prices
        val purchasePrices = pricingRepository.getAllPurchasePricesForBackup()

        val purchasePriceDtos = purchasePrices.mapNotNull { item ->
            val product = productRepository.getProductById(item.productId)
                ?: return@mapNotNull null

            val productUid = product.uid.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            ProductPurchasePriceSyncDto(
                uid = item.uid,
                productUid = productUid,
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

        if (purchasePriceDtos.isNotEmpty()) {
            if (!syncApi.pushPurchasePrices(purchasePriceDtos)) return false
            pricingRepository.markPurchasePricesSynced(
                purchasePriceDtos.map { it.uid }
            )
        }


// 6) Sale Prices
        val salePrices = pricingRepository.getAllSalePricesForBackup()

        val salePriceDtos = salePrices.mapNotNull { item ->
            val product = productRepository.getProductById(item.productId)
                ?: return@mapNotNull null

            val productUid = product.uid.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            ProductSalePriceSyncDto(
                uid = item.uid,
                productUid = productUid,
                priceType = item.priceType,
                salePriceToman = item.salePriceToman,
                profitPercent = item.profitPercent,
                baseDollarPrice = item.baseDollarPrice,
                dollarRateToman = item.dollarRateToman,
                basePurchasePriceToman = item.basePurchasePriceToman,
                note = item.note,
                isActive = item.isActive,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                deletedAt = item.deletedAt
            )
        }

        if (salePriceDtos.isNotEmpty()) {
            if (!syncApi.pushSalePrices(salePriceDtos)) return false
            pricingRepository.markSalePricesSynced(
                salePriceDtos.map { it.uid }
            )
        }


// 7) Currency Rates
        val currencyRates = pricingRepository.getAllCurrencyRatesForBackup()

        val currencyRateDtos = currencyRates.map { item ->
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

        if (currencyRateDtos.isNotEmpty()) {
            if (!syncApi.pushCurrencyRates(currencyRateDtos)) return false
            pricingRepository.markCurrencyRatesSynced(
                currencyRateDtos.map { it.uid }
            )
        }

        // 8) Inventory Transactions بعد از Product
        val inventoryTransactions =
            inventoryRepository.getAllInventoryTransactionsForBackup()

        val inventoryTransactionDtos =
            inventoryTransactions.mapNotNull { item ->

                val product =
                    productRepository.getProductById(item.productId)
                        ?: return@mapNotNull null

                val productUid =
                    product.uid.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                InventoryTransactionSyncDto(
                    uid = item.uid,
                    productUid = productUid,
                    quantity = item.quantity,
                    transactionType = item.transactionType.name,
                    note = item.note,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt
                )
            }

        if (inventoryTransactionDtos.isNotEmpty()) {
            if (!syncApi.pushInventoryTransactions(inventoryTransactionDtos)) return false

            inventoryRepository.markInventoryTransactionsSynced(
                inventoryTransactionDtos.map { it.uid }
            )
        }

        // 3) عکس‌های محصول بعد از Product
        val images = productImageRepository.getAllImagesForBackup()
        val imageDtos = images.mapNotNull { image ->
            val uid = image.uid?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val product = productRepository.getProductById(image.productId)
                ?: return@mapNotNull null

            val productUid = product.uid.takeIf { it.isNotBlank() }
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

        if (!syncApi.pushProductImages(imageDtos)) return false
        productImageRepository.markProductImagesSynced(imageDtos.map { it.uid })

        // =========================================================
// Clients full upload
// =========================================================

        val allClients =
            clientRepository.getAllClientsForSync()

        val clientDtos =
            allClients.map { item ->

                ClientSyncDto(
                    uid = item.uid,

                    name = item.name,
                    mobilePhone = item.mobilePhone,
                    landlinePhone = item.landlinePhone,
                    nationalCode = item.nationalCode,
                    workshop = item.workshop,
                    address = item.address,
                    note = item.note,

                    archive = item.archive,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        if (clientDtos.isNotEmpty()) {

            val success =
                syncApi.pushClients(clientDtos)

            if (!success) {
                Log.e(
                    "SYNC_TEST",
                    "Full upload clients failed"
                )

                return false
            }

            clientRepository.markClientsSynced(
                clientDtos.map { dto ->
                    dto.uid
                }
            )
        }

        // =========================================================
// Orders full upload
// =========================================================

        val allOrders =
            orderRepository.getAllOrdersForSync()

        val orderDtos =
            allOrders.mapNotNull { item ->

                val client =
                    clientRepository.getClientByIdForSync(
                        item.clientId
                    )

                if (client == null) {
                    Log.e(
                        "SYNC_TEST",
                        "Full upload skipped order uid=${item.uid}, missing clientId=${item.clientId}"
                    )

                    return@mapNotNull null
                }

                OrderSyncDto(
                    uid = item.uid,
                    clientUid = client.uid,

                    name = item.name,
                    note = item.note,

                    status = item.status,
                    archive = item.archive,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        if (orderDtos.isNotEmpty()) {

            val success =
                syncApi.pushOrders(orderDtos)

            if (!success) {
                Log.e(
                    "SYNC_TEST",
                    "Full upload orders failed"
                )

                return false
            }

            orderRepository.markOrdersSynced(
                orderDtos.map { dto ->
                    dto.uid
                }
            )
        }

        // =========================================================
// Invoice documents full upload
// =========================================================

        val allInvoiceDocuments =
            invoiceDao.getAllInvoicesForSync()

        val invoiceDocumentDtos =
            allInvoiceDocuments.mapNotNull { item ->

                val order =
                    orderRepository.getOrderByIdForSync(
                        item.orderId
                    )

                if (order == null) {
                    Log.e(
                        "SYNC_TEST",
                        "Full upload skipped invoice uid=${item.uid}, missing orderId=${item.orderId}"
                    )

                    return@mapNotNull null
                }

                InvoiceDocumentSyncDto(
                    uid = item.uid,
                    orderUid = order.uid,

                    type = item.type.name,
                    number = item.number,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,

                    status = item.status.name,

                    sellerLabel = item.sellerLabel,
                    sellerName = item.sellerName,
                    sellerPhone = item.sellerPhone,
                    sellerAddress = item.sellerAddress,
                    sellerNationalId =
                    item.sellerNationalId,

                    sellerEconomicCode =
                    item.sellerEconomicCode,

                    buyerLabel = item.buyerLabel,
                    buyerName = item.buyerName,
                    buyerPhone = item.buyerPhone,
                    buyerAddress = item.buyerAddress,
                    buyerNationalId =
                    item.buyerNationalId,

                    subtotalBeforeDiscount =
                    item.subtotalBeforeDiscount,

                    totalDiscount =
                    item.totalDiscount,

                    totalBeforeTax =
                    item.totalBeforeTax,

                    totalTax =
                    item.totalTax,

                    totalFinal =
                    item.totalFinal,

                    notes = item.notes,

                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        if (invoiceDocumentDtos.isNotEmpty()) {

            val success =
                syncApi.pushInvoiceDocuments(
                    invoiceDocumentDtos
                )

            if (!success) {
                Log.e(
                    "SYNC_TEST",
                    "Full upload invoice documents failed"
                )

                return false
            }

            invoiceDao.markInvoicesSynced(
                invoiceDocumentDtos.map { dto ->
                    dto.uid
                }
            )
        }

// =========================================================
// Invoice items full upload
// =========================================================

        val allInvoiceItems =
            invoiceDao.getAllInvoiceItemsForSync()

        val invoiceItemDtos =
            allInvoiceItems.mapNotNull { item ->

                val invoice =
                    invoiceDao.getInvoiceByIdForSync(
                        item.invoiceId
                    )

                if (invoice == null) {
                    Log.e(
                        "SYNC_TEST",
                        "Full upload skipped invoice item uid=${item.uid}, missing invoiceId=${item.invoiceId}"
                    )

                    return@mapNotNull null
                }

                InvoiceItemSyncDto(
                    uid = item.uid,
                    invoiceUid = invoice.uid,

                    rowIndex = item.rowIndex,
                    description = item.description,

                    quantity = item.quantity,
                    unit = item.unit,

                    unitPrice = item.unitPrice,

                    rowDiscount =
                    item.rowDiscount,

                    rowSubtotal =
                    item.rowSubtotal,

                    taxPercent =
                    item.taxPercent,

                    taxAmount =
                    item.taxAmount,

                    rowTotal =
                    item.rowTotal,

                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt,

                    createdByUserId =
                    item.createdByUserId,

                    updatedByUserId =
                    item.updatedByUserId,

                    shopUid = item.shopUid
                )
            }

        if (invoiceItemDtos.isNotEmpty()) {

            val success =
                syncApi.pushInvoiceItems(
                    invoiceItemDtos
                )

            if (!success) {
                Log.d(
                    "SYNC_TEST",
                    """
    Full Upload Completed:
    categories=${categoryDtos.size},
    brands=${brandDtos.size},
    units=${unitDtos.size},
    products=${productDtos.size},
    attributes=${attributeDefinitionDtos.size},
    attributeValues=${attributeValueDtos.size},
    purchasePrices=${purchasePriceDtos.size},
    salePrices=${salePriceDtos.size},
    currencyRates=${currencyRateDtos.size},
    inventory=${inventoryTransactionDtos.size},
    images=${imageDtos.size},
    clients=${clientDtos.size},
    orders=${orderDtos.size},
    invoiceDocuments=${invoiceDocumentDtos.size},
    invoiceItems=${invoiceItemDtos.size}
    """.trimIndent()
                )

                return false
            }

            invoiceDao.markInvoiceItemsSynced(
                invoiceItemDtos.map { dto ->
                    dto.uid
                }
            )
        }


        val status = syncApi.getStatus()
        syncRepository.updateLastSyncAt(status.serverTime)

        Log.d(
            "SYNC_TEST",
            "Full Upload Completed: categories=${categoryDtos.size}, brands=${brandDtos.size}, units=${unitDtos.size}, products=${productDtos.size}, attributes=${attributeDefinitionDtos.size}, attributeValues=${attributeValueDtos.size}, purchasePrices=${purchasePriceDtos.size}, salePrices=${salePriceDtos.size}, currencyRates=${currencyRateDtos.size}, inventory=${inventoryTransactionDtos.size}, images=${imageDtos.size}"
        )

        return true
    }

    suspend fun pullAllFromServerWithProgress(
        onProgress: suspend (SyncProgress) -> Unit
    ): Boolean {
        val registered = registerDevice()
        if (!registered) return false

        val total = 15
        var step = 0

        suspend fun runStep(
            title: String,
            block: suspend () -> Int
        ) {
            step++
            onProgress(SyncProgress(step, total, title, 0))
            val count = block()
            onProgress(
                SyncProgress(
                    step = step,
                    totalSteps = total,
                    title = title,
                    count = count,
                    percent = step / total.toFloat(),
                    log = "$title کامل شد؛ تعداد: $count"
                )
            )
        }

        runStep("دریافت دسته‌بندی‌ها") { pullCategories() }
        runStep("دریافت برندها") { pullBrands() }
        runStep("دریافت واحدها") { pullUnits() }
        runStep("دریافت کالاها") { pullProducts() }
        step++
        val downloadedImages = pullProductImagesWithProgress(
            step = step,
            totalSteps = total,
            onProgress = onProgress
        )

        onProgress(
            SyncProgress(
                step = step,
                totalSteps = total,
                title = "دریافت تصاویر کالاها",
                count = downloadedImages,
                percent = step / total.toFloat(),
                log = "تصاویر کامل شد؛ تعداد دانلود شده: $downloadedImages",
                imageIndex = downloadedImages,
                imageTotal = downloadedImages
            )
        )
        runStep("دریافت ویژگی‌های دسته‌بندی") { pullCategoryAttributeDefinitions() }
        runStep("دریافت مقدار ویژگی‌های کالا") { pullProductAttributeValues() }
        runStep("دریافت قیمت‌های خرید") { pullPurchasePrices() }
        runStep("دریافت قیمت‌های فروش") { pullSalePrices() }
        runStep("دریافت موجودی") { pullInventoryTransactions() }
        runStep("دریافت نرخ ارز") { pullCurrencyRates() }

        runStep("دریافت مشتریان") {
            pullClients()
        }

        runStep("دریافت سفارش‌ها") {
            pullOrders()
        }

        runStep("دریافت فاکتورها و پیش‌فاکتورها") {
            pullInvoiceDocuments()
        }

        runStep("دریافت ردیف‌های فاکتور") {
            pullInvoiceItems()
        }

        val status = syncApi.getStatus()
        syncRepository.updateLastSyncAt(status.serverTime)

        return true
    }

    suspend fun autoSyncInBackground(
        reason: String
    ): Boolean {
        val now = System.currentTimeMillis()

        if (now - lastAutoSyncStartedAt < 30_000L) {
            Log.d("SYNC_AUTO", "Skipped auto sync, too soon. reason=$reason")
            return true
        }

        return syncMutex.withLock {
            lastAutoSyncStartedAt = System.currentTimeMillis()

            try {
                Log.d("SYNC_AUTO", "Auto sync started. reason=$reason")

                val result = syncCategoriesOnce()

                Log.d("SYNC_AUTO", "Auto sync finished. result=$result, reason=$reason")

                result
            } catch (t: Throwable) {
                Log.e("SYNC_AUTO", "Auto sync failed. reason=$reason", t)
                false
            }
        }
    }

}