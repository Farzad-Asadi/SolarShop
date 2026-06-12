package com.example.solarShop.feature.product.viewmodel.brand

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.repo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BrandEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val brandId: Int =
        checkNotNull(savedStateHandle["brandId"])

    private val _uiState = MutableStateFlow(
        BrandEditUiState(
            brandId = if (brandId == -1) null else brandId
        )
    )

    val uiState = _uiState.asStateFlow()

    init {
        if (brandId != -1) {
            viewModelScope.launch {
                val brand = productRepository.getBrandById(brandId)
                    ?: return@launch

                _uiState.update {
                    it.copy(
                        name = brand.name,
                        description = brand.description,
                        imageFileName = brand.imageFileName
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    suspend fun save(): Boolean {
        val state = _uiState.value

        if (state.name.isBlank()) return false

        val oldBrand =
            if (brandId == -1) null
            else productRepository.getBrandById(brandId)

        val now = System.currentTimeMillis()

        productRepository.upsertBrand(
            ProductBrandEntity(
                id = oldBrand?.id,
                name = state.name.trim(),
                description = state.description.trim(),
                imageFileName = state.imageFileName,
                isActive = oldBrand?.isActive ?: true,
                uid = oldBrand?.uid ?: UUID.randomUUID().toString(),
                createdAt = oldBrand?.createdAt ?: now,
                updatedAt = now,
                deletedAt = oldBrand?.deletedAt,
                isSynced = false
            )
        )

        return true
    }
}