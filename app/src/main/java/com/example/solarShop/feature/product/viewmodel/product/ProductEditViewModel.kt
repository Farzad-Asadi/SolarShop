package com.example.solarShop.feature.product.viewmodel.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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

    private val categoryId =
        checkNotNull(
            savedStateHandle.get<Int>("categoryId")
        )

    private val formState = MutableStateFlow(
        ProductEditUiState(
            categoryId = categoryId
        )
    )

    val uiState = combine(
        formState,
        attributeRepository.observeProductAttributeDisplayInfo(
            productId = -1,
            categoryId = categoryId
        )
    ) { form, attributes ->
        form.copy(
            attributes = attributes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductEditUiState(categoryId = categoryId)
    )

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

            val productId = productRepository.upsertProduct(
                ProductEntity(
                    categoryId = state.categoryId,
                    name = state.name.trim(),
                    model = state.model.trim()
                )
            ).toInt()

            state.attributeValues.forEach { (attributeDefinitionId, value) ->
                if (value.isNotBlank()) {
                    attributeRepository.upsertAttributeValue(
                        ProductAttributeValueEntity(
                            productId = productId,
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
}