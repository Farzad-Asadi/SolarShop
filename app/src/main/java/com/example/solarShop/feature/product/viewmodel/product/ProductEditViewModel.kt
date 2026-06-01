package com.example.solarShop.feature.product.viewmodel.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.product.ProductRepository
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
import javax.inject.Inject

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val attributeRepository: AttributeRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val productId: Int =
        checkNotNull(savedStateHandle["productId"])

    private val isEditMode: Boolean =
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
                    productRepository.observeActiveBrands()
                ) { attributes, brands ->

                    form.copy(
                        attributes = attributes,
                        attributeValues = attributes.associate {
                            it.attributeDefinitionId to (
                                    form.attributeValues[it.attributeDefinitionId]
                                        ?: it.valueText.orEmpty()
                                    )
                        },
                        brands = brands
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
}