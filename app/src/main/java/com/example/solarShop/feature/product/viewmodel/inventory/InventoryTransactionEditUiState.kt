package com.example.solarShop.feature.product.viewmodel.inventory

import com.example.solarShop.InventoryTransactionType


data class InventoryTransactionEditUiState(
    val productId: Int? = null,

    val quantity: String = "",
    val transactionType: InventoryTransactionType = InventoryTransactionType.PURCHASE,

    val note: String = "",
    val isSaving: Boolean = false
)