package com.example.solarShop.data.repository.pricing

import com.example.solarShop.data.local.dao.pricing.PricingDao
import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProfitRuleEntity
import com.example.solarShop.domain.product.ProductPriceCalculator
import com.example.solarShop.domain.product.ProductSalePriceResult
import javax.inject.Inject

class PricingRepositoryImpl @Inject constructor(
    private val pricingDao: PricingDao,
    private val productDao: ProductDao
) : PricingRepository {

    override fun observeCurrencyRateHistory(
        currencyCode: String
    ) = pricingDao.observeCurrencyRateHistory(currencyCode)

    override suspend fun insertCurrencyRate(rate: CurrencyRateEntity) =
        pricingDao.insertCurrencyRate(rate)

    override fun observeActiveProfitRules() =
        pricingDao.observeActiveProfitRules()

    override suspend fun upsertProfitRule(rule: ProfitRuleEntity) =
        pricingDao.upsertProfitRule(rule)

    override suspend fun deactivateProfitRule(ruleId: Int) =
        pricingDao.deactivateProfitRule(ruleId)

    override suspend fun setNewActivePurchasePrice(
        price: ProductPurchasePriceEntity
    ) {
        pricingDao.setNewActivePurchasePrice(price)
    }

    override fun observePurchasePriceHistory(
        productId: Int
    ) = pricingDao.observePurchasePriceHistory(productId)

    override suspend fun calculateSalePrice(
        productId: Int,
        todayDollarRateToman: Long?
    ): ProductSalePriceResult? {
        val productFullInfo = productDao.getProductFullInfo(productId)
            ?: return null

        val product = productFullInfo.product

        val purchasePrice = pricingDao.getActivePurchasePrice(productId)
            ?: return null

        val latestDollarRate = pricingDao.getLatestCurrencyRate("USD")

        val effectiveTodayRate =
            todayDollarRateToman ?: latestDollarRate?.rateToman

        val profitRule = pricingDao.getProfitRuleForCategory(product.categoryId)

        return ProductPriceCalculator.calculate(
            buyPriceDollar = purchasePrice.buyPriceDollar,
            buyPriceToman = purchasePrice.buyPriceToman,
            purchaseDollarRateToman = purchasePrice.dollarRateToman,
            todayDollarRateToman = effectiveTodayRate,
            profitPercent = profitRule?.profitPercent ?: 0.0,
            fixedProfitToman = profitRule?.fixedProfitToman ?: 0L
        )
    }

    override suspend fun setNewPurchasePrice(
        price: ProductPurchasePriceEntity
    ) {
        pricingDao.setNewActivePurchasePrice(price)
    }

    override suspend fun getLatestCurrencyRate(
        currencyCode: String
    ): CurrencyRateEntity? {
        return pricingDao.getLatestCurrencyRate(currencyCode)
    }

    override suspend fun getActivePurchasePrice(
        productId: Int
    ): ProductPurchasePriceEntity? {
        return pricingDao.getActivePurchasePrice(productId)
    }

    override fun observePurchasePrices(
        productId: Int
    ) = pricingDao.observePurchasePrices(productId)

    override fun observeSalePrices(
        productId: Int
    ) = pricingDao.observeSalePrices(productId)

    override suspend fun setNewActiveSalePrice(
        price: ProductSalePriceEntity
    ) {
        pricingDao.setNewActiveSalePrice(price)
    }

    override suspend fun getActiveSalePrice(
        productId: Int,
        priceType: String
    ): ProductSalePriceEntity? {
        return pricingDao.getActiveSalePrice(
            productId = productId,
            priceType = priceType
        )
    }

    override suspend fun deletePurchasePriceById(id: Int) {
        pricingDao.deletePurchasePriceById(id)
    }

    override suspend fun deleteSalePriceById(id: Int) {
        pricingDao.deleteSalePriceById(id)
    }

    override suspend fun updatePurchasePrice(
        id: Int,
        buyPriceDollar: Double?,
        buyPriceToman: Long,
        dollarRateToman: Long?,
        quantity: Double?,
        purchasedAt: Long,
        note: String
    ) {
        pricingDao.updatePurchasePrice(
            id = id,
            buyPriceDollar = buyPriceDollar,
            buyPriceToman = buyPriceToman,
            dollarRateToman = dollarRateToman,
            quantity = quantity,
            purchasedAt = purchasedAt,
            note = note
        )
    }

    override suspend fun updateSalePrice(
        id: Int,
        salePriceToman: Long,
        profitPercent: Double?,
        baseDollarPrice: Double?,
        dollarRateToman: Long?,
        basePurchasePriceToman: Long?,
        note: String,
        createdAt: Long
    ) {
        pricingDao.updateSalePrice(
            id = id,
            salePriceToman = salePriceToman,
            profitPercent = profitPercent,
            baseDollarPrice = baseDollarPrice,
            dollarRateToman = dollarRateToman,
            basePurchasePriceToman = basePurchasePriceToman,
            note = note,
            createdAt = createdAt
        )
    }

    override suspend fun getUnsyncedPurchasePrices():
            List<ProductPurchasePriceEntity> {
        return pricingDao.getUnsyncedPurchasePrices()
    }

    override suspend fun markPurchasePricesSynced(
        uids: List<String>
    ) {
        pricingDao.markPurchasePricesSynced(uids)
    }

    override suspend fun upsertPurchasePriceByUid(
        item: ProductPurchasePriceEntity
    ): Long {
        val existing =
            pricingDao.getPurchasePriceByUid(item.uid)

        if (existing == null) {
            return pricingDao.upsertPurchasePrice(item)
        }

        if (existing.deletedAt != null && item.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > item.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return pricingDao.upsertPurchasePrice(
            item.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getUnsyncedSalePrices():
            List<ProductSalePriceEntity> {
        return pricingDao.getUnsyncedSalePrices()
    }

    override suspend fun markSalePricesSynced(
        uids: List<String>
    ) {
        pricingDao.markSalePricesSynced(uids)
    }

    override suspend fun upsertSalePriceByUid(
        item: ProductSalePriceEntity
    ): Long {
        val existing =
            pricingDao.getSalePriceByUid(item.uid)

        if (existing == null) {
            return pricingDao.upsertSalePrice(item)
        }

        if (existing.deletedAt != null && item.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > item.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return pricingDao.upsertSalePrice(
            item.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getUnsyncedCurrencyRates():
            List<CurrencyRateEntity> {
        return pricingDao.getUnsyncedCurrencyRates()
    }

    override suspend fun markCurrencyRatesSynced(
        uids: List<String>
    ) {
        if (uids.isEmpty()) return
        pricingDao.markCurrencyRatesSynced(uids)
    }

    override suspend fun upsertCurrencyRateByUid(
        item: CurrencyRateEntity
    ): Long {
        val existing =
            pricingDao.getCurrencyRateByUid(item.uid)

        if (existing == null) {
            return pricingDao.upsertCurrencyRate(item)
        }

        // Delete Wins
        if (existing.deletedAt != null && item.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > item.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return pricingDao.upsertCurrencyRate(
            item.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getAllPurchasePricesForBackup(): List<ProductPurchasePriceEntity> =
        pricingDao.getAllPurchasePricesForBackup()

    override suspend fun getAllSalePricesForBackup(): List<ProductSalePriceEntity> =
        pricingDao.getAllSalePricesForBackup()

    override suspend fun getAllCurrencyRatesForBackup(): List<CurrencyRateEntity> =
        pricingDao.getAllCurrencyRatesForBackup()


    override fun observeAllPurchasePrices() =
        pricingDao.observeAllPurchasePrices()

    override fun observeAllSalePrices() =
        pricingDao.observeAllSalePrices()
}