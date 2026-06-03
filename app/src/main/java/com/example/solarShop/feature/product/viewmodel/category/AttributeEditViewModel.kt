package com.example.solarShop.feature.product.viewmodel.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.domain.product.AttributeValueType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val attributeRepository: AttributeRepository
) : ViewModel() {

    private val categoryId: Int =
        checkNotNull(savedStateHandle["categoryId"])

    private val attributeId: Int =
        savedStateHandle["attributeId"] ?: -1

    val isEditMode: Boolean
        get() = attributeId != -1

    private val _uiState = MutableStateFlow(
        AttributeEditUiState(
            categoryId = categoryId
        )
    )

    val uiState = _uiState.asStateFlow()

    init {
        if (attributeId != -1) {
            viewModelScope.launch {
                val attribute =
                    attributeRepository.getAttributeDefinitionById(attributeId)
                        ?: return@launch



                _uiState.update {
                    it.copy(
                        title = attribute.title,
                        key = attribute.key,
                        valueType = AttributeValueType.valueOf(attribute.valueType.uppercase()),
                        description = attribute.description,
                        unit = attribute.unit.orEmpty(),
                        isRequired = attribute.isRequired,
                        enumOptions = attribute.enumOptions.orEmpty(),
                        sortOrder = attribute.sortOrder,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onKeyChange(value: String) {
        _uiState.update { it.copy(key = value) }
    }

    fun onValueTypeChange(value: AttributeValueType) {
        _uiState.update {
            it.copy(valueType = value)
        }
    }

    fun onDescriptionChange(
        value: String
    ) {
        _uiState.update {
            it.copy(
                description = value
            )
        }
    }

    fun onUnitChange(value: String) {
        _uiState.update { it.copy(unit = value) }
    }

    fun onRequiredChange(value: Boolean) {
        _uiState.update { it.copy(isRequired = value) }
    }

    fun save(
        onSuccess: () -> Unit
    ) {
        val state = uiState.value

        if (state.categoryId == null) return
        if (state.title.isBlank()) return
        if (state.key.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val sortOrder = if (attributeId == -1) {
                attributeRepository.getNextAttributeSortOrder(state.categoryId)
            } else {
                state.sortOrder
            }

            attributeRepository.upsertAttributeDefinition(
                CategoryAttributeDefinitionEntity(
                    id = if (attributeId == -1) null else attributeId,
                    categoryId = state.categoryId,
                    title = state.title.trim(),
                    key = state.key.trim(),
                    valueType = state.valueType.name.lowercase(),
                    description = state.description,
                    unit = state.unit.trim().ifBlank { null },
                    isRequired = state.isRequired,
                    enumOptions = state.enumOptions
                        .split("\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .joinToString(",")
                        .ifBlank { null },
                    sortOrder = sortOrder,
                )
            )

            _uiState.update { it.copy(isSaving = false) }

            onSuccess()
        }
    }

    fun onEnumOptionsChange(value: String) {
        _uiState.update {
            it.copy(enumOptions = value)
        }
    }
}