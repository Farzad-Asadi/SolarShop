package com.example.solarShop.feature.product.viewmodel.pricing

data class PurchasePriceEditUiState(

    val productId: Int? = null,

    val buyPriceDollar: String = "",

    val buyPriceToman: Long? = null,

    val dollarRateToman: Long? = null,

    val note: String = "",

    val isSaving: Boolean = false,

    val lastInputSource: PurchasePriceInputSource = PurchasePriceInputSource.DOLLAR
)


enum class PurchasePriceInputSource {
    DOLLAR,
    TOMAN
}