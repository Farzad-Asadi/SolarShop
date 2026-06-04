package com.example.solarShop.feature.product.viewmodel.product

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import com.example.solarShop.feature.product.model.ProductEditImageItem
import com.example.solarShop.repo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val attributeRepository: AttributeRepository,
    private val productRepository: ProductRepository,
    private val pricingRepository: PricingRepository,
    private val inventoryRepository: InventoryRepository,
    private val productImageRepository: ProductImageRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val productId: Int =
        checkNotNull(savedStateHandle["productId"])

    val startedAsNewProduct: Boolean =
        productId == -1

    val isEditMode: Boolean =
        productId != -1

    private val categoryId =
        checkNotNull(
            savedStateHandle.get<Int>("categoryId")
        )

    private val formState = MutableStateFlow(
        ProductEditUiState(
            productId = if (isEditMode) productId else null,
            categoryId = categoryId.takeIf { it != -1 }
        )
    )


    @OptIn(ExperimentalCoroutinesApi::class)
    private val attributesFlow =
        formState
            .map { it.productId to it.categoryId }
            .distinctUntilChanged()
            .flatMapLatest { (currentProductId, currentCategoryId) ->
                if (currentCategoryId == null) {
                    flowOf(emptyList())
                } else {
                    attributeRepository.observeProductAttributeDisplayInfo(
                        productId = currentProductId ?: -1,
                        categoryId = currentCategoryId
                    )
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val imagesFlow =
        formState
            .map { it.productId }
            .distinctUntilChanged()
            .flatMapLatest { currentProductId ->
                if (currentProductId == null) {
                    flowOf(emptyList())
                } else {
                    productImageRepository.observeImagesForProduct(
                        productId = currentProductId
                    )
                }
            }


    @OptIn(ExperimentalCoroutinesApi::class)
    private val purchasePricesFlow =
        formState
            .map { it.productId }
            .distinctUntilChanged()
            .flatMapLatest { currentProductId ->

                if (currentProductId == null) {
                    flowOf(emptyList())
                } else {
                    pricingRepository.observePurchasePrices(
                        currentProductId
                    )
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val salePricesFlow =
        formState
            .map { it.productId }
            .distinctUntilChanged()
            .flatMapLatest { currentProductId ->

                if (currentProductId == null) {
                    flowOf(emptyList())
                } else {
                    pricingRepository.observeSalePrices(
                        currentProductId
                    )
                }
            }

    private val brandsFlow =
        productRepository.observeActiveBrands()

    private val extraDataFlow =
        combine(
            brandsFlow,
            attributesFlow,
            imagesFlow,
            purchasePricesFlow,
            salePricesFlow
        ) { brands, attributes, images, purchasePrices, salePrices ->
            ProductEditExtraData(
                brands = brands,
                attributes = attributes,
                images = images,
                purchasePrices = purchasePrices,
                salePrices = salePrices
            )
        }




    val uiState =
        combine(
            formState,
            extraDataFlow
        ) { form, extra ->

            form.copy(
                brands = extra.brands,
                attributes = extra.attributes,
                attributeValues = extra.attributes.associate {
                    it.attributeDefinitionId to (
                            form.attributeValues[it.attributeDefinitionId]
                                ?: it.valueText.orEmpty()
                            )
                },
                images = extra.images,
                purchasePrices = extra.purchasePrices,
                salePrices = extra.salePrices,
                coverImageFileName = extra.images.firstOrNull()?.fileName
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProductEditUiState(
                    productId = if (isEditMode) productId else null,
                    categoryId = categoryId.takeIf { it != -1 }
                )
            )




    init {
        if (isEditMode) {
            viewModelScope.launch {
                val productFullInfo =
                    productRepository.getProductFullInfo(productId)
                        ?: return@launch

                val product = productFullInfo.product

                val activePrice =
                    pricingRepository.getActivePurchasePrice(productId)

                val currentStock =
                    inventoryRepository.observeCurrentStock(productId).first()

                val activeConsumerSale =
                    pricingRepository.getActiveSalePrice(
                        productId = productId,
                        priceType = "consumer"
                    )

                val activeColleagueSale =
                    pricingRepository.getActiveSalePrice(
                        productId = productId,
                        priceType = "colleague"
                    )

                val latestRate =
                    pricingRepository.getLatestCurrencyRate("USD")

                val buyDollarText =
                    activePrice?.buyPriceDollar?.let { value ->
                        java.lang.String.format(
                            java.util.Locale.US,
                            "%.2f",
                            value
                        )
                    }.orEmpty()

                val saleBaseDollar =
                    activeConsumerSale?.baseDollarPrice
                        ?: activeColleagueSale?.baseDollarPrice
                        ?: activePrice?.buyPriceDollar

                val saleDollarRate =
                    activeConsumerSale?.dollarRateToman
                        ?: activeColleagueSale?.dollarRateToman
                        ?: latestRate?.rateToman

                formState.update { state ->
                    recalculateSalePrices(
                        state.copy(
                            productId = product.id,
                            categoryId = product.categoryId,
                            name = product.name,
                            model = product.model,
                            brandId = product.brandId,

                            buyPriceToman = activePrice?.buyPriceToman,
                            buyPriceDollar = buyDollarText,
                            dollarRateToman =
                            activePrice?.dollarRateToman
                                ?: latestRate?.rateToman,

                            priceNote = activePrice?.note.orEmpty(),
                            selectedPurchasePriceId = activePrice?.id,

                            initialQuantity =
                            if (currentStock % 1.0 == 0.0) {
                                currentStock.toInt().toString()
                            } else {
                                currentStock.toString()
                            },

                            saleBaseDollarPrice =
                            saleBaseDollar?.let { value ->
                                java.lang.String.format(
                                    java.util.Locale.US,
                                    "%.2f",
                                    value
                                )
                            }.orEmpty(),

                            saleDollarRateToman = saleDollarRate,

                            selectedConsumerSalePriceId = activeConsumerSale?.id,
                            selectedColleagueSalePriceId = activeColleagueSale?.id,

                            consumerProfitPercent =
                            activeConsumerSale?.profitPercent
                                ?.toString()
                                .orEmpty(),

                            colleagueProfitPercent =
                            activeColleagueSale?.profitPercent
                                ?.toString()
                                .orEmpty(),

                            saleDate =
                            activeConsumerSale?.createdAt
                                ?: activeColleagueSale?.createdAt
                                ?: System.currentTimeMillis(),

                            saleNote =
                            activeConsumerSale?.note
                                ?: activeColleagueSale?.note
                                ?: ""
                        )
                    )
                }
            }
        } else {
            viewModelScope.launch {
                val latestRate =
                    pricingRepository.getLatestCurrencyRate("USD")

                val draftId =
                    productRepository.upsertProduct(
                        ProductEntity(
                            categoryId = categoryId,
                            name = "",
                            model = "",
                            brandId = null,
                            isDraft = true
                        )
                    ).toInt()

                formState.update { state ->
                    recalculateSalePrices(
                        state.copy(
                            productId = draftId,
                            categoryId = categoryId,

                            dollarRateToman = latestRate?.rateToman,
                            saleDollarRateToman = latestRate?.rateToman,

                            purchaseDate = System.currentTimeMillis(),
                            saleDate = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }







    fun onNameChange(value: String) {
        formState.update { it.copy(name = value) }
    }

    fun onModelChange(value: String) {
        formState.update { it.copy(model = value) }
    }

    fun onAttributeValueChange(
        attributeDefinitionId: Int,
        value: String
    ) {
        formState.update { state ->
            state.copy(
                attributeValues = state.attributeValues + (attributeDefinitionId to value)
            )
        }
    }

    fun save(
        onSuccess: () -> Unit
    ) {
        val state = uiState.value

        if (state.categoryId == null) return
        if (state.productId == null) return
        if (state.name.isBlank()) return

        viewModelScope.launch {
            formState.update {
                it.copy(isSaving = true)
            }

            val savedProductId = state.productId

            productRepository.finalizeDraftProduct(
                id = savedProductId,
                name = state.name.trim(),
                model = state.model.trim(),
                brandId = state.brandId
            )

            state.attributeValues.forEach { (attributeDefinitionId, value) ->
                if (value.isNotBlank()) {
                    attributeRepository.upsertAttributeValue(
                        ProductAttributeValueEntity(
                            productId = savedProductId,
                            attributeDefinitionId = attributeDefinitionId,
                            valueText = value.trim()
                        )
                    )
                }
            }

            formState.update {
                it.copy(
                    isSaving = false,
                    productId = savedProductId,
                    pendingImageFiles = emptyList()
                )
            }

            onSuccess()
        }
    }

    fun onBrandChange(value: Int?) {
        formState.update {
            it.copy(brandId = value)
        }
    }

    fun onBuyPriceDollarChange(value: String) {
        val cleanValue = value.filter { it.isDigit() || it == '.' }
        val dollar = cleanValue.toDoubleOrNull()
        val rate = formState.value.dollarRateToman

        formState.update {
            it.copy(
                buyPriceDollar = cleanValue,
                buyPriceToman =
                if (cleanValue.isBlank()) null
                else if (dollar != null && rate != null) (dollar * rate).toLong()
                else it.buyPriceToman
            )
        }
    }

    fun onBuyPriceTomanChange(value: Long?) {
        val rate = formState.value.dollarRateToman

        formState.update {
            it.copy(
                buyPriceToman = value,
                buyPriceDollar =
                if (value == null) ""
                else if (rate != null && rate > 0)
                    java.lang.String.format(
                        java.util.Locale.US,
                        "%.2f",
                        value.toDouble() / rate.toDouble()
                    )
                else it.buyPriceDollar
            )
        }
    }

    fun onDollarRateChange(value: Long?) {
        val state = formState.value
        val dollar = state.buyPriceDollar.toDoubleOrNull()

        formState.update {
            it.copy(
                dollarRateToman = value,
                buyPriceToman =
                if (state.buyPriceDollar.isBlank()) it.buyPriceToman
                else if (dollar != null && value != null) (dollar * value).toLong()
                else it.buyPriceToman
            )
        }
    }

    fun onPriceNoteChange(value: String) {
        formState.update {
            it.copy(priceNote = value)
        }
    }

    fun onInitialQuantityChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }

        formState.update {
            it.copy(initialQuantity = filtered)
        }
    }

    fun onInventoryNoteChange(value: String) {
        formState.update {
            it.copy(inventoryNote = value)
        }
    }

    fun createProductCameraTempUri(): Pair<File, Uri> {
        return productImageRepository.createCameraTempUri()
    }

    fun importProductImageFromGallery(src: Uri) {
        viewModelScope.launch {
            val currentProductId = formState.value.productId

            if (currentProductId != null) {
                productImageRepository.addImageFromUri(
                    productId = currentProductId,
                    src = src
                )
            } else {
                val (file, _) = imageRepository.saveCompressedToInternal(src)

                formState.update {
                    it.copy(
                        pendingImageFiles = it.pendingImageFiles + file
                    )
                }
            }
        }
    }

    fun importProductImageFromCameraTemp(tempFile: File) {
        viewModelScope.launch {
            val currentProductId = formState.value.productId

            if (currentProductId != null) {
                productImageRepository.addImageFromCameraTemp(
                    productId = currentProductId,
                    tempFile = tempFile
                )
            } else {
                val (file, _) = imageRepository.compressCameraTempToInternal(tempFile)

                formState.update {
                    it.copy(
                        pendingImageFiles = it.pendingImageFiles + file
                    )
                }
            }
        }
    }

    fun createBrandAndSelect(
        name: String,
        onDone: () -> Unit = {}
    ) {
        val cleanName = name.trim()

        if (cleanName.isBlank()) return

        viewModelScope.launch {
            val brandId = productRepository.upsertBrand(
                ProductBrandEntity(
                    name = cleanName
                )
            ).toInt()

            formState.update {
                it.copy(
                    brandId = brandId
                )
            }

            onDone()
        }
    }
    fun removePendingImage(
        fileName: String
    ) {
        formState.update { state ->
            state.copy(
                pendingImageFiles =
                state.pendingImageFiles.filterNot {
                    it.name == fileName
                }
            )
        }
    }

    fun deleteImage(
        item: ProductEditImageItem
    ) {
        viewModelScope.launch {

            if (item.isPending) {

                removePendingImage(
                    item.fileName
                )

            } else {

                item.id?.let {
                    productImageRepository.deleteImage(it)
                }
            }
        }
    }

    fun saveWithImageOrder(
        imageItems: List<ProductEditImageItem>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val orderedSavedImageIds =
                imageItems.mapNotNull { it.id }

            val orderedPendingFileNames =
                imageItems
                    .filter { it.isPending }
                    .map { it.fileName }

            val pendingMap =
                formState.value.pendingImageFiles.associateBy { it.name }

            formState.update { state ->
                state.copy(
                    pendingImageFiles = orderedPendingFileNames.mapNotNull { fileName ->
                        pendingMap[fileName]
                    }
                )
            }

            if (orderedSavedImageIds.isNotEmpty()) {
                productImageRepository.saveImageOrder(orderedSavedImageIds)
            }

            save(onSuccess = onSuccess)
        }
    }

    fun onPurchaseDateChange(value: Long) {
        formState.update {
            it.copy(purchaseDate = value)
        }
    }

    fun addCurrentPurchaseToHistory() {
        viewModelScope.launch {
            val currentProductId =
                uiState.value.productId ?: return@launch

            val state = formState.value
            val priceToman =
                state.buyPriceToman ?: return@launch

            val selectedId =
                state.selectedPurchasePriceId

            if (selectedId != null) {
                pricingRepository.updatePurchasePrice(
                    id = selectedId,
                    buyPriceDollar = state.buyPriceDollar.toDoubleOrNull(),
                    buyPriceToman = priceToman,
                    dollarRateToman = state.dollarRateToman,
                    quantity = state.initialQuantity.toDoubleOrNull(),
                    purchasedAt = state.purchaseDate,
                    note = state.priceNote.trim()
                )
            } else {
                pricingRepository.setNewPurchasePrice(
                    ProductPurchasePriceEntity(
                        productId = currentProductId,
                        buyPriceToman = priceToman,
                        buyPriceDollar = state.buyPriceDollar.toDoubleOrNull(),
                        dollarRateToman = state.dollarRateToman,
                        quantity = state.initialQuantity.toDoubleOrNull(),
                        purchasedAt = state.purchaseDate,
                        note = state.priceNote.trim()
                    )
                )
            }

            formState.update {
                recalculateSalePrices(
                    it.copy(
                        selectedPurchasePriceId = null,

                        buyPriceToman = null,
                        buyPriceDollar = "",
                        initialQuantity = "",
                        priceNote = "",
                        purchaseDate = System.currentTimeMillis(),

                        saleBaseDollarPrice = state.buyPriceDollar

                        // dollarRateToman عمداً پاک نمی‌شود
                    )
                )
            }
        }
    }

    fun deletePurchasePrice(id: Int) {
        viewModelScope.launch {
            pricingRepository.deletePurchasePriceById(id)
        }
    }

    fun selectPurchasePrice(
        price: ProductPurchasePriceEntity
    ) {
        val priceId = price.id ?: return

        formState.update { state ->

            if (state.selectedPurchasePriceId == priceId) {
                state.copy(
                    selectedPurchasePriceId = null,

                    buyPriceToman = null,
                    buyPriceDollar = "",
                    initialQuantity = "",
                    priceNote = "",
                    purchaseDate = System.currentTimeMillis()

                    // dollarRateToman عمداً پاک نمی‌شود
                )
            } else {
                state.copy(
                    selectedPurchasePriceId = priceId,

                    buyPriceToman = price.buyPriceToman,
                    buyPriceDollar = price.buyPriceDollar?.let { value ->
                        java.lang.String.format(
                            java.util.Locale.US,
                            "%.2f",
                            value
                        )
                    }.orEmpty(),
                    dollarRateToman = price.dollarRateToman,
                    initialQuantity = price.quantity?.let { value ->
                        if (value % 1.0 == 0.0) {
                            value.toInt().toString()
                        } else {
                            value.toString()
                        }
                    }.orEmpty(),
                    purchaseDate = price.purchasedAt,
                    priceNote = price.note
                )
            }
        }
    }

    private fun calculateSalePriceToman(
        baseDollarPrice: String,
        dollarRateToman: Long?,
        profitPercent: String
    ): Long? {

        val baseDollar = baseDollarPrice.toDoubleOrNull()
        val rate = dollarRateToman
        val profit = profitPercent.toDoubleOrNull()

        if (baseDollar == null || rate == null || profit == null) {
            return null
        }

        val basePrice = baseDollar * rate

        val profitAmount =
            basePrice * profit / 100.0

        return (basePrice + profitAmount).toLong()
    }

    private fun recalculateSalePrices(state: ProductEditUiState): ProductEditUiState {
        return state.copy(
            consumerSalePriceToman = calculateSalePriceToman(
                baseDollarPrice = state.saleBaseDollarPrice,
                dollarRateToman = state.saleDollarRateToman,
                profitPercent = state.consumerProfitPercent
            ),
            colleagueSalePriceToman = calculateSalePriceToman(
                baseDollarPrice = state.saleBaseDollarPrice,
                dollarRateToman = state.saleDollarRateToman,
                profitPercent = state.colleagueProfitPercent
            )
        )
    }

    fun onSaleBaseDollarPriceChange(value: String) {
        val clean = value.filter { it.isDigit() || it == '.' }

        formState.update {
            recalculateSalePrices(
                it.copy(saleBaseDollarPrice = clean)
            )
        }
    }

    fun onSaleDollarRateChange(value: Long?) {
        formState.update {
            recalculateSalePrices(
                it.copy(saleDollarRateToman = value)
            )
        }
    }

    fun onConsumerProfitPercentChange(value: String) {
        val clean = value.filter { it.isDigit() || it == '.' }

        formState.update {
            recalculateSalePrices(
                it.copy(consumerProfitPercent = clean)
            )
        }
    }

    fun onColleagueProfitPercentChange(value: String) {
        val clean = value.filter { it.isDigit() || it == '.' }

        formState.update {
            recalculateSalePrices(
                it.copy(colleagueProfitPercent = clean)
            )
        }
    }

    fun onSaleDateChange(value: Long) {
        formState.update {
            it.copy(saleDate = value)
        }
    }

    fun onSaleNoteChange(value: String) {
        formState.update {
            it.copy(saleNote = value)
        }
    }

    fun addCurrentSalePricesToHistory() {
        val state = uiState.value
        val currentProductId = state.productId ?: return
        val baseDollarPrice = state.saleBaseDollarPrice.toDoubleOrNull()
        val dollarRate = state.saleDollarRateToman

        viewModelScope.launch {
            val selectedConsumerId =
                state.selectedConsumerSalePriceId

            val selectedColleagueId =
                state.selectedColleagueSalePriceId

            if (state.consumerSalePriceToman != null) {
                if (selectedConsumerId != null) {
                    pricingRepository.updateSalePrice(
                        id = selectedConsumerId,
                        salePriceToman = state.consumerSalePriceToman,
                        profitPercent = state.consumerProfitPercent.toDoubleOrNull(),
                        baseDollarPrice = baseDollarPrice,
                        dollarRateToman = dollarRate,
                        basePurchasePriceToman = state.buyPriceToman,
                        note = state.saleNote.trim(),
                        createdAt = state.saleDate
                    )
                } else {
                    pricingRepository.setNewActiveSalePrice(
                        ProductSalePriceEntity(
                            productId = currentProductId,
                            priceType = "consumer",
                            salePriceToman = state.consumerSalePriceToman,
                            profitPercent = state.consumerProfitPercent.toDoubleOrNull(),
                            baseDollarPrice = baseDollarPrice,
                            dollarRateToman = dollarRate,
                            basePurchasePriceToman = state.buyPriceToman,
                            note = state.saleNote.trim(),
                            createdAt = state.saleDate
                        )
                    )
                }
            }

            if (state.colleagueSalePriceToman != null) {
                if (selectedColleagueId != null) {
                    pricingRepository.updateSalePrice(
                        id = selectedColleagueId,
                        salePriceToman = state.colleagueSalePriceToman,
                        profitPercent = state.colleagueProfitPercent.toDoubleOrNull(),
                        baseDollarPrice = baseDollarPrice,
                        dollarRateToman = dollarRate,
                        basePurchasePriceToman = state.buyPriceToman,
                        note = state.saleNote.trim(),
                        createdAt = state.saleDate
                    )
                } else {
                    pricingRepository.setNewActiveSalePrice(
                        ProductSalePriceEntity(
                            productId = currentProductId,
                            priceType = "colleague",
                            salePriceToman = state.colleagueSalePriceToman,
                            profitPercent = state.colleagueProfitPercent.toDoubleOrNull(),
                            baseDollarPrice = baseDollarPrice,
                            dollarRateToman = dollarRate,
                            basePurchasePriceToman = state.buyPriceToman,
                            note = state.saleNote.trim(),
                            createdAt = state.saleDate
                        )
                    )
                }
            }

            formState.update {
                recalculateSalePrices(
                    it.copy(
                        selectedConsumerSalePriceId = null,
                        selectedColleagueSalePriceId = null,

                        consumerProfitPercent = "",
                        consumerSalePriceToman = null,
                        colleagueProfitPercent = "",
                        colleagueSalePriceToman = null,

                        saleBaseDollarPrice = "",
                        saleNote = "",
                        saleDate = System.currentTimeMillis()

                        // saleDollarRateToman عمداً پاک نمی‌شود
                    )
                )
            }
        }
    }

    fun deleteSalePrice(id: Int) {
        viewModelScope.launch {
            pricingRepository.deleteSalePriceById(id)

            formState.update {
                it.copy(
                    selectedConsumerSalePriceId =
                    if (it.selectedConsumerSalePriceId == id) null else it.selectedConsumerSalePriceId,
                    selectedColleagueSalePriceId =
                    if (it.selectedColleagueSalePriceId == id) null else it.selectedColleagueSalePriceId
                )
            }
        }
    }

    fun selectSaleGroup(
        consumer: ProductSalePriceEntity?,
        colleague: ProductSalePriceEntity?
    ) {
        val base =
            consumer ?: colleague ?: return

        formState.update { state ->

            val isSameGroup =
                (consumer?.id != null && consumer.id == state.selectedConsumerSalePriceId) ||
                        (colleague?.id != null && colleague.id == state.selectedColleagueSalePriceId)

            if (isSameGroup) {
                recalculateSalePrices(
                    state.copy(
                        selectedConsumerSalePriceId = null,
                        selectedColleagueSalePriceId = null,

                        saleBaseDollarPrice = "",
                        consumerProfitPercent = "",
                        consumerSalePriceToman = null,
                        colleagueProfitPercent = "",
                        colleagueSalePriceToman = null,
                        saleNote = "",
                        saleDate = System.currentTimeMillis()

                        // saleDollarRateToman عمداً پاک نمی‌شود
                    )
                )
            } else {
                recalculateSalePrices(
                    state.copy(
                        selectedConsumerSalePriceId = consumer?.id,
                        selectedColleagueSalePriceId = colleague?.id,

                        saleBaseDollarPrice = base.baseDollarPrice?.let {
                            java.lang.String.format(
                                java.util.Locale.US,
                                "%.2f",
                                it
                            )
                        }.orEmpty(),

                        saleDollarRateToman = base.dollarRateToman,
                        saleDate = base.createdAt,
                        saleNote = base.note,

                        consumerProfitPercent =
                        consumer?.profitPercent?.toString().orEmpty(),

                        colleagueProfitPercent =
                        colleague?.profitPercent?.toString().orEmpty()
                    )
                )
            }
        }
    }

}


private data class ProductEditExtraData(
    val brands: List<ProductBrandEntity>,
    val attributes: List<ProductAttributeDisplayInfo>,
    val images: List<ProductImageEntity>,
    val purchasePrices: List<ProductPurchasePriceEntity>,
    val salePrices: List<ProductSalePriceEntity>
)