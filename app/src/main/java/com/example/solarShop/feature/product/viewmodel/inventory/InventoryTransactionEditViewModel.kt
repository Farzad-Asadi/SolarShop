package com.example.solarShop.feature.product.viewmodel.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.data.repository.inventory.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryTransactionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val productId: Int =
        checkNotNull(savedStateHandle["productId"])

    private val _uiState = MutableStateFlow(
        InventoryTransactionEditUiState(
            productId = productId
        )
    )

    val uiState = _uiState.asStateFlow()

    fun onQuantityChange(value: String) {
        val filtered = value.filter {
            it.isDigit() || it == '.'
        }

        _uiState.update {
            it.copy(quantity = filtered)
        }
    }

    fun onTransactionTypeChange(value: InventoryTransactionType) {
        _uiState.update {
            it.copy(transactionType = value)
        }
    }

    fun onNoteChange(value: String) {
        _uiState.update {
            it.copy(note = value)
        }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val productId = state.productId ?: return
        val quantity = state.quantity.toDoubleOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            when (state.transactionType) {
                InventoryTransactionType.PURCHASE ->
                    inventoryRepository.purchase(productId, quantity, state.note)

                InventoryTransactionType.SALE ->
                    inventoryRepository.sale(productId, quantity, state.note)

                InventoryTransactionType.ADJUSTMENT ->
                    inventoryRepository.adjust(productId, quantity, state.note)

                InventoryTransactionType.PURCHASE_RETURN ->
                    inventoryRepository.adjust(productId, -quantity, state.note)

                InventoryTransactionType.SALE_RETURN ->
                    inventoryRepository.adjust(productId, quantity, state.note)
            }

            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }
}