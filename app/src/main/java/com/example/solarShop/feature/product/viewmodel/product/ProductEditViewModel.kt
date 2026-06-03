package com.example.solarShop.feature.product.viewmodel.product

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val productImageRepository: ProductImageRepository
) : ViewModel() {

    private val productId: Int =
        checkNotNull(savedStateHandle["productId"])

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
    val uiState = formState
        .flatMapLatest { form ->
            val realCategoryId = form.categoryId

            if (realCategoryId == null) {
                productRepository.observeActiveBrands()
                    .map { brands ->
                        form.copy(brands = brands)
                    }
            } else {
                combine(
                    attributeRepository.observeProductAttributeDisplayInfo(
                        productId = form.productId ?: -1,
                        categoryId = realCategoryId
                    ),
                    productRepository.observeActiveBrands(),
                    productImageRepository.observeImagesForProduct(
                        productId = form.productId ?: -1
                    )
                ) { attributes, brands, images ->

                    form.copy(
                        attributes = attributes,
                        attributeValues = attributes.associate {
                            it.attributeDefinitionId to (
                                    form.attributeValues[it.attributeDefinitionId]
                                        ?: it.valueText.orEmpty()
                                    )
                        },
                        brands = brands,
                        images = images,
                        coverImageFileName = images.firstOrNull()?.fileName
                    )
                }
            }
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
                val productFullInfo = productRepository.getProductFullInfo(productId)
                    ?: return@launch

                val product = productFullInfo.product

                formState.update {
                    it.copy(
                        productId = product.id,
                        categoryId = product.categoryId,
                        name = product.name,
                        model = product.model,
                        brandId = product.brandId,
                    )
                }
            }
        }
        viewModelScope.launch {
            val latestRate = pricingRepository.getLatestCurrencyRate("USD")

            formState.update {
                it.copy(
                    dollarRateToman = latestRate?.rateToman
                )
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
        if (state.name.isBlank()) return

        viewModelScope.launch {
            formState.update { it.copy(isSaving = true) }

            val savedProductId = productRepository.upsertProduct(
                ProductEntity(
                    id = state.productId,
                    categoryId = state.categoryId,
                    name = state.name.trim(),
                    model = state.model.trim(),
                    brandId = state.brandId,
                )
            ).toInt()

            formState.update {
                it.copy(productId = savedProductId)
            }

            if (!isEditMode && state.buyPriceToman != null) {
                pricingRepository.setNewPurchasePrice(
                    ProductPurchasePriceEntity(
                        productId = savedProductId,
                        buyPriceDollar = state.buyPriceDollar.toDoubleOrNull(),
                        buyPriceToman = state.buyPriceToman,
                        dollarRateToman = state.dollarRateToman,
                        note = state.priceNote.trim()
                    )
                )
            }

            val initialQuantity = state.initialQuantity.toDoubleOrNull()

            if (!isEditMode && initialQuantity != null && initialQuantity > 0.0) {
                inventoryRepository.purchase(
                    productId = savedProductId,
                    quantity = initialQuantity,
                    note = state.inventoryNote.trim().ifBlank {
                        "موجودی اولیه"
                    }
                )
            }

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

            formState.update { it.copy(isSaving = false) }
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

    fun onCoverImageSelected(
        fileName: String
    ) {
        formState.update {
            it.copy(
                coverImageFileName = fileName
            )
        }
    }



    fun createProductCameraTempUri(): Pair<File, Uri> {
        return productImageRepository.createCameraTempUri()
    }

    fun importProductImageFromGallery(src: Uri) {
        viewModelScope.launch {
            val currentProductId = formState.value.productId ?: return@launch

            productImageRepository.addImageFromUri(
                productId = currentProductId,
                src = src
            )
        }
    }

    fun importProductImageFromCameraTemp(tempFile: File) {
        viewModelScope.launch {
            val currentProductId = formState.value.productId ?: return@launch

            productImageRepository.addImageFromCameraTemp(
                productId = currentProductId,
                tempFile = tempFile
            )
        }
    }
}