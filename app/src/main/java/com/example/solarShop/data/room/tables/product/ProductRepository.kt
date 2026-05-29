package com.example.solarShop.data.room.tables.product


import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {

    suspend fun calculateSalePrice(productId: Int): ProductSalePriceResult? {
        val productFullInfo = productDao.getProductFullInfo(productId)
            ?: return null

        val product = productFullInfo.product

        val purchasePrice = productDao.getActivePurchasePrice(productId)
            ?: return null

        val latestDollarRate = productDao.getLatestCurrencyRate("USD")

        val profitRule = productDao.getProfitRuleForCategory(product.categoryId)

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