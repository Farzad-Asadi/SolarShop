package com.example.solarShop.data.local.dao.pricing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
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


    // ---------- Restore ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasePricesForRestore(items: List<ProductPurchasePriceEntity>)


}