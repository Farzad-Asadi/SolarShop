package com.example.solarShop.ui.orderScreen.orderInvoice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.InvoiceType
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceTemplateEntity
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceWithItems
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.ConfirmDialogRtl
import com.example.solarShop.utils.MyCurrencyField
import com.example.solarShop.utils.PersianDateUiAdapter
import com.example.solarShop.utils.TopBarGeneral
import com.example.solarShop.utils.formatPersianDateTime
import com.example.solarShop.utils.iranMobileVisualTransformationNew
import com.example.solarShop.utils.toCurrencyText
import kotlin.math.roundToLong


@Composable
fun OrderInvoiceScreen(
    modifier: Modifier = Modifier,
    orderId: Int?,
    onClose: () -> Unit,
    vm: OrderInvoiceViewModel = hiltViewModel()
) {

    val ui by vm.uiState.collectAsStateWithLifecycle()

    val prefs by vm.displayPrefsState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        vm.setOrderIdFromNav(orderId)
    }

    val topBarTitle = buildString {
        append("پیش فاکتور/فاکتور")
        ui.order?.name?.let { orderName ->
            append("         ")
            append(orderName)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // 👇 هندلر برگشت: اگر ادیتور باز است، فقط ادیتور را ببند، وگرنه از صفحه خارج شو
    val handleBack: () -> Unit = remember(ui.isEditorVisible) {
        {
            if (ui.isEditorVisible) {
                vm.onCloseEditor()
            } else {
                onClose()
            }
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }










    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        Scaffold(
            modifier = modifier.fillMaxSize()
                .imePadding(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopBarGeneral(
                    title = topBarTitle,
                    onBack = handleBack
                )
            },
        ) { inner ->


            Column(
                modifier = modifier
                    .padding(inner)
                    .fillMaxSize()
            ) {

                // 🔹 تب‌های بالا: انتخاب بین پیش‌فاکتور و فاکتور
                InvoiceTypeTabs(
                    currentType = ui.type,
                    onTypeSelected = { type -> vm.onInvoiceTypeTabSelected(type) }
                )

                Spacer(Modifier.height(8.dp))

                // 🔹 محتوای هر تب
                when (ui.type) {
                    InvoiceType.PROFORMA -> {
                        PreInvoiceContent(
                            ui = ui,
                            prefs = prefs,
                            onPreviewInvoicePdfClicked = vm::onPreviewInvoicePdfClicked,
                            onOpenInvoice = vm::onOpenInvoice,
                            onCreateNewFromTemplate = vm::onCreateNewFromTemplate,
                            onCreateNewDocument = vm::onCreateNewDocument,
                            onDeleteInvoice = vm::onDeleteInvoice,
                            onSaveAndPreview= { headerInput, items ->
                                vm.saveHeaderItemsAndPreview(headerInput, items)
                            }, // ✅
                            onConvertFromProforma = { templateId, _ ->
                                vm.onConvertProformaToInvoice(templateId) // ✅
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    InvoiceType.INVOICE -> {
                        InvoiceContent(
                            ui = ui,
                            prefs = prefs,
                            onPreviewInvoicePdfClicked = vm::onPreviewInvoicePdfClicked,
                            onOpenInvoice = vm::onOpenInvoice,
                            onCreateNewFromTemplate = vm::onCreateNewFromTemplate,
                            onDeleteInvoice = vm::onDeleteInvoice,
                            onSaveAndPreview= { headerInput, items ->
                                vm.saveHeaderItemsAndPreview(headerInput, items)
                            },
                            onConvertFromProforma = { templateId, _ ->
                                vm.onConvertProformaToInvoice(templateId) // ✅
                            },
                            onCreateNewDocument = vm::onCreateNewDocument,
                            modifier = Modifier.weight(1f)
                        )
                    }

                }
            }
        }
    }
}

@Composable
fun PreInvoiceContent(
    ui: OrderInvoiceUiState,
    prefs: DisplayPreferences,
    onPreviewInvoicePdfClicked: (invoiceId: Int) -> Unit,
    onOpenInvoice: (invoiceId: Int) -> Unit,
    onCreateNewFromTemplate: (templateId: Int) -> Unit,
    onCreateNewDocument: () -> Unit,
    onDeleteInvoice: (id: Int) -> Unit,
    onSaveAndPreview: (InvoiceHeaderInput, List<InvoiceItemInput>) -> Unit,
    onConvertFromProforma: (templateId: Int, proformaId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            if (ui.isEditorVisible && ui.selectedInvoiceId != null) {
                val id = ui.selectedInvoiceId
                //حالت ادیتور
                InvoiceEditorSection(
                    invoice = ui.editorInvoice,
                    prefs = prefs,
                    invoiceProducts = ui.invoiceProducts,
                    onSaveAndPreview = { headerInput, items ->
                        onSaveAndPreview(headerInput, items)
                    }
                )

            } else {
                //حالت غیر ادیتور

                val lastInvoice = ui.invoices.firstOrNull()
                LastInvoiceCard(
                    invoice = lastInvoice,
                    type = ui.type,
                    onPreviewPdf = onPreviewInvoicePdfClicked,
                    onOpenInvoice = onOpenInvoice,
                    onDeleteInvoice = { id ->
                        // اینجا ViewModel رو صدا بزن
                        onDeleteInvoice(id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )


                Spacer(Modifier.height(16.dp))

                if (ui.invoices.isEmpty()) {
                    Spacer(Modifier.height(16.dp))

                    InvoiceTemplatesCard(
                        templates = ui.templates,
                        type = ui.type,
                        onCreateNewDocument = onCreateNewDocument,
                        proformaIdForConvert = ui.proformaInvoices.firstOrNull()?.id,
                        onConvertFromProforma = { templateId, proformaId ->
                            onConvertFromProforma(templateId, proformaId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // اگر سند داریم، فقط لیست سندها را نشان بده
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "برای این سفارش یک سند ثبت شده است.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }


            }
        }
    }

}


@Composable
fun InvoiceContent(
    ui: OrderInvoiceUiState,
    prefs: DisplayPreferences,
    onPreviewInvoicePdfClicked: (invoiceId: Int) -> Unit,
    onOpenInvoice: (invoiceId: Int) -> Unit,
    onCreateNewFromTemplate: (templateId: Int) -> Unit,
    onCreateNewDocument: () -> Unit,
    onDeleteInvoice: (id: Int) -> Unit,
    onSaveAndPreview: (InvoiceHeaderInput, List<InvoiceItemInput>) -> Unit,
    onConvertFromProforma: (templateId: Int, proformaId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PreInvoiceContent(
        ui = ui,
        prefs = prefs,
        onPreviewInvoicePdfClicked = onPreviewInvoicePdfClicked,
        onOpenInvoice = onOpenInvoice,
        onCreateNewFromTemplate = onCreateNewFromTemplate,
        onDeleteInvoice = onDeleteInvoice,
        onSaveAndPreview = { headerInput, items ->
            onSaveAndPreview(headerInput, items)
        },
        onConvertFromProforma = { templateId, proformaId ->
            onConvertFromProforma(templateId, proformaId)
        },
        onCreateNewDocument = onCreateNewDocument,
        modifier = modifier
    )
}

private const val INSTALLATION_FEE_PREFIX = "حق نصب"

private fun String.normalizeInvoiceNumberText(): String {
    return this
        .replace('۰', '0')
        .replace('۱', '1')
        .replace('۲', '2')
        .replace('۳', '3')
        .replace('۴', '4')
        .replace('۵', '5')
        .replace('۶', '6')
        .replace('۷', '7')
        .replace('۸', '8')
        .replace('۹', '9')
        .replace('٠', '0')
        .replace('١', '1')
        .replace('٢', '2')
        .replace('٣', '3')
        .replace('٤', '4')
        .replace('٥', '5')
        .replace('٦', '6')
        .replace('٧', '7')
        .replace('٨', '8')
        .replace('٩', '9')
        .replace('٫', '.')
}

private fun parseInstallationPercent(description: String): String? {
    if (!description.trim().startsWith(INSTALLATION_FEE_PREFIX)) return null

    val match =
        Regex("""حق نصب\s*\(([^٪%]+)[٪%]\)""")
            .find(description)

    return match
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.normalizeInvoiceNumberText()
}

private fun buildInstallationDescription(percentText: String): String {
    val cleanPercent =
        percentText
            .normalizeInvoiceNumberText()
            .trim()
            .ifBlank { "0" }

    return "$INSTALLATION_FEE_PREFIX ($cleanPercent٪)"
}

@Composable
private fun InvoiceEditorSection(
    invoice: InvoiceWithItems?,
    prefs: DisplayPreferences,
    invoiceProducts: List<InvoiceProductPickerUi>,
    onSaveAndPreview: (InvoiceHeaderInput, List<InvoiceItemInput>) -> Unit
) {
    // اگر هنوز لود نشده
    if (invoice == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // نوع سند (فقط برای نمایش)
    val invoiceTypeLabel = remember(invoice.invoice.id) {
        when (invoice.invoice.type) {
            InvoiceType.PROFORMA -> "پیش‌فاکتور"
            InvoiceType.INVOICE -> "فاکتور"
        }
    }

    // تاریخ سند (قابل تغییر)
    var headerDateMillis by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.createdAt)
    }

    // متن نمایشی تاریخ
    val headerDateText = remember(headerDateMillis) {
        formatPersianDateTime(headerDateMillis, true)
    }


    // شماره سند
    var invoiceNumber by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.number)
    }

    // سربرگ‌های دیگر
    var sellerName by rememberSaveable(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.sellerName)
    }
    var buyerName by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.buyerName)
    }
    var notes by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.notes ?: "")
    }

    /*
 * فقط با تغییر واقعی مجموعه ردیف‌ها عوض می‌شود.
 * Recomposition معمولی Draft کاربر را Reset نمی‌کند.
 */
    val itemDraftVersion =
        remember(invoice.items) {

            invoice.items.joinToString(
                separator = "|"
            ) { item ->

                "${item.uid}:${item.updatedAt}:${item.rowIndex}"
            }
        }

    // درفت اقلام
    var itemDrafts by remember(
        invoice.invoice.id,
        itemDraftVersion
    ) {
        mutableStateOf(
            if (invoice.items.isEmpty()) {
                listOf(InvoiceItemDraft())
            } else {
                invoice.items.map { item ->
                    val installationPercent =
                        parseInstallationPercent(item.description)

                    InvoiceItemDraft(
                        id = item.id,
                        uid = item.uid,

                        description = item.description,
                        unit = item.unit ?: "",
                        quantity = item.quantity.toString(),
                        unitPrice = item.unitPrice.toString(),
                        discount = if (item.rowDiscount != 0L) item.rowDiscount else 0L,

                        isInstallationFee = installationPercent != null,
                        installationPercent = installationPercent ?: ""
                    )
                }
            }
        )
    }

    // جمع‌ها
    fun normalDraftRowSubtotal(draft: InvoiceItemDraft): Long {
        val qty =
            draft.quantity
                .normalizeInvoiceNumberText()
                .toDoubleOrNull()
                ?: return 0L

        val price =
            draft.unitPrice
                .normalizeInvoiceNumberText()
                .toLongOrNull()
                ?: return 0L

        return (qty * price)
            .toLong()
            .coerceAtLeast(0L)
    }

    fun normalDraftRowDiscount(draft: InvoiceItemDraft): Long {
        return draft.discount.coerceAtLeast(0L)
    }

    val installationBaseTotal: Long =
        itemDrafts
            .filterNot { it.isInstallationFee }
            .sumOf { draft ->
                (normalDraftRowSubtotal(draft) - normalDraftRowDiscount(draft))
                    .coerceAtLeast(0L)
            }

    fun installationFeeAmount(percentText: String): Long {
        val percent =
            percentText
                .normalizeInvoiceNumberText()
                .toDoubleOrNull()
                ?: 0.0

        return ((installationBaseTotal * percent) / 100.0)
            .roundToLong()
            .coerceAtLeast(0L)
    }

    fun draftRowSubtotal(draft: InvoiceItemDraft): Long {
        return if (draft.isInstallationFee) {
            installationFeeAmount(draft.installationPercent)
        } else {
            normalDraftRowSubtotal(draft)
        }
    }

    fun draftRowDiscount(draft: InvoiceItemDraft): Long {
        return if (draft.isInstallationFee) {
            0L
        } else {
            normalDraftRowDiscount(draft)
        }
    }

    val calculatedSubtotal =
        itemDrafts.sumOf { draftRowSubtotal(it) }

    val calculatedTotalDiscount =
        itemDrafts.sumOf { draftRowDiscount(it) }

    val calculatedTotal: Long =
        (calculatedSubtotal - calculatedTotalDiscount).coerceAtLeast(0L)

    // برچسب فروشنده
    val sellerLabelOptions = listOf(
        "نام",
        "نام و نام خانوادگی",
        "فروشنده",
        "کارگاه",
        "فروشگاه",
        "پیمانکار",
    )

    var sellerLabel by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.sellerLabel ?: "نام")
    }


    var sellerAddress by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.sellerAddress.orEmpty())
    }

    var sellerPhone by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.sellerPhone.orEmpty())
    }

    val buyerLabelOptions = listOf(
        "نام",
        "نام و نام خانوادگی",
        "مشتری",
        "کارفرما",
        "خریدار"
    )

    var buyerLabel by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.buyerLabel ?: "نام")
    }



    var buyerAddress by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.buyerAddress.orEmpty())
    }



    var buyerPhone by remember(invoice.invoice.id) {
        mutableStateOf(invoice.invoice.buyerPhone.orEmpty())
    }


    var showDatePicker by remember { mutableStateOf(false) }

    var showProductPicker by rememberSaveable {
        mutableStateOf(false)
    }

    fun InvoiceItemDraft.isBlankDraft(): Boolean {
        return id == null &&
                description.isBlank() &&
                unit.isBlank() &&
                quantity.isBlank() &&
                unitPrice.isBlank() &&
                discount == 0L
    }

    fun appendItemDraft(newDraft: InvoiceItemDraft) {
        itemDrafts =
            if (itemDrafts.size == 1 && itemDrafts.first().isBlankDraft()) {
                listOf(newDraft)
            } else {
                itemDrafts + newDraft
            }
    }

    if (showDatePicker) {
        PersianDateUiAdapter.Picker(
            currentEpochMs = headerDateMillis,
            onPick = { picked ->
                headerDateMillis = picked
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showProductPicker) {
        InvoiceProductPickerBottomSheet(
            products = invoiceProducts,
            prefs = prefs,
            onDismiss = {
                showProductPicker = false
            },
            onAddProduct = { product, price ->
                appendItemDraft(
                    InvoiceItemDraft(
                        description = product.title,
                        unit = product.unit,
                        quantity = "1",
                        unitPrice = price.toString(),
                        discount = 0L
                    )
                )

                showProductPicker = false
            }
        )
    }


    // ================== UI ==================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // 🔹 هدر ادیتور: فقط عنوان
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ویرایش سند شماره ${invoice.invoice.number}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(8.dp))

        // 🟦 دسته ۱: سربرگ
        EditorSectionCard(title = "سربرگ") {

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = invoiceTypeLabel,
                onValueChange = {},
                label = { Text("نوع سند") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = headerDateText,
                onValueChange = { },
                label = { Text("تاریخ سند") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("انتخاب تاریخ")
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = invoiceNumber,
                onValueChange = { invoiceNumber = it },
                label = { Text("شماره سند") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // 🟦 دسته ۲: فروشنده
        EditorSectionCard(title = "فروشنده") {

            Spacer(Modifier.height(10.dp))

            // ردیف: "لیست برچسب" + ":" + نام فروشنده
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                var sellerLabelMenuExpanded by remember { mutableStateOf(false) }

                // انتخاب‌گر برچسب (لیست بازشو)
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { sellerLabelMenuExpanded = true }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sellerLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = sellerLabelMenuExpanded,
                        onDismissRequest = { sellerLabelMenuExpanded = false }
                    ) {
                        sellerLabelOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    sellerLabel = option
                                    sellerLabelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // دو نقطه بین برچسب و مقدار
                Text(
                    text = ":",
                    style = MaterialTheme.typography.bodyMedium
                )

                // مقدار نام / عنوان فروشنده
                OutlinedTextField(
                    value = sellerName,
                    onValueChange = { sellerName = it },
//                    label = { Text("نام فروشنده") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))



            // تلفن فروشنده
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تلفن:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = sellerPhone,
                    onValueChange = { sellerPhone = it },
                    label = { Text("شماره تلفن فروشنده") },
                    visualTransformation = iranMobileVisualTransformationNew(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // آدرس فروشنده
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آدرس:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = sellerAddress,
                    onValueChange = { sellerAddress = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("آدرس فروشنده") }
                )
            }
        }



        Spacer(Modifier.height(16.dp))

               // 🟦 دسته ۳: خریدار
        EditorSectionCard(title = "خریدار") {

            Spacer(Modifier.height(10.dp))

            // ردیف: لیست برچسب + ":" + نام خریدار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                var buyerLabelMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { buyerLabelMenuExpanded = true }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buyerLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = buyerLabelMenuExpanded,
                        onDismissRequest = { buyerLabelMenuExpanded = false }
                    ) {
                        buyerLabelOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    buyerLabel = option
                                    buyerLabelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = ":",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = buyerName,
                    onValueChange = { buyerName = it },
//                    label = { Text("نام خریدار") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // تلفن خریدار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تلفن:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = buyerPhone,
                    onValueChange = { buyerPhone = it },
                    label = { Text("شماره تلفن خریدار") },
                    visualTransformation = iranMobileVisualTransformationNew(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // آدرس خریدار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آدرس:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = buyerAddress,
                    onValueChange = { buyerAddress = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("آدرس خریدار") }
                )
            }
        }



        Spacer(Modifier.height(16.dp))

        // 🟦 دسته ۴: محصولات / خدمات
        EditorSectionCard(title = "محصولات / خدمات") {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        showProductPicker = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("افزودن کالا")
                }

                OutlinedButton(
                    onClick = {
                        appendItemDraft(
                            InvoiceItemDraft(
                                quantity = "1",
                                unit = "عدد"
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ردیف دستی")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                enabled = itemDrafts.none { it.isInstallationFee },
                onClick = {
                    appendItemDraft(
                        InvoiceItemDraft(
                            description = buildInstallationDescription("7"),
                            unit = "درصد",
                            quantity = "1",
                            unitPrice = "",
                            discount = 0L,
                            isInstallationFee = true,
                            installationPercent = "7"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("افزودن حق نصب درصدی")
            }



            Spacer(Modifier.height(12.dp))

            itemDrafts.forEachIndexed { index, item ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "ردیف ${index + 1}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (item.isInstallationFee) {
                            Text(
                                text = "حق نصب درصدی",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = item.installationPercent,
                                onValueChange = { new ->
                                    val clean =
                                        new
                                            .normalizeInvoiceNumberText()
                                            .filter { it.isDigit() || it == '.' }
                                            .take(6)

                                    itemDrafts = itemDrafts.toMutableList().also {
                                        it[index] = it[index].copy(
                                            installationPercent = clean,
                                            description = buildInstallationDescription(clean),
                                            unit = "درصد",
                                            quantity = "1",
                                            unitPrice = "",
                                            discount = 0L
                                        )
                                    }
                                },
                                label = { Text("درصد حق نصب") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "مبنای محاسبه: ${installationBaseTotal.toCurrencyText(prefs)}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "مبلغ حق نصب: ${
                                    installationFeeAmount(item.installationPercent).toCurrencyText(prefs)
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Spacer(Modifier.height(4.dp))

                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { new ->
                                    itemDrafts = itemDrafts.toMutableList().also {
                                        it[index] = it[index].copy(description = new)
                                    }
                                },
                                label = { Text("شرح کالا / خدمات") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp),
                                maxLines = 3
                            )

                            Spacer(Modifier.height(6.dp))

                            OutlinedTextField(
                                value = item.unit,
                                onValueChange = { new ->
                                    itemDrafts = itemDrafts.toMutableList().also {
                                        it[index] = it[index].copy(unit = new)
                                    }
                                },
                                label = { Text("واحد") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(6.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = item.quantity,
                                    onValueChange = { new ->
                                        itemDrafts = itemDrafts.toMutableList().also {
                                            it[index] = it[index].copy(quantity = new)
                                        }
                                    },
                                    label = { Text("تعداد") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(6.dp))

                                MyCurrencyField(
                                    label = "فی",
                                    value = item.unitPrice.filter { it.isDigit() }.toLongOrNull(), // تومان
                                    toman = prefs.currencyUnit == CurrencyUnit.TOMAN,
                                    onValueChange = { newToman: Long? ->
                                        itemDrafts = itemDrafts.toMutableList().also { list ->
                                            list[index] = list[index].copy(
                                                unitPrice = newToman?.toString() ?: ""
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )



                                Spacer(Modifier.height(6.dp))

                                MyCurrencyField(
                                    label = "تخفیف این ردیف",
                                    value = item.discount,
                                    toman = prefs.currencyUnit == CurrencyUnit.TOMAN,
                                    onValueChange = { new ->
                                        itemDrafts = itemDrafts.toMutableList().also {
                                            it[index] = it[index].copy(discount = new ?:0L)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )


                                val rowAmount: Long =
                                    (draftRowSubtotal(item) - draftRowDiscount(item))
                                        .coerceAtLeast(0L)

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "مبلغ این ردیف: " +
                                            (rowAmount.toCurrencyText(prefs)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }



                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (itemDrafts.size > 1) {
                                TextButton(
                                    onClick = {
                                        itemDrafts = itemDrafts.toMutableList().also {
                                            it.removeAt(index)
                                        }
                                    }
                                ) {
                                    Text("حذف ردیف")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = {
                        itemDrafts = itemDrafts + InvoiceItemDraft()
                    }
                ) {
                    Text("افزودن ردیف جدید")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "جمع جزء (قبل تخفیف):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "جمع تخفیف:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "جمع نهایی:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = calculatedSubtotal.toCurrencyText(prefs),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = calculatedTotalDiscount.toCurrencyText(prefs),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = calculatedTotal.toCurrencyText(prefs),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 🟦 دسته ۵: توضیحات
        EditorSectionCard(title = "توضیحات") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("توضیحات") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 4
            )
        }

        Spacer(Modifier.height(24.dp))

        // 🔵 دکمه‌ی نهایی: ذخیره + ساخت و نمایش PDF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    // ۱) هدر
                    val header = InvoiceHeaderInput(
                        invoiceId = invoice.invoice.id,
                        type = invoice.invoice.type,
                        number = invoiceNumber.trim(),
                        dateMillis = headerDateMillis,

                        sellerLabel = sellerLabel,
                        sellerName = sellerName.trim(),
                        sellerPhone = sellerPhone.trim().ifEmpty { null },
                        sellerAddress = sellerAddress.trim().ifEmpty { null },

                        buyerLabel = buyerLabel,
                        buyerName = buyerName.trim(),
                        buyerPhone = buyerPhone.trim().ifEmpty { null },
                        buyerAddress = buyerAddress.trim().ifEmpty { null },

                        notes = notes.trim().ifEmpty { null }
                    )

                    // ۲) اقلام
                    val itemsInput = itemDrafts
                        .filter {
                            it.isInstallationFee ||
                                    it.description.isNotBlank() ||
                                    it.unitPrice.isNotBlank()
                        }
                        .map { draft ->
                            if (draft.isInstallationFee) {
                                val percent =
                                    draft.installationPercent
                                        .normalizeInvoiceNumberText()
                                        .trim()
                                        .ifBlank { "0" }

                                val amount =
                                    installationFeeAmount(percent)

                                InvoiceItemInput(
                                    id = draft.id,
                                    uid = draft.uid,

                                    description =
                                    buildInstallationDescription(percent),
                                    unit = "درصد",
                                    quantity = 1.0,
                                    unitPrice = amount,
                                    discount = 0L
                                )
                            } else {
                                val quantity =
                                    draft.quantity
                                        .normalizeInvoiceNumberText()
                                        .toDoubleOrNull()
                                        ?: 0.0

                                val unitPrice =
                                    draft.unitPrice
                                        .normalizeInvoiceNumberText()
                                        .toLongOrNull()
                                        ?: 0L

                                val discount =
                                    draft.discount

                                InvoiceItemInput(
                                    id = draft.id,
                                    uid = draft.uid,

                                    description =
                                    draft.description.trim(),
                                    unit = draft.unit.trim().ifEmpty { null },
                                    quantity = quantity,
                                    unitPrice = unitPrice,
                                    discount = discount
                                )
                            }
                        }

                    // ۳) ارسال به ViewModel
                    onSaveAndPreview(header, itemsInput)
                }
            ) {
                Text("ذخیره و نمایش PDF")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceProductPickerBottomSheet(
    products: List<InvoiceProductPickerUi>,
    prefs: DisplayPreferences,
    onDismiss: () -> Unit,
    onAddProduct: (InvoiceProductPickerUi, Long) -> Unit
) {
    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    val filteredProducts =
        remember(searchText, products) {
            val query = searchText.trim()

            if (query.isBlank()) {
                products
            } else {
                products.filter { product ->
                    product.title.contains(query, ignoreCase = true)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "افزودن کالا از فروشگاه",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("جستجوی کالا") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            if (filteredProducts.isEmpty()) {
                Text(
                    text = "کالایی پیدا نشد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            filteredProducts.take(80).forEach { product ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "واحد: ${product.unit}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                enabled = product.consumerPriceToman != null,
                                onClick = {
                                    product.consumerPriceToman?.let { price ->
                                        onAddProduct(product, price)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "مشتری\n${
                                        product.consumerPriceToman?.toCurrencyText(prefs) ?: "-"
                                    }",
                                    maxLines = 2
                                )
                            }

                            OutlinedButton(
                                enabled = product.colleaguePriceToman != null,
                                onClick = {
                                    product.colleaguePriceToman?.let { price ->
                                        onAddProduct(product, price)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "همکار\n${
                                        product.colleaguePriceToman?.toCurrencyText(prefs) ?: "-"
                                    }",
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun InvoiceTopBar(
    title: String,
    onBack: () -> Unit,
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
                .padding(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }


        }
    }


}


@Composable
fun LastInvoiceCard(
    invoice: InvoiceDocumentEntity?,
    type: InvoiceType,
    modifier: Modifier = Modifier,
    onPreviewPdf: (invoiceId: Int) -> Unit,
    onOpenInvoice: (invoiceId: Int) -> Unit,
    onDeleteInvoice: (invoiceId: Int) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }



    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // کارت اصلی
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),   // جا برای لیبل بالای کارت
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // اگه سندی نبود
                if (invoice == null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "هنوز سندی برای این سفارش ثبت نشده.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // اگه قبلاً سند داشت
                else {

                    Spacer(Modifier.height(16.dp))

                    // عنوان داخلی کارت، بر اساس نوع سند
                    val kindTitle = when (type) {
                        InvoiceType.PROFORMA -> "پیش‌فاکتور این سفارش"
                        InvoiceType.INVOICE -> "فاکتور این سفارش"
                    }

                    Text(
                        text = kindTitle,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(Modifier.height(8.dp))

                    // تاریخ نمایشی: تازه‌ترِ بین createdAt و updatedAt
                    val displayEpoch = maxOf(invoice.createdAt, invoice.updatedAt)

                    Text(
                        text = "تاریخ: ${formatPersianDateTime(displayEpoch)}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // پیش‌نمایش PDF
                        TextButton(onClick = { onPreviewPdf(invoice.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = "نمایش PDF"
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("نمایش PDF")
                        }

                        Spacer(Modifier.width(8.dp))

                        // باز کردن برای ویرایش
                        TextButton(onClick = { onOpenInvoice(invoice.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "باز کردن"
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("باز کردن")
                        }

                        Spacer(Modifier.width(8.dp))

                        // حذف سند (با دیالوگ تأیید)
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "حذف سند",
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("حذف")
                        }
                    }
                }
            }
        }

        // لیبل بالای کارت، چسبیده به لبه‌ی بالا-راست
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(end = 12.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 4.dp
        ) {
            Text(
                text = "سند قبلی این سفارش",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    // دیالوگ تأیید حذف
    ConfirmDialogRtl(
        visible = showDeleteDialog && invoice != null,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            invoice?.let { onDeleteInvoice(it.id); showDeleteDialog = false }
        },
        title = "حذف سند",
        message = "آیا از حذف این سند مطمئن هستید؟ این عملیات غیرقابل بازگشت است.",
        confirmText = "حذف",
        dismissText = "انصراف",
        confirmIsDestructive = true
    )

}


@Composable
fun InvoiceTemplatesCard(
    templates: List<InvoiceTemplateEntity>,
    type: InvoiceType,
    onCreateNewDocument: () -> Unit,
    proformaIdForConvert: Int?,
    onConvertFromProforma: (templateId: Int, proformaId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {

        // کارت اصلی
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                val firstTemplateId =
                    templates.firstOrNull()?.id ?: 0

                val canConvert =
                    type == InvoiceType.INVOICE && proformaIdForConvert != null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCreateNewDocument,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ساخت سند جدید")
                    }

                    if (canConvert) {
                        Button(
                            onClick = {
                                onConvertFromProforma(
                                    firstTemplateId,
                                    proformaIdForConvert!!
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ساخت از پیش‌فاکتور")
                        }
                    }
                }

                if (templates.isEmpty()) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "تمپلیت پیش‌فرض به‌صورت خودکار ساخته می‌شود.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // لیبل بالای کارت، چسبیده به لبه بالا-راست
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(end = 12.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 4.dp
        ) {

            val typeTitle = when (type) {
                InvoiceType.PROFORMA -> "تمپلیت‌های پیش‌فاکتور"
                InvoiceType.INVOICE -> "تمپلیت‌های فاکتور"
            }

            Text(
                text = typeTitle,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceTypeTabs(
    currentType: InvoiceType,
    onTypeSelected: (InvoiceType) -> Unit
) {
    val types = listOf(InvoiceType.PROFORMA, InvoiceType.INVOICE)
    val selectedIndex = types.indexOf(currentType)

    androidx.compose.material3.SecondaryTabRow(
        selectedTabIndex = selectedIndex
    ) {
        types.forEachIndexed { index, type ->
            val title = when (type) {
                InvoiceType.PROFORMA -> "پیش‌فاکتور"
                InvoiceType.INVOICE -> "فاکتور"
            }
            androidx.compose.material3.Tab(
                selected = index == selectedIndex,
                onClick = { onTypeSelected(type) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
private fun EditorSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                content = content
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(end = 12.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 4.dp
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

