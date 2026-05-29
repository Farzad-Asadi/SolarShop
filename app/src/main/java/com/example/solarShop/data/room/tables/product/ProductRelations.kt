package com.example.solarShop.data.room.tables.product

import androidx.room.Embedded
import androidx.room.Relation
import com.example.solarShop.data.room.tables.product.*

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