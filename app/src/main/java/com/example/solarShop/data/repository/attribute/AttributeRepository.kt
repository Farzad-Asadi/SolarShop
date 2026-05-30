package com.example.solarShop.data.repository.attribute

import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import kotlinx.coroutines.flow.Flow

interface AttributeRepository {

    fun observeActiveAttributeDefinitions(
        categoryId: Int
    ): Flow<List<CategoryAttributeDefinitionEntity>>

    suspend fun upsertAttributeDefinition(
        definition: CategoryAttributeDefinitionEntity
    ): Long

    suspend fun deactivateAttributeDefinition(id: Int)

    fun observeProductAttributeDisplayInfo(
        productId: Int,
        categoryId: Int
    ): Flow<List<ProductAttributeDisplayInfo>>

    suspend fun upsertAttributeValue(
        value: ProductAttributeValueEntity
    ): Long

    suspend fun deleteAttributeValue(
        productId: Int,
        attributeDefinitionId: Int
    )
}