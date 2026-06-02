package com.example.solarShop.data.backup.product

import com.example.solarShop.data.backup.core.BackupModuleProvider
import com.example.solarShop.data.local.dao.attribute.AttributeDao
import com.example.solarShop.data.local.dao.inventory.InventoryDao
import com.example.solarShop.data.local.dao.pricing.PricingDao
import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.dao.product.ProductImageDao
import com.google.gson.Gson
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
            inventoryTransactions = inventoryDao.getAllInventoryTransactionsForBackup()
        )

        return gson.toJson(data)
    }

    override suspend fun importData(json: String) {
        val data = gson.fromJson(
            json,
            ProductBackupData::class.java
        )

        productDao.insertCategoriesForRestore(data.categories)
        productDao.insertBrandsForRestore(data.brands)

        attributeDao.insertAttributeDefinitionsForRestore(data.attributeDefinitions)

        productDao.insertProductsForRestore(data.products)

        productImageDao.insertImagesForRestore(data.productImages)
        attributeDao.insertAttributeValuesForRestore(data.attributeValues)

        pricingDao.insertPurchasePricesForRestore(data.purchasePrices)
        inventoryDao.insertInventoryTransactionsForRestore(data.inventoryTransactions)
    }
}