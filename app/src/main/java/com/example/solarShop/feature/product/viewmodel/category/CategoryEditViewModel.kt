package com.example.solarShop.feature.product.viewmodel.category

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.repo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val categoryId =
        checkNotNull(
            savedStateHandle.get<Int>("categoryId")
        )

    private val _uiState = MutableStateFlow(
        CategoryEditUiState()
    )

    val uiState = _uiState.asStateFlow()

    init {

        if (categoryId != -1) {

            viewModelScope.launch {

                val category =
                    productRepository.getCategoryById(categoryId)
                        ?: return@launch

                _uiState.update {
                    it.copy(
                        name = category.name,
                        description = category.description,
                        imageFileName = category.imageFileName
                    )
                }
            }
        }
    }

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
                id = if (categoryId == -1) null else categoryId,
                name = state.name.trim(),
                description = state.description.trim(),
                imageFileName = state.imageFileName,
            )
        )

        return true
    }

    fun createCategoryCameraTempUri(): Pair<File, Uri> {
        return imageRepository.createCameraTempUri()
    }

    fun importCategoryImage(src: Uri) {
        viewModelScope.launch {
            val (file, _) = imageRepository.saveCompressedToInternal(src)

            _uiState.update {
                it.copy(imageFileName = file.name)
            }
        }
    }

    fun importCategoryImageFromCameraTemp(tempFile: File) {
        viewModelScope.launch {
            val (file, _) = imageRepository.compressCameraTempToInternal(tempFile)

            _uiState.update {
                it.copy(imageFileName = file.name)
            }
        }
    }
}