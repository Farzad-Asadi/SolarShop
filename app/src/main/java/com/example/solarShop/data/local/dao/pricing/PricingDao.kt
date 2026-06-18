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
        WHERE productId = :productId 
        AND isActive = 1
        AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getActivePurchasePrice(productId: Int): ProductPurchasePriceEntity?

    @Query("""
    UPDATE product_purchase_prices
    SET 
        isActive = 0,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE productId = :productId
      AND deletedAt IS NULL
""")
    suspend fun deactivateActivePrices(
        productId: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

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
    AND deletedAt IS NULL
    ORDER BY createdAt DESC
    LIMIT 1
""")
    suspend fun getLatestCurrencyRate(
        currencyCode: String = "USD"
    ): CurrencyRateEntity?

    @Query("""
    SELECT * FROM currency_rates
    WHERE currencyCode = :currencyCode
    AND deletedAt IS NULL
    ORDER BY createdAt DESC
""")
    fun observeCurrencyRateHistory(
        currencyCode: String = "USD"
    ): Flow<List<CurrencyRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencyRate(rate: CurrencyRateEntity): Long

    @Query("""
    SELECT * FROM currency_rates
    WHERE uid = :uid
    LIMIT 1
""")
    suspend fun getCurrencyRateByUid(
        uid: String
    ): CurrencyRateEntity?

    @Query("""
    SELECT * FROM currency_rates
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedCurrencyRates():
            List<CurrencyRateEntity>

    @Query("""
    UPDATE currency_rates
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markCurrencyRatesSynced(
        uids: List<String>
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrencyRate(
        item: CurrencyRateEntity
    ): Long

    @Query("""
    UPDATE currency_rates
    SET
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :id
      AND deletedAt IS NULL
""")
    suspend fun softDeleteCurrencyRateById(
        id: Int,
        deletedAt: Long = System.currentTimeMillis()
    )

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
        AND deletedAt IS NULL
        ORDER BY createdAt DESC
    """)
    fun observePurchasePriceHistory(productId: Int): Flow<List<ProductPurchasePriceEntity>>



    // ---------- Backup ----------
    @Query("SELECT * FROM product_purchase_prices")
    suspend fun getAllPurchasePricesForBackup(): List<ProductPurchasePriceEntity>

    @Query("SELECT * FROM product_sale_prices")
    suspend fun getAllSalePricesForBackup(): List<ProductSalePriceEntity>

    @Query("SELECT * FROM currency_rates")
    suspend fun getAllCurrencyRatesForBackup(): List<CurrencyRateEntity>


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
     AND deletedAt IS NULL
    ORDER BY purchasedAt DESC, createdAt DESC
    """)
    fun observePurchasePrices(
        productId: Int
    ): Flow<List<ProductPurchasePriceEntity>>


    // Sale Price

    @Query("""
    SELECT * FROM product_sale_prices
    WHERE productId = :productId
    AND deletedAt IS NULL
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
    AND deletedAt IS NULL
    ORDER BY createdAt DESC
    LIMIT 1
""")
    suspend fun getActiveSalePrice(
        productId: Int,
        priceType: String
    ): ProductSalePriceEntity?

    @Query("""
    UPDATE product_sale_prices
    SET 
        isActive = 0,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE productId = :productId
      AND priceType = :priceType
      AND deletedAt IS NULL
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
    UPDATE product_purchase_prices
    SET
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :id
      AND deletedAt IS NULL
""")
    suspend fun deletePurchasePriceById(
        id: Int,
        deletedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    UPDATE product_sale_prices
    SET
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :id
      AND deletedAt IS NULL
""")
    suspend fun deleteSalePriceById(
        id: Int,
        deletedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    UPDATE product_purchase_prices
    SET
        buyPriceDollar = :buyPriceDollar,
        buyPriceToman = :buyPriceToman,
        dollarRateToman = :dollarRateToman,
        quantity = :quantity,
        purchasedAt = :purchasedAt,
        note = :note,
        updatedAt = :updatedAt,
        isSynced = 0
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
        updatedAt = :updatedAt,
        isSynced = 0
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


    @Query("""
    SELECT * FROM product_purchase_prices
    WHERE uid = :uid
    LIMIT 1
""")
    suspend fun getPurchasePriceByUid(
        uid: String
    ): ProductPurchasePriceEntity?


    @Query("""
    SELECT * FROM product_purchase_prices
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedPurchasePrices():
            List<ProductPurchasePriceEntity>


    @Query("""
    UPDATE product_purchase_prices
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markPurchasePricesSynced(
        uids: List<String>
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchasePrice(
        item: ProductPurchasePriceEntity
    ): Long

    @Query("""
    UPDATE product_purchase_prices
    SET
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :id
      AND deletedAt IS NULL
""")
    suspend fun softDeletePurchasePriceById(
        id: Int,
        deletedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    SELECT * FROM product_sale_prices
    WHERE uid = :uid
    LIMIT 1
""")
    suspend fun getSalePriceByUid(
        uid: String
    ): ProductSalePriceEntity?

    @Query("""
    SELECT * FROM product_sale_prices
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedSalePrices():
            List<ProductSalePriceEntity>

    @Query("""
    UPDATE product_sale_prices
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markSalePricesSynced(
        uids: List<String>
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSalePrice(
        item: ProductSalePriceEntity
    ): Long


    @Query("""
    UPDATE product_sale_prices
    SET
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :id
      AND deletedAt IS NULL
""")
    suspend fun softDeleteSalePriceById(
        id: Int,
        deletedAt: Long = System.currentTimeMillis()
    )


    @Query("""
    SELECT * FROM product_purchase_prices
    WHERE deletedAt IS NULL
""")
    fun observeAllPurchasePrices(): Flow<List<ProductPurchasePriceEntity>>

    @Query("""
    SELECT * FROM product_sale_prices
    WHERE deletedAt IS NULL
""")
    fun observeAllSalePrices(): Flow<List<ProductSalePriceEntity>>


}