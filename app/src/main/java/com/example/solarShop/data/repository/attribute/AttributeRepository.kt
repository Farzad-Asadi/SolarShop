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



    suspend fun getAttributeDefinitionById(
        id: Int
    ): CategoryAttributeDefinitionEntity?


    // ---------- Sort Order ----------
    suspend fun updateAttributeSortOrder(
        id: Int,
        sortOrder: Int
    )

    suspend fun getNextAttributeSortOrder(
        categoryId: Int
    ): Int

    // ---------- Sync: Attribute Definitions ----------
    suspend fun upsertAttributeDefinitionByUid(
        definition: CategoryAttributeDefinitionEntity
    ): Long

    suspend fun getUnsyncedAttributeDefinitions():
            List<CategoryAttributeDefinitionEntity>

    suspend fun markAttributeDefinitionsSynced(
        uids: List<String>
    )


    // ---------- Sync: Product Attribute Values ----------
    suspend fun upsertProductAttributeValueByUid(
        value: ProductAttributeValueEntity
    ): Long

    suspend fun getUnsyncedProductAttributeValues():
            List<ProductAttributeValueEntity>

    suspend fun markProductAttributeValuesSynced(
        uids: List<String>
    )


    suspend fun getAttributeDefinitionByUid(uid: String): CategoryAttributeDefinitionEntity?






}