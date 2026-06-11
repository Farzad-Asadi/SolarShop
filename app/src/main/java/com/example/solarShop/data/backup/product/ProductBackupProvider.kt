package com.example.solarShop.data.backup.product

import com.example.solarShop.data.backup.core.BackupModuleProvider
import com.example.solarShop.data.local.dao.attribute.AttributeDao
import com.example.solarShop.data.local.dao.inventory.InventoryDao
import com.example.solarShop.data.local.dao.pricing.PricingDao
import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.dao.product.ProductImageDao
import com.google.gson.Gson
import java.util.UUID
import javax.inject.Inject

class ProductBackupProvider @Inject constructor(
    private val productDao: ProductDao,
    private val productImageDao: ProductImageDao,
    private val attributeDao: AttributeDao,
    private val pricingDao: PricingDao,
    private val inventoryDao: InventoryDao,
    private val gson: Gson
) : BackupModuleProvider {

    override val moduleName: String = "product"

    override suspend fun exportData(): String {
        val data = ProductBackupData(
            categories = productDao.getAllCategoriesForBackup(),
            brands = productDao.getAllBrandsForBackup(),
            products = productDao.getAllProductsForBackup(),
            productImages = productImageDao.getAllImagesForBackup(),
            attributeDefinitions = attributeDao.getAllAttributeDefinitionsForBackup(),
            attributeValues = attributeDao.getAllAttributeValuesForBackup(),
            purchasePrices = pricingDao.getAllPurchasePricesForBackup(),
            salePrices = pricingDao.getAllSalePricesForBackup(),
            inventoryTransactions = inventoryDao.getAllInventoryTransactionsForBackup(),
        )

        return gson.toJson(data)
    }

    override suspend fun importData(json: String) {
        val data = gson.fromJson(
            json,
            ProductBackupData::class.java
        )

        productDao.insertCategoriesForRestore(
            data.categories.map { category ->
                category.copy(
                    uid = category.uid?.takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString(),
                    updatedAt = if (category.updatedAt == 0L) {
                        System.currentTimeMillis()
                    } else {
                        category.updatedAt
                    },
                    deletedAt = category.deletedAt,
                    isSynced = false
                )
            }
        )
        productDao.insertBrandsForRestore(
            data.brands.map { brand ->
                brand.copy(
                    uid = brand.uid?.takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString(),
                    updatedAt = if (brand.updatedAt == 0L) {
                        System.currentTimeMillis()
                    } else {
                        brand.updatedAt
                    },
                    deletedAt = brand.deletedAt,
                    isSynced = false
                )
            }
        )

        attributeDao.insertAttributeDefinitionsForRestore(data.attributeDefinitions)

        productDao.insertProductsForRestore(
            data.products.map { product ->
                product.copy(
                    uid = product.uid.ifBlank {
                        UUID.randomUUID().toString()
                    },
                    updatedAt = if (product.updatedAt == 0L) {
                        System.currentTimeMillis()
                    } else {
                        product.updatedAt
                    },
                    deletedAt = product.deletedAt,
                    isSynced = false
                )
            }
        )

        productImageDao.insertImagesForRestore(
            data.productImages.map { image ->
                image.copy(
                    uid = image.uid?.takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString(),
                    updatedAt = if (image.updatedAt == 0L) System.currentTimeMillis() else image.updatedAt,
                    deletedAt = image.deletedAt,
                    isSynced = false
                )
            }
        )
        attributeDao.insertAttributeValuesForRestore(data.attributeValues)

        pricingDao.insertPurchasePricesForRestore(data.purchasePrices)
        pricingDao.insertSalePricesForRestore(data.salePrices)
        inventoryDao.insertInventoryTransactionsForRestore(data.inventoryTransactions)
    }
}