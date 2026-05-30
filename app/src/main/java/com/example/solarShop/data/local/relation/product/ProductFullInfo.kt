package com.example.solarShop.data.local.relation.product

import androidx.room.Embedded
import androidx.room.Relation
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity

data class ProductFullInfo(
    @Embedded
    val product: ProductEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: ProductCategoryEntity?,

    @Relation(
        parentColumn = "brandId",
        entityColumn = "id"
    )
    val brand: ProductBrandEntity?,

    @Relation(
        parentColumn = "unitId",
        entityColumn = "id"
    )
    val unit: ProductUnitEntity?
)