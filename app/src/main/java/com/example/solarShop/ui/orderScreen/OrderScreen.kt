package com.example.solarShop.ui.orderScreen

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.North
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.South
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.room.tables.orderAll.OrderSummary
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemEntity
import com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep.OrderWorkflowStepEntity
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.UserWorkflowStepEntity
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.ConfirmDimmedDialog
import com.example.solarShop.utils.DimmedDialog
import com.example.solarShop.utils.NoteEditorDialog
import com.example.solarShop.utils.SnackbarController
import com.example.solarShop.utils.formatPersianDateTime
import com.example.solarShop.utils.toCurrencyText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.max

//region xxxxxx
//endregion xxxxxx

@Composable
fun OrderScreen(                               //صفحه سفارش
    modifier: Modifier = Modifier,
    onClickPriceEstimate: (Int) -> Unit,
    onClickContract: (Int) -> Unit,
    onClickCost: (Int) -> Unit,
    onClickPicture: (Int) -> Unit,
    onClickCatalog: (Int) -> Unit,
    onClickInvoice: (Int) -> Unit,
    onClickBack: () -> Unit,
    vm: OrderViewModel = hiltViewModel()
) {

    val ui by vm.uiState.collectAsStateWithLifecycle()
    val prefs by vm.displayPrefsState.collectAsStateWithLifecycle()

    // ✅ Snackbar infra (واحد)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember(snackbarHostState, scope) {
        SnackbarController(hostState = snackbarHostState, scope = scope)
    }

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }

    val workflowTemplate by vm.workflowTemplateFlow
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val orderWorkflowSteps by vm.orderWorkflowStepsFlow
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showWorkflowDialog by rememberSaveable { mutableStateOf(false) }


    val orderId = ui.currentOrderEntity?.id

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(orderId, lifecycleOwner) {
        val oid = orderId
        if (oid == null) return@DisposableEffect onDispose { }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    vm.onOrderScreenVisible(oid)
                }

                Lifecycle.Event.ON_PAUSE -> vm.onOrderScreenHidden()       // وقتی رفتی بیرون
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(orderId) {
        val oid = orderId ?: return@LaunchedEffect
        vm.debugWatchDataStore(oid)
    }


// ✅ وقتی پیشنهاد جدید آمد، فقط یک snackbar Indefinite نشان بده
    LaunchedEffect(ui.pendingSuggestion, orderId) {
        val oid = orderId ?: return@LaunchedEffect
        val sug = ui.pendingSuggestion ?: return@LaunchedEffect

        // اگر یک snackbar قبلاً باز است، ببندیم و جدید را نشان دهیم
        snackbarHostState.currentSnackbarData?.dismiss()

        val result = snackbarHostState.showSnackbar(
            message = sug.message,
            actionLabel = "بله",
            withDismissAction = true, // دکمه X
            duration = androidx.compose.material3.SnackbarDuration.Indefinite
        )

        when (result) {
            androidx.compose.material3.SnackbarResult.ActionPerformed -> {
                // ✅ کاربر گفت بله → ثبت در workflow/timeline با systemKey
                vm.onSuggestionAccepted(oid, sug.key.systemKey)
            }

            androidx.compose.material3.SnackbarResult.Dismissed -> {
                // ✅ کاربر “نه/بستن” → فقط checkpoint جلو می‌رود که تکرار نشود
                vm.onSuggestionDismissed(oid)
            }
        }
    }






    Scaffold(
        topBar = {
            ui.currentOrderEntity?.let { order ->
                OrderTopBar(
                    order = order,
                    onClickBack = onClickBack,
                    onClickTitle = {
                        newName = order.name.orEmpty()
                        showRenameDialog = true
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        OrderContent(
            order = ui.currentOrderEntity,
            timeline = ui.currentOrderWithTimelineItem?.timelineItemEntity ?: listOf(),
            orderSummary = ui.currentOrderSummary,
            prefs = prefs,
            onClickCatalog = { onClickCatalog(ui.currentOrderEntity?.id ?: -1) },
            onClickShowPriceEstimateWindow = {
                onClickPriceEstimate(ui.currentOrderEntity?.id ?: -1)
            },
            onClickContract = { onClickContract(ui.currentOrderEntity?.id ?: -1) },
            onClickCosts = { onClickCost(ui.currentOrderEntity?.id ?: -1) },
            onClickPicture = { onClickPicture(ui.currentOrderEntity?.id ?: -1) },
            onClickFacture = { onClickInvoice(ui.currentOrderEntity?.id ?: -1) },
            modifier = Modifier.padding(innerPadding),
            progressPercent = ui.progressPercent,
            onClickUpdateStatus = { showWorkflowDialog = true },
            onSaveNote = { note ->
                vm.onSaveOrderNote(note)
                snackbar.show("یادداشت سفارش ذخیره شد")
            },
            snackbar = snackbar
        )
    }
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("ویرایش نام سفارش") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("نام سفارش") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.onRenameOrder(newName)
                        showRenameDialog = false
                        snackbar.show("نام سفارش ذخیره شد")
                    }
                ) {
                    Text("تأیید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
    if (showWorkflowDialog) {
        WorkflowStatusDialog(
            template = workflowTemplate,
            orderSteps = orderWorkflowSteps,
            onApply = { stepId ->
                vm.onApplyWorkflowStatus(stepId)
                showWorkflowDialog = false
                snackbar.show("وضعیت سفارش به‌روزرسانی شد")
            },
            onDismiss = { showWorkflowDialog = false },
            onSubmitEdits = { drafts, deletedIds ->
                vm.onSubmitWorkflowEdits(drafts, deletedIds)
                snackbar.show("مراحل ذخیره شد")
            }
        )
    }


}

//region Sheets
//endregion Sheets


//region Components
//endregion Components


//region Utils
//endregion Utils


/* --------------- mainScreen --------------- */

@Composable
fun OrderContent(
    order: OrderEntity?,
    timeline: List<TimelineItemEntity>,
    orderSummary: OrderSummary?,
    prefs: DisplayPreferences,
    onClickCatalog: () -> Unit,
    onClickShowPriceEstimateWindow: () -> Unit,
    onClickContract: () -> Unit,
    onClickCosts: () -> Unit,
    onClickPicture: () -> Unit,
    onClickFacture: () -> Unit,
    modifier: Modifier,
    progressPercent: Int,
    onClickUpdateStatus: () -> Unit,
    onSaveNote: (String) -> Unit,
    snackbar: SnackbarController,
) {
    val progressValue = progressPercent.coerceIn(0, 100)


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        /* ---- Order Parameter Card ---- */
        item {
            OrderDetailsQuickGrid(
                order = order,
                orderSummary = orderSummary,
                prefs = prefs,
                onClickShowPriceEstimateWindow = { onClickShowPriceEstimateWindow() },
                onClickCatalog = { onClickCatalog() },
                onClickContract = { onClickContract() },
                onClickCosts = { onClickCosts() },
                onClickPicture = onClickPicture,
                onClickFacture = onClickFacture,
                onSaveNote = { onSaveNote(it) }
            )
        }

        /* ---- Actions ---- */
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onClickUpdateStatus() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("به روز رسانی وضعیت") }


            }
        }

        /* ---- Timeline Card ---- */
        item {
            Card {
                Column(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    //پروگرس بار
                    Column(
                        Modifier.padding(8.dp),
                    ) {

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "پیشرفت پروژه",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "$progressValue%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressValue / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                        )

                    }


                    // ناحیه‌ی تایم‌لاین با اسکرول داخلی
                    TimelineCard(timeline = timeline)


                }
            }
        }


    }
}


@Composable
private fun TimelineCard(
    timeline: List<TimelineItemEntity>,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var newestOnTop by rememberSaveable { mutableStateOf(true) }

    val shownTimeline = remember(timeline, newestOnTop) {
        if (newestOnTop) timeline.sortedByDescending { it.date }
        else timeline.sortedBy { it.date }
    }

    // ✅ برای تشخیص آیتم جدید
    var prevKeys by remember { mutableStateOf<List<Any>>(emptyList()) }

    // ✅ هر وقت لیست تغییر کرد، اگر آیتم جدید اضافه شد برو سراغش
    LaunchedEffect(shownTimeline) {
        val newKeys = shownTimeline.map { it.id ?: "${it.orderId}_${it.date}" }

        // اگر فقط اضافه شدن اتفاق افتاده (نه حذف/جایگزینی کلی)
        if (newKeys.size > prevKeys.size && prevKeys.isNotEmpty()) {
            // اولین کلیدی که قبلاً نبود = آیتم جدید
            val addedKey = newKeys.firstOrNull { it !in prevKeys }
            val addedIndex = addedKey?.let { k -> newKeys.indexOf(k) } ?: -1

            if (addedIndex >= 0) {
                // اسکرول کن که آیتم جدید دیده بشه
                listState.animateScrollToItem(addedIndex)
            } else {
                // fallback: اگر نشد پیدا کنیم
                if (newestOnTop) listState.animateScrollToItem(0)
                else listState.animateScrollToItem((shownTimeline.size - 1).coerceAtLeast(0))
            }
        }

        prevKeys = newKeys
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Box(contentAlignment = Alignment.CenterEnd) {
            HorizontalDivider(Modifier.padding(4.dp))
            IconButton(
                onClick = {
                    newestOnTop = !newestOnTop
                    scope.launch {
                        if (shownTimeline.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (!newestOnTop) Icons.Outlined.South else Icons.Outlined.North,
                    contentDescription = "Toggle sort"
                )
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = shownTimeline,
                key = { it.id ?: ((it.orderId ?: 0) * 1_000_000 + it.date.toInt()) }
            ) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = formatPersianDateTime(item.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}



/* --------------- TopBars --------------- */

@Composable
private fun OrderTopBar(
    order: OrderEntity,
    onClickBack: () -> Unit,
    onClickTitle: () -> Unit
) {

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp   // فقط گوشه‌های پایین
        ),
        color = BambooTheme.sections.topBarContainer,
        contentColor = BambooTheme.sections.topBarContent,
        tonalElevation = 0.dp,
//        shadowElevation = 6.dp

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.padding(end = 12.dp),
                onClick = { onClickBack() },
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "Catalog Q&A")
            }

            // نام سفارش
            Row(
                modifier = Modifier.clickable { onClickTitle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = order.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
//            Spacer(modifier = Modifier.width(40.dp))
//            Text(
//                text = ":سفارش",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.SemiBold
//            )
            Spacer(modifier = Modifier.weight(1f))


        }
    }
}


/* --------------- BottomBar --------------- */


/* --------------- temporalWindows --------------- */


/* --------------- Pieces --------------- */


@Composable
fun OrderDetailsQuickGrid(
    order: OrderEntity?,
    orderSummary: OrderSummary?,
    prefs: DisplayPreferences,
    modifier: Modifier = Modifier,
    onClickShowPriceEstimateWindow: () -> Unit,
    onClickCatalog: () -> Unit,
    onClickContract: () -> Unit,
    onClickCosts: () -> Unit,
    onClickPicture: () -> Unit,
    onClickFacture: () -> Unit,
    onSaveNote: (String) -> Unit,
) {

    var noteDialogVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // یادداشت سفارش
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { noteDialogVisible = true }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.EditNote, // اگر آیکون مناسب‌تری داری عوض کن
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "یادداشت سفارش",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))

        }


        val hPF = orderSummary?.hasPreFacture
        val hF = orderSummary?.hasFacture


        // گرید دسترسی سریع
        val tiles = listOf(
            QuickTileModel(
                title = "تخمین قیمت",
                subtitle = orderSummary?.priceEstimateTotal?.toCurrencyText(prefs) ?: "0",
                icon = Icons.Outlined.AttachMoney,
                onClick = onClickShowPriceEstimateWindow,
                accent = MaterialTheme.colorScheme.primary
            ),
            QuickTileModel(
                title = "کاتالوگ",
                subtitle = "پاسخ داده‌شده: ${orderSummary?.catalogSelectedCount ?: 0}",
                icon = Icons.Outlined.CalendarToday,
                onClick = onClickCatalog,
                accent = MaterialTheme.colorScheme.tertiary
            ),
            QuickTileModel(
                title = "قرارداد",
                subtitle = if (orderSummary?.hasContract == true) "قرارداد تنظیم شده" else "قرارداد تنظیم نشده",
                icon = Icons.Outlined.Contacts,
                onClick = onClickContract,
                accent = MaterialTheme.colorScheme.secondary
            ),
            QuickTileModel(
                title = "دریافت/پرداخت",
                subtitle = orderSummary?.costResult?.toCurrencyText(prefs) ?: "0",
                icon = Icons.Outlined.Money,
                onClick = onClickCosts,
                accent = MaterialTheme.colorScheme.error
            ),
            QuickTileModel(
                title = "عکسهای پروژه",
                subtitle = "در گالری: ${orderSummary?.galleryPhotoCount ?: 0} - پین شده: ${orderSummary?.pinnedPhotoCount ?: 0}",
                icon = Icons.Outlined.PictureInPicture,
                onClick = onClickPicture,
                accent = MaterialTheme.colorScheme.error
            ),

            QuickTileModel(
                title = "پیش/فاکتور",
                subtitle =
                when {
                    hPF == true && hF == true -> "پیش فاکتور و فاکتور دارد"
                    hPF == false && hF == false -> "پیش فاکتور و فاکتور ندارد"
                    hPF == true && hF == false -> "پیش فاکتور دارد"
                    hPF == false && hF == true -> "فاکتور دارد"
                    else -> {
                        ""
                    }
                },
                icon = Icons.Outlined.RequestQuote,
                onClick = onClickFacture,
                accent = MaterialTheme.colorScheme.error
            ),
        )
        val rows = remember(tiles.size, 2) {
            ((tiles.size + 2 - 1) / 2).coerceAtLeast(1)
        }
        val gridHeight = 80.dp * rows + 12.dp * (rows - 1)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
        ) {
            items(tiles) { t ->

                QuickActionTile(
                    title = t.title,
                    subtitle = t.subtitle,
                    icon = t.icon,
                    onClick = t.onClick,
                    accent = t.accent,
                )
            }
        }
    }

    // ✅ دیالوگ ویرایش/مشاهده نوت
    NoteEditorDialog(
        visible = noteDialogVisible,
        label = "یادداشت سفارش",
        initialText = order?.note.orEmpty(),
        onDismiss = { noteDialogVisible = false },
        onSave = { newNote ->
            onSaveNote(newNote)
        }
    )

}

@Composable
fun QuickActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Color,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }


            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

        }
    }
}


@Composable
fun QuestionHeaderCard(
    title: String,
    modifier: Modifier = Modifier,
    onClickEdit: (() -> Unit)? = null,
    onClickOpenTree: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // سمت چپ عنوان → آیکون ادیت
            if (onClickEdit != null) {
                IconButton(onClick = onClickEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "ویرایش سؤال"
                    )
                }
            }


            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // سمت راست عنوان → آیکون گراف/درخت
            if (onClickOpenTree != null) {
                IconButton(onClick = onClickOpenTree) {
                    Icon(
                        imageVector = Icons.Filled.AccountTree, // موقت؛ اینجا هر آیکون گراف‌مانندی که دوست داری بگذار
                        contentDescription = "نمایش درخت سؤال"
                    )
                }
            }

        }
    }
}


/* --------------- Utils --------------- */


// ---- Components --------------------------------------------------------------


@Composable
fun WorkflowStatusDialog(
    template: List<UserWorkflowStepEntity>,
    orderSteps: List<OrderWorkflowStepEntity>,   // فعلاً اینجا استفاده نمی‌کنیم
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSubmitEdits: (List<DraftWorkflowStep>, List<Int>) -> Unit = { _, _ -> }

) {
    var selectedStepId by remember { mutableStateOf<Int?>(null) }

    // ✅ مود نمایش/ویرایش
    var editMode by rememberSaveable { mutableStateOf(false) }

    // ✅ لیست ادیت فقط یک منبع حقیقت دارد (draftsState)
    val draftsState = remember { mutableStateOf<List<DraftWorkflowStep>>(emptyList()) }

    // ✅ برای درگ/ری‌اوردر
    var dragging by remember { mutableStateOf(false) }

    // ✅ نگهداری متن درصد برای تجربه تایپ بهتر
    val weightTextMap = remember { mutableStateMapOf<Int, String>() }
    val weightFocusedMap = remember { mutableStateMapOf<Int, Boolean>() }

    // ✅ وقتی وارد editMode می‌شویم (یا template عوض شد) draftها را از template بساز
    LaunchedEffect(editMode, template) {
        if (!editMode) return@LaunchedEffect
        if (dragging) return@LaunchedEffect

        val newDrafts = template.map {
            DraftWorkflowStep(
                id = it.id,
                title = it.title,
                weightPercent = it.weightPercent,
                sortOrder = it.sortOrder,
                isLocked = it.isLocked
            )
        }
        draftsState.value = newDrafts

        // init weight text map
        newDrafts.forEach { d ->
            val id = d.id ?: return@forEach
            if (!weightTextMap.containsKey(id)) {
                weightTextMap[id] = if (d.weightPercent == 0) "" else d.weightPercent.toString()
            }
        }
    }

    // MODE 1 list state
    val listState = rememberLazyListState()

    // ✅ بک‌دراپ تیره مثل DimmedDialog
    DimmedDialog(
        onDismiss = onDismiss,
        dismissOnBackdropClick = true
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // --- Title Row + Edit/Close ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!editMode) "به‌روزرسانی وضعیت سفارش" else "ویرایش مراحل وضعیت",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit workflow steps"
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // --- Percent Summary (بالا) ---
                val totalView = remember(template) { template.sumOf { it.weightPercent } }
                val totalEditTop = draftsState.value.sumOf { it.weightPercent }
                val totalTop = if (!editMode) totalView else totalEditTop

                Text(
                    text = "مجموع درصدها: $totalTop٪",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (totalTop == 100) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )

                HorizontalDivider()

                if (!editMode) {
                    // =======================
                    // MODE 1: انتخاب یک مرحله و Apply
                    // =======================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(template, key = { it.id ?: it.title.hashCode() }) { step ->
                                val stepId = step.id
                                if (stepId != null) {
                                    val checked = (selectedStepId == stepId)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = checked,
                                            onClick = { selectedStepId = stepId }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = step.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "${step.weightPercent}٪",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        // ✅ اسکرول‌بار روی لیست
                        VerticalScrollbar(
                            state = listState,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("بستن") }

                        TextButton(
                            onClick = { selectedStepId?.let(onApply) },
                            enabled = selectedStepId != null
                        ) { Text("اعمال") }
                    }

                } else {
                    var confirmDeleteVisible by rememberSaveable { mutableStateOf(false) }
                    var pendingDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }


                    // --- Reorder state ---
                    val reorderState = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            dragging = true
                            val list = draftsState.value.toMutableList()
                            val item = list.removeAt(from.index)
                            list.add(to.index.coerceIn(0, list.size), item)
                            draftsState.value = list
                        },
                        onDragEnd = { _, _ ->
                            dragging = false
                        }
                    )

                    // ✅ اسکرول روی آیتم جدید
                    var scrollToId by remember { mutableStateOf<Int?>(null) }
                    LaunchedEffect(scrollToId, draftsState.value.size) {
                        val id = scrollToId ?: return@LaunchedEffect
                        delay(50)
                        val index = draftsState.value.indexOfFirst { it.id == id }
                        if (index >= 0) reorderState.listState.animateScrollToItem(index)
                        scrollToId = null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        LazyColumn(
                            state = reorderState.listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .reorderable(reorderState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = draftsState.value,
                                key = { it.id ?: it.hashCode() }
                            ) { d ->
                                val id = d.id ?: return@items

                                ReorderableItem(
                                    state = reorderState,
                                    key = id
                                ) { isDragging ->

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        border = CardDefaults.outlinedCardBorder(true),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDragging)
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        elevation = CardDefaults.cardElevation(if (isDragging) 6.dp else 1.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.Start
                                        ) {

                                            // ✅ Handle + Delete

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .detectReorderAfterLongPress(reorderState),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {


                                                Spacer(Modifier.weight(1f))

                                                IconButton(
                                                    enabled = !d.isLocked,
                                                    onClick = {
                                                        pendingDeleteId = (id)
                                                        confirmDeleteVisible = true
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete step",
                                                        tint = if (d.isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.3f
                                                        )
                                                        else MaterialTheme.colorScheme.error
                                                    )
                                                }

                                                Spacer(Modifier.width(20.dp))

                                                Icon(
                                                    imageVector = Icons.Outlined.DragHandle,
                                                    contentDescription = "Reorder",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }


                                            // ✅ Title + Weight in one row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = d.title,
                                                    onValueChange = { newTitle ->
                                                        if (!d.isLocked) {
                                                            draftsState.value =
                                                                draftsState.value.map {
                                                                    if (it.id == d.id) it.copy(title = newTitle) else it
                                                                }
                                                        }
                                                    },
                                                    label = { Text("عنوان") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    enabled = !d.isLocked,           // ✅ قفل => غیرفعال
                                                    readOnly = d.isLocked            // ✅ حتی اگر enabled اشتباه شد، باز هم امن‌تره
                                                )


                                                // init text
                                                LaunchedEffect(id) {
                                                    if (!weightTextMap.containsKey(id)) {
                                                        weightTextMap[id] =
                                                            if (d.weightPercent == 0) "" else d.weightPercent.toString()
                                                    }
                                                }
                                                val weightText = weightTextMap[id].orEmpty()

                                                OutlinedTextField(
                                                    value = weightText,
                                                    onValueChange = { raw ->
                                                        val digits =
                                                            raw.filter(Char::isDigit).take(3)
                                                        weightTextMap[id] = digits
                                                        val n = digits.toIntOrNull() ?: 0

                                                        draftsState.value = draftsState.value.map {
                                                            if (it.id == id) it.copy(
                                                                weightPercent = n.coerceIn(
                                                                    0,
                                                                    100
                                                                )
                                                            )
                                                            else it
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .width(70.dp)
                                                        .onFocusChanged { fs ->
                                                            weightFocusedMap[id] = fs.isFocused

                                                            if (fs.isFocused) {
                                                                if (weightTextMap[id] == "0") weightTextMap[id] =
                                                                    ""
                                                            } else {
                                                                if (weightTextMap[id].isNullOrBlank()) {
                                                                    weightTextMap[id] = "0"
                                                                    draftsState.value =
                                                                        draftsState.value.map {
                                                                            if (it.id == id) it.copy(
                                                                                weightPercent = 0
                                                                            ) else it
                                                                        }
                                                                }
                                                            }
                                                        },
                                                    label = { Text("درصد") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        VerticalScrollbar(
                            state = reorderState.listState,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    // ✅ total + validation
                    val totalEdit = draftsState.value.sumOf { it.weightPercent }
                    val canSave = (totalEdit == 100)

//                    Text(
//                        text = "مجموع درصدها: $totalEdit٪",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = if (canSave) MaterialTheme.colorScheme.primary
//                        else MaterialTheme.colorScheme.error
//                    )

                    if (!canSave) {
                        Text(
                            text = "برای ذخیره، مجموع درصدها باید دقیقاً 100 باشد.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Add new
                    OutlinedButton(
                        onClick = {
                            val minId = draftsState.value.minOfOrNull { it.id ?: 0 } ?: 0
                            val newId = minId - 1

                            val newItem = DraftWorkflowStep(
                                id = newId,
                                title = "مرحله جدید",
                                weightPercent = 0
                            )
                            draftsState.value += newItem

                            weightTextMap[newId] = ""
                            scrollToId = newId
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("افزودن مرحله جدید") }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                // خروج بدون اعمال
                                draftsState.value = template.map {
                                    DraftWorkflowStep(it.id, it.title, it.weightPercent)
                                }
                                editMode = false
                            }
                        ) { Text("انصراف") }

                        TextButton(
                            enabled = canSave,
                            onClick = {
                                val normalized = draftsState.value
                                    .mapIndexed { index, d -> d.copy(sortOrder = index) }

                                val oldIds = template.mapNotNull { it.id }.filter { it > 0 }.toSet()
                                val newIds =
                                    normalized.mapNotNull { it.id }.filter { it > 0 }.toSet()
                                val deletedIds = (oldIds - newIds).toList()

                                onSubmitEdits(normalized, deletedIds)   // ✅
                                editMode = false
                            }
                        ) { Text("ذخیره تغییرات") }
                    }

                    //تایید حذف
                    ConfirmDimmedDialog(
                        visible = confirmDeleteVisible,
                        title = "حذف مرحله",
                        message = {
                            val id = pendingDeleteId
                            val title =
                                draftsState.value.firstOrNull { it.id == id }?.title.orEmpty()
                            Text("این مرحله حذف شود؟\n$title")
                        },
                        confirmText = "بله، حذف شود",
                        dismissText = "انصراف",
                        onConfirm = {
                            val id = pendingDeleteId ?: return@ConfirmDimmedDialog

                            // حذف از لیست UI
                            draftsState.value = draftsState.value.filterNot { it.id == id }

                            // تمیزکاری استیت‌های جانبی
                            weightTextMap.remove(id)
                            weightFocusedMap.remove(id)

                            // sync به drafts اصلی (اگه می‌خوای همین لحظه همگام باشه)
//                            drafts = draftsState.value

                            // بستن
                            pendingDeleteId = null
                            confirmDeleteVisible = false
                        },
                        onDismiss = {
                            pendingDeleteId = null
                            confirmDeleteVisible = false
                        }
                    )

                }
            }
        }
    }
}


@SuppressLint("FrequentlyChangingValue")
@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    thicknessPx: Float = 6f,
    paddingPx: Float = 2f,
    minThumbHeightPx: Float = 24f,
    alpha: Float = 0.45f,
) {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount

    if (totalItems <= 0 || visibleItems.isEmpty()) return

    val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    if (viewportHeightPx <= 0f) return

    // اگر همه آیتم‌ها جا میشن، اسکرول‌بار لازم نیست
    if (visibleItems.size >= totalItems && state.firstVisibleItemIndex == 0) return

    // ✅ میانگین ارتفاع آیتم‌های قابل مشاهده (برای تخمین دقیق‌تر پیکسل)
    val avgItemSizePx = visibleItems.map { it.size }.average().toFloat().coerceAtLeast(1f)

    // ✅ کل طول محتوا (تقریبی)
    val totalContentHeightPx = totalItems * avgItemSizePx

    // ✅ رنج اسکرول
    val scrollRangePx = (totalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)

    // ✅ اسکرول فعلی به پیکسل (ایندکس * میانگین + آفست پیکسلی)
    val currentScrollPx =
        (state.firstVisibleItemIndex * avgItemSizePx + state.firstVisibleItemScrollOffset)
            .coerceIn(0f, scrollRangePx)

    // ✅ ارتفاع thumb: نسبت ویوپورت به کل محتوا
    val rawThumbHeight = viewportHeightPx * (viewportHeightPx / totalContentHeightPx)
    val thumbHeightPx = max(minThumbHeightPx, rawThumbHeight)

    val maxThumbTopPx = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f)
    val rawThumbTopPx = (currentScrollPx / scrollRangePx) * maxThumbTopPx

    // ✅ انیمیشن نرم
    val thumbTopPx by animateFloatAsState(
        targetValue = rawThumbTopPx,
        animationSpec = tween(durationMillis = 120),
        label = "thumbTop"
    )

    Canvas(modifier = modifier) {
        val x = size.width - thicknessPx - paddingPx
        val y = thumbTopPx + paddingPx

        drawRoundRect(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(x, y),
            size = Size(thicknessPx, thumbHeightPx - paddingPx * 2),
            cornerRadius = CornerRadius(thicknessPx, thicknessPx)
        )
    }
}




