package com.example.solarShop.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.solarShop.data.local.converter.EstimateConverters
import com.example.solarShop.data.local.converter.GeneralTypeConverters
import com.example.solarShop.data.local.converter.InvoiceTypeConverters
import com.example.solarShop.data.local.dao.attribute.AttributeDao
import com.example.solarShop.data.local.dao.inventory.InventoryDao
import com.example.solarShop.data.local.dao.pricing.PricingDao
import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.dao.product.ProductImageDao
import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity
import com.example.solarShop.data.local.entity.attribute.ProductAttributeValueEntity
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProfitRuleEntity
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoDao
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.client.ClientDao
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.contract.ContractInstanceDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceEntity
import com.example.solarShop.data.room.tables.contract.ContractInstanceFullDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceNoteDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractInstancePartyDao
import com.example.solarShop.data.room.tables.contract.ContractInstancePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractInstanceSectionDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceSectionEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateFullDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyDao
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionEntity
import com.example.solarShop.data.room.tables.market_prices.ClosetMarketDefaultsDao
import com.example.solarShop.data.room.tables.market_prices.ClosetMarketDefaultsEntity
import com.example.solarShop.data.room.tables.market_prices.MarketPricesDao
import com.example.solarShop.data.room.tables.market_prices.MarketPricesEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderDao
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseAllocationEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseAttachmentEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseCatalogEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseCategoryEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostDao
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderExpenseEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderReceiptEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ReceiptAllocationEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ReceiptAttachmentEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentDao
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceItemEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceTemplateDao
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceTemplateEntity
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoMetaDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoMetaEntity
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefEntity
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.OrderTimelineSuggestionEntity
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemDao
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemEntity
import com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep.OrderWorkflowStepDao
import com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep.OrderWorkflowStepEntity
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateDao
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateEntity
import com.example.solarShop.data.room.tables.premiumEntitlementCache.PremiumEntitlementCacheEntity
import com.example.solarShop.data.room.tables.premiumEntitlementCache.PremiumEntitlementDao
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerDao
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerImageEntity
import com.example.solarShop.data.room.tables.question_answers.question.AnswerNextQuestionCrossRef
import com.example.solarShop.data.room.tables.question_answers.question.QuestionDao
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceDao
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceEntity
import com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto.OrderAnswerSelectedPhotoDao
import com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto.OrderAnswerSelectedPhotoEntity
import com.example.solarShop.data.room.tables.user.UserDao
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.userData.userMarketPrices.UserMarketPricesDao
import com.example.solarShop.data.room.tables.user.userData.userMarketPrices.UserMarketPricesEntity
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.UserWorkflowStepDao
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.UserWorkflowStepEntity


@Database(
    entities =
    [UserEntity::class,
        ClientEntity::class,
        OrderEntity::class,
        QuestionEntity::class,
        AnswerNextQuestionCrossRef::class,
        AnswerEntity::class,
        AppInfoEntity::class,
        PriceEstimateEntity::class,
        SelectedChoiceEntity::class,
        TimelineItemEntity::class,
        AnswerImageEntity::class,
        MarketPricesEntity::class,
        ClosetMarketDefaultsEntity::class,
        ContractTemplateEntity::class,
        ContractTemplatePartyEntity::class,
        ContractTemplateSectionEntity::class,
        ContractTemplateNoteEntity::class,
        ContractInstanceEntity::class,
        ContractInstancePartyEntity::class,
        ContractInstanceSectionEntity::class,
        ContractInstanceNoteEntity::class,
        PremiumEntitlementCacheEntity::class,
        OrderReceiptEntity::class,
        OrderExpenseEntity::class,
        ExpenseCatalogEntity::class,
        ReceiptAttachmentEntity::class,
        ExpenseAttachmentEntity::class,
        OrderPhotoRefEntity::class,
        OrderPhotoMetaEntity::class,
        UserMarketPricesEntity::class,
        UserWorkflowStepEntity::class,
        OrderWorkflowStepEntity::class,
        InvoiceTemplateEntity::class,
        InvoiceDocumentEntity::class,
        InvoiceItemEntity::class,
        ReceiptAllocationEntity::class,
        ExpenseAllocationEntity::class,
        ExpenseCategoryEntity::class,
        OrderTimelineSuggestionEntity::class,
        OrderAnswerSelectedPhotoEntity::class,

        ProductCategoryEntity::class,
        ProductBrandEntity::class,
        ProductUnitEntity::class,
        ProductEntity::class,
        ProductPurchasePriceEntity::class,
        CategoryAttributeDefinitionEntity::class,
        ProductAttributeValueEntity::class,
        ProductImageEntity::class,
        CurrencyRateEntity::class,
        ProfitRuleEntity::class,
        InventoryTransactionEntity::class,

    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(
// ... کانورترهای قبلی ...
    EstimateConverters::class,
    InvoiceTypeConverters::class,
    GeneralTypeConverters::class,
)

abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun clientDao(): ClientDao
    abstract fun orderDao(): OrderDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerDao(): AnswerDao
    abstract fun appInfoDao(): AppInfoDao
    abstract fun selectedChoiceDao(): SelectedChoiceDao
    abstract fun timelineItemDao(): TimelineItemDao
    abstract fun priceEstimateDao(): PriceEstimateDao
    abstract fun marketPricesDao(): MarketPricesDao
    abstract fun closetMarketDefaultsDao(): ClosetMarketDefaultsDao
    abstract fun contractTemplateDao(): ContractTemplateDao
    abstract fun contractTemplatePartyDao(): ContractTemplatePartyDao
    abstract fun contractTemplateSectionDao(): ContractTemplateSectionDao
    abstract fun contractTemplateNoteDao(): ContractTemplateNoteDao
    abstract fun contractTemplateFullDao(): ContractTemplateFullDao
    abstract fun contractInstanceDao(): ContractInstanceDao
    abstract fun contractInstancePartyDao(): ContractInstancePartyDao
    abstract fun contractInstanceSectionDao(): ContractInstanceSectionDao
    abstract fun contractInstanceNoteDao(): ContractInstanceNoteDao
    abstract fun contractInstanceFullDao(): ContractInstanceFullDao
    abstract fun premiumEntitlementDao(): PremiumEntitlementDao
    abstract fun orderCostDao(): OrderCostDao
    abstract fun orderPhotoRefDao(): OrderPhotoRefDao
    abstract fun orderPhotoMetaDao(): OrderPhotoMetaDao
    abstract fun userMarketPricesDao(): UserMarketPricesDao
    abstract fun userWorkflowStepDao(): UserWorkflowStepDao
    abstract fun orderWorkflowStepDao(): OrderWorkflowStepDao
    abstract fun invoiceTemplateDao(): InvoiceTemplateDao
    abstract fun invoiceDocumentDao(): InvoiceDocumentDao
    abstract fun orderAnswerSelectedPhotoDao(): OrderAnswerSelectedPhotoDao

    abstract fun productDao(): ProductDao
    abstract fun attributeDao(): AttributeDao
    abstract fun pricingDao(): PricingDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun productImageDao(): ProductImageDao

}