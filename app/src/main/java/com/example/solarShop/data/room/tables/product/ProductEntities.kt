package com.example.solarShop.data.room.tables.product


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.InventoryTransactionType
import java.util.UUID

@Entity(
    tableName = "product_categories",
    indices = [Index("name", unique = true)]
)
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)







@Entity(
    tableName = "product_brands",
    indices = [Index("name", unique = true)]
)
data class ProductBrandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val name: String,
    val description: String = "",
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)










@Entity(
    tableName = "product_units",
    indices = [Index("name", unique = true)]
)
data class ProductUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val name: String,
    val symbol: String = name,
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)












@Entity(
    tableName = "products",
    indices = [
        Index("categoryId"),
        Index("brandId"),
        Index("unitId"),
        Index("name"),
        Index("model"),
        Index("isArchived")
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val categoryId: Int,
    val brandId: Int? = null,
    val unitId: Int? = null,

    val name: String,
    val model: String = "",
    val description: String = "",

    val isArchived: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)













@Entity(
    tableName = "product_purchase_prices",
    indices = [
        Index("productId"),
        Index("createdAt"),
        Index(value = ["productId", "isActive"])
    ]
)
data class ProductPurchasePriceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    val buyPriceDollar: Double? = null,
    val buyPriceToman: Long? = null,
    val dollarRateToman: Long? = null,

    val note: String = "",
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)







@Entity(
    tableName = "category_attribute_definitions",
    indices = [
        Index("categoryId"),
        Index(value = ["categoryId", "key"], unique = true)
    ]
)
data class CategoryAttributeDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val categoryId: Int,

    val title: String,
    val key: String,

    // text, number, boolean, enum
    val valueType: String = "text",

    val unit: String? = null,
    val isRequired: Boolean = false,
    val sortOrder: Int = 0,

    // مثلا: "تک فاز,سه فاز"
    val enumOptions: String? = null,

    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)










@Entity(
    tableName = "product_attribute_values",
    indices = [
        Index("productId"),
        Index("attributeDefinitionId"),
        Index(value = ["productId", "attributeDefinitionId"], unique = true)
    ]
)
data class ProductAttributeValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,
    val attributeDefinitionId: Int,

    // همه‌چیز فعلاً به صورت String ذخیره می‌شود
    val valueText: String = "",

    val updatedAt: Long = System.currentTimeMillis()
)



@Entity(
    tableName = "product_images",
    indices = [
        Index("productId"),
        Index(value = ["productId", "sortOrder"])
    ]
)
data class ProductImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    val relativePath: String,

    val thumbnailRelativePath: String? = null,

    val sortOrder: Int = 0,

    val createdAt: Long = System.currentTimeMillis()
)


@Entity(
    tableName = "currency_rates",
    indices = [
        Index("currencyCode"),
        Index("createdAt")
    ]
)
data class CurrencyRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val currencyCode: String = "USD",
    val rateToman: Long,

    val source: String = "",
    val note: String = "",

    val createdAt: Long = System.currentTimeMillis()
)






@Entity(
    tableName = "profit_rules",
    indices = [
        Index("categoryId"),
        Index("isDefault")
    ]
)
data class ProfitRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    // null یعنی قانون عمومی
    val categoryId: Int? = null,

    val title: String = "پیش‌فرض",

    val profitPercent: Double = 0.0,
    val fixedProfitToman: Long = 0L,

    val isDefault: Boolean = false,
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


@Entity(
    tableName = "inventory_transactions",
    indices = [
        Index("productId"),
        Index("transactionType"),
        Index("createdAt")
    ]
)
data class InventoryTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    val quantity: Double,

    val transactionType: InventoryTransactionType,

    val note: String = "",

    val createdAt: Long = System.currentTimeMillis()
)