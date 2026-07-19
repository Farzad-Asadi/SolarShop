package com.example.solarShop.data.repository.product

import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun observeActiveCategories() = productDao.observeActiveCategories()
    override fun observeActiveBrands() = productDao.observeActiveBrands()
    override fun observeActiveUnits() = productDao.observeActiveUnits()

    override fun observeActiveProducts() = productDao.observeActiveProducts()

    override fun observeProductsForReports() =
        productDao.observeProductsForReports()

    override fun observeProductsByCategory(
        categoryId: Int
    ) = productDao.observeProductsByCategory(categoryId)

    override fun observeActiveProductsFullInfo() =
        productDao.observeActiveProductsFullInfo()

    override suspend fun getProductFullInfo(productId: Int) =
        productDao.getProductFullInfo(productId)

    override suspend fun upsertCategory(category: ProductCategoryEntity) =
        productDao.upsertCategory(category)

    override suspend fun upsertBrand(brand: ProductBrandEntity) =
        productDao.upsertBrand(brand)

    override suspend fun upsertUnit(unit: ProductUnitEntity) =
        productDao.upsertUnit(unit)

    override suspend fun upsertProduct(product: ProductEntity) =
        productDao.upsertProduct(product)

    override suspend fun softDeleteProduct(productId: Int) {
        productDao.softDeleteProductById(productId)
    }

    override fun observeProductImages(productId: Int) =
        productDao.observeProductImages(productId)

    override suspend fun upsertProductImage(image: ProductImageEntity) =
        productDao.upsertProductImage(image)

    override suspend fun deleteProductImageById(imageId: Int) =
        productDao.deleteProductImageById(imageId)

    override suspend fun getCategoryById(
        categoryId: Int
    ) = productDao.getCategoryById(categoryId)

    override fun observeProductsByCategoryFullInfo(
        categoryId: Int
    ) = productDao.observeProductsByCategoryFullInfo(categoryId)

    override fun observeProductFullInfo(
        productId: Int
    ) = productDao.observeProductFullInfo(productId)

    override suspend fun getBrandById(
        brandId: Int
    ) = productDao.getBrandById(brandId)

    override suspend fun updateProductBasicInfo(
        id: Int,
        categoryId: Int,
        name: String,
        model: String,
        brandId: Int?
    ) {
        productDao.updateProductBasicInfo(
            id = id,
            categoryId = categoryId,
            name = name,
            model = model,
            brandId = brandId
        )
    }

    override suspend fun finalizeDraftProduct(
        id: Int,
        name: String,
        model: String,
        brandId: Int?
    ) {
        productDao.finalizeDraftProduct(
            id = id,
            name = name,
            model = model,
            brandId = brandId
        )
    }

    override suspend fun cleanupOldDraftProducts(
        olderThanMillis: Long
    ) {
        val threshold =
            System.currentTimeMillis() - olderThanMillis

        productDao.deleteOldDraftProducts(
            threshold = threshold
        )
    }

    override fun observeProductCountByCategory():
            Flow<Map<Int, Int>> {

        return productDao.observeProductCountByCategory()
            .map { list ->
                list.associate {
                    it.categoryId to it.count
                }
            }
    }

    override suspend fun updateCategorySortOrders(
        categories: List<ProductCategoryEntity>
    ) {
        categories.forEachIndexed { index, category ->
            val id = category.id ?: return@forEachIndexed

            productDao.updateCategorySortOrder(
                categoryId = id,
                sortOrder = (index + 1) * 10
            )
        }
    }

    override suspend fun upsertCategoryByUid(
        category: ProductCategoryEntity
    ): Long {
        val uid = category.uid

        if (uid.isNullOrBlank()) {
            return productDao.upsertCategory(category)
        }

        val existing = productDao.getCategoryByUid(uid) ?: return productDao.upsertCategory(category)

        if (!existing.isSynced && existing.updatedAt > category.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return productDao.upsertCategory(
            category.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getUnsyncedCategories(): List<ProductCategoryEntity> {
        return productDao.getUnsyncedCategories()
    }


    override suspend fun markCategoriesSynced(uids: List<String>) {
        if (uids.isEmpty()) return
        productDao.markCategoriesSynced(uids)
    }

    override suspend fun upsertBrandByUid(
        brand: ProductBrandEntity
    ): Long {
        val uid = brand.uid

        if (uid.isNullOrBlank()) {
            return productDao.upsertBrand(brand)
        }

        val existing = productDao.getBrandByUid(uid) ?: return productDao.upsertBrand(brand)

        if (!existing.isSynced && existing.updatedAt > brand.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return productDao.upsertBrand(
            brand.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getUnsyncedBrands(): List<ProductBrandEntity> {
        return productDao.getUnsyncedBrands()
    }

    override suspend fun markBrandsSynced(uids: List<String>) {
        if (uids.isEmpty()) return
        productDao.markBrandsSynced(uids)
    }


    override suspend fun upsertProductByUid(
        product: ProductEntity
    ): Long {
        val existing = productDao.getProductByUid(product.uid) ?: return productDao.upsertProduct(product)

        if (existing.deletedAt != null && product.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > product.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return productDao.upsertProduct(
            product.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getUnsyncedProducts(): List<ProductEntity> {
        return productDao.getUnsyncedProducts()
    }

    override suspend fun markProductsSynced(uids: List<String>) {
        if (uids.isEmpty()) return
        productDao.markProductsSynced(uids)
    }
    override suspend fun getCategoryByUid(
        uid: String
    ): ProductCategoryEntity? {
        return productDao.getCategoryByUid(uid)
    }

    override suspend fun getBrandByUid(
        uid: String
    ): ProductBrandEntity? {
        return productDao.getBrandByUid(uid)
    }

    override suspend fun countProductsInCategory(
        categoryId: Int
    ): Int {
        return productDao.countProductsInCategory(categoryId)
    }

    override suspend fun deleteCategoryWithProducts(
        categoryId: Int
    ) {
        val now = System.currentTimeMillis()

        productDao.softDeleteProductsByCategory(
            categoryId = categoryId,
            deletedAt = now
        )

        productDao.deactivateCategory(
            id = categoryId,
            updatedAt = now
        )
    }

    override suspend fun countProductsWithBrand(
        brandId: Int
    ): Int {
        return productDao.countProductsWithBrand(brandId)
    }

    override suspend fun deleteBrandAndClearProducts(
        brandId: Int
    ) {
        val now = System.currentTimeMillis()

        productDao.clearBrandFromProducts(
            brandId = brandId,
            updatedAt = now
        )

        productDao.deactivateBrand(
            id = brandId,
            updatedAt = now
        )
    }

    override suspend fun getProductByUid(
        uid: String
    ): ProductEntity? {
        return productDao.getProductByUid(uid)
    }

    override suspend fun getProductById(
        productId: Int
    ): ProductEntity? {
        return productDao.getProductById(productId)
    }

    override suspend fun upsertUnitByUid(
        unit: ProductUnitEntity
    ): Long {
        val existing =
            productDao.getUnitByUid(unit.uid)
                ?: return productDao.upsertUnit(unit)

        // Delete Wins
        if (existing.deletedAt != null && unit.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > unit.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return productDao.upsertUnit(
            unit.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getUnsyncedUnits(): List<ProductUnitEntity> {
        return productDao.getUnsyncedUnits()
    }

    override suspend fun markUnitsSynced(
        uids: List<String>
    ) {
        if (uids.isEmpty()) return
        productDao.markUnitsSynced(uids)
    }

    override suspend fun getUnitByUid(
        uid: String
    ): ProductUnitEntity? {
        return productDao.getUnitByUid(uid)
    }

    override suspend fun getUnitById(
        unitId: Int
    ): ProductUnitEntity? {
        return productDao.getUnitById(unitId)
    }

    override suspend fun deactivateUnit(
        id: Int
    ) {
        productDao.deactivateUnit(id)
    }

    override suspend fun getAllCategoriesForBackup(): List<ProductCategoryEntity> =
        productDao.getAllCategoriesForBackup()

    override suspend fun getAllBrandsForBackup(): List<ProductBrandEntity> =
        productDao.getAllBrandsForBackup()

    override suspend fun getAllProductsForBackup(): List<ProductEntity> =
        productDao.getAllProductsForBackup()

    override suspend fun getAllUnitsForBackup(): List<ProductUnitEntity> =
        productDao.getAllUnitsForBackup()

}
