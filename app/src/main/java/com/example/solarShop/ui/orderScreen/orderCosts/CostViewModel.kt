package com.example.solarShop.ui.orderScreen.orderCosts

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Embedded
import com.example.solarShop.data.backupRestore.AttachmentController
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.dataStore.DisplayPreferencesDataSource
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseAllocationEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseAttachmentEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseCategoryEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostRepository
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderExpenseEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderReceiptEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ReceiptAllocationEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ReceiptAttachmentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CostViewModel @Inject constructor(
    private val repo: OrderCostRepository,
    private val clientRepo: ClientRepository,
    private val attachmentController: AttachmentController,
    displayPrefs: DisplayPreferencesDataSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var attachJob: Job? = null

    private val scopeFlow = MutableStateFlow(CostScope.Order)


    val displayPrefsState: StateFlow<DisplayPreferences> =
        displayPrefs.prefsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DisplayPreferences()
        )


    private val actions = MutableSharedFlow<CostAction>(extraBufferCapacity = 32)

    private val orderIdFlow: StateFlow<Int?> =
        savedStateHandle.getStateFlow("orderId", -1)
            .map { if (it == -1) null else it }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow(CostUiState())
    val state: StateFlow<CostUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            orderIdFlow.filterNotNull().collect { oid ->
                _state.update { it.copy(orderId = oid, loading = true, error = null) }



                bindStreams(oid)
            }
        }
        viewModelScope.launch { handleActions() }
    }








    @OptIn(ExperimentalCoroutinesApi::class)
    private fun bindStreams(orderId: Int) {

        // 1) clientId را از سفارش می‌گیریم
        val clientIdFlow = repo.clientIdOfOrder(orderId)
            .distinctUntilChanged()

        // 2) state: clientId + avatar
        viewModelScope.launch {
            clientIdFlow
                .flatMapLatest { cid ->
                    clientRepo.observeClientById(cid)
                }
                .collect { client ->
                    _state.update {
                        it.copy(
                            clientId = client?.id ?: it.clientId, // اگر کلاینت null شد، clientId قبلی را دست نزن (اختیاری)
                            clientAvatarPath = client?.avatar.orEmpty()
                        )
                    }
                }
        }

        // 3) بقیه‌ی استریم‌های هزینه/دریافتی (همان قبلی خودت)
        viewModelScope.launch {
            combine(scopeFlow, clientIdFlow) { scope, cid -> scope to cid }
                .flatMapLatest { (scope, cid) ->
                    combine(
                        repo.receipts(scope, orderId, cid),
                        repo.expenses(scope, orderId, cid),
                        repo.receiptsSum(scope, orderId, cid),
                        repo.expensesSum(scope, orderId, cid)
                    ) { rRows, eRows, sR, sE ->
                        _state.value.copy(
                            loading = false,
                            scope = scope,
                            receipts = rRows.map { ReceiptUI(it) },
                            expenses = eRows.map { ExpenseUI(it) },
                            sumReceiptsToman = sR,
                            sumExpensesToman = sE,
                            netToman = sR - sE,
                            error = null
                        )
                    }
                }
                .collect { _state.value = it }
        }
        viewModelScope.launch {
            repo.observeAll().collect { list ->
                _state.update { it.copy(expenseCategories = list) }
            }
        }

    }




    private suspend fun handleActions() {
        actions.collect { act ->
            when (act) {
                is CostAction.Init -> savedStateHandle["orderId"] = act.orderId
                is CostAction.SelectTab -> _state.update { it.copy(selectedTab = act.tab) }
                is CostAction.ToggleTab -> _state.update {
                    it.copy(selectedTab = if (it.selectedTab == CostTab.Receipts) CostTab.Expenses else CostTab.Receipts)
                }

                is CostAction.AddReceipt -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val oid = state.value.orderId ?: return@launch
                            val newId = repo.addReceipt(act.entity).toInt()

                            val drafts = state.value.draftReceiptAttachments
                            drafts.forEach { d ->
                                attachmentController.addReceiptImage(oid, newId, d.uri)
                                deleteTemp(d.tempFilePath)
                            }
                            _state.update { it.copy(draftReceiptAttachments = emptyList()) }

                        } catch (t: Throwable) {
                            _state.update { it.copy(error = "خطا در ثبت دریافتی/پیوست‌ها: ${t.message ?: ""}") }
                        }
                    }
                }

                is CostAction.UpdateReceipt -> repo.updateReceipt(act.entity)
                is CostAction.DeleteReceipt -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            attachmentController.deleteReceiptWithAttachments(act.entity)
                        } catch (t: Throwable) {
                            _state.update { it.copy(error = "خطا در حذف دریافتی/پیوست‌ها: ${t.message ?: ""}") }
                        }
                    }
                }

                is CostAction.AddExpense -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val oid = state.value.orderId ?: return@launch
                            val newId = repo.addExpense(act.entity).toInt()

                            // ✅ draft attachments را واقعاً ذخیره کن
                            val drafts = state.value.draftExpenseAttachments
                            drafts.forEach { d ->
                                attachmentController.addExpenseImage(oid, newId, d.uri)
                                deleteTemp(d.tempFilePath)  // ✅ اگر از دوربین آمده بود temp پاک شود
                            }

                            // ✅ پاک کردن draft ها بعد از موفقیت
                            _state.update { it.copy(draftExpenseAttachments = emptyList()) }


                        } catch (t: Throwable) {
                            _state.update { it.copy(error = "خطا در ثبت هزینه/پیوست‌ها: ${t.message ?: ""}") }
                        }
                    }
                }

                is CostAction.UpdateExpense -> repo.updateExpense(act.entity)
                is CostAction.DeleteExpense -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            attachmentController.deleteExpenseWithAttachments(act.entity)
                        } catch (t: Throwable) {
                            _state.update { it.copy(error = "خطا در حذف هزینه/پیوست‌ها: ${t.message ?: ""}") }
                        }
                    }
                }

                is CostAction.AddReceiptAttachment -> repo.addReceiptAttachment(act.entity)
                is CostAction.DeleteReceiptAttachment -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            attachmentController.deleteReceiptAttachment(act.entity)
                        } catch (t: Throwable) {
                            _state.update { it.copy(error = "خطا در حذف پیوست: ${t.message ?: ""}") }
                        }
                    }
                }
                is CostAction.AddExpenseAttachment -> repo.addExpenseAttachment(act.entity)
                is CostAction.DeleteExpenseAttachment -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            attachmentController.deleteExpenseAttachment(act.entity)
                        } catch (t: Throwable) {
                            _state.update { it.copy(error = "خطا در حذف پیوست: ${t.message ?: ""}") }
                        }
                    }
                }

                is CostAction.ShowAddEditReceipt -> _state.update {
                    val editing = act.initial
                    it.copy(
                        showAddEditDialog = true,
                        editingReceipt = editing,
                        editingExpense = null,
                        // ✅ وقتی وارد حالت Edit می‌شیم draft ها باید خالی بشن
                        draftReceiptAttachments = if (editing != null) emptyList() else it.draftReceiptAttachments
                    )
                }

                is CostAction.ShowAddEditExpense -> _state.update {
                    val editing = act.initial
                    it.copy(
                        showAddEditDialog = true,
                        editingExpense = editing,
                        editingReceipt = null,
                        // ✅ وقتی وارد حالت Edit می‌شیم draft ها باید خالی بشن
                        draftExpenseAttachments = if (editing != null) emptyList() else it.draftExpenseAttachments
                    )
                }


                CostAction.HideDialog ->
                    _state.update { it.copy(showAddEditDialog = false, editingReceipt = null, editingExpense = null) }

                is CostAction.OpenAttachmentManager -> bindAttachFlow(act.target, act.ownerId)
                CostAction.CloseAttachmentManager -> {
                    attachJob?.cancel(); attachJob = null
                    _state.update { it.copy(attachmentPanel = null) }
                }

                is CostAction.SelectScope -> {
                    scopeFlow.value = act.scope
                    _state.update { it.copy(scope = act.scope) }
                }

                is CostAction.OpenAllocation -> openAllocation(act.target, act.ownerId, act.totalToman)
                CostAction.CloseAllocation -> _state.update { it.copy(allocationPanel = null) }

                is CostAction.ChangeAllocationAmount -> {
                    _state.update { st ->
                        val p = st.allocationPanel ?: return@update st
                        val newRows = p.rows.map {
                            if (it.orderId == act.orderId) it.copy(amount = act.amount) else it
                        }
                        st.copy(allocationPanel = p.copy(rows = newRows, error = null))
                    }
                }

                CostAction.SaveAllocation -> saveAllocation()

                is CostAction.AddDraftAttachment -> _state.update { st ->
                    when (act.target) {
                        AttachTarget.Receipt -> st.copy(draftReceiptAttachments = st.draftReceiptAttachments + act.attachment)
                        AttachTarget.Expense -> st.copy(draftExpenseAttachments = st.draftExpenseAttachments + act.attachment)
                    }
                }


                is CostAction.RemoveDraftAttachment -> _state.update { st ->
                    when (act.target) {
                        AttachTarget.Receipt -> {
                            val list = st.draftReceiptAttachments.toMutableList()
                            if (act.index in list.indices) {
                                deleteTemp(list[act.index].tempFilePath)
                                list.removeAt(act.index)
                            }
                            st.copy(draftReceiptAttachments = list)
                        }
                        AttachTarget.Expense -> {
                            val list = st.draftExpenseAttachments.toMutableList()
                            if (act.index in list.indices) {
                                deleteTemp(list[act.index].tempFilePath)
                                list.removeAt(act.index)
                            }
                            st.copy(draftExpenseAttachments = list)
                        }
                    }
                }


                is CostAction.ClearDraftAttachments -> _state.update { st ->
                    when (act.target) {
                        AttachTarget.Receipt -> {
                            st.draftReceiptAttachments.forEach { deleteTemp(it.tempFilePath) }
                            st.copy(draftReceiptAttachments = emptyList())
                        }
                        AttachTarget.Expense -> {
                            st.draftExpenseAttachments.forEach { deleteTemp(it.tempFilePath) }
                            st.copy(draftExpenseAttachments = emptyList())
                        }
                    }
                }


                is CostAction.AddExpenseWithCategory -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val d = act.draft

                            val finalCategoryId =
                                d.customCategoryTitle?.trim()?.takeIf { it.isNotBlank() }?.let { title ->
                                    repo.getOrCreateId(title)
                                } ?: d.pickedCategoryId

                            val newId = repo.addExpense(
                                OrderExpenseEntity(
                                    orderId = d.orderId,
                                    clientId = d.clientId,
                                    amountToman = d.amountToman,
                                    dateEpoch = d.dateEpoch,
                                    categoryId = finalCategoryId,
                                    note = d.note
                                )
                            ).toInt()

                            // ✅ این سفارش-اسکرین برای مسیر فایل لازم است (مثل AddExpense)
                            val oid = state.value.orderId ?: return@launch

                            // ✅ draft attachments را واقعاً ذخیره کن
                            // ✅ draft attachments را واقعاً ذخیره کن
                            val drafts = state.value.draftExpenseAttachments
                            drafts.forEach { d ->
                                attachmentController.addExpenseImage(oid, newId, d.uri)
                                deleteTemp(d.tempFilePath)  // ✅ اگر از دوربین آمده بود temp پاک شود
                            }

                            // ✅ پاک کردن draft ها بعد از موفقیت
                            _state.update { it.copy(draftExpenseAttachments = emptyList()) }


                        } catch (t: Throwable) {
                            _state.update {
                                it.copy(error = "خطا در ثبت هزینه/پیوست‌ها: ${t.message ?: ""}")
                            }
                        }
                    }
                }

                is CostAction.UpdateExpenseWithCategory -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val finalCategoryId =
                                act.customCategoryTitle?.trim()?.takeIf { it.isNotBlank() }?.let { title ->
                                    repo.getOrCreateId(title)
                                } ?: act.pickedCategoryId

                            repo.updateExpense(
                                act.editing.copy(
                                    // orderId/clientId دست نخورند
                                    amountToman = act.amountToman,
                                    dateEpoch = act.dateEpoch,
                                    categoryId = finalCategoryId,
                                    note = act.note
                                )
                            )
                        } catch (t: Throwable) {
                            _state.update { it.copy(error = "خطا در ویرایش هزینه: ${t.message ?: ""}") }
                        }
                    }
                }





            }
        }
    }

    private fun openAllocation(target: AttachTarget, ownerId: Int, totalToman: Long) {
        val cid = _state.value.clientId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val orders = repo.ordersOfClientMini(cid)
                    .first() // نیاز به import kotlinx.coroutines.flow.first

                val existing = when (target) {
                    AttachTarget.Receipt -> repo.receiptAllocationsOnce(ownerId)
                        .associate { it.orderId to it.amountToman }
                    AttachTarget.Expense -> repo.expenseAllocationsOnce(ownerId)
                        .associate { it.orderId to it.amountToman }
                }

                val rows = orders.map { o ->
                    AllocationEditRow(
                        orderId = o.id,
                        title = o.name,
                        amount = existing[o.id]
                    )
                }

                _state.update {
                    it.copy(
                        allocationPanel = AllocationPanelState(
                            target = target,
                            ownerId = ownerId,
                            totalToman = totalToman,
                            rows = rows,
                            open = true
                        )
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(error = "خطا در باز کردن تخصیص: ${t.message ?: ""}") }
            }
        }
    }

    private fun saveAllocation() {
        val panel = _state.value.allocationPanel ?: return
        val total = panel.totalToman
        val items = panel.rows
            .mapNotNull { r -> r.amount?.takeIf { it > 0 }?.let { amt -> r.orderId to amt } }

        val sum = items.sumOf { it.second }
        if (sum > total) {
            _state.update {
                it.copy(allocationPanel = panel.copy(error = "جمع تخصیص‌ها از مبلغ کل بیشتر است"))
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (panel.target) {
                    AttachTarget.Receipt -> {
                        val entities = items.map { (oid, amt) ->
                            ReceiptAllocationEntity(receiptId = panel.ownerId, orderId = oid, amountToman = amt)
                        }
                        repo.replaceReceiptAllocations(panel.ownerId, entities)
                    }
                    AttachTarget.Expense -> {
                        val entities = items.map { (oid, amt) ->
                            ExpenseAllocationEntity(expenseId = panel.ownerId, orderId = oid, amountToman = amt)
                        }
                        repo.replaceExpenseAllocations(panel.ownerId, entities)
                    }
                }
                _state.update { it.copy(allocationPanel = null) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(allocationPanel = panel.copy(error = "خطا در ذخیره تخصیص: ${t.message ?: ""}"))
                }
            }
        }
    }


    // API ساده برای UI:
    fun onAction(a: CostAction) { actions.tryEmit(a) }


    fun addReceiptImageFromUri(receiptId: Int, uri: Uri, tempFilePath: String? = null) {
        val orderId = state.value.orderId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                attachmentController.addReceiptImage(orderId, receiptId, uri)
                deleteTemp(tempFilePath) // ✅ بعد از موفقیت
            } catch (t: Throwable) {
                _state.update { it.copy(error = "خطا در افزودن تصویر: ${t.message ?: ""}") }
            }
        }
    }

    fun addExpenseImageFromUri(expenseId: Int, uri: Uri, tempFilePath: String? = null) {
        val orderId = state.value.orderId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                attachmentController.addExpenseImage(orderId, expenseId, uri)
                deleteTemp(tempFilePath) // ✅ بعد از موفقیت
            } catch (t: Throwable) {
                _state.update { it.copy(error = "خطا در افزودن تصویر: ${t.message ?: ""}") }
            }
        }
    }


    fun deleteReceiptAttachmentSafe(entity: ReceiptAttachmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try { attachmentController.deleteReceiptAttachment(entity) }
            catch (t: Throwable) {
                _state.update { it.copy(error = "خطا در حذف پیوست: ${t.message ?: ""}") }
            }
        }
    }

    fun deleteExpenseAttachmentSafe(entity: ExpenseAttachmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try { attachmentController.deleteExpenseAttachment(entity) }
            catch (t: Throwable) {
                _state.update { it.copy(error = "خطا در حذف پیوست: ${t.message ?: ""}") }
            }
        }
    }


    private fun bindAttachFlow(target: AttachTarget, ownerId: Int) {
        attachJob?.cancel()
        attachJob = viewModelScope.launch {
            val flow: Flow<List<Any>> = when (target) {
                AttachTarget.Receipt -> repo.receiptAttachments(ownerId).map { it as List<Any> }
                AttachTarget.Expense -> repo.expenseAttachments(ownerId).map { it as List<Any> }
            }
            flow.collect { list ->
                _state.update {
                    it.copy(attachmentPanel = AttachmentPanelState(target, ownerId, list, open = true))
                }
            }
        }
    }

    private fun deleteTemp(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { java.io.File(path).delete() }
    }

    fun onCameraMessage(msg: String) {
        _state.update { it.copy(error = msg) }
    }



}

data class CostUiState(
    val loading: Boolean = true,
    val orderId: Int? = null,
    val clientId: Int? = null,
    val selectedTab: CostTab = CostTab.Receipts,
    val receipts: List<ReceiptUI> = emptyList(),
    val expenses: List<ExpenseUI> = emptyList(),
    val sumReceiptsToman: Long = 0,
    val sumExpensesToman: Long = 0,
    val netToman: Long = 0,
    val error: String? = null,
    val showAddEditDialog: Boolean = false,
    val editingReceipt: OrderReceiptEntity? = null,
    val editingExpense: OrderExpenseEntity? = null,
    val attachmentPanel: AttachmentPanelState? = null,
    val scope: CostScope = CostScope.Order,
    val allocationPanel: AllocationPanelState? = null,
    val draftReceiptAttachments: List<DraftAttachment> = emptyList(),
    val draftExpenseAttachments: List<DraftAttachment> = emptyList(),
    val customerAvatarUri: Uri? = null,
    val customerInitials: String? = null, // اگر عکس نبود، با حروف اول نمایش بده
    val clientAvatarPath: String = "",
    val expenseCategories: List<ExpenseCategoryEntity> = emptyList()


)

data class ReceiptUI(
    val row: ReceiptRow,
    val isSelected: Boolean = false
)

data class ExpenseUI(
    val row: ExpenseRow,
    val isSelected: Boolean = false
)

data class AttachmentPanelState(
    val target: AttachTarget,
    val ownerId: Int,                     // receiptId یا expenseId
    val items: List<Any> = emptyList(),   // ReceiptAttachmentEntity | ExpenseAttachmentEntity
    val open: Boolean = false
)

data class ReceiptRow(
    @Embedded val entity: OrderReceiptEntity,
    val attachCount: Int,
    val allocatedSumToman: Long,   // فقط برای customer-level مهمه
    val allocatedToOrderToman: Long,   // ✅ سهم این سفارش از این رسید (اگر از کلی مشتری آمده)
    val isFromCustomerAllocation: Int  // ✅ 0/1
)

data class ExpenseRow(
    @Embedded val entity: OrderExpenseEntity,
    val attachCount: Int,
    val allocatedSumToman: Long,
    val allocatedToOrderToman: Long,
    val isFromCustomerAllocation: Int
)

data class OrderMini(
    val id: Int,
    val name: String
)

data class AllocationEditRow(
    val orderId: Int,
    val title: String,
    val amount: Long? = null
)

data class AllocationPanelState(
    val target: AttachTarget,      // Receipt / Expense
    val ownerId: Int,              // receiptId / expenseId
    val totalToman: Long,
    val rows: List<AllocationEditRow>,
    val open: Boolean = false,
    val error: String? = null
)

data class CostCardModel(
    val kind: CostItemKind,
    val id: Int,
    val totalAmountToman: Long,          // مبلغ اصلی آیتم (برای customer-level هم همین)
    val shownAmountToman: Long,          // مبلغی که روی کارت نشان می‌دی (برای fromAlloc = allocatedToOrder)
    val dateEpoch: Long,
    val titleLine: String,               // مثلا: "روش دریافت: کارتخوان" یا "دسته: یراق"
    val note: String?,
    val attachCount: Int,

    // allocation
    val allocatedSumToman: Long,         // جمع تخصیص‌ها (فقط در customer-scope معنی‌دار)
    val allocatedToOrderToman: Long,     // سهم این سفارش (وقتی fromAlloc)
    val isFromCustomerAllocation: Boolean
)

data class ExpenseDraft(
    val orderId: Int?,
    val clientId: Int,
    val amountToman: Long,
    val dateEpoch: Long,
    val pickedCategoryId: Int?,
    val customCategoryTitle: String?,
    val note: String?
)

data class DraftAttachment(
    val uri: Uri,
    val tempFilePath: String? = null // فقط برای دوربین
)




sealed interface CostAction {
    data class Init(val orderId: Int): CostAction
    data object ToggleTab: CostAction
    data class SelectTab(val tab: CostTab): CostAction

    // Receipt
    data class AddReceipt(val entity: OrderReceiptEntity): CostAction
    data class UpdateReceipt(val entity: OrderReceiptEntity): CostAction
    data class DeleteReceipt(val entity: OrderReceiptEntity): CostAction

    // Expense
    data class AddExpense(val entity: OrderExpenseEntity): CostAction
    data class UpdateExpense(val entity: OrderExpenseEntity): CostAction
    data class DeleteExpense(val entity: OrderExpenseEntity): CostAction

    // Attachments (اختیاری در همین VM یا VM جدا)
    data class AddReceiptAttachment(val entity: ReceiptAttachmentEntity): CostAction
    data class DeleteReceiptAttachment(val entity: ReceiptAttachmentEntity): CostAction
    data class AddExpenseAttachment(val entity: ExpenseAttachmentEntity): CostAction
    data class DeleteExpenseAttachment(val entity: ExpenseAttachmentEntity): CostAction

    // Dialogs
    data class ShowAddEditReceipt(val initial: OrderReceiptEntity? = null): CostAction
    data class ShowAddEditExpense(val initial: OrderExpenseEntity? = null): CostAction
    data object HideDialog: CostAction

    data class OpenAttachmentManager(val target: AttachTarget, val ownerId: Int): CostAction
    data object CloseAttachmentManager: CostAction

    data class SelectScope(val scope: CostScope): CostAction

    data class OpenAllocation(val target: AttachTarget, val ownerId: Int, val totalToman: Long): CostAction
    data object CloseAllocation: CostAction
    data class ChangeAllocationAmount(val orderId: Int, val amount: Long?): CostAction
    data object SaveAllocation: CostAction

    data class AddDraftAttachment(val target: AttachTarget, val attachment: DraftAttachment) : CostAction


    data class RemoveDraftAttachment(val target: AttachTarget, val index: Int) : CostAction
    data class ClearDraftAttachments(val target: AttachTarget) : CostAction

    data class AddExpenseWithCategory(
        val draft: ExpenseDraft
    ) : CostAction

    data class UpdateExpenseWithCategory(
        val editing: OrderExpenseEntity,
        val amountToman: Long,
        val dateEpoch: Long,
        val pickedCategoryId: Int?,
        val customCategoryTitle: String?,
        val note: String?
    ) : CostAction

}



enum class CostScope { Order, Customer }
enum class AttachTarget { Receipt, Expense }
enum class CostTab { Receipts, Expenses }
enum class CostItemKind { Receipt, Expense }

