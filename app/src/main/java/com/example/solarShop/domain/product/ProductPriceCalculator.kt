package com.example.solarShop.domain.product

import kotlin.math.roundToLong

data class ProductSalePriceResult(
    val baseBuyPriceToman: Long,
    val baseBuyPriceDollar: Double?,
    val purchaseDollarRateToman: Long?,
    val todayDollarRateToman: Long?,
    val effectiveDollarRateToman: Long?,
    val profitAmountToman: Long,
    val fixedProfitToman: Long,
    val finalSalePriceToman: Long
)

object ProductPriceCalculator {

    fun calculate(
        buyPriceDollar: Double?,
        buyPriceToman: Long?,
        purchaseDollarRateToman: Long?,
        todayDollarRateToman: Long?,
        profitPercent: Double,
        fixedProfitToman: Long
    ): ProductSalePriceResult? {

        val effectiveRate =
            listOfNotNull(
                purchaseDollarRateToman,
                todayDollarRateToman
            ).maxOrNull()

        val baseDollarPrice =
            buyPriceDollar
                ?: if (
                    buyPriceToman != null &&
                    purchaseDollarRateToman != null &&
                    purchaseDollarRateToman > 0L
                ) {
                    buyPriceToman.toDouble() / purchaseDollarRateToman.toDouble()
                } else {
                    null
                }

        val basePriceToman = when {
            baseDollarPrice != null && effectiveRate != null ->
                (baseDollarPrice * effectiveRate).roundToLong()

            buyPriceToman != null ->
                buyPriceToman

            else -> return null
        }

        val profitAmount =
            (basePriceToman * profitPercent / 100.0).roundToLong()

        val finalPrice =
            basePriceToman + profitAmount + fixedProfitToman

        return ProductSalePriceResult(
            baseBuyPriceToman = basePriceToman,
            baseBuyPriceDollar = baseDollarPrice,
            purchaseDollarRateToman = purchaseDollarRateToman,
            todayDollarRateToman = todayDollarRateToman,
            effectiveDollarRateToman = effectiveRate,
            profitAmountToman = profitAmount,
            fixedProfitToman = fixedProfitToman,
            finalSalePriceToman = finalPrice
        )
    }
}