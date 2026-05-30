package com.example.solarShop.data.room.tables.product


import com.example.solarShop.data.local.dao.pricing.PricingDao
import com.example.solarShop.data.local.dao.product.ProductDao
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val pricingDao: PricingDao
) {

    suspend fun calculateSalePrice(productId: Int): ProductSalePriceResult? {
        val productFullInfo = productDao.getProductFullInfo(productId)
            ?: return null

        val product = productFullInfo.product

        val purchasePrice = pricingDao.getActivePurchasePrice(productId)
            ?: return null

        val latestDollarRate = pricingDao.getLatestCurrencyRate("USD")

        val profitRule = pricingDao.getProfitRuleForCategory(product.categoryId)

        return ProductPriceCalculator.calculate(
            buyPriceDollar = purchasePrice.buyPriceDollar,
            buyPriceToman = purchasePrice.buyPriceToman,
            dollarRateToman = purchasePrice.dollarRateToman
                ?: latestDollarRate?.rateToman,
            profitPercent = profitRule?.profitPercent ?: 0.0,
            fixedProfitToman = profitRule?.fixedProfitToman ?: 0L
        )
    }
}