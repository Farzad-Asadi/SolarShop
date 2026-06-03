package com.example.solarShop.data.repository.product

import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun observeActiveCategories() = productDao.observeActiveCategories()
    override fun observeActiveBrands() = productDao.observeActiveBrands()
    override fun observeActiveUnits() = productDao.observeActiveUnits()

    override fun observeActiveProducts() = productDao.observeActiveProducts()

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

    override suspend fun archiveProduct(productId: Int) =
        productDao.archiveProduct(productId)

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

}