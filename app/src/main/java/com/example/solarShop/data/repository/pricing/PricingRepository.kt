package com.example.solarShop.data.repository.pricing

import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
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
}