package com.example.solarShop.data.room.tables.product

import kotlin.math.roundToLong

data class ProductSalePriceResult(
    val baseBuyPriceToman: Long,
    val profitAmountToman: Long,
    val fixedProfitToman: Long,
    val finalSalePriceToman: Long
)

object ProductPriceCalculator {

    fun calculate(
        buyPriceDollar: Double?,
        buyPriceToman: Long?,
        dollarRateToman: Long?,
        profitPercent: Double,
        fixedProfitToman: Long
    ): ProductSalePriceResult? {

        val basePriceToman = when {
            buyPriceToman != null -> buyPriceToman

            buyPriceDollar != null && dollarRateToman != null ->
                (buyPriceDollar * dollarRateToman).roundToLong()

            else -> return null
        }

        val profitAmount = (basePriceToman * profitPercent / 100.0).roundToLong()

        val finalPrice = basePriceToman + profitAmount + fixedProfitToman

        return ProductSalePriceResult(
            baseBuyPriceToman = basePriceToman,
            profitAmountToman = profitAmount,
            fixedProfitToman = fixedProfitToman,
            finalSalePriceToman = finalPrice
        )
    }
}