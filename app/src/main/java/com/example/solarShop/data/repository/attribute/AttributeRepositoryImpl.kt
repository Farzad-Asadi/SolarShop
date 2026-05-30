package com.example.solarShop.data.repository.attribute

import com.example.solarShop.data.local.dao.attribute.AttributeDao
import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import javax.inject.Inject

class AttributeRepositoryImpl @Inject constructor(
    private val attributeDao: AttributeDao
) : AttributeRepository {

    override fun observeActiveAttributeDefinitions(categoryId: Int) =
        attributeDao.observeActiveAttributeDefinitions(categoryId)

    override suspend fun upsertAttributeDefinition(
        definition: CategoryAttributeDefinitionEntity
    ) = attributeDao.upsertAttributeDefinition(definition)

    override suspend fun deactivateAttributeDefinition(id: Int) =
        attributeDao.deactivateAttributeDefinition(id)

    override fun observeProductAttributeDisplayInfo(
        productId: Int,
        categoryId: Int
    ) = attributeDao.observeProductAttributeDisplayInfo(productId, categoryId)

    override suspend fun upsertAttributeValue(
        value: ProductAttributeValueEntity
    ) = attributeDao.upsertAttributeValue(value)

    override suspend fun deleteAttributeValue(
        productId: Int,
        attributeDefinitionId: Int
    ) = attributeDao.deleteAttributeValue(productId, attributeDefinitionId)
}