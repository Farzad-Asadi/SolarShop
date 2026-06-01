package com.example.solarShop.feature.product.viewmodel.product

import android.net.Uri
import com.example.solarShop.data.local.relation.product.ProductFullInfo

data class ProductGridItemUi(
    val productFullInfo: ProductFullInfo,
    val coverUri: Uri? = null
)