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

    override suspend fun getAttributeDefinitionById(
        id: Int
    ): CategoryAttributeDefinitionEntity? {
        return attributeDao.getAttributeDefinitionById(id)
    }

    override suspend fun updateAttributeSortOrder(
        id: Int,
        sortOrder: Int
    ) {
        attributeDao.updateAttributeSortOrder(
            id = id,
            sortOrder = sortOrder
        )
    }

    override suspend fun getNextAttributeSortOrder(
        categoryId: Int
    ): Int {
        return attributeDao.getNextAttributeSortOrder(categoryId)
    }

    override suspend fun upsertAttributeDefinitionByUid(
        definition: CategoryAttributeDefinitionEntity
    ): Long {
        val existing =
            attributeDao.getAttributeDefinitionByUid(definition.uid)
                ?: return attributeDao.upsertAttributeDefinition(definition)

        // Delete Wins
        if (existing.deletedAt != null && definition.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > definition.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return attributeDao.upsertAttributeDefinition(
            definition.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getUnsyncedAttributeDefinitions():
            List<CategoryAttributeDefinitionEntity> {
        return attributeDao.getUnsyncedAttributeDefinitions()
    }

    override suspend fun markAttributeDefinitionsSynced(
        uids: List<String>
    ) {
        if (uids.isEmpty()) return
        attributeDao.markAttributeDefinitionsSynced(uids)
    }

    override suspend fun upsertProductAttributeValueByUid(
        value: ProductAttributeValueEntity
    ): Long {
        val existing =
            attributeDao.getProductAttributeValueByUid(value.uid)
                ?: return attributeDao.upsertAttributeValue(value)

        // Delete Wins
        if (existing.deletedAt != null && value.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > value.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return attributeDao.upsertAttributeValue(
            value.copy(
                id = existing.id
            )
        )
    }

    override suspend fun getUnsyncedProductAttributeValues():
            List<ProductAttributeValueEntity> {
        return attributeDao.getUnsyncedProductAttributeValues()
    }

    override suspend fun markProductAttributeValuesSynced(
        uids: List<String>
    ) {
        if (uids.isEmpty()) return
        attributeDao.markProductAttributeValuesSynced(uids)
    }


    override suspend fun getAttributeDefinitionByUid(
        uid: String
    ): CategoryAttributeDefinitionEntity? {
        return attributeDao.getAttributeDefinitionByUid(uid)
    }

    override suspend fun getAllAttributeDefinitionsForBackup(): List<CategoryAttributeDefinitionEntity> {
        return attributeDao.getAllAttributeDefinitionsForBackup()
    }

    override suspend fun getAllAttributeValuesForBackup(): List<ProductAttributeValueEntity> {
        return attributeDao.getAllAttributeValuesForBackup()
    }


}