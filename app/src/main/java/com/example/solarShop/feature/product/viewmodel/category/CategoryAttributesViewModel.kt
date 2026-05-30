package com.example.solarShop.feature.product.viewmodel.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.repository.attribute.AttributeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CategoryAttributesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    attributeRepository: AttributeRepository
) : ViewModel() {

    private val categoryId: Int =
        checkNotNull(savedStateHandle["categoryId"])

    val uiState = attributeRepository
        .observeActiveAttributeDefinitions(categoryId)
        .map { attributes ->
            CategoryAttributesUiState(
                isLoading = false,
                categoryId = categoryId,
                attributes = attributes
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryAttributesUiState()
        )
}