package com.example.solarShop.ui.orderScreen.orderInvoice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.InvoiceStatus
import com.example.solarShop.InvoiceType
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.dataStore.DisplayPreferencesDataSource
import com.example.solarShop.data.dataStore.DollarRatePreferencesDataSource
import com.example.solarShop.data.dataStore.SessionDataStore.Keys.currentUserId
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentDao
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceItemEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceTemplateDao
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceTemplateEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceWithItems
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import com.example.solarShop.domain.product.ProductPriceCalculator
import com.example.solarShop.utils.PdfInvoiceExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OrderInvoiceViewModel @Inject constructor(
    private val orderRepo: OrderRepository,
    private val clientRepo: ClientRepository,
    private val invoiceDao: InvoiceDocumentDao,
    private val templateDao: InvoiceTemplateDao,
    private val pdfExporter: PdfInvoiceExporter,
    private val dataStore: DataStore<Preferences>,
    private val userRepo: UserRepository,
    private val productRepository: ProductRepository,
    private val pricingRepository: PricingRepository,
    private val dollarRatePrefs: DollarRatePreferencesDataSource,
    @ApplicationContext private val app: Context,
    private val savedStateHandle: SavedStateHandle,
    private val displayPrefs: DisplayPreferencesDataSource,
) : ViewModel() {


    val displayPrefsState: StateFlow<DisplayPreferences> =
        displayPrefs.prefsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DisplayPreferences()
        )


    private val currentUserIdFlow: Flow<Int?> =
        dataStore.data.map { prefs ->
            prefs[currentUserId].takeIf { it != -1 }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserFlow: Flow<UserEntity?> =
        currentUserIdFlow
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(null)
                else userRepo.observeUserById(id)
            }
            .flowOn(Dispatchers.IO)

    // orderId از NavBackStackEntry می‌آد
    private val initialOrderId: Int? =
        savedStateHandle
            .get<Int>("orderId")
            ?.takeIf { it != -1 }

    private val orderIdFromNavHostFlow =
        MutableStateFlow<Int?>(null)

    fun setOrderIdFromNav(orderId: Int?) {
        if (orderId != null && orderId != -1 && orderIdFromNavHostFlow.value != orderId) {
            Log.d("OrderInvoiceVM", "setOrderIdFromNav orderId=$orderId")
            orderIdFromNavHostFlow.value = orderId
        }
    }

    private val savedOrderIdFlow: StateFlow<Int?> =
        savedStateHandle.getStateFlow("orderId", -1)
            .map { value ->
                value.takeIf { it != -1 }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                savedStateHandle.get<Int>("orderId")?.takeIf { it != -1 }
            )

    private val orderIdFlow: StateFlow<Int?> =
        combine(
            savedOrderIdFlow,
            orderIdFromNavHostFlow
        ) { savedOrderId, navHostOrderId ->
            navHostOrderId ?: savedOrderId
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            orderIdFromNavHostFlow.value
                ?: savedStateHandle.get<Int>("orderId")?.takeIf { it != -1 }
        )

    private fun currentOrderIdOrNull(): Int? {
        return orderIdFromNavHostFlow.value
            ?: savedStateHandle.get<Int>("orderId")?.takeIf { it != -1 }
            ?: orderIdFlow.value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val proformaInvoicesFlow: Flow<List<InvoiceDocumentEntity>> =
        orderIdFlow.flatMapLatest { orderId ->
            if (orderId == null) flowOf(emptyList())
            else invoiceDao.observeInvoicesForOrder(orderId, InvoiceType.PROFORMA)
        }

    // نوع سند: "PROFORMA" یا "INVOICE" (از NavArg به صورت String)
    private val typeFlow: StateFlow<InvoiceType> =
        savedStateHandle.getStateFlow("invoiceType", InvoiceType.PROFORMA.name)
            .map { name ->
                runCatching { InvoiceType.valueOf(name) }.getOrDefault(InvoiceType.PROFORMA)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, InvoiceType.PROFORMA)

    // سفارش
    @OptIn(ExperimentalCoroutinesApi::class)
    private val orderFlow: Flow<OrderEntity?> =
        orderIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else orderRepo.observeOrderById(id)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val templatesFlow: Flow<List<InvoiceTemplateEntity>> =
        typeFlow.flatMapLatest { type ->
            flow {
                ensureDefaultTemplate(type)

                val list =
                    templateDao.getTemplatesByType(type)

                emit(list)
            }.flowOn(Dispatchers.IO)
        }

    // فاکتورهای این سفارش با این نوع
    @OptIn(ExperimentalCoroutinesApi::class)
    private val invoicesFlow: Flow<List<InvoiceDocumentEntity>> =
        combine(orderIdFlow, typeFlow) { orderId, type ->
            orderId to type
        }.flatMapLatest { (orderId, type) ->
            if (orderId == null) {
                flowOf(emptyList())
            } else {
                invoiceDao.observeInvoicesForOrder(orderId, type)
            }
        }



    private val editorState = MutableStateFlow(InvoiceEditorUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val editorInvoiceFlow: Flow<InvoiceWithItems?> =
        editorState.flatMapLatest { editor ->
            val id = editor.selectedInvoiceId
            if (!editor.isVisible || id == null) {
                flowOf(null)
            } else {
                flow {
                    val data = invoiceDao.getInvoiceWithItems(id)
                    emit(data)
                }
            }
        }

    private val invoiceProductsFlow: Flow<List<InvoiceProductPickerUi>> =
        combine(
            productRepository.observeActiveProductsFullInfo(),
            pricingRepository.observeCurrencyRateHistory("USD"),
            dollarRatePrefs.manualDollarRateFlow
        ) { products, currencyRates, manualDollarRate ->

            val apiDollarRate =
                currencyRates.firstOrNull()?.rateToman

            val dailyDollarRate =
                manualDollarRate ?: apiDollarRate

            products.mapNotNull { productFullInfo ->

                val productId =
                    productFullInfo.product.id ?: return@mapNotNull null

                val activePurchasePrice =
                    pricingRepository.getActivePurchasePrice(productId)

                val consumerSale =
                    pricingRepository.getActiveSalePrice(
                        productId = productId,
                        priceType = "consumer"
                    )

                val colleagueSale =
                    pricingRepository.getActiveSalePrice(
                        productId = productId,
                        priceType = "colleague"
                    )

                val consumerPrice =
                    ProductPriceCalculator.calculate(
                        buyPriceDollar = consumerSale?.baseDollarPrice
                            ?: activePurchasePrice?.buyPriceDollar,
                        buyPriceToman = activePurchasePrice?.buyPriceToman,
                        purchaseDollarRateToman = activePurchasePrice?.dollarRateToman,
                        todayDollarRateToman = dailyDollarRate,
                        profitPercent = consumerSale?.profitPercent ?: 0.0,
                        fixedProfitToman = 0L
                    )?.finalSalePriceToman

                val colleaguePrice =
                    ProductPriceCalculator.calculate(
                        buyPriceDollar = colleagueSale?.baseDollarPrice
                            ?: activePurchasePrice?.buyPriceDollar,
                        buyPriceToman = activePurchasePrice?.buyPriceToman,
                        purchaseDollarRateToman = activePurchasePrice?.dollarRateToman,
                        todayDollarRateToman = dailyDollarRate,
                        profitPercent = colleagueSale?.profitPercent ?: 0.0,
                        fixedProfitToman = 0L
                    )?.finalSalePriceToman

                val titleParts =
                    listOfNotNull(
                        productFullInfo.product.name,
                        productFullInfo.product.model
                            .takeIf { it.isNotBlank() }
                            ?.let { "مدل $it" },
                        productFullInfo.brand?.name
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "برند $it" }
                    )

                InvoiceProductPickerUi(
                    productId = productId,
                    title = titleParts.joinToString(" - "),
                    unit = productFullInfo.unit?.name ?: "عدد",
                    consumerPriceToman = consumerPrice,
                    colleaguePriceToman = colleaguePrice
                )
            }
        }.flowOn(Dispatchers.IO)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<OrderInvoiceUiState> =
        combine(
            orderFlow,
            typeFlow,
            templatesFlow,
            invoicesFlow,
            editorState,
            editorInvoiceFlow,
            currentUserFlow,
            proformaInvoicesFlow,
            invoiceProductsFlow,
        ) { arr ->
            val order = arr[0] as OrderEntity?
            val type = arr[1] as InvoiceType
            val templates = arr[2] as List<InvoiceTemplateEntity>
            val invoices = arr[3] as List<InvoiceDocumentEntity>
            val editor = arr[4] as InvoiceEditorUiState
            val editorInvoice = arr[5] as InvoiceWithItems?
            val currentUser = arr[6] as UserEntity?
            val proformas = arr[7] as List<InvoiceDocumentEntity>
            val invoiceProducts = arr[8] as List<InvoiceProductPickerUi>

            OrderInvoiceUiState(
                isLoading = false,
                order = order,
                type = type,
                templates = templates,
                invoices = invoices,
                errorMessage = null,
                isEditorVisible = editor.isVisible,
                selectedInvoiceId = editor.selectedInvoiceId,
                editorInvoice = editorInvoice,
                currentUser = currentUser,
                proformaInvoices = proformas,
                invoiceProducts = invoiceProducts,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrderInvoiceUiState(isLoading = true)
        )




    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting // اگر لازم شد در UI نشان بدهی




    fun onCreateNewFromTemplate(templateId: Int) {
        onCreateNewDocument()
    }

    fun onOpenInvoice(invoiceId: Int) {
        editorState.value = InvoiceEditorUiState(
            isVisible = true,
            selectedInvoiceId = invoiceId
        )
    }

    fun onCloseEditor() {
        editorState.value = InvoiceEditorUiState(
            isVisible = false,
            selectedInvoiceId = null
        )
    }

    private fun generateInvoiceNumber(type: InvoiceType, timestamp: Long, orderId: Int): String {
        val prefix = when (type) {
            InvoiceType.PROFORMA -> "PR"
            InvoiceType.INVOICE -> "IN"
        }
        val shortTs = (timestamp / 1000) % 10_000
        return "$prefix-$shortTs-$orderId"
    }

    fun updateInvoiceHeader(
        invoiceId: Int,
        header: InvoiceHeaderInput
    ) {
        viewModelScope.launch {
            updateInvoiceHeaderInternal(header)
        }
    }


    fun saveItems(invoiceId: Int, items: List<InvoiceItemInput>) {
        viewModelScope.launch {
            saveItemsInternal(invoiceId, items)
        }
    }

    private suspend fun updateInvoiceHeaderInternal(
        header: InvoiceHeaderInput
    ) {
        val current =
            invoiceDao
                .getInvoiceWithItems(header.invoiceId)
                ?.invoice
                ?: return

        val now =
            System.currentTimeMillis()

        val updated =
            current.copy(
                number = header.number,
                createdAt = header.dateMillis,

                sellerLabel = header.sellerLabel,
                sellerName = header.sellerName,
                sellerPhone = header.sellerPhone,
                sellerAddress = header.sellerAddress,

                buyerLabel = header.buyerLabel,
                buyerName = header.buyerName,
                buyerPhone = header.buyerPhone,
                buyerAddress = header.buyerAddress,

                notes = header.notes,

                updatedAt = now,
                deletedAt = null,
                isSynced = false
            )

        invoiceDao.updateInvoice(updated)

        editorState.value =
            InvoiceEditorUiState(
                isVisible = false,
                selectedInvoiceId = null
            )
    }

    private suspend fun saveItemsInternal(
        invoiceId: Int,
        items: List<InvoiceItemInput>
    ) {
        val now =
            System.currentTimeMillis()

        val existingItems =
            invoiceDao.getItemsForInvoice(invoiceId)

        val existingById =
            existingItems.associateBy { item ->
                item.id
            }

        /*
         * شناسه ردیف‌هایی که هنوز در فرم وجود دارند.
         */
        val submittedIds =
            items.mapNotNull { input ->
                input.id?.takeIf { it > 0 }
            }.toSet()

        /*
         * ردیف‌هایی که قبلاً وجود داشتند ولی کاربر از فرم حذف کرده است.
         */
        val removedItemIds =
            existingItems
                .filter { existing ->
                    existing.id > 0 &&
                            existing.id !in submittedIds
                }
                .map { existing ->
                    existing.id
                }

        val activeEntities =
            items.mapIndexed { index, input ->

                val rowSubtotal =
                    (input.quantity * input.unitPrice)
                        .toLong()
                        .coerceAtLeast(0L)

                val rowDiscount =
                    input.discount.coerceAtLeast(0L)

                val taxAmount =
                    0L

                val rowTotal =
                    (
                            rowSubtotal -
                                    rowDiscount +
                                    taxAmount
                            ).coerceAtLeast(0L)

                val existing =
                    input.id
                        ?.takeIf { it > 0 }
                        ?.let { itemId ->
                            existingById[itemId]
                        }

                if (existing != null) {
                    /*
                     * UID و createdAt ردیف قبلی حفظ می‌شوند.
                     */
                    existing.copy(
                        invoiceId = invoiceId,
                        rowIndex = index,
                        description = input.description,
                        quantity = input.quantity,
                        unit = input.unit,
                        unitPrice = input.unitPrice,
                        rowDiscount = rowDiscount,
                        rowSubtotal = rowSubtotal,
                        taxPercent = null,
                        taxAmount = taxAmount,
                        rowTotal = rowTotal,

                        updatedAt = now,
                        deletedAt = null,
                        isSynced = false
                    )
                } else {
                    /*
                     * ردیف جدید UID تازه می‌گیرد.
                     */
                    InvoiceItemEntity(
                        id = 0,
                        invoiceId = invoiceId,
                        rowIndex = index,
                        description = input.description,
                        quantity = input.quantity,
                        unit = input.unit,
                        unitPrice = input.unitPrice,
                        rowDiscount = rowDiscount,
                        rowSubtotal = rowSubtotal,
                        taxPercent = null,
                        taxAmount = taxAmount,
                        rowTotal = rowTotal,

                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                        isSynced = false
                    )
                }
            }

        val subtotalBeforeDiscount =
            activeEntities.sumOf { item ->
                item.rowSubtotal
            }

        val totalDiscount =
            activeEntities.sumOf { item ->
                item.rowDiscount
            }

        val totalBeforeTax =
            (
                    subtotalBeforeDiscount -
                            totalDiscount
                    ).coerceAtLeast(0L)

        val totalTax =
            activeEntities.sumOf { item ->
                item.taxAmount
            }

        val totalFinal =
            (
                    totalBeforeTax +
                            totalTax
                    ).coerceAtLeast(0L)

        val currentInvoice =
            invoiceDao.getInvoiceById(invoiceId)
                ?: return

        val updatedInvoice =
            currentInvoice.copy(
                subtotalBeforeDiscount = subtotalBeforeDiscount,
                totalDiscount = totalDiscount,
                totalBeforeTax = totalBeforeTax,
                totalTax = totalTax,
                totalFinal = totalFinal,

                updatedAt = now,
                deletedAt = null,
                isSynced = false
            )

        invoiceDao.saveInvoiceItemsAndTotals(
            updatedInvoice = updatedInvoice,
            activeItems = activeEntities,
            removedItemIds = removedItemIds,
            now = now
        )
    }

    fun onInvoiceTypeTabSelected(type: InvoiceType) {
        // نوع سند را عوض می‌کنیم
        savedStateHandle["invoiceType"] = type.name

        // هر بار تب عوض شد، ادیتور بسته شود
        editorState.value = InvoiceEditorUiState(
            isVisible = false,
            selectedInvoiceId = null
        )
    }

    fun saveHeaderItemsAndPreview(
        header: InvoiceHeaderInput,
        items: List<InvoiceItemInput>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // ۱) هدر
            updateInvoiceHeaderInternal(header)

            // ۲) آیتم‌ها
            saveItemsInternal(header.invoiceId, items)

            // ۳) ساخت و نمایش PDF
            // از همان منطق قبلی استفاده می‌کنیم، فقط به‌صورت سلسله‌وار
            if (_isExporting.value) return@launch

            _isExporting.value = true
            try {
                // TODO: به‌زودی از تنظیمات/سوییچ UI گرفته می‌شود
                val showPdfAsToman = displayPrefsState.value.currencyUnit == CurrencyUnit.TOMAN

                val file = pdfExporter.exportInvoice(header.invoiceId, showAsToman = showPdfAsToman)
                openPdf(file)
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                _isExporting.value = false
            }
        }
    }


    fun onDeleteInvoice(
        invoiceId: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            invoiceDao.softDeleteInvoiceWithItems(
                invoiceId = invoiceId,
                deletedAt = System.currentTimeMillis()
            )

            editorState.value =
                InvoiceEditorUiState(
                    isVisible = false,
                    selectedInvoiceId = null
                )
        }
    }











    fun onPreviewInvoicePdfClicked(invoiceId: Int) {
        if (_isExporting.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val showPdfAsToman = displayPrefsState.value.currencyUnit == CurrencyUnit.TOMAN

                val file = pdfExporter.exportInvoice(invoiceId, showAsToman = showPdfAsToman)

                openPdf(file)
            } catch (t: Throwable) {
                t.printStackTrace()
                // TODO: بعداً با UiEvent Snackbar نشان بده
            } finally {
                _isExporting.value = false
            }
        }
    }


    @SuppressLint("QueryPermissionsNeeded")
    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(intent)
    }


    fun onConvertProformaToInvoice(templateId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val orderId = currentOrderIdOrNull() ?: return@launch

            // اگر قبلاً فاکتور داریم، همون رو باز کن
            val existingInvoice = invoiceDao.getInvoicesForOrder(orderId, InvoiceType.INVOICE).firstOrNull()
            if (existingInvoice != null) {
                editorState.value = InvoiceEditorUiState(isVisible = true, selectedInvoiceId = existingInvoice.id)
                return@launch
            }

            // آخرین پیش‌فاکتور این سفارش
            val sourceProforma = invoiceDao.getInvoicesForOrder(orderId, InvoiceType.PROFORMA).firstOrNull()
                ?: return@launch

            val source = invoiceDao.getInvoiceWithItems(sourceProforma.id) ?: return@launch

            val now = System.currentTimeMillis()
            val newNumber = generateInvoiceNumber(InvoiceType.INVOICE, now, orderId)

            val safeTemplate =
                templateDao.getTemplateById(templateId)
                    ?: ensureDefaultTemplate(InvoiceType.INVOICE)

            // ✅ ساخت سند جدید Invoice با کپی فیلدهای پرشده
            val newDoc =
                source.invoice.copy(
                    id = 0,

                    /*
                     * سند جدید نباید UID پیش‌فاکتور را حفظ کند.
                     */
                    uid = UUID.randomUUID().toString(),

                    type = InvoiceType.INVOICE,
                    templateId = safeTemplate.id,
                    number = newNumber,
                    status = InvoiceStatus.DRAFT,

                    updatedAt = now,

                    pdfAttachmentId = null,

                    deletedAt = null,
                    isSynced = false,

                    createdByUserId = null,
                    updatedByUserId = null
                )

            val newInvoiceId = invoiceDao.insertInvoice(newDoc).toInt()

            // ✅ کپی آیتم‌ها
            val newItems =
                source.items.mapIndexed { index, sourceItem ->

                    sourceItem.copy(
                        id = 0,

                        /*
                         * ردیف فاکتور جدید باید UID مستقل داشته باشد.
                         */
                        uid = UUID.randomUUID().toString(),

                        invoiceId = newInvoiceId,
                        rowIndex = index,

                        createdAt = now,
                        updatedAt = now,

                        deletedAt = null,
                        isSynced = false,

                        createdByUserId = null,
                        updatedByUserId = null
                    )
                }
            invoiceDao.insertItems(newItems)

            // ✅ باز کردن ادیتور فاکتور
            editorState.value = InvoiceEditorUiState(isVisible = true, selectedInvoiceId = newInvoiceId)
        }
    }

    private fun buildDefaultTemplate(type: InvoiceType): InvoiceTemplateEntity {
        return when (type) {
            InvoiceType.PROFORMA -> {
                InvoiceTemplateEntity(
                    id = 0,
                    name = "پیش‌فاکتور فروش پنل‌کده",
                    type = InvoiceType.PROFORMA,
                    title = "پیش‌فاکتور فروش",
                    hasTax = false,
                    defaultTaxPercent = null,
                    showAmountInWords = false,
                    isDefaultForType = true
                )
            }

            InvoiceType.INVOICE -> {
                InvoiceTemplateEntity(
                    id = 0,
                    name = "فاکتور فروش پنل‌کده",
                    type = InvoiceType.INVOICE,
                    title = "فاکتور فروش",
                    hasTax = false,
                    defaultTaxPercent = null,
                    showAmountInWords = true,
                    isDefaultForType = true
                )
            }
        }
    }

    private suspend fun ensureDefaultTemplate(type: InvoiceType): InvoiceTemplateEntity {
        val existingDefault =
            templateDao.getDefaultTemplateForType(type)

        if (existingDefault != null) {
            return existingDefault
        }

        val existingTemplates =
            templateDao.getTemplatesByType(type)

        if (existingTemplates.isNotEmpty()) {
            return existingTemplates.first()
        }

        val defaultTemplate =
            buildDefaultTemplate(type)

        val newId =
            templateDao.insertTemplate(defaultTemplate).toInt()

        return defaultTemplate.copy(id = newId)
    }

    fun onCreateNewDocument() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val orderId = currentOrderIdOrNull()

                Log.d("OrderInvoiceVM", "onCreateNewDocument clicked. orderId=$orderId")

                if (orderId == null) {
                    Log.e("OrderInvoiceVM", "Cannot create invoice: orderId is null")
                    return@launch
                }

                val type = typeFlow.value

                Log.d("OrderInvoiceVM", "invoice type=$type")

                val existing =
                    invoiceDao.getInvoicesForOrder(orderId, type)

                Log.d("OrderInvoiceVM", "existing invoices count=${existing.size}")

                if (existing.isNotEmpty()) {
                    val first = existing.first()

                    Log.d("OrderInvoiceVM", "Opening existing invoice id=${first.id}")

                    editorState.value = InvoiceEditorUiState(
                        isVisible = true,
                        selectedInvoiceId = first.id
                    )

                    return@launch
                }

                val order =
                    orderRepo.getOrderById(orderId)

                Log.d("OrderInvoiceVM", "order=$order")

                if (order == null) {
                    Log.e("OrderInvoiceVM", "Cannot create invoice: order not found for id=$orderId")
                    return@launch
                }

                val template =
                    ensureDefaultTemplate(type)

                Log.d("OrderInvoiceVM", "template=$template")

                val now =
                    System.currentTimeMillis()

                val number =
                    generateInvoiceNumber(type, now, orderId)

                val buyer: ClientEntity? =
                    clientRepo.getClientById(order.clientId)

                Log.d("OrderInvoiceVM", "buyer=$buyer")

                val doc =
                    InvoiceDocumentEntity(
                        id = 0,
                        orderId = orderId,
                        templateId = template.id,
                        type = type,
                        number = number,
                        createdAt = now,
                        updatedAt = now,
                        status = InvoiceStatus.DRAFT,

                        sellerLabel = "فروشگاه",
                        sellerName = "پنل‌کده",
                        sellerPhone = "09199910369",
                        sellerAddress = "سقز، خیابان ملت، مقابل برق آوازه",
                        sellerNationalId = "",
                        sellerEconomicCode = "",

                        buyerLabel = "مشتری",
                        buyerName = buyer?.name ?: "",
                        buyerPhone = buyer?.mobilePhone ?: "",
                        buyerAddress = buyer?.address ?: "",
                        buyerNationalId = buyer?.nationalCode ?: "",

                        subtotalBeforeDiscount = 0L,
                        totalDiscount = 0L,
                        totalBeforeTax = 0L,
                        totalTax = 0L,
                        totalFinal = 0L,

                        notes = if (type == InvoiceType.PROFORMA) {
                            "اعتبار قیمت‌ها وابسته به نوسان نرخ ارز و موجودی کالا است."
                        } else {
                            null
                        },

                        pdfAttachmentId = null,

                        deletedAt = null,
                        isSynced = false
                    )

                val newId =
                    invoiceDao.insertInvoice(doc).toInt()

                Log.d("OrderInvoiceVM", "invoice inserted. newId=$newId")

                editorState.value = InvoiceEditorUiState(
                    isVisible = true,
                    selectedInvoiceId = newId
                )

                Log.d("OrderInvoiceVM", "editor opened for invoiceId=$newId")
            } catch (t: Throwable) {
                Log.e("OrderInvoiceVM", "Create invoice failed", t)
            }
        }
    }


}







data class OrderInvoiceUiState(
    val isLoading: Boolean = true,

    val order: OrderEntity? = null,
    val type: InvoiceType = InvoiceType.PROFORMA,

    val templates: List<InvoiceTemplateEntity> = emptyList(),
    val invoices: List<InvoiceDocumentEntity> = emptyList(),

    val errorMessage: String? = null,

    val isEditorVisible: Boolean = false,
    val selectedInvoiceId: Int? = null,
    val editorInvoice: InvoiceWithItems? = null,

    val currentUser: UserEntity?=null,

    val proformaInvoices: List<InvoiceDocumentEntity> = emptyList(),
    val invoiceProducts: List<InvoiceProductPickerUi> = emptyList(),


    )

data class InvoiceProductPickerUi(
    val productId: Int,
    val title: String,
    val unit: String,
    val consumerPriceToman: Long?,
    val colleaguePriceToman: Long?
)

data class InvoiceEditorUiState(
    val isVisible: Boolean = false,
    val selectedInvoiceId: Int? = null
)

data class InvoiceHeaderInput(
    val invoiceId: Int,
    val type: InvoiceType,
    val number: String,
    val dateMillis: Long,

    val sellerLabel: String?,
    val sellerName: String,
    val sellerPhone: String?,
    val sellerAddress: String?,

    val buyerLabel: String?,
    val buyerName: String,
    val buyerPhone: String?,
    val buyerAddress: String?,

    val notes: String?
)

data class InvoiceItemInput(
    val id: Int?,
    val description: String,
    val unit: String?,
    val quantity: Double,
    val unitPrice: Long,
    val discount: Long
)

data class InvoiceItemDraft(
    val id: Int? = null,
    val description: String = "",
    val unit: String = "",
    val quantity: String = "",
    val unitPrice: String = "",
    val discount: Long = 0L,

    val isInstallationFee: Boolean = false,
    val installationPercent: String = ""
)




sealed class OrderInvoiceEvent {
    data class OpenInvoiceEditor(val invoiceId: Int) : OrderInvoiceEvent()
}

