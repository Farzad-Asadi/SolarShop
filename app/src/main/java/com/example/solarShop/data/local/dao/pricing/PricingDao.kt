package com.example.solarShop.data.local.dao.pricing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProfitRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PricingDao{

    // Purchase Price

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




    // Currency


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



    // Profit Rule


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


    @Query("""
        SELECT * FROM product_purchase_prices
        WHERE productId = :productId
        ORDER BY createdAt DESC
    """)
    fun observePurchasePriceHistory(productId: Int): Flow<List<ProductPurchasePriceEntity>>



    // ---------- Backup ----------
    @Query("SELECT * FROM product_purchase_prices")
    suspend fun getAllPurchasePricesForBackup(): List<ProductPurchasePriceEntity>

    @Query("SELECT * FROM product_sale_prices")
    suspend fun getAllSalePricesForBackup(): List<ProductSalePriceEntity>


    // ---------- Restore ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasePricesForRestore(items: List<ProductPurchasePriceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalePricesForRestore(
        items: List<ProductSalePriceEntity>
    )



    // ---------- Observe ----------
    @Query("""
    SELECT * FROM product_purchase_prices
    WHERE productId = :productId
    ORDER BY purchasedAt DESC, createdAt DESC
""")
    fun observePurchasePrices(
        productId: Int
    ): Flow<List<ProductPurchasePriceEntity>>


    // Sale Price

    @Query("""
    SELECT * FROM product_sale_prices
    WHERE productId = :productId
    ORDER BY createdAt DESC
""")
    fun observeSalePrices(
        productId: Int
    ): Flow<List<ProductSalePriceEntity>>

    @Query("""
    SELECT * FROM product_sale_prices
    WHERE productId = :productId
    AND priceType = :priceType
    AND isActive = 1
    ORDER BY createdAt DESC
    LIMIT 1
""")
    suspend fun getActiveSalePrice(
        productId: Int,
        priceType: String
    ): ProductSalePriceEntity?

    @Query("""
    UPDATE product_sale_prices
    SET isActive = 0, updatedAt = :updatedAt
    WHERE productId = :productId
    AND priceType = :priceType
""")
    suspend fun deactivateActiveSalePrices(
        productId: Int,
        priceType: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Insert
    suspend fun insertSalePrice(
        price: ProductSalePriceEntity
    ): Long

    @Transaction
    suspend fun setNewActiveSalePrice(
        price: ProductSalePriceEntity
    ) {
        deactivateActiveSalePrices(
            productId = price.productId,
            priceType = price.priceType
        )

        insertSalePrice(
            price.copy(isActive = true)
        )
    }

    @Query("""
    DELETE FROM product_purchase_prices
    WHERE id = :id
""")
    suspend fun deletePurchasePriceById(id: Int)

    @Query("""
    DELETE FROM product_sale_prices
    WHERE id = :id
""")
    suspend fun deleteSalePriceById(id: Int)

    @Query("""
    UPDATE product_purchase_prices
    SET
        buyPriceDollar = :buyPriceDollar,
        buyPriceToman = :buyPriceToman,
        dollarRateToman = :dollarRateToman,
        quantity = :quantity,
        purchasedAt = :purchasedAt,
        note = :note,
        updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun updatePurchasePrice(
        id: Int,
        buyPriceDollar: Double?,
        buyPriceToman: Long,
        dollarRateToman: Long?,
        quantity: Double?,
        purchasedAt: Long,
        note: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    UPDATE product_sale_prices
    SET
        salePriceToman = :salePriceToman,
        profitPercent = :profitPercent,
        baseDollarPrice = :baseDollarPrice,
        dollarRateToman = :dollarRateToman,
        basePurchasePriceToman = :basePurchasePriceToman,
        note = :note,
        createdAt = :createdAt,
        updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun updateSalePrice(
        id: Int,
        salePriceToman: Long,
        profitPercent: Double?,
        baseDollarPrice: Double?,
        dollarRateToman: Long?,
        basePurchasePriceToman: Long?,
        note: String,
        createdAt: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

}