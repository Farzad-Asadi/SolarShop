package com.example.solarShop.data.room.tables.product

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // ---------- Category ----------

    @Query("SELECT * FROM product_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeActiveCategories(): Flow<List<ProductCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: ProductCategoryEntity): Long

    @Query("UPDATE product_categories SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateCategory(id: Int, updatedAt: Long = System.currentTimeMillis())

    // ---------- Brand ----------

    @Query("SELECT * FROM product_brands WHERE isActive = 1 ORDER BY name ASC")
    fun observeActiveBrands(): Flow<List<ProductBrandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBrand(brand: ProductBrandEntity): Long

    // ---------- Unit ----------

    @Query("SELECT * FROM product_units WHERE isActive = 1 ORDER BY name ASC")
    fun observeActiveUnits(): Flow<List<ProductUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnit(unit: ProductUnitEntity): Long

    // ---------- Product ----------

    @Query("""
        SELECT * FROM products
        WHERE isArchived = 0
        ORDER BY name ASC
    """)
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE categoryId = :categoryId AND isArchived = 0
        ORDER BY name ASC
    """)
    fun observeProductsByCategory(categoryId: Int): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity): Long

    @Query("UPDATE products SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveProduct(id: Int, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    @Query("""
    SELECT * FROM products
    WHERE isArchived = 0
    ORDER BY name ASC
""")
    fun observeActiveProductsFullInfo(): Flow<List<ProductFullInfo>>

    @Transaction
    @Query("""
    SELECT * FROM products
    WHERE id = :productId
    LIMIT 1
""")
    suspend fun getProductFullInfo(productId: Int): ProductFullInfo?

    // ---------- Purchase Price ----------

    @Query("""
        SELECT * FROM product_purchase_prices
        WHERE productId = :productId
        ORDER BY createdAt DESC
    """)
    fun observePurchasePriceHistory(productId: Int): Flow<List<ProductPurchasePriceEntity>>

    @Query("""
        SELECT * FROM product_purchase_prices
        WHERE productId = :productId AND isActive = 1
        LIMIT 1
    """)
    suspend fun getActivePurchasePrice(productId: Int): ProductPurchasePriceEntity?

    @Query("UPDATE product_purchase_prices SET isActive = 0 WHERE productId = :productId")
    suspend fun deactivateActivePrices(productId: Int)

    @Insert
    suspend fun insertPurchasePrice(price: ProductPurchasePriceEntity): Long

    @Transaction
    suspend fun setNewActivePurchasePrice(price: ProductPurchasePriceEntity) {
        deactivateActivePrices(price.productId)
        insertPurchasePrice(price.copy(isActive = true))
    }


    // ---------- Attribute Definitions ----------

    @Query("""
    SELECT * FROM category_attribute_definitions
    WHERE categoryId = :categoryId AND isActive = 1
    ORDER BY sortOrder ASC, title ASC
""")
    fun observeActiveAttributeDefinitions(
        categoryId: Int
    ): Flow<List<CategoryAttributeDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttributeDefinition(
        definition: CategoryAttributeDefinitionEntity
    ): Long

    @Query("""
    UPDATE category_attribute_definitions
    SET isActive = 0, updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun deactivateAttributeDefinition(
        id: Int,
        updatedAt: Long = System.currentTimeMillis()
    )


// ---------- Product Attribute Values ----------

    @Query("""
    SELECT * FROM product_attribute_values
    WHERE productId = :productId
""")
    fun observeAttributeValues(
        productId: Int
    ): Flow<List<ProductAttributeValueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttributeValue(
        value: ProductAttributeValueEntity
    ): Long

    @Query("""
    DELETE FROM product_attribute_values
    WHERE productId = :productId 
    AND attributeDefinitionId = :attributeDefinitionId
""")
    suspend fun deleteAttributeValue(
        productId: Int,
        attributeDefinitionId: Int
    )

    @Query("""
    SELECT 
        product_attribute_values.id AS valueId,
        :productId AS productId,
        category_attribute_definitions.id AS attributeDefinitionId,

        category_attribute_definitions.title AS title,
        category_attribute_definitions.`key` AS `key`,
        category_attribute_definitions.valueType AS valueType,
        category_attribute_definitions.unit AS unit,
        category_attribute_definitions.isRequired AS isRequired,
        category_attribute_definitions.sortOrder AS sortOrder,

        product_attribute_values.valueText AS valueText

    FROM category_attribute_definitions
    LEFT JOIN product_attribute_values
        ON product_attribute_values.attributeDefinitionId = category_attribute_definitions.id
        AND product_attribute_values.productId = :productId

    WHERE category_attribute_definitions.categoryId = :categoryId
    AND category_attribute_definitions.isActive = 1

    ORDER BY category_attribute_definitions.sortOrder ASC, category_attribute_definitions.title ASC
""")
    fun observeProductAttributeDisplayInfo(
        productId: Int,
        categoryId: Int
    ): Flow<List<ProductAttributeDisplayInfo>>


    // ---------- Product Images ----------

    @Query("""
    SELECT * FROM product_images
    WHERE productId = :productId
    ORDER BY sortOrder ASC, createdAt ASC
""")
    fun observeProductImages(productId: Int): Flow<List<ProductImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductImage(image: ProductImageEntity): Long

    @Delete
    suspend fun deleteProductImage(image: ProductImageEntity)

    @Query("""
    DELETE FROM product_images
    WHERE id = :imageId
""")
    suspend fun deleteProductImageById(imageId: Int)

    @Query("""
    UPDATE product_images
    SET sortOrder = :sortOrder
    WHERE id = :imageId
""")
    suspend fun updateProductImageSortOrder(
        imageId: Int,
        sortOrder: Int
    )


    // ---------- Currency Rate ----------

    @Query("""
    SELECT * FROM currency_rates
    WHERE currencyCode = :currencyCode
    ORDER BY createdAt DESC
    LIMIT 1
""")
    suspend fun getLatestCurrencyRate(
        currencyCode: String = "USD"
    ): CurrencyRateEntity?

    @Query("""
    SELECT * FROM currency_rates
    WHERE currencyCode = :currencyCode
    ORDER BY createdAt DESC
""")
    fun observeCurrencyRateHistory(
        currencyCode: String = "USD"
    ): Flow<List<CurrencyRateEntity>>

    @Insert
    suspend fun insertCurrencyRate(rate: CurrencyRateEntity): Long


// ---------- Profit Rule ----------

    @Query("""
    SELECT * FROM profit_rules
    WHERE isActive = 1
    ORDER BY isDefault DESC, title ASC
""")
    fun observeActiveProfitRules(): Flow<List<ProfitRuleEntity>>

    @Query("""
    SELECT * FROM profit_rules
    WHERE isActive = 1
    AND (
        categoryId = :categoryId
        OR categoryId IS NULL
    )
    ORDER BY categoryId DESC, isDefault DESC
    LIMIT 1
""")
    suspend fun getProfitRuleForCategory(
        categoryId: Int
    ): ProfitRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfitRule(rule: ProfitRuleEntity): Long

    @Query("""
    UPDATE profit_rules
    SET isActive = 0, updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun deactivateProfitRule(
        id: Int,
        updatedAt: Long = System.currentTimeMillis()
    )


    // ---------- Inventory ----------

    @Insert
    suspend fun insertInventoryTransaction(
        transaction: InventoryTransactionEntity
    ): Long

    @Query("""
    SELECT * FROM inventory_transactions
    WHERE productId = :productId
    ORDER BY createdAt DESC
""")
    fun observeInventoryTransactions(
        productId: Int
    ): Flow<List<InventoryTransactionEntity>>


    @Query("""
SELECT COALESCE(
    SUM(
        CASE
            WHEN transactionType IN ('PURCHASE','SALE_RETURN')
                THEN quantity

            WHEN transactionType IN ('SALE','PURCHASE_RETURN')
                THEN -quantity

            ELSE quantity
        END
    ),
0
)
FROM inventory_transactions
WHERE productId = :productId
""")
    fun observeCurrentStock(
        productId: Int
    ): Flow<Double>


}