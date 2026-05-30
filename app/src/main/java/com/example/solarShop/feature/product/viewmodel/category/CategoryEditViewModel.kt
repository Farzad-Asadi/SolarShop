package com.example.solarShop.feature.product.viewmodel.category

import androidx.lifecycle.ViewModel
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.repository.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CategoryEditUiState()
    )

    val uiState = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(name = value)
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update {
            it.copy(description = value)
        }
    }

    suspend fun save(): Boolean {

        val state = _uiState.value

        if (state.name.isBlank()) {
            return false
        }

        productRepository.upsertCategory(
            ProductCategoryEntity(
                name = state.name.trim(),
                description = state.description.trim()
            )
        )

        return true
    }
}