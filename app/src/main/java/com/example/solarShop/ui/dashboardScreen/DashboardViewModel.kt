package com.example.solarShop.ui.dashboardScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.LengthUnit
import com.example.solarShop.OrderTimeline
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.dataStore.DisplayPreferencesDataSource
import com.example.solarShop.data.dataStore.DollarRatePreferencesDataSource
import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.entitlement.EntitlementCenter
import com.example.solarShop.data.entitlement.EntitlementState
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.sales.ProductSaleTransactionEntity
import com.example.solarShop.data.network.ServerConnectionState
import com.example.solarShop.data.network.ServerState
import com.example.solarShop.data.remote.currency.CurrencyRemoteDataSource
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.sales.ProductSaleTransactionRepository
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.client.ClientWithOrders
import com.example.solarShop.data.room.tables.contract.ContractTemplateFull
import com.example.solarShop.data.room.tables.contract.ContractTemplateFullDao
import com.example.solarShop.data.room.tables.orderAll.OrderAllRepository
import com.example.solarShop.data.room.tables.orderAll.OrderSummary
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemRepository
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import com.example.solarShop.data.sync.SyncManager
import com.example.solarShop.domain.product.ProductPriceCalculator
import com.example.solarShop.repo.AuthRepository
import com.example.solarShop.utils.createTimelineItemEntityForOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val clientRepo: ClientRepository,
    private val orderRepo: OrderRepository,
    private val timelineItemRepo: TimelineItemRepository,
    session: SessionDataStore,
    private val displayPrefs: DisplayPreferencesDataSource,
    templateFullDao: ContractTemplateFullDao,
    private val authRepository: AuthRepository,
    entitlementCenter: EntitlementCenter,
    private val orderAllRepository: OrderAllRepository,
    private val pricingRepository: PricingRepository,
    private val dollarRatePrefs: DollarRatePreferencesDataSource,
    private val currencyRemoteDataSource: CurrencyRemoteDataSource,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val productSaleTransactionRepository: ProductSaleTransactionRepository,
    private val syncManager: SyncManager,
    private val serverConnectionState: ServerConnectionState,

    ) : ViewModel() {

//region State

    // جریان‌های دیتابیس
    private val orderEntityListFlow = orderRepo.observeAllOrders()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val orderSummariesFlow: Flow<List<OrderSummary>> =
        orderEntityListFlow.flatMapLatest { orders ->
            if (orders.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    orders.map { order ->
                        // برای هر سفارش، OrderSummary بگیر
                        orderAllRepository.observeOrderSummary(order.id!!)
                    }
                ) { arr ->
                    arr.toList()
                }
            }
        }.flowOn(Dispatchers.IO)


    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserFlow: Flow<UserEntity?> =
        session.currentUserIdFlow
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(null)
                else userRepo.observeUserById(id)
            }
            .flowOn(Dispatchers.IO)

    val displayPrefsState: StateFlow<DisplayPreferences> =
        displayPrefs.prefsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DisplayPreferences()
        )


    @OptIn(ExperimentalCoroutinesApi::class)
    val currentClientWithOrdersFlow: Flow<List<ClientWithOrders>?> =
        currentUserFlow
            .map { it?.userKey }
            .distinctUntilChanged()
            .flatMapLatest { userKey ->
                if (userKey == null) flowOf(null) else clientRepo.observeClientsWithOrdersByUserId(
                    userKey
                )
            }
            .flowOn(Dispatchers.IO)           // اگر ریپو روی IO نیست


    // در ViewModel جایی که templateFlow را می‌سازی:
    private val templatesFlow: Flow<List<ContractTemplateFull>> =
        templateFullDao.observeAllFull().flowOn(Dispatchers.IO)

    private val templateFlow: Flow<ContractTemplateFull?> =
        templatesFlow
            .map { list ->
                if (list.isEmpty()) return@map null

                // 1) بیشترین تعداد ماده‌ها (بر اساس sectionsWithNotes)
                val maxCount = list.maxOf { it.sectionsWithNotes.size }

                // 2) کاندیداها: همهٔ تمپلیت‌هایی که همین تعداد ماده را دارند
                val candidates = list.filter { it.sectionsWithNotes.size == maxCount }

                // 3) اگر بین کاندیداها، موردی با «پیشرفته» در عنوان باشد، همان را برگردان
                candidates.find { it.template.title.contains("پیشرفته") }
                    ?: candidates.first() // در غیر این صورت، اولین کاندیدا
            }
            .distinctUntilChanged()

    private val latestDollarRateFlow: Flow<Long?> =
        pricingRepository
            .observeCurrencyRateHistory("USD")
            .map { list ->
                list.firstOrNull()?.rateToman
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    private val manualDollarRateFlow =
        dollarRatePrefs.manualDollarRateFlow

    private val dollarRateUiState =
        MutableStateFlow(
            DollarRateUiState()
        )

    @Suppress("UNCHECKED_CAST")
    private val dashboardIndicatorsFlow: Flow<DashboardIndicators> =
        combine(
            productRepository.observeActiveProducts(),
            inventoryRepository.observeAllTransactions(),
            pricingRepository.observeAllPurchasePrices(),
            pricingRepository.observeAllSalePrices(),
            productSaleTransactionRepository.observeAllSaleTransactions(),
            latestDollarRateFlow,
            manualDollarRateFlow,
        ) { arr: Array<Any?> ->

            val products = arr[0] as List<ProductEntity>
            val transactions = arr[1] as List<InventoryTransactionEntity>
            val purchasePrices = arr[2] as List<ProductPurchasePriceEntity>
            val salePrices = arr[3] as List<ProductSalePriceEntity>
            val saleTransactions = arr[4] as List<ProductSaleTransactionEntity>
            val latestDollarRateToman = arr[5] as Long?
            val manualDollarRateToman = arr[6] as Long?

            val todayRate =
                manualDollarRateToman ?: latestDollarRateToman

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val monthEnd = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, 1)
            }.timeInMillis

            val salesInMonth = saleTransactions.filter { sale ->
                sale.soldAt >= monthStart && sale.soldAt < monthEnd
            }

            val monthSales = salesInMonth.sumOf { sale ->
                sale.totalSalePriceToman
            }

            val monthProfit = salesInMonth.sumOf { sale ->
                sale.totalProfitToman ?: 0L
            }

            val isMonthProfitComplete = salesInMonth.none { sale ->
                sale.totalProfitToman == null
            }

            var totalValueToman = 0L
            var totalValueDollar = 0.0

            products.forEach { product ->

                val productId = product.id

                val stock = transactions
                    .filter { it.productId == productId }
                    .sumOf { tx ->
                        when (tx.transactionType) {
                            InventoryTransactionType.PURCHASE,
                            InventoryTransactionType.SALE_RETURN -> tx.quantity

                            InventoryTransactionType.SALE,
                            InventoryTransactionType.PURCHASE_RETURN -> -tx.quantity

                            InventoryTransactionType.ADJUSTMENT -> tx.quantity
                        }
                    }

                val activePurchase = purchasePrices
                    .filter { it.productId == productId && it.isActive }
                    .maxByOrNull { it.createdAt }

                val activeConsumerSale = salePrices
                    .filter {
                        it.productId == productId &&
                                it.priceType == "consumer" &&
                                it.isActive
                    }
                    .maxByOrNull { it.createdAt }

                val priceResult = ProductPriceCalculator.calculate(
                    buyPriceDollar = activeConsumerSale?.baseDollarPrice
                        ?: activePurchase?.buyPriceDollar,
                    buyPriceToman = activePurchase?.buyPriceToman
                        ?: activeConsumerSale?.basePurchasePriceToman,
                    purchaseDollarRateToman = activePurchase?.dollarRateToman
                        ?: activeConsumerSale?.dollarRateToman,
                    todayDollarRateToman = todayRate,
                    profitPercent = activeConsumerSale?.profitPercent ?: 0.0,
                    fixedProfitToman = 0L
                )

                val unitBuyPriceToman =
                    priceResult?.baseBuyPriceToman
                        ?: activePurchase?.buyPriceToman
                        ?: 0L

                val unitBuyPriceDollar =
                    priceResult?.baseBuyPriceDollar
                        ?: activePurchase?.buyPriceDollar
                        ?: 0.0

                totalValueToman +=
                    (stock * unitBuyPriceToman).roundToLong()

                totalValueDollar +=
                    stock * unitBuyPriceDollar
            }

            DashboardIndicators(
                totalInventoryValueToman = totalValueToman,
                totalInventoryValueDollar = totalValueDollar,
                currentMonthSalesToman = monthSales,
                currentMonthProfitToman = monthProfit,
                isCurrentMonthProfitComplete = isMonthProfitComplete
            )
        }.flowOn(Dispatchers.IO)

    // UiState نهایی
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ProfileUiState> =
        combine(
            currentUserFlow,
            currentClientWithOrdersFlow,
            templateFlow,
            orderEntityListFlow,
            orderSummariesFlow,
            latestDollarRateFlow,
            manualDollarRateFlow,
            dollarRateUiState,
            dashboardIndicatorsFlow,
            serverConnectionState.state,
        ) { arr: Array<Any?> ->


            val currentUser = arr[0] as UserEntity?
            val currentClientWithOrders = arr[1] as List<ClientWithOrders>?
            val template = arr[2] as ContractTemplateFull?
            val orderEntityList = arr[3] as List<OrderEntity>?
            val orderSummaries = arr[4] as List<OrderSummary>?
            val latestDollarRateToman = arr[5] as Long?
            val manualDollarRateToman = arr[6] as Long?
            val dollarRateUi = arr[7] as DollarRateUiState
            val indicators = arr[8] as DashboardIndicators
            val serverState = arr[9] as ServerState


            val effectiveDollarRateToman =
                manualDollarRateToman ?: latestDollarRateToman

            ProfileUiState(
                currentUserEntity = currentUser,
                currentClientWithOrders = currentClientWithOrders,
                template = template,
                orderEntityList = orderEntityList,
                orderSummaries = orderSummaries,
                isDataLoaded = true,
                latestDollarRateToman = latestDollarRateToman,
                apiDollarRateToman = latestDollarRateToman,
                manualDollarRateToman = manualDollarRateToman,
                effectiveDollarRateToman = effectiveDollarRateToman,
                isFetchingDollarRate = dollarRateUi.isFetchingDollarRate,
                dollarRateMessage = dollarRateUi.message,
                totalInventoryValueToman = indicators.totalInventoryValueToman,
                totalInventoryValueDollar = indicators.totalInventoryValueDollar,
                currentMonthSalesToman = indicators.currentMonthSalesToman,
                currentMonthProfitToman = indicators.currentMonthProfitToman,
                isCurrentMonthProfitComplete = indicators.isCurrentMonthProfitComplete,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )






//endregion State

//region funs

    fun onClickConfirmInAddClient(clientEntity: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            clientRepo.insertClient(clientEntity)
        }
    }

    fun onClickConfirmInEditClient(clientEntity: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            clientRepo.updateClient(clientEntity)
        }
    }

    fun onClickDeleteClient(clientEntity: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            clientRepo.deleteClient(clientEntity)
        }
    }

    fun onClickAddOrder(
        clientId: Int,
        onOrderCreated: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) {
                val newId = orderRepo.insertOrder(
                    OrderEntity(clientId = clientId)
                ).toInt()

                val timelineItem = createTimelineItemEntityForOrder(
                    orderId = newId,
                    orderTimeline = OrderTimeline.CREATE_ORDER
                )
                timelineItemRepo.insertTimelineItem(timelineItem)

                val name = "سفارش $newId"
                orderRepo.updateOrderName(newId, name)

                newId
            }

            // روی Main هستیم → امن برای صدا زدن ناوبری
            onOrderCreated(newId)
        }
    }

    fun onClickConfirmInEditProfile(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepo.updateUser(user)
        }
    }

    /** جریان وضعیت پریمیوم سراسری */
    val entitlement: Flow<EntitlementState> = entitlementCenter.state

    /** سنک پروفایل از سرور → Room */
    fun refresh() = viewModelScope.launch {
        userRepo.syncMe().onFailure { /* TODO: Snackbar/Log */ }
        // اختیاراً می‌توانی در همین‌جا entitlements را هم سنک کنی:
        // entitlementCenter.refresh()
    }

    fun signOut(onDone: () -> Unit) = viewModelScope.launch {
        authRepository.signOut(clearLocal = false)
        onDone()
    }

    fun onChangeLength(unit: LengthUnit) {
        viewModelScope.launch { displayPrefs.setLengthUnit(unit) }
    }

    fun onChangeCurrency(unit: CurrencyUnit) {
        viewModelScope.launch { displayPrefs.setCurrencyUnit(unit) }
    }

    fun onClickDeleteOrder(orderSummary: OrderSummary) {
        viewModelScope.launch {

            val orderId = orderSummary.order.id
            if (orderId != null) {
                orderRepo.deleteOrderWithChildren(orderId)
            }


        }
    }


    fun onManualDollarRateChange(value: Long?) {
        viewModelScope.launch {
            dollarRatePrefs.setManualDollarRate(value)
        }
    }

    fun fetchDollarRateFromApi() {
        viewModelScope.launch {
            dollarRateUiState.update {
                it.copy(
                    isFetchingDollarRate = true,
                    message = null
                )
            }

            try {
                val rate = withContext(Dispatchers.IO) {
                    currencyRemoteDataSource.fetchUsdRateToman()
                }

                if (rate == null) {
                    dollarRateUiState.update {
                        it.copy(
                            isFetchingDollarRate = false,
                            message = "نرخ دلار از سرور دریافت نشد."
                        )
                    }
                    return@launch
                }

                syncManager.pullCurrencyRates()

                dollarRateUiState.update {
                    it.copy(
                        isFetchingDollarRate = false,
                        message = "نرخ دلار توسط سرور دریافت و ذخیره شد."
                    )
                }

            } catch (e: Exception) {
                dollarRateUiState.update {
                    it.copy(
                        isFetchingDollarRate = false,
                        message = "خطا در دریافت نرخ دلار از سرور: ${e.message.orEmpty()}"
                    )
                }

                Log.e("DashboardViewModel", "Error fetching dollar rate from server: ${e.message}", e)
            }
        }
    }

    fun saveManualDollarRate() {
        viewModelScope.launch {
            val rate =
                uiState.value.manualDollarRateToman

            if (rate == null || rate <= 0L) {
                dollarRateUiState.update {
                    it.copy(
                        message = "لطفاً نرخ دلار دستی را وارد کن."
                    )
                }
                return@launch
            }

            pricingRepository.insertCurrencyRate(
                CurrencyRateEntity(
                    currencyCode = "USD",
                    rateToman = rate,
                    source = "manual",
                    note = "ثبت دستی از داشبورد",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            )
            dollarRatePrefs.setManualDollarRate(null)
            dollarRateUiState.update {
                it.copy(
                    message = "نرخ دلار دستی ثبت شد."
                )
            }
        }
    }

    fun onDashboardEntered() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepo.syncMe()
                .onFailure { error ->
                    Log.e(
                        "DashboardViewModel",
                        "syncMe failed on dashboard entered",
                        error
                    )
                }

            syncManager.autoSyncInBackground(
                reason = "dashboard_entered"
            )
        }
    }



//endregion funs


//region dataClasses

    data class ProfileUiState(
        val currentUserEntity: UserEntity? = null,
        val currentClientWithOrders: List<ClientWithOrders>? = null,
        val currentUserClientEntityList: List<ClientEntity>? = listOf(),
        val orderEntityList: List<OrderEntity>? = listOf(),
        val template: ContractTemplateFull? = null,
        val orderSummaries: List<OrderSummary>? = null,
        val latestDollarRateToman: Long? = null,

        val apiDollarRateToman: Long? = null,
        val manualDollarRateToman: Long? = null,
        val effectiveDollarRateToman: Long? = null,
        val isFetchingDollarRate: Boolean = false,
        val dollarRateMessage: String? = null,

        val serverState: ServerState = ServerState.Connected,

        val totalInventoryValueToman: Long = 0L,
        val totalInventoryValueDollar: Double = 0.0,
        val currentMonthSalesToman: Long = 0L,
        val currentMonthProfitToman: Long = 0L,
        val isCurrentMonthProfitComplete: Boolean = true,

        val isDataLoaded: Boolean = false
    )

    data class DollarRateUiState(
        val isFetchingDollarRate: Boolean = false,
        val message: String? = null
    )

    data class DashboardIndicators(
        val totalInventoryValueToman: Long = 0L,
        val totalInventoryValueDollar: Double = 0.0,
        val currentMonthSalesToman: Long = 0L,
        val currentMonthProfitToman: Long = 0L,
        val isCurrentMonthProfitComplete: Boolean = true,
    )
//endregion dataClasses

//region sealedClasses

//endregion sealedClasses

}

