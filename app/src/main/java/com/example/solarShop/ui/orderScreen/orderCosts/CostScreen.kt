package com.example.solarShop.ui.orderScreen.orderCosts

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.R
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseAttachmentEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseCategoryEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderExpenseEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderReceiptEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.ReceiptAttachmentEntity
import com.example.solarShop.utils.ConfirmDimmedDialog
import com.example.solarShop.utils.DateUi
import com.example.solarShop.utils.DimmedDialog
import com.example.solarShop.utils.FullscreenImageViewer
import com.example.solarShop.utils.MyCurrencyField
import com.example.solarShop.utils.PersianDateUiAdapter
import com.example.solarShop.utils.TopBarGeneral
import com.example.solarShop.utils.formatPersianDateTime
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import com.example.solarShop.utils.toCurrencyText
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    vm: CostViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prefs by vm.displayPrefsState.collectAsStateWithLifecycle()



    // اسنک‌بار ساده (در صورت نیاز بعداً استفاده می‌کنیم)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val net = state.netToman
    val netContainerColor = when {
        net > 0 -> MaterialTheme.colorScheme.tertiaryContainer
        net < 0 -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val netContentColor = when {
        net > 0 -> MaterialTheme.colorScheme.onTertiaryContainer
        net < 0 -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }


    var pendingTarget by remember { mutableStateOf<AttachTarget?>(null) }
    var pendingId by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

    var showPickSheet by remember { mutableStateOf(false) }


    fun createCameraTemp(): Pair<File, Uri> {
        val dir = File(context.cacheDir, "camera_tmp").apply { mkdirs() }
        val file = File(dir, "c_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }


    // Photo Picker (AndroidX)
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val id = pendingId
        val target = pendingTarget

        if (uri != null && target != null) {
            if (id != null) {
                when (target) {
                    AttachTarget.Receipt -> vm.addReceiptImageFromUri(id, uri)
                    AttachTarget.Expense -> vm.addExpenseImageFromUri(id, uri)
                }
            } else {
                vm.onAction(CostAction.AddDraftAttachment(target, DraftAttachment(uri)))
            }
        }

        pendingTarget = null
        pendingId = null
    }

    // دوربین (temp)
    var pendingTempFile by remember { mutableStateOf<File?>(null) }
    val camera = rememberCameraCaptureLauncher(
        requiredPermissions = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                arrayOf(android.Manifest.permission.CAMERA)
            } else emptyArray()
        },
        createOutputUri = {
            val (f, u) = createCameraTemp()
            pendingTempFile = f
            u
        },
        onResult = { uri, success ->
            val target = pendingTarget
            val id = pendingId
            val tmpFile = pendingTempFile

            if (success && target != null && tmpFile != null) {
                if (id != null) {
                    when (target) {
                        AttachTarget.Receipt -> vm.addReceiptImageFromUri(id, uri, tmpFile.absolutePath)
                        AttachTarget.Expense -> vm.addExpenseImageFromUri(id, uri, tmpFile.absolutePath)
                    }

                } else {
                    vm.onAction(
                        CostAction.AddDraftAttachment(
                            target,
                            DraftAttachment(uri = uri, tempFilePath = tmpFile.absolutePath)
                        )
                    )
                }
            } else {
                tmpFile?.let { runCatching { it.delete() } }
            }

            pendingTempFile = null
            pendingTarget = null
            pendingId = null
        },
        onMessage = { msg ->
            // چون این VM خطاها را با state.error نشان می‌دهد،
            // سریع‌ترین: همان را ست کنیم:
            vm.onCameraMessage(msg)
        }
    )












    fun openPickSheet(target: AttachTarget, id: Int?) {
        pendingTarget = target
        pendingId = id
        showPickSheet = true
    }



    fun launchPickForReceipt(receiptId: Int) {
        openPickSheet(AttachTarget.Receipt, receiptId)
    }


    fun launchPickForExpense(expenseId: Int) {
        openPickSheet(AttachTarget.Expense, expenseId)
    }






    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBarGeneral(
                title = "دریافتی\u200Cها و هزینه\u200Cها",
                onBack = onClose
            )
        },
        bottomBar = {
            // BottomBar: جمع‌ها + خالص
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.navigationBarsPadding()   // ⟵ مهم
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryChip(
                            label = "جمع دریافتی",
                            amount = state.sumReceiptsToman,
                            prefs = prefs,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryChip(
                            label = "جمع هزینه",
                            amount = state.sumExpensesToman,
                            prefs = prefs,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // خالص
                        AssistChip(
                            onClick = {},
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("خالص پروژه")
                                    Text(
                                        text = state.netToman.toCurrencyText(prefs),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = netContainerColor,
                                labelColor = netContentColor
                            ),
                            border = null
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // FAB وابسته به تب
            val isReceipt = state.selectedTab == CostTab.Receipts
            ExtendedFloatingActionButton(
                onClick = {
                    if (isReceipt) vm.onAction(CostAction.ShowAddEditReceipt())
                    else vm.onAction(CostAction.ShowAddEditExpense())
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 2.dp,
                    focusedElevation = 2.dp,
                    hoveredElevation = 2.dp
                ),
                content = {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val scopeLabel = if (state.scope == CostScope.Customer) "کلی" else ""
                        Text(
                            if (isReceipt) "افزودن دریافتی $scopeLabel".trim()
                            else "افزودن هزینه $scopeLabel".trim()
                        )

                        Icon(Icons.Rounded.Add, contentDescription = "add")
                    }

                },
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // Tabs
            val tabs = listOf(CostTab.Receipts, CostTab.Expenses)
            val selectedIndex = tabs.indexOf(state.selectedTab).coerceAtLeast(0)

            ScopeRadioRow(
                scope = state.scope,
                customerAvatarPath = state.clientAvatarPath,
                onSelect = { vm.onAction(CostAction.SelectScope(it)) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )





            PrimaryTabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { i, tab ->
                    Tab(
                        selected = selectedIndex == i,
                        onClick = { vm.onAction(CostAction.SelectTab(tab)) },
                        text = { Text(if (tab == CostTab.Receipts) "دریافتی‌ها" else "هزینه‌ها") }
                    )
                }
            }

            // محتوا
            when (state.selectedTab) {
                CostTab.Receipts -> ReceiptListMock(
                    rows = state.receipts,
                    prefs = prefs,
                    scope=state.scope,
                    onEdit = { vm.onAction(CostAction.ShowAddEditReceipt(it.row.entity)) },
                    onDeleteEntity = { vm.onAction(CostAction.DeleteReceipt(it)) },
                    launchPickForReceipt = { launchPickForReceipt(it) },
                    openAttachmentManager = { target, ownerId ->
                        vm.onAction(
                            CostAction.OpenAttachmentManager(
                                target,
                                ownerId
                            )
                        )
                    },
                    onOpenAllocation = { target, ownerId, total ->
                        vm.onAction(CostAction.OpenAllocation(target, ownerId, total))
                    },
                    customerAvatarPath = state.clientAvatarPath,
                )

                CostTab.Expenses -> ExpenseListMock(
                    rows = state.expenses,
                    prefs = prefs,
                    scope = state.scope,
                    onEdit = { vm.onAction(CostAction.ShowAddEditExpense(it.row.entity)) },
                    onDeleteEntity = { vm.onAction(CostAction.DeleteExpense(it)) },
                    launchPickForExpense = { launchPickForExpense(it) },
                    openAttachmentManager = { target, ownerId -> vm.onAction(CostAction.OpenAttachmentManager(target, ownerId)) },
                    onOpenAllocation = { target, ownerId, total -> vm.onAction(CostAction.OpenAllocation(target, ownerId, total)) },
                    customerAvatarPath = state.clientAvatarPath,
                    expenseCategories = state.expenseCategories,      // ✅ این
                )

            }
            // --- Dialogs ---
            if (state.showAddEditDialog) {
                val orderId = state.orderId ?: -1
                if (state.editingReceipt != null || state.selectedTab == CostTab.Receipts) {
                    val editing = state.editingReceipt
                    val draftCount = state.draftReceiptAttachments.size
                    val dbCount = editing?.let { r ->
                        state.receipts.firstOrNull { it.row.entity.id == r.id }?.row?.attachCount ?: 0
                    } ?: 0

                    val shownAttachCount = if (editing == null) draftCount else dbCount
                    AddEditReceiptDialog(
                        initial = editing,
                        prefs = prefs,
                        attachCount = shownAttachCount,
                        onDismiss = {
                            vm.onAction(CostAction.ClearDraftAttachments(AttachTarget.Receipt))
                            vm.onAction(CostAction.HideDialog)
                        },

                        onConfirm = { amount, date, method, note ->
                            val oid = state.orderId ?: -1
                            val cid = state.clientId

                            if (oid != -1 && cid != null) {

                                // ✅ اگر کاربر در Customer scope هست و "در حال افزودن" است => orderId = null
                                // ✅ اگر در Order scope هست => orderId = همین سفارش
                                val targetOrderId: Int? = when {
                                    editing != null -> editing.orderId // 🔒 ادیت: orderId را تغییر نمی‌دهیم
                                    state.scope == CostScope.Customer -> null
                                    else -> oid
                                }

                                if (editing == null) {
                                    vm.onAction(
                                        CostAction.AddReceipt(
                                            OrderReceiptEntity(
                                                orderId = targetOrderId,
                                                clientId = cid,
                                                amountToman = amount,
                                                dateEpoch = date,
                                                method = method,
                                                note = note
                                            )
                                        )
                                    )
                                } else {
                                    vm.onAction(
                                        CostAction.UpdateReceipt(
                                            editing.copy(
                                                // orderId/clientId دست نخورند
                                                amountToman = amount,
                                                dateEpoch = date,
                                                method = method,
                                                note = note
                                            )
                                        )
                                    )
                                }
                            }

                            vm.onAction(CostAction.HideDialog)
                        },
                        onAddAttachment = {
                            openPickSheet(AttachTarget.Receipt, editing?.id)
                        },

                        onManageAttachments = {
                            val rid = editing?.id
                            if (rid != null) vm.onAction(CostAction.OpenAttachmentManager(AttachTarget.Receipt, rid))
                        }

                    )
                }
                else {
                    val editing = state.editingExpense
                    val draftCount = state.draftExpenseAttachments.size
                    val dbCount = editing?.let { r ->
                        state.expenses.firstOrNull { it.row.entity.id == r.id }?.row?.attachCount ?: 0
                    } ?: 0

                    val shownAttachCount = if (editing == null) draftCount else dbCount

                    AddEditExpenseDialog(
                        initial = editing,
                        prefs = prefs,
                        attachCount =shownAttachCount,
                        onDismiss = {
                            vm.onAction(CostAction.ClearDraftAttachments(AttachTarget.Expense))
                            vm.onAction(CostAction.HideDialog)
                        },
                        onConfirm = { amount, date, pickedId, customTitle, note ->
                            val oid = state.orderId ?: return@AddEditExpenseDialog
                            val cid = state.clientId ?: return@AddEditExpenseDialog

                            val targetOrderId: Int? = when {
                                editing != null -> editing.orderId
                                state.scope == CostScope.Customer -> null
                                else -> oid
                            }

                            if (editing == null) {
                                // ✅ افزودن
                                vm.onAction(
                                    CostAction.AddExpenseWithCategory(
                                        ExpenseDraft(
                                            orderId = targetOrderId,
                                            clientId = cid,
                                            amountToman = amount,
                                            dateEpoch = date,
                                            pickedCategoryId = pickedId,
                                            customCategoryTitle = customTitle,
                                            note = note
                                        )
                                    )
                                )
                            } else {
                                // ✅ ویرایش (بدون ساخت رکورد جدید)
                                vm.onAction(
                                    CostAction.UpdateExpenseWithCategory(
                                        editing = editing,
                                        amountToman = amount,
                                        dateEpoch = date,
                                        pickedCategoryId = pickedId,
                                        customCategoryTitle = customTitle,
                                        note = note
                                    )
                                )
                            }

                            vm.onAction(CostAction.HideDialog)
                        },
                        onAddAttachment = {
                            openPickSheet(AttachTarget.Expense, editing?.id)
                        },
                        onManageAttachments = {
                            val eid = editing?.id
                            if (eid != null) vm.onAction(CostAction.OpenAttachmentManager(AttachTarget.Expense, eid))
                        },
                        expenseCategories=state.expenseCategories

                    )
                }
            }
            state.attachmentPanel?.takeIf { it.open }?.let { panel ->
                AttachmentsManagerSheet(
                    panel = panel,
                    onClose = { vm.onAction(CostAction.CloseAttachmentManager) },
                    onAdd = { target, ownerId ->
                        openPickSheet(target, ownerId)
                    },
                    onDeleteReceipt = { vm.onAction(CostAction.DeleteReceiptAttachment(it)) },
                    onDeleteExpense = { vm.onAction(CostAction.DeleteExpenseAttachment(it)) }
                )
            }
            state.allocationPanel?.takeIf { it.open }?.let { panel ->
                AllocationSheet(
                    panel = panel,
                    prefs = prefs,
                    onClose = { vm.onAction(CostAction.CloseAllocation) },
                    onChange = { orderId, amount -> vm.onAction(CostAction.ChangeAllocationAmount(orderId, amount)) },
                    onSave = { vm.onAction(CostAction.SaveAllocation) }
                )
            }


        }
    }

    if (showPickSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickSheet = false }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("افزودن تصویر", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = {
                        showPickSheet = false
                        pickImage.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("از گالری") }


                Button(
                    onClick = {
                        showPickSheet = false
                        camera.launch()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("از دوربین") }


                Spacer(Modifier.height(12.dp))
            }
        }
    }


}

@Composable
private fun SummaryChip(
    label: String,
    amount: Long,
    prefs: DisplayPreferences,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    AssistChip(
        modifier = modifier,
        onClick = {},
        label = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(label)
                Text(
                    amount.toCurrencyText(prefs),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor
        ),
        border = null
    )
}



// --- Receipt List (ماکتی: فقط نمایش؛ اتصال دیتا آماده است) ---
@Composable
private fun ReceiptListMock(
    rows: List<ReceiptUI>,
    prefs: DisplayPreferences,
    scope: CostScope,
    onEdit: (ReceiptUI) -> Unit,
    onDeleteEntity: (OrderReceiptEntity) -> Unit,
    customerAvatarPath: String,
    launchPickForReceipt: (receiptId: Int) -> Unit,
    openAttachmentManager: (target: AttachTarget, ownerId: Int) -> Unit,
    onOpenAllocation: (target: AttachTarget, ownerId: Int, totalToman: Long) -> Unit
) {
    if (rows.isEmpty()) {
        val msg = if (scope == CostScope.Customer)
            "هنوز دریافتی کلی برای این مشتری ثبت نشده است."
        else
            "هنوز دریافتی برای این سفارش ثبت نشده است."

        EmptyState(text = msg)
        return
    }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rows, key = { it.row.entity.id }) { ui ->
            val r = ui.row

            val model = CostCardModel(
                kind = CostItemKind.Receipt,
                id = r.entity.id,
                totalAmountToman = r.entity.amountToman,
                shownAmountToman = if (r.isFromCustomerAllocation == 1) r.allocatedToOrderToman else r.entity.amountToman,
                dateEpoch = r.entity.dateEpoch,
                titleLine = "روش دریافت: ${r.entity.method}",
                note = r.entity.note,
                attachCount = r.attachCount,
                allocatedSumToman = r.allocatedSumToman,
                allocatedToOrderToman = r.allocatedToOrderToman,
                isFromCustomerAllocation = r.isFromCustomerAllocation == 1
            )

            CostItemCard(
                m = model,
                prefs = prefs,
                scope = scope,
                onAddAttach = { id -> launchPickForReceipt(id) },
                onEdit = { _ -> onEdit(ui) },                    // یا مستقیم id-based
                onRequestDelete = { id ->
                    pendingDeleteId = id
                    showDeleteConfirm = true
                },
                openAttachmentManager = openAttachmentManager,
                onOpenAllocation = onOpenAllocation,
                customerAvatarPath = customerAvatarPath,

            )

        }
    }
    ConfirmDimmedDialog(
        visible = showDeleteConfirm,
        title = "حذف دریافتی",
        message = { Text("این دریافتی حذف شود؟") },
        onConfirm = {
            val id = pendingDeleteId ?: return@ConfirmDimmedDialog
            val entity = rows.firstOrNull { it.row.entity.id == id }?.row?.entity ?: return@ConfirmDimmedDialog
            onDeleteEntity(entity)
        },

        onDismiss = {
            showDeleteConfirm = false
            pendingDeleteId = null
        }
    )

}




// --- Expense List (ماکتی) ---
@Composable
private fun ExpenseListMock(
    rows: List<ExpenseUI>,
    prefs: DisplayPreferences,
    scope: CostScope,
    onEdit: (ExpenseUI) -> Unit,
    onDeleteEntity: (OrderExpenseEntity) -> Unit,
    customerAvatarPath: String,
    launchPickForExpense: (expenseId: Int) -> Unit,
    openAttachmentManager: (target: AttachTarget, ownerId: Int) -> Unit,
    onOpenAllocation: (target: AttachTarget, ownerId: Int, totalToman: Long) -> Unit,
    expenseCategories: List<ExpenseCategoryEntity>,   // ✅ اضافه شد
) {
    if (rows.isEmpty()) {
        val msg = if (scope == CostScope.Customer)
            "هنوز هزینه ای کلی برای این مشتری ثبت نشده است."
        else
            "هنوز هزینه ای برای این سفارش ثبت نشده است."

        EmptyState(text = msg)
        return
    }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }

    val catTitleById = remember(expenseCategories) {
        expenseCategories.associate { it.id to it.title }
    }



    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rows, key = { it.row.entity.id }) { ui ->
            val e = ui.row

            val categoryTitle = e.entity.categoryId?.let { catTitleById[it] } ?: "—"

            val model = CostCardModel(
                kind = CostItemKind.Expense,
                id = e.entity.id,
                totalAmountToman = e.entity.amountToman,
                shownAmountToman = if (e.isFromCustomerAllocation == 1) e.allocatedToOrderToman else e.entity.amountToman,
                dateEpoch = e.entity.dateEpoch,
                titleLine = "دسته: $categoryTitle",
                note = e.entity.note,
                attachCount = e.attachCount,
                allocatedSumToman = e.allocatedSumToman,
                allocatedToOrderToman = e.allocatedToOrderToman,
                isFromCustomerAllocation = e.isFromCustomerAllocation == 1
            )

            CostItemCard(
                m = model,
                prefs = prefs,
                scope = scope,
                onAddAttach = { id -> launchPickForExpense(id) },
                onEdit = { _ -> onEdit(ui) },
                onRequestDelete = { id ->
                    pendingDeleteId = id
                    showDeleteConfirm = true
                },
                openAttachmentManager = openAttachmentManager,
                onOpenAllocation = onOpenAllocation,
                customerAvatarPath = customerAvatarPath
            )

        }
    }
    ConfirmDimmedDialog(
        visible = showDeleteConfirm,
        title = "حذف هزینه",
        message = { Text("این هزینه حذف شود؟") },
        onConfirm = {
            val id = pendingDeleteId ?: return@ConfirmDimmedDialog
            val entity = rows.firstOrNull { it.row.entity.id == id }?.row?.entity ?: return@ConfirmDimmedDialog
            onDeleteEntity(entity)
        },

        onDismiss = {
            showDeleteConfirm = false
            pendingDeleteId = null
        }
    )
}


@Composable
private fun CostItemCard(
    m: CostCardModel,
    prefs: DisplayPreferences,
    scope: CostScope,
    onAddAttach: (id: Int) -> Unit,
    onEdit: (id: Int) -> Unit,
    onRequestDelete: (id: Int) -> Unit,
    customerAvatarPath: String,
    openAttachmentManager: (target: AttachTarget, ownerId: Int) -> Unit,
    onOpenAllocation: (target: AttachTarget, ownerId: Int, totalToman: Long) -> Unit,
) {
    var expanded by rememberSaveable(m.kind.name + "_" + m.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "expandRotation")

    val target = if (m.kind == CostItemKind.Receipt) AttachTarget.Receipt else AttachTarget.Expense

    ElevatedCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            // --- ردیف خلاصه ---
            Row(verticalAlignment = Alignment.CenterVertically) {

                //مبلغ
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    Text(m.shownAmountToman.toCurrencyText(prefs))
                }

                Spacer(Modifier.width(12.dp))

                // ✅ تاریخ + خلاصه (برای هزینه: دسته)
                Column(verticalArrangement = Arrangement.Center) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        Text(formatPersianDateTime(m.dateEpoch, true))
                    }

                    if (m.kind == CostItemKind.Expense) {
                        Spacer(Modifier.height(2.dp))
                        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                            // اگر m.titleLine الان چیزی مثل "دسته: ..." است همون را نشان بده
                            // بهتر: titleLine فقط عنوان دسته باشد (مثلاً "یراق") و اینجا خودت "دسته:" را اضافه کن
                            Text(m.titleLine)
                        }
                    }
                }

                //اگر اختصاص یافته
                if (m.isFromCustomerAllocation) {
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("از")
                        Spacer(Modifier.width(6.dp))
                        CustomerAvatarMini(avatarPath = customerAvatarPath, sizeDp = 24)
                    }
                    Spacer(Modifier.width(8.dp))
                }

                //اگر در اسکوپ مشتری ساخته شده
                if (scope == CostScope.Customer && !m.isFromCustomerAllocation) {
                    Spacer(Modifier.width(12.dp))
                    CustomerAvatarMini(avatarPath = customerAvatarPath, sizeDp = 24)
                }

                Spacer(Modifier.weight(1f))

                //پیوست
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { openAttachmentManager(target, m.id) }
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = "پیوست‌ها")
                    Text(" ${m.attachCount}")
                }

                //ایکون اکسپند
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "بستن" else "باز کردن",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }




            // --- جزئیات + اکشن‌ها ---
            AnimatedVisibility(visible = expanded) {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {

                    HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 8.dp), thickness = 1.dp)



                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        if (m.kind == CostItemKind.Receipt) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                Text(m.titleLine)
                                m.note?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 3) }
                            }

                        }else{
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                m.note?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 3) }
                            }
                        }





                        Spacer(Modifier.height(8.dp))

                        // بخش تخصیص فقط وقتی customer-scope و آیتم اصلی (نه fromAlloc)
                        if (scope == CostScope.Customer && !m.isFromCustomerAllocation) {
                            val remaining =
                                (m.totalAmountToman - m.allocatedSumToman).coerceAtLeast(0)

                            Column {
                                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                                    Text("تخصیص‌یافته: ${m.allocatedSumToman.toCurrencyText(prefs)}")
                                    Text("باقی‌مانده: ${remaining.toCurrencyText(prefs)}")
                                }
                                Spacer(Modifier.height(6.dp))
                                Button(onClick = {
                                    onOpenAllocation(
                                        target,
                                        m.id,
                                        m.totalAmountToman
                                    )
                                }) {
                                    Text("تخصیص")
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }

                        // اکشن‌ها (دو ردیف تا حذف دیده شود)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onAddAttach(m.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("افزودن تصویر") }

                            OutlinedButton(
                                onClick = { onEdit(m.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("ویرایش") }

                            IconButton(onClick = { onRequestDelete(m.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }
    }
}





// برای دریافتی‌ها
private val paymentMethods = listOf("کارتخوان", "نقد", "کارت به کارت", "چک")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodDropdown(
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("روش دریافت") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            paymentMethods.forEach { m ->
                DropdownMenuItem(text = { Text(m) }, onClick = {
                    onValueChange(m); expanded = false
                })
            }
        }
    }
}

// برای هزینه‌ها (نسخه ساده؛ بعداً اتوساجست را وصل می‌کنیم)
@Composable
private fun CategoryDropdown(
    currentTitle: String?,
    onClickChoose: () -> Unit
) {
    OutlinedTextField(
        value = currentTitle ?: "",
        onValueChange = {},
        label = { Text("دسته هزینه") },
        readOnly = true,
        trailingIcon = {
            TextButton(onClick = onClickChoose) { Text("انتخاب") } // بعداً بازکنندهٔ لیست/دیالوگ کاتالوگ
        },
        placeholder = { Text("مثلاً: یراق، حمل، رنگ...") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AttachmentsRow(
    count: Int,
    enabled: Boolean = true,
    onAdd: () -> Unit,
    onManage: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistChip(
            onClick = { if (enabled) onManage() },
            enabled = enabled,
            label = { Text("پیوست‌ها: $count") }
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onAdd,
            enabled = enabled
        ) { Text("افزودن تصویر") }
    }
}


@Composable
private fun AddEditReceiptDialog(
    initial: OrderReceiptEntity? = null,
    attachCount: Int = 0,
    prefs: DisplayPreferences,
    onDismiss: () -> Unit,
    onConfirm: (amountToman: Long, dateEpoch: Long, method: String, note: String?) -> Unit,
    onAddAttachment: () -> Unit,
    onManageAttachments: () -> Unit
) {
    var amount by remember { mutableStateOf(initial?.amountToman) }
    var date by remember { mutableLongStateOf(initial?.dateEpoch ?: System.currentTimeMillis()) }
    var method by remember { mutableStateOf(initial?.method ?: "کارتخوان") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val canAttach = initial != null // ✅ فقط وقتی id داریم

    DimmedDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (initial == null) "افزودن دریافتی" else "ویرایش دریافتی",
                    style = MaterialTheme.typography.titleMedium
                )

                MyCurrencyField(
                    label = "مبلغ",
                    value = amount,
                    toman = prefs.currencyUnit == CurrencyUnit.TOMAN,
                    onValueChange = { amount = it }
                )

                DatePickerField(label = "تاریخ دریافت", epochMs = date, onPick = { date = it })
                MethodDropdown(value = method, onValueChange = { method = it })

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیح") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // ✅ Attach row
                AttachmentsRow(
                    count = attachCount,
                    enabled = true,
                    onAdd = onAddAttachment,
                    onManage = onManageAttachments
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    TextButton(onClick = {
                        val amt = amount ?: 0L
                        if (amt <= 0) { error = "مبلغ معتبر وارد کنید"; return@TextButton }
                        onConfirm(amt, date, method, note.ifBlank { null })
                    }) { Text(if (initial == null) "ثبت" else "ذخیره") }
                }
            }
        }
    }
}


@Composable
private fun AddEditExpenseDialog(
    initial: OrderExpenseEntity? = null,
    prefs: DisplayPreferences,
    attachCount: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (amountToman: Long, dateEpoch: Long, pickedCategoryId: Int?, customTitle: String?, note: String?) -> Unit,
    onAddAttachment: () -> Unit,
    onManageAttachments: () -> Unit,
    expenseCategories: List<ExpenseCategoryEntity>
)
 {
    var amount by remember { mutableStateOf(initial?.amountToman) }
    var date by remember { mutableLongStateOf(initial?.dateEpoch ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

     var selectedCategoryId by remember { mutableStateOf<Int?>(initial?.categoryId) }
     var categoryText by remember { mutableStateOf("") } // از CategoryField پر میشه

     var customCategoryMode by remember { mutableStateOf(false) }




    DimmedDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = if (initial == null) "افزودن هزینه" else "ویرایش هزینه",
                    style = MaterialTheme.typography.titleMedium
                )

                MyCurrencyField(
                    label = "مبلغ",
                    value = amount,
                    toman = prefs.currencyUnit == CurrencyUnit.TOMAN,
                    onValueChange = { amount = it }
                )

                DatePickerField(label = "تاریخ هزینه", epochMs = date, onPick = { date = it })

                // ✅ دسته هزینه (فعلاً Title را نشان می‌دهیم، انتخاب با onChooseCategory)
                CategoryField(
                    categories = expenseCategories,
                    selectedId = selectedCategoryId,
                    text = categoryText,
                    onTextChange = { categoryText = it },
                    onPickCategory = { id -> selectedCategoryId = id },
                    onClearPick = { selectedCategoryId = null }
                )



                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیح") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // ✅ پیوست‌ها
                AttachmentsRow(
                    count = attachCount,
                    enabled = true,
                    onAdd = onAddAttachment,
                    onManage = onManageAttachments
                )



                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }

                    TextButton(onClick = {
                        val amt = amount ?: 0L
                        if (amt <= 0L) { error = "مبلغ معتبر وارد کنید"; return@TextButton }

                        val customTitle = if (selectedCategoryId == null)
                            categoryText.trim().ifBlank { null }
                        else null

                        if (selectedCategoryId == null && customTitle == null) {
                            error = "دسته هزینه را وارد یا انتخاب کنید"
                            return@TextButton
                        }

                        onConfirm(amt, date, selectedCategoryId, customTitle, note.ifBlank { null })

                    }) { Text(if (initial == null) "ثبت" else "ذخیره") }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllocationSheet(
    panel: AllocationPanelState,
    prefs: DisplayPreferences,
    onClose: () -> Unit,
    onChange: (orderId: Int, amount: Long?) -> Unit,
    onSave: () -> Unit
) {
    val sumAllocated = panel.rows.sumOf { it.amount ?: 0L }
    val remaining = (panel.totalToman - sumAllocated).coerceAtLeast(0)

    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "تخصیص به سفارش‌ها",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Text("مبلغ کل: ${panel.totalToman.toCurrencyText(prefs)}")
                Text("جمع تخصیص: ${sumAllocated.toCurrencyText(prefs)}")
                Text("باقی‌مانده: ${remaining.toCurrencyText(prefs)}")
            }

            panel.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(panel.rows, key = { it.orderId }) { row ->
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Text(row.title, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))

                            MyCurrencyField(
                                label = "مبلغ تخصیص",
                                value = row.amount,
                                toman = prefs.currencyUnit == CurrencyUnit.TOMAN,
                                onValueChange = { onChange(row.orderId, it) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                ) { Text("انصراف") }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = sumAllocated <= panel.totalToman
                ) { Text("ذخیره تخصیص") }
            }

            Spacer(Modifier.navigationBarsPadding())
        }


    }
}




// --- Empty state ---
@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize()) {
        ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = text
            )
        }
    }
}




// ------- Date utils -------
@Composable
private fun DatePickerField(
    label: String,
    epochMs: Long,
    onPick: (Long) -> Unit,
    noClock: Boolean = true,              // ✅ پیش‌فرض: بدون ساعت
    dateUi: DateUi = PersianDateUiAdapter // Picker همونه
) {
    var open by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = formatPersianDateTime(epochMs, noClock = noClock),  // ✅ اینجا همسان شد
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = { open = true }) { Text("انتخاب") }
        }
    )

    if (open) {
        dateUi.Picker(
            currentEpochMs = epochMs,
            onPick = { picked ->
                onPick(picked)
                open = false // ✅ بعد از انتخاب ببند
            },
            onDismiss = { open = false }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentsManagerSheet(
    panel: AttachmentPanelState,
    onClose: () -> Unit,
    onAdd: (target: AttachTarget, ownerId: Int) -> Unit,
    onDeleteReceipt: (ReceiptAttachmentEntity) -> Unit,
    onDeleteExpense: (ExpenseAttachmentEntity) -> Unit
) {
    val context = LocalContext.current

    var showViewer by remember { mutableStateOf(false) }
    var viewerUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var viewerInitialPage by remember { mutableIntStateOf(0) }

    val viewerTitle = remember(panel.target, panel.ownerId) {
        if (panel.target == AttachTarget.Receipt) "پیوست‌های دریافتی #${panel.ownerId}"
        else "پیوست‌های هزینه #${panel.ownerId}"
    }

    // یک resolve ثابت مثل قبل
    val resolveFile: (String) -> File = remember(context) {
        { rel -> File(context.filesDir, "media").resolve(rel) }
    }

    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDeleteAny by remember { mutableStateOf<Any?>(null) }



    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Text(
                text = if (panel.target == AttachTarget.Receipt) "پیوست‌های دریافتی" else "پیوست‌های هزینه",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onAdd(panel.target, panel.ownerId) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("افزودن تصویر") }

            Spacer(Modifier.height(12.dp))

            if (panel.items.isEmpty()) {
                Text("هنوز تصویری ثبت نشده است.", style = MaterialTheme.typography.bodyMedium)
            }
            else {
                // گرید ساده از تامب‌ها
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(panel.items.size) { idx ->
                        val any = panel.items[idx]
                        when (any) {
                            is ReceiptAttachmentEntity -> AttachmentThumbCard(
                                path = any.thumbName ?: any.fileName,
                                onOpen = {
                                    val items = panel.items.filterIsInstance<ReceiptAttachmentEntity>()
                                    val clicked = any // همون ReceiptAttachmentEntity
                                    val initial = items.indexOfFirst { it.id == clicked.id }.coerceAtLeast(0)
                                    val uris = items.map { ent ->
                                        val f = resolveFile(ent.fileName) // ✅ فایل اصلی، نه thumb
                                        Uri.fromFile(f)
                                    }
                                    viewerUris = uris
                                    viewerInitialPage = initial
                                    showViewer = true
                                },

                                onDelete = {
                                    pendingDeleteAny = any
                                    confirmDelete = true
                                },
                                resolveFile = { rel ->
                                    File(
                                        context.filesDir,
                                        "media"
                                    ).resolve(rel)
                                }
                            )

                            is ExpenseAttachmentEntity -> AttachmentThumbCard(
                                path = any.thumbName ?: any.fileName,
                                onOpen = {
                                    val items = panel.items.filterIsInstance<ExpenseAttachmentEntity>()
                                    val clicked = any // همون ExpenseAttachmentEntity
                                    val initial = items.indexOfFirst { it.id == clicked.id }.coerceAtLeast(0)
                                    val uris = items.map { ent ->
                                        val f = resolveFile(ent.fileName)
                                        Uri.fromFile(f)
                                    }
                                    viewerUris = uris
                                    viewerInitialPage =initial
                                    showViewer = true
                                },

                                onDelete = {
                                    pendingDeleteAny = any
                                    confirmDelete = true
                                },
                                resolveFile = { rel ->
                                    File(
                                        context.filesDir,
                                        "media"
                                    ).resolve(rel)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
        if (showViewer && viewerUris.isNotEmpty()) {
            FullscreenImageViewer(
                title = viewerTitle,
                imageUris = viewerUris,
                initialPage = viewerInitialPage,
                onClose = { showViewer = false }
            )
        }

    }
    ConfirmDimmedDialog(
        visible = confirmDelete,
        title = "حذف تصویر",
        message = { Text("این تصویر حذف شود؟") },
        onConfirm = {
            when (val x = pendingDeleteAny) {
                is ReceiptAttachmentEntity -> onDeleteReceipt(x)
                is ExpenseAttachmentEntity -> onDeleteExpense(x)
            }
            confirmDelete = false
            pendingDeleteAny = null

        },
        onDismiss = {
            confirmDelete = false
            pendingDeleteAny = null
        }
    )

}

@Composable
private fun AttachmentThumbCard(
    path: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    resolveFile: (String) -> File
) {
    ElevatedCard(
        modifier = Modifier.size(120.dp),
        onClick = onOpen
    ) {
        Box(Modifier.fillMaxSize()) {
            val file = remember(path) { resolveFile(path) }
            // Coil
            Image(
                painter = rememberAsyncImagePainter(model = file),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف پاسخ",
                    Modifier.size(22.dp),
                    tint = Color.White
                )
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف پاسخ",
                    Modifier.size(14.dp),
                    tint = Color.Black
                )
            } }
        }
    }
}







@Composable
fun CustomerAvatarMini(
    avatarPath: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 30
) {
    val size = sizeDp.dp
    if (avatarPath.isNotBlank() && File(avatarPath).exists()) {
        AsyncImage(
            model = File(avatarPath),
            contentDescription = "آواتار مشتری",
            modifier = modifier
                .size(size)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_client_avatar_home),
            contentDescription = "آواتار مشتری",
            modifier = modifier
                .size(size)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
    }
}


@Composable
private fun ScopeRadioRow(
    scope: CostScope,
    customerAvatarPath: String,
    onSelect: (CostScope) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // گزینه: سفارش
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { onSelect(CostScope.Order) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            RadioButton(
                selected = scope == CostScope.Order,
                onClick = { onSelect(CostScope.Order) }
            )
            Spacer(Modifier.width(6.dp))
            Text("برای این سفارش")
        }

        // گزینه: مشتری (با آواتار به جای "(کلی)")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { onSelect(CostScope.Customer) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            RadioButton(
                selected = scope == CostScope.Customer,
                onClick = { onSelect(CostScope.Customer) }
            )
            Spacer(Modifier.width(6.dp))
            Text("برای مشتری")
            Spacer(Modifier.width(8.dp))
            CustomerAvatarMini(avatarPath = customerAvatarPath, sizeDp = 30)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    categories: List<ExpenseCategoryEntity>,
    selectedId: Int?,
    text: String,
    onTextChange: (String) -> Unit,
    onPickCategory: (Int) -> Unit,
    onClearPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val query = text.trim()
    val filtered = remember(categories, query) {
        if (query.isBlank()) categories
        else categories.filter { it.title.contains(query, ignoreCase = true) }
    }

    // اگر selectedId داری و text خالیه، عنوانش رو نشون بده (برای حالت edit)
    val selectedTitle = categories.firstOrNull { it.id == selectedId }?.title
    LaunchedEffect(selectedId, selectedTitle) {
        if (selectedId != null && selectedTitle != null && text.isBlank()) {
            onTextChange(selectedTitle)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { v ->
                onTextChange(v)
                // وقتی کاربر تایپ می‌کنه یعنی ممکنه دسته جدید باشه
                onClearPick()       // selectedId = null
                expanded = true
            },
            singleLine = true,
            label = { Text("دسته هزینه") },
            placeholder = { Text("مثلاً: یراق، حمل، رنگ...") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // آیتم‌های فیلتر شده
            filtered.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.title) },
                    onClick = {
                        expanded = false
                        onPickCategory(c.id)     // selectedId = c.id
                        onTextChange(c.title)    // متن هم با عنوان سینک
                    }
                )
            }

            // اگر چیزی تایپ شده و دقیقاً مشابه هیچ عنوانی نیست، یک آیتم “افزودن” نشان بده
            val existsExact = categories.any { it.title.equals(query, ignoreCase = true) }
            if (query.isNotBlank() && !existsExact) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("➕ افزودن «$query»") },
                    onClick = {
                        // اینجا چیزی در DB درج نمی‌کنیم؛ فقط می‌گذاریم موقع ذخیره انجام بشه
                        expanded = false
                        onClearPick() // selectedId = null
                    }
                )
            }
        }
    }
}








