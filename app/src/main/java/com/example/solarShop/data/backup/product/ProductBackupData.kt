package com.example.solarShop.data.backup.product

import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity

data class ProductBackupData(
    val categories: List<ProductCategoryEntity> = emptyList(),
    val brands: List<ProductBrandEntity> = emptyList(),

    val products: List<ProductEntity> = emptyList(),
    val productImages: List<ProductImageEntity> = emptyList(),

    val attributeDefinitions: List<CategoryAttributeDefinitionEntity> = emptyList(),
    val attributeValues: List<ProductAttributeValueEntity> = emptyList(),

    val purchasePrices: List<ProductPurchasePriceEntity> = emptyList(),
    val inventoryTransactions: List<InventoryTransactionEntity> = emptyList()
)