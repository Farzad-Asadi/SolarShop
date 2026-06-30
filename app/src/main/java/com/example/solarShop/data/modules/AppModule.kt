package com.example.solarShop.data.modules


import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.solarShop.SESSION_PREFS
import com.example.solarShop.data.backup.core.BackupModuleProvider
import com.example.solarShop.data.backup.product.ProductBackupProvider
import com.example.solarShop.data.backupRestore.AttachmentController
import com.example.solarShop.data.backupRestore.v2.BackupCategory
import com.example.solarShop.data.backupRestore.v2.BackupContext
import com.example.solarShop.data.backupRestore.v2.BackupProvider
import com.example.solarShop.data.backupRestore.v2.ContractsBackupProvider
import com.example.solarShop.data.backupRestore.v2.QnaBackupProvider
import com.example.solarShop.data.backupRestore.v2.RestoreContext
import com.example.solarShop.data.dataStore.DisplayPreferencesDataSource
import com.example.solarShop.data.dataStore.OrderCheckpointStore
import com.example.solarShop.data.dataStore.QuestionTreePrefsDataStore
import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.local.dao.attribute.AttributeDao
import com.example.solarShop.data.local.dao.inventory.InventoryDao
import com.example.solarShop.data.local.dao.pricing.PricingDao
import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.dao.product.ProductImageDao
import com.example.solarShop.data.local.dao.sales.ProductSaleTransactionDao
import com.example.solarShop.data.local.dao.sync.SyncMetadataDao
import com.example.solarShop.data.local.database.AppDatabase
import com.example.solarShop.data.network.remote.FileApi
import com.example.solarShop.data.remote.api.SyncApi
import com.example.solarShop.data.remote.api.SyncApiImpl
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.attribute.AttributeRepositoryImpl
import com.example.solarShop.data.repository.file.FileSyncRepository
import com.example.solarShop.data.repository.file.FileSyncRepositoryImpl
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.inventory.InventoryRepositoryImpl
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.pricing.PricingRepositoryImpl
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.product.ProductRepositoryImpl
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepositoryImpl
import com.example.solarShop.data.repository.sales.ProductSaleTransactionRepository
import com.example.solarShop.data.repository.sales.ProductSaleTransactionRepositoryImpl
import com.example.solarShop.data.repository.sync.SyncRepository
import com.example.solarShop.data.repository.sync.SyncRepositoryImpl
import com.example.solarShop.data.room.tables.appInfo.AppInfoDao
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.appInfo.OfflineAppInfoRepository
import com.example.solarShop.data.room.tables.client.ClientDao
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.client.OfflineClientRepository
import com.example.solarShop.data.room.tables.contract.ContractInstanceDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceFullDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceNoteDao
import com.example.solarShop.data.room.tables.contract.ContractInstancePartyDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceSectionDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateFullDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteDao
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateRepository
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionDao
import com.example.solarShop.data.room.tables.contract.OfflineContractTemplateRepository
import com.example.solarShop.data.room.tables.market_prices.ClosetMarketDefaultsDao
import com.example.solarShop.data.room.tables.market_prices.MarketPricesDao
import com.example.solarShop.data.room.tables.orderAll.OrderAllRepository
import com.example.solarShop.data.room.tables.orderAll.order.OfflineOrderRepository
import com.example.solarShop.data.room.tables.orderAll.order.OrderDao
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostDao
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostRepository
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostRepositoryImpl
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoMetaDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoMetaRepository
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRepository
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.OfflineTimelineItemRepository
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemDao
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemRepository
import com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep.OrderWorkflowStepDao
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateDao
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateRepository
import com.example.solarShop.data.room.tables.question_answers.CatalogRepository
import com.example.solarShop.data.room.tables.question_answers.CatalogRepositoryImpl
import com.example.solarShop.data.room.tables.question_answers.QuestionAnswersRepository
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerDao
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerRepository
import com.example.solarShop.data.room.tables.question_answers.answer.OfflineAnswerRepository
import com.example.solarShop.data.room.tables.question_answers.question.OfflineQuestionRepository
import com.example.solarShop.data.room.tables.question_answers.question.QuestionDao
import com.example.solarShop.data.room.tables.question_answers.question.QuestionRepository
import com.example.solarShop.data.room.tables.selectedChoice.OfflineSelectedChoiceRepository
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceDao
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceRepository
import com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto.OrderAnswerSelectedPhotoDao
import com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto.OrderAnswerSelectedPhotoRepImp
import com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto.OrderAnswerSelectedPhotoRepository
import com.example.solarShop.data.room.tables.user.OfflineUserRepository
import com.example.solarShop.data.room.tables.user.UserDao
import com.example.solarShop.data.room.tables.user.UserRepository
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.UserWorkflowStepDao
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.WorkflowRepository
import com.example.solarShop.repo.AuthRepository
import com.example.solarShop.repo.AuthRepositoryImpl
import com.example.solarShop.repo.EntitlementRepository
import com.example.solarShop.repo.EntitlementRepositoryImpl
import com.example.solarShop.repo.ImageRepository
import com.google.gson.Gson
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Named
import javax.inject.Singleton


//کلیدِ نقشهٔ مولتی‌بایندینگ
@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BackupCategoryKey(val value: BackupCategory)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --------- مرحله ۲: SharedPreferences + FirstRunManager ---------

    @Provides @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)


    // --------- Database ---------

    @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bambo_db")
//            .createFromAsset("seed/bambo_seed.db")   // ← فقط نصبِ اول            2.1  3.1
            .fallbackToDestructiveMigration(true)     // فقط DEV
            .build()


    // --------- AppScope ---------

    @Provides @Singleton
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)



    // --------- DataStore ---------

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(
                // اگر قبلاً SharedPreferences داشتی، نامش را اینجا بده
                SharedPreferencesMigration(context, "legacy_session_prefs")
            ),
            produceFile = { context.preferencesDataStoreFile(SESSION_PREFS) }
        )

    @Provides @Singleton
    fun provideSessionDataStore(ds: DataStore<Preferences>) = SessionDataStore(ds)

    @Provides @Singleton
    fun provideDisplayPrefsDataStore(@ApplicationContext app: Context) = DisplayPreferencesDataSource(app)

    @Provides @Singleton
    fun provideOrderCheckpointStore(dataStore: DataStore<Preferences>) = OrderCheckpointStore(dataStore)




    @Provides
    @Singleton
    fun provideAppLanguageDataStore(
        dataStore: DataStore<Preferences>
    ): com.example.solarShop.data.dataStore.AppLanguageDataStore {
        return com.example.solarShop.data.dataStore.AppLanguageDataStore(dataStore)
    }

    @Provides
    @Singleton
    fun provideQuestionTreePrefsDataStore(
        @ApplicationContext context: Context
    ): QuestionTreePrefsDataStore = QuestionTreePrefsDataStore(context)


    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()



    @Provides @Singleton @Named("appJson")
    fun provideAppJson(): Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Provides
    fun provideBackupContext(
        @ApplicationContext app: Context,
        db: AppDatabase,
        @Named("appJson") json: Json,            // ⬅️ اینجا
        @Named("currentUserKey") userKey: String
    ): BackupContext = BackupContext(app, db, json, userKey)

    @Provides
    fun provideRestoreContext(
        @ApplicationContext app: Context,
        db: AppDatabase,
        @Named("appJson") json: Json,            // ⬅️ اینجا
        @Named("currentUserKey") userKey: String
    ): RestoreContext = RestoreContext(app, db, json, userKey)


    // منبع userKey (ساده و مستقیم از SharedPreferences؛ اگر ریپو داری، همان را تزریق کن)
    @Provides
    @Singleton
    @Named("currentUserKey")
    fun provideCurrentUserKey(prefs: SharedPreferences): String {
        // اگر userKey قبلاً ذخیره نشده، یکی می‌سازیم و همان‌جا ذخیره می‌کنیم
        val existing = prefs.getString("userKey", null)
        if (!existing.isNullOrBlank()) return existing

        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString("userKey", fresh).apply()
        return fresh
    }





    // --------- Daos ---------

    @Provides fun provideAnswerDao(db: AppDatabase): AnswerDao = db.answerDao()
    @Provides fun provideAppInfoDao(db: AppDatabase): AppInfoDao = db.appInfoDao()
    @Provides fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()
    @Provides fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()
    @Provides fun providePriceEstimateDao(db: AppDatabase): PriceEstimateDao = db.priceEstimateDao()
    @Provides fun provideSelectedChoiceDao(db: AppDatabase): SelectedChoiceDao = db.selectedChoiceDao()
    @Provides fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideTimelineItemDao(db: AppDatabase): TimelineItemDao = db.timelineItemDao()
    @Provides fun provideMarketPricesDao(db: AppDatabase): MarketPricesDao = db.marketPricesDao()
    @Provides fun provideClosetMarketDefaultsDao(db: AppDatabase): ClosetMarketDefaultsDao = db.closetMarketDefaultsDao()
    @Provides fun provideContractTemplateDao(db: AppDatabase): ContractTemplateDao = db.contractTemplateDao()
    @Provides fun provideContractTemplatePartyDao(db: AppDatabase): ContractTemplatePartyDao = db.contractTemplatePartyDao()
    @Provides fun provideContractTemplateSectionDao(db: AppDatabase): ContractTemplateSectionDao = db.contractTemplateSectionDao()
    @Provides fun provideContractTemplateNoteDao(db: AppDatabase): ContractTemplateNoteDao = db.contractTemplateNoteDao()
    @Provides fun provideContractTemplateFullDao(db: AppDatabase): ContractTemplateFullDao = db.contractTemplateFullDao()
    @Provides fun provideContractInstanceDao(db: AppDatabase): ContractInstanceDao = db.contractInstanceDao()
    @Provides fun provideContractInstancePartyDao(db: AppDatabase): ContractInstancePartyDao = db.contractInstancePartyDao()
    @Provides fun provideContractInstanceSectionDao(db: AppDatabase): ContractInstanceSectionDao = db.contractInstanceSectionDao()
    @Provides fun provideContractInstanceNoteDao(db: AppDatabase): ContractInstanceNoteDao = db.contractInstanceNoteDao()
    @Provides fun provideContractInstanceFullDao(db: AppDatabase): ContractInstanceFullDao = db.contractInstanceFullDao()
    @Provides fun providePremiumEntitlementDao(db: AppDatabase) = db.premiumEntitlementDao()
    @Provides fun provideOrderCostDao(db: AppDatabase) = db.orderCostDao()
    @Provides fun provideOrderPhotoDao(db: AppDatabase) = db.orderPhotoRefDao()
    @Provides fun provideOrderPhotoMetaDao(db: AppDatabase) = db.orderPhotoMetaDao()
    @Provides fun provideUserMarketPricesDao(db: AppDatabase) = db.userMarketPricesDao()
    @Provides fun provideUserWorkflowStepDao(db: AppDatabase) = db.userWorkflowStepDao()
    @Provides fun provideOrderWorkflowStepDao(db: AppDatabase) = db.orderWorkflowStepDao()
    @Provides fun provideInvoiceTemplateDao(db: AppDatabase) = db.invoiceTemplateDao()
    @Provides fun provideInvoiceDocumentDao(db: AppDatabase) = db.invoiceDocumentDao()
    @Provides fun provideOrderAnswerSelectedPhotoDao(db: AppDatabase) = db.orderAnswerSelectedPhotoDao()

    @Provides fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
    @Provides fun providePricingDao(db: AppDatabase): PricingDao = db.pricingDao()
    @Provides fun provideInventoryDao(db: AppDatabase): InventoryDao = db.inventoryDao()
    @Provides fun provideAttributeDao(db: AppDatabase): AttributeDao = db.attributeDao()
    @Provides fun provideProductImageDao(db: AppDatabase): ProductImageDao = db.productImageDao()
    @Provides fun provideSyncMetadataDao (db: AppDatabase): SyncMetadataDao = db.syncMetadataDao ()
    @Provides
    fun provideProductSaleTransactionDao(
        db: AppDatabase
    ): ProductSaleTransactionDao = db.productSaleTransactionDao()




    // --------- Repositories ---------

    @Provides fun provideAnswerRepository(dao: AnswerDao,db: AppDatabase): AnswerRepository =
        OfflineAnswerRepository(dao,db)
    @Provides fun provideAppInfoRepository(dao: AppInfoDao): AppInfoRepository =
        OfflineAppInfoRepository(dao)
    @Provides fun provideClientRepository(dao: ClientDao): ClientRepository =
        OfflineClientRepository(dao)
    @Provides fun provideOrderRepository(
        db: AppDatabase,
        orderDao: OrderDao,
        orderCostDao: OrderCostDao,
        attachmentController: AttachmentController,
        orderPhotoDao: OrderPhotoRefDao,
        orderPhotoMetaRepo: OrderPhotoMetaRepository
    ): OrderRepository =
        OfflineOrderRepository(
            db,
            orderDao,
            orderCostDao,
            attachmentController,
            orderPhotoDao,
            orderPhotoMetaRepo
        )
    @Provides @Singleton
    fun providePriceEstimateRepository(
        marketDao: MarketPricesDao,
        closetMarketDefaultsDao: ClosetMarketDefaultsDao,
        estimateDao: PriceEstimateDao,
        @Named("networkJson")json: kotlinx.serialization.json.Json): PriceEstimateRepository =
        PriceEstimateRepository(marketDao,closetMarketDefaultsDao, estimateDao ,json)
    @Provides fun provideSelectedChoiceRepository(selectedDao: SelectedChoiceDao,answerDao : AnswerDao): SelectedChoiceRepository =
        OfflineSelectedChoiceRepository(selectedDao,answerDao)
    @Provides fun provideQuestionRepository(dao: QuestionDao): QuestionRepository =
        OfflineQuestionRepository(dao)

    @Provides fun provideOfflineTimelineItemRepository(dao: TimelineItemDao): TimelineItemRepository =
        OfflineTimelineItemRepository(dao)
    @Provides fun provideQuestionAnswersRepository(questionDao: QuestionDao,answerDao: AnswerDao,questionRepo: QuestionRepository,selectedChoiceDao: SelectedChoiceDao): QuestionAnswersRepository =
        QuestionAnswersRepository(questionDao,answerDao,questionRepo,selectedChoiceDao)
    @Provides @Singleton fun provideCatalogRepository(@ApplicationContext  context : Context ,answerDao: AnswerDao,selectedChoiceDao: SelectedChoiceDao): CatalogRepository =
        CatalogRepositoryImpl(context,answerDao,selectedChoiceDao)
    @Provides
    @Singleton
    fun provideImageRepository(
        @ApplicationContext context: Context
    ): ImageRepository = ImageRepository(context)

    @Provides
    @Singleton
    fun provideOrderPhotoMetaRepository(
        dao: OrderPhotoRefDao,
        metaDao: OrderPhotoMetaDao
    ): OrderPhotoMetaRepository {
        return OrderPhotoMetaRepository(dao, metaDao)
    }

    @Provides
    @Singleton
    fun provideOrderPhotoRepository(
        @ApplicationContext context: Context,
        dao: OrderPhotoRefDao
    ): OrderPhotoRepository = OrderPhotoRepository(dao)

    @Provides fun provideContractTemplateRepository(
        db: AppDatabase,
        templateDao: ContractTemplateDao,
        sectionDao: ContractTemplateSectionDao,
        noteDao: ContractTemplateNoteDao,
        templateFullDao: ContractTemplateFullDao,
        instanceDao: ContractInstanceDao,
        instanceSectionDao: ContractInstanceSectionDao,
        instanceNoteDao: ContractInstanceNoteDao,
    ): ContractTemplateRepository =
        OfflineContractTemplateRepository(db,templateDao,sectionDao,noteDao,templateFullDao,instanceDao,instanceSectionDao,instanceNoteDao)
    @Provides fun provideOrderCostRepositoryImpl(dao: OrderCostDao,orderDao: OrderDao): OrderCostRepository =
        OrderCostRepositoryImpl(dao,orderDao)



    @Provides
    @Singleton
    fun provideWorkflowRepository(
        userWorkflowStepDao: UserWorkflowStepDao,
        orderWorkflowStepDao: OrderWorkflowStepDao
    ): WorkflowRepository = WorkflowRepository(userWorkflowStepDao ,orderWorkflowStepDao )


    @Provides
    @Singleton
    fun provideOrderAllRepository(
        orderDao: OrderDao,
        workflowRepository: WorkflowRepository,
        priceEstimateRepository: PriceEstimateRepository,
        selectedChoiceRepo: SelectedChoiceRepository,
        contractInstanceDao: ContractInstanceDao,
        orderCostRepo: OrderCostRepository,
        orderPhotoRefDao: OrderPhotoRefDao,
        invoiceDao: InvoiceDocumentDao,
    ): OrderAllRepository = OrderAllRepository(
        orderDao ,
        workflowRepository ,
        priceEstimateRepository,
        selectedChoiceRepo,
        contractInstanceDao,
        orderCostRepo,
        orderPhotoRefDao,
        invoiceDao
    )

    @Provides
    @Singleton
    fun provideOrderAnswerSelectedPhotoRepository(
        answerSelectedPhotoDao: OrderAnswerSelectedPhotoDao
    ): OrderAnswerSelectedPhotoRepository = OrderAnswerSelectedPhotoRepImp(answerSelectedPhotoDao )


    @Provides
    @Singleton
    fun provideProductRepository(
        productDao: ProductDao
    ): ProductRepository = ProductRepositoryImpl(productDao )

    @Provides
    @Singleton
    fun providePricingRepository(
        pricingDao: PricingDao,
        productDao: ProductDao
    ): PricingRepository = PricingRepositoryImpl(pricingDao,productDao )

    @Provides
    @Singleton
    fun provideInventoryRepository(
        inventoryDao: InventoryDao
    ): InventoryRepository = InventoryRepositoryImpl(inventoryDao)

    @Provides
    @Singleton
    fun provideAttributeRepository(
        attributeDao: AttributeDao
    ): AttributeRepository = AttributeRepositoryImpl(attributeDao)


    @Provides
    @Singleton
    fun provideProductImageRepository(
        productImageDao: ProductImageDao,
        imageRepository: ImageRepository
    ): ProductImageRepository = ProductImageRepositoryImpl(productImageDao , imageRepository)


    @Provides
    @Singleton
    fun provideSyncRepository(
        syncMetadataDao: SyncMetadataDao
    ): SyncRepository = SyncRepositoryImpl(syncMetadataDao)



    @Provides
    @Singleton
    fun provideFileSyncRepository(
        fileApi: FileApi,
        imageRepository: ImageRepository
    ): FileSyncRepository = FileSyncRepositoryImpl(fileApi,imageRepository)

    @Provides
    @Singleton
    fun provideProductSaleTransactionRepository(
        dao: ProductSaleTransactionDao
    ): ProductSaleTransactionRepository {
        return ProductSaleTransactionRepositoryImpl(dao)
    }








    //remote


    @Provides
    @Singleton
    fun provideSyncApi(
        httpClient: HttpClient
    ): SyncApi = SyncApiImpl(httpClient)





}


//backUp restore solarnew
@Module
@InstallIn(SingletonComponent::class)
abstract class SolarBackupBindModule {

    @Binds
    @IntoSet
    abstract fun bindProductBackupProvider(
        impl: ProductBackupProvider
    ): BackupModuleProvider
}



@Module
@InstallIn(SingletonComponent::class)
abstract class BackupRestoreBindModule {

    @Binds
    @IntoMap
    @BackupCategoryKey(BackupCategory.QNA)
    abstract fun bindQnaProvider(p: QnaBackupProvider): BackupProvider

    @Binds
    @IntoMap
    @BackupCategoryKey(BackupCategory.CONTRACTS)
    abstract fun bindContractsProvider(p: ContractsBackupProvider): BackupProvider

    // وقتی آماده شد:
    // @Binds @IntoMap @BackupCategoryKey(BackupCategory.CUSTOMERS_ACTIVE)
    // abstract fun bindCustomersActive(p: CustomersActiveProvider): BackupProvider
    //
    // @Binds @IntoMap @BackupCategoryKey(BackupCategory.CUSTOMERS_ARCHIVED)
    // abstract fun bindCustomersArchived(p: CustomersArchivedProvider): BackupProvider
}


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: OfflineUserRepository): UserRepository

    @Binds @Singleton
    abstract fun bindEntitlementRepository(impl: EntitlementRepositoryImpl): EntitlementRepository
}