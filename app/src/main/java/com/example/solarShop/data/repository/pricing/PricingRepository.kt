package com.example.solarShop.data.repository.pricing

import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProfitRuleEntity
import com.example.solarShop.domain.product.ProductSalePriceResult
import kotlinx.coroutines.flow.Flow

interface PricingRepository {

    fun observeCurrencyRateHistory(
        currencyCode: String = "USD"
    ): Flow<List<CurrencyRateEntity>>

    suspend fun insertCurrencyRate(rate: CurrencyRateEntity): Long

    fun observeActiveProfitRules(): Flow<List<ProfitRuleEntity>>

    suspend fun upsertProfitRule(rule: ProfitRuleEntity): Long

    suspend fun deactivateProfitRule(ruleId: Int)

    suspend fun setNewActivePurchasePrice(
        price: ProductPurchasePriceEntity
    )

    fun observePurchasePriceHistory(
        productId: Int
    ): Flow<List<ProductPurchasePriceEntity>>

    suspend fun calculateSalePrice(
        productId: Int,
        todayDollarRateToman: Long? = null
    ): ProductSalePriceResult?

    suspend fun setNewPurchasePrice(
        price: ProductPurchasePriceEntity
    )

    suspend fun getLatestCurrencyRate(
        currencyCode: String = "USD"
    ): CurrencyRateEntity?

    suspend fun getActivePurchasePrice(
        productId: Int
    ): ProductPurchasePriceEntity?

    fun observePurchasePrices(
        productId: Int
    ): Flow<List<ProductPurchasePriceEntity>>

    fun observeSalePrices(
        productId: Int
    ): Flow<List<ProductSalePriceEntity>>

    suspend fun setNewActiveSalePrice(
        price: ProductSalePriceEntity
    )

    suspend fun getActiveSalePrice(
        productId: Int,
        priceType: String
    ): ProductSalePriceEntity?


    suspend fun deletePurchasePriceById(id: Int)

    suspend fun deleteSalePriceById(id: Int)

    suspend fun updatePurchasePrice(
        id: Int,
        buyPriceDollar: Double?,
        buyPriceToman: Long,
        dollarRateToman: Long?,
        quantity: Double?,
        purchasedAt: Long,
        note: String
    )

    suspend fun updateSalePrice(
        id: Int,
        salePriceToman: Long,
        profitPercent: Double?,
        baseDollarPrice: Double?,
        dollarRateToman: Long?,
        basePurchasePriceToman: Long?,
        note: String,
        createdAt: Long
    )

    suspend fun getUnsyncedPurchasePrices(): List<ProductPurchasePriceEntity>

    suspend fun markPurchasePricesSynced(uids: List<String>)

    suspend fun upsertPurchasePriceByUid(
        item: ProductPurchasePriceEntity
    ): Long


    suspend fun getUnsyncedSalePrices():
            List<ProductSalePriceEntity>

    suspend fun markSalePricesSynced(
        uids: List<String>
    )

    suspend fun upsertSalePriceByUid(
        item: ProductSalePriceEntity
    ): Long

    suspend fun getUnsyncedCurrencyRates():
            List<CurrencyRateEntity>

    suspend fun markCurrencyRatesSynced(
        uids: List<String>
    )

    suspend fun upsertCurrencyRateByUid(
        item: CurrencyRateEntity
    ): Long

    suspend fun getAllPurchasePricesForBackup(): List<ProductPurchasePriceEntity>

    suspend fun getAllSalePricesForBackup(): List<ProductSalePriceEntity>

    suspend fun getAllCurrencyRatesForBackup(): List<CurrencyRateEntity>

    fun observeAllPurchasePrices(): Flow<List<ProductPurchasePriceEntity>>

    fun observeAllSalePrices(): Flow<List<ProductSalePriceEntity>>

}