package com.example.solarShop.data.local.dao.attribute

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AttributeDao {

    @Query("""
        SELECT * FROM category_attribute_definitions
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY sortOrder ASC, title ASC
    """)
    fun observeActiveAttributeDefinitions(
        categoryId: Int
    ): Flow<List<CategoryAttributeDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttributeDefinition(
        definition: CategoryAttributeDefinitionEntity
    ): Long

    @Query("""
        UPDATE category_attribute_definitions
        SET isActive = 0, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun deactivateAttributeDefinition(
        id: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        SELECT * FROM product_attribute_values
        WHERE productId = :productId
    """)
    fun observeAttributeValues(
        productId: Int
    ): Flow<List<ProductAttributeValueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttributeValue(
        value: ProductAttributeValueEntity
    ): Long

    @Query("""
        DELETE FROM product_attribute_values
        WHERE productId = :productId 
        AND attributeDefinitionId = :attributeDefinitionId
    """)
    suspend fun deleteAttributeValue(
        productId: Int,
        attributeDefinitionId: Int
    )

    @Query("""
        SELECT 
            product_attribute_values.id AS valueId,
            :productId AS productId,
            category_attribute_definitions.id AS attributeDefinitionId,

            category_attribute_definitions.title AS title,
            category_attribute_definitions.`key` AS `key`,
            category_attribute_definitions.valueType AS valueType,
            category_attribute_definitions.unit AS unit,
            category_attribute_definitions.enumOptions AS enumOptions,
            category_attribute_definitions.isRequired AS isRequired,
            category_attribute_definitions.sortOrder AS sortOrder,

            product_attribute_values.valueText AS valueText

        FROM category_attribute_definitions
        LEFT JOIN product_attribute_values
            ON product_attribute_values.attributeDefinitionId = category_attribute_definitions.id
            AND product_attribute_values.productId = :productId

        WHERE category_attribute_definitions.categoryId = :categoryId
        AND category_attribute_definitions.isActive = 1

        ORDER BY category_attribute_definitions.sortOrder ASC, category_attribute_definitions.title ASC
    """)
    fun observeProductAttributeDisplayInfo(
        productId: Int,
        categoryId: Int
    ): Flow<List<ProductAttributeDisplayInfo>>


    // ---------- Backup ----------
    @Query("SELECT * FROM category_attribute_definitions")
    suspend fun getAllAttributeDefinitionsForBackup(): List<CategoryAttributeDefinitionEntity>

    @Query("SELECT * FROM product_attribute_values")
    suspend fun getAllAttributeValuesForBackup(): List<ProductAttributeValueEntity>



    // ---------- Restore ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributeDefinitionsForRestore(items: List<CategoryAttributeDefinitionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributeValuesForRestore(items: List<ProductAttributeValueEntity>)



    // ---------- Helper ----------
    @Query("""
    SELECT * FROM category_attribute_definitions
    WHERE id = :id
    LIMIT 1
""")
    suspend fun getAttributeDefinitionById(
        id: Int
    ): CategoryAttributeDefinitionEntity?



    // ---------- Sort Order ----------
    @Query("""
    UPDATE category_attribute_definitions
    SET sortOrder = :sortOrder, updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun updateAttributeSortOrder(
        id: Int,
        sortOrder: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    SELECT COALESCE(MAX(sortOrder), -1) + 1
    FROM category_attribute_definitions
    WHERE categoryId = :categoryId
""")
    suspend fun getNextAttributeSortOrder(categoryId: Int): Int

}