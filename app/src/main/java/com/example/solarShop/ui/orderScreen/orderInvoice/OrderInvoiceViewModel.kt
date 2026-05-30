package com.example.solarShop.ui.orderScreen.orderInvoice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.example.solarShop.data.dataStore.SessionDataStore.Keys.currentUserId
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
    private val orderIdFlow: StateFlow<Int?> =
        savedStateHandle.getStateFlow("orderId", -1)
            .map { if (it == -1) null else it }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    // تمپلیت‌ها برای این نوع سند
    @OptIn(ExperimentalCoroutinesApi::class)
    private val templatesFlow: Flow<List<InvoiceTemplateEntity>> =
        typeFlow.flatMapLatest { type ->
            flow {
                val list = templateDao.getTemplatesByType(type)
                emit(list)
            }
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
        ) { arr ->
            val order = arr[0] as OrderEntity?
            val type = arr[1] as InvoiceType
            val templates = arr[2] as List<InvoiceTemplateEntity>
            val invoices = arr[3] as List<InvoiceDocumentEntity>
            val editor = arr[4] as InvoiceEditorUiState
            val editorInvoice = arr[5] as InvoiceWithItems?
            val currentUser = arr[6] as UserEntity?
            val proformas = arr[7] as List<InvoiceDocumentEntity>

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
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrderInvoiceUiState(isLoading = true)
        )




    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting // اگر لازم شد در UI نشان بدهی




    fun onCreateNewFromTemplate(templateId: Int) {
        viewModelScope.launch {
            val orderId = orderIdFlow.value ?: return@launch
            val type = typeFlow.value

            // 👇 اگر قبلاً برای این سفارش و این نوع، سند داریم: همان را باز کن
            val existing = invoiceDao.getInvoicesForOrder(orderId, type)
            if (existing.isNotEmpty()) {
                val first = existing.first()
                editorState.value = InvoiceEditorUiState(
                    isVisible = true,
                    selectedInvoiceId = first.id
                )
                return@launch
            }

            // 👇 از این‌جا به بعد فقط وقتی اجرا می‌شود که هنوز سندی وجود ندارد

            val order = orderRepo.getOrderById(orderId) ?: return@launch
            val template = templateDao.getTemplateById(templateId) ?: return@launch

            val now = System.currentTimeMillis()
            val number = generateInvoiceNumber(type, now, orderId)

            val seller = uiState.value.currentUser
            val buyer: ClientEntity? = order.clientId.let { clientId ->
                clientRepo.getClientById(clientId)    // یا clientDao.getById(...)
            }


            val doc = InvoiceDocumentEntity(
                id = 0,
                orderId = orderId,
                templateId = template.id,
                type = type,
                number = number,
                createdAt = now,
                updatedAt = now,
                status = InvoiceStatus.DRAFT,

                sellerLabel = "فروشنده",
                sellerName = seller?.name ?:"",
                sellerPhone = seller?.mobilePhone ?:"",
                sellerAddress = seller?.address ?:"",
                sellerNationalId = seller?.nationalCode ?:"",
                sellerEconomicCode = seller?.workshop ?:"",

                buyerLabel = "مشتری",
                buyerName = buyer?.name ?:"",
                buyerPhone = buyer?.mobilePhone ?:"",
                buyerAddress = buyer?.address ?:"",
                buyerNationalId = buyer?.nationalCode ?:"",



                subtotalBeforeDiscount = 0L,
                totalDiscount = 0L,
                totalBeforeTax = 0L,
                totalTax = 0L,
                totalFinal = 0L,

                notes = null,
                pdfAttachmentId = null
            )

            val newId = invoiceDao.insertInvoice(doc).toInt()

            editorState.value = InvoiceEditorUiState(
                isVisible = true,
                selectedInvoiceId = newId
            )
        }
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
        val current = invoiceDao.getInvoiceWithItems(header.invoiceId)?.invoice ?: return

        val updated = current.copy(
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
            updatedAt = System.currentTimeMillis()
        )

        invoiceDao.updateInvoice(updated)

        // اگه می‌خوای بعد از ذخیره، ادیتور بسته بشه:
        editorState.value = InvoiceEditorUiState(
            isVisible = false,
            selectedInvoiceId = null
        )
    }

    private suspend fun saveItemsInternal(
        invoiceId: Int,
        items: List<InvoiceItemInput>
    ) {
        val entities = items.mapIndexed { index, it ->
            val rowSubtotal = (it.quantity * it.unitPrice).toLong().coerceAtLeast(0L)
            val rowDiscount = it.discount.coerceAtLeast(0L)
            val taxAmount = 0L
            val rowTotal = (rowSubtotal - rowDiscount + taxAmount).coerceAtLeast(0L)

            InvoiceItemEntity(
                id = it.id ?: 0,
                invoiceId = invoiceId,
                rowIndex = index,
                description = it.description,
                quantity = it.quantity,
                unit = it.unit,
                unitPrice = it.unitPrice,
                rowDiscount = rowDiscount,
                rowSubtotal = rowSubtotal,
                taxPercent = null,
                taxAmount = taxAmount,
                rowTotal = rowTotal
            )
        }

        invoiceDao.deleteItemsForInvoice(invoiceId)
        invoiceDao.insertItems(entities)

        val subtotalBeforeDiscount = entities.sumOf { it.rowSubtotal }
        val totalDiscount = entities.sumOf { it.rowDiscount }
        val totalBeforeTax = (subtotalBeforeDiscount - totalDiscount).coerceAtLeast(0L)
        val totalTax = entities.sumOf { it.taxAmount }
        val totalFinal = (totalBeforeTax + totalTax).coerceAtLeast(0L)

        val currentInvoice = invoiceDao.getInvoiceWithItems(invoiceId)?.invoice ?: return
        val updatedInvoice = currentInvoice.copy(
            subtotalBeforeDiscount = subtotalBeforeDiscount,
            totalDiscount = totalDiscount,
            totalBeforeTax = totalBeforeTax,
            totalTax = totalTax,
            totalFinal = totalFinal,
            updatedAt = System.currentTimeMillis()
        )
        invoiceDao.updateInvoice(updatedInvoice)
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


    fun onDeleteInvoice(invoiceId: Int) {
        viewModelScope.launch {
            val invoice = invoiceDao.getInvoiceById(invoiceId) ?: return@launch
            invoiceDao.deleteInvoice(invoice)
            // اگر خواستی، اینجا editorState رو هم reset کن و یه Snackbar رو هم بعداً اضافه می‌کنیم
            editorState.value = InvoiceEditorUiState(
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
            val orderId = orderIdFlow.value ?: return@launch

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

            // ✅ ساخت سند جدید Invoice با کپی فیلدهای پرشده
            val newDoc = source.invoice.copy(
                id = 0,
                type = InvoiceType.INVOICE,
                templateId = templateId,
                number = newNumber,
                status = InvoiceStatus.DRAFT,
                updatedAt = now,
                pdfAttachmentId = null
                // createdAt را من دست نزدم تا تاریخ سند هم از پیش‌فاکتور منتقل شود.
                // اگر خواستی تاریخ فاکتور «الان» باشد: createdAt = now
            )

            val newInvoiceId = invoiceDao.insertInvoice(newDoc).toInt()

            // ✅ کپی آیتم‌ها
            val newItems = source.items.map { it.copy(id = 0, invoiceId = newInvoiceId) }
            invoiceDao.insertItems(newItems)

            // ✅ باز کردن ادیتور فاکتور
            editorState.value = InvoiceEditorUiState(isVisible = true, selectedInvoiceId = newInvoiceId)
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
    val discount: Long = 0L
)




sealed class OrderInvoiceEvent {
    data class OpenInvoiceEditor(val invoiceId: Int) : OrderInvoiceEvent()
}

