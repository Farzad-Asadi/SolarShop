package com.example.solarShop.feature.product.viewmodel.product

import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.sales.ProductSaleTransactionEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import com.example.solarShop.data.local.relation.product.ProductFullInfo
import com.example.solarShop.domain.product.ProductSalePriceResult

data class ProductDetailUiState(
    val isLoading: Boolean = true,

    val product: ProductFullInfo? = null,

    val attributes: List<ProductAttributeDisplayInfo> = emptyList(),

    val activePurchasePrice: ProductPurchasePriceEntity? = null,

    val salePriceResult: ProductSalePriceResult? = null,

    val consumerSalePriceResult: ProductSalePriceResult? = null,
    val colleagueSalePriceResult: ProductSalePriceResult? = null,

    val salePrices: List<ProductSalePriceEntity> = emptyList(),

    val dailyDollarRateToman: Long? = null,

    val currentStock: Double = 0.0,

    val inventoryTransactions: List<InventoryTransactionEntity> = emptyList(),

    val images: List<ProductImageUi> = emptyList(),

    val saleTransactions: List<ProductSaleTransactionEntity> = emptyList()
)