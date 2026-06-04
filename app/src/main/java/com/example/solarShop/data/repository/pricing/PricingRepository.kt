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
        productId: Int
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
}