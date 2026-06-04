package com.example.solarShop.feature.product.viewmodel.product

import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import java.io.File

data class ProductEditUiState(

    val productId: Int? = null,
    val categoryId: Int? = null,

    val name: String = "",
    val model: String = "",

    val brandId: Int? = null,
    val brands: List<ProductBrandEntity> = emptyList(),

    val attributes: List<ProductAttributeDisplayInfo> = emptyList(),

    val attributeValues: Map<Int, String> = emptyMap(),

    val coverImageFileName: String? = null,
    val images: List<ProductImageEntity> = emptyList(),
    val pendingImageFiles: List<File> = emptyList(),

    val purchasePrices: List<ProductPurchasePriceEntity> = emptyList(),

    val newPurchasePriceToman: Long? = null,
    val newPurchaseDollarRateToman: Long? = null,
    val newPurchasePriceDollar: String = "",
    val newPurchaseQuantity: String = "",
    val newPurchaseNote: String = "",

    val salePrices: List<ProductSalePriceEntity> = emptyList(),

    val selectedPurchasePriceId: Int? = null,

    val buyPriceDollar: String = "",
    val buyPriceToman: Long? = null,
    val dollarRateToman: Long? = null,
    val priceNote: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),

    val initialQuantity: String = "",
    val inventoryNote: String = "",


    val isSaving: Boolean = false
)