package com.example.solarShop.feature.product.viewmodel.product

import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import com.example.solarShop.data.local.relation.product.ProductFullInfo
import com.example.solarShop.domain.product.ProductSalePriceResult

data class ProductDetailUiState(
    val isLoading: Boolean = true,

    val product: ProductFullInfo? = null,

    val attributes: List<ProductAttributeDisplayInfo> = emptyList(),

    val activePurchasePrice: ProductPurchasePriceEntity? = null,

    val salePriceResult: ProductSalePriceResult? = null,

    val currentStock: Double = 0.0
)