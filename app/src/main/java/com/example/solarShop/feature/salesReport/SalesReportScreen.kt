package com.example.solarShop.feature.salesReport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solarShop.domain.sales.PersianMonthPeriod
import com.example.solarShop.utils.currency.toPriceString
import com.example.solarShop.utils.formatPersianDateTime
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    onBack: () -> Unit,
    viewModel: SalesReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember {
        mutableStateOf<SalesReportEntry?>(null)
    }
    var showFinalDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    pendingDelete?.let { entry ->
        if (showFinalDeleteConfirmation) {
            FinalDeleteSalesEntryDialog(
                entry = entry,
                onDismiss = {
                    pendingDelete = null
                    showFinalDeleteConfirmation = false
                },
                onConfirm = {
                    viewModel.deleteEntry(entry)
                    pendingDelete = null
                    showFinalDeleteConfirmation = false
                }
            )
        } else {
            FirstDeleteSalesEntryDialog(
                entry = entry,
                onDismiss = {
                    pendingDelete = null
                },
                onContinue = {
                    showFinalDeleteConfirmation = true
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گزارش فروش") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SalesSummaryCard(uiState.summary)
                }

                item {
                    SalesFiltersCard(
                        uiState = uiState,
                        onMonthSelected = viewModel::onMonthSelected,
                        onProductSelected = viewModel::onProductSelected,
                        onSearchChanged = viewModel::onSearchQueryChanged,
                        onSortSelected = viewModel::onSortOptionSelected
                    )
                }

                if (uiState.entries.isEmpty()) {
                    item { SalesReportEmptyState() }
                } else {
                    items(
                        items = uiState.entries,
                        key = { entry -> entry.stableId }
                    ) { entry ->
                        SalesEntryCard(
                            entry = entry,
                            onDelete = {
                                pendingDelete = entry
                                showFinalDeleteConfirmation = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesSummaryCard(summary: SalesReportSummary) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "خلاصه فروش",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${summary.totalSalesToman.toPriceString()} تومان",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SummaryLine("تعداد رکوردهای فروش", summary.saleCount.toString())
            SummaryLine("مجموع تعداد فروخته‌شده", formatQuantity(summary.soldQuantity))
            SummaryLine(
                title = "برگشت از فروش",
                value = "${summary.returnCount} رکورد ـ ${formatQuantity(summary.returnedQuantity)} عدد"
            )

            if (summary.returnCount > 0) {
                HorizontalDivider()
                Text(
                    text = "مبلغ برگشت‌ها در ساختار فعلی ثبت نشده و از مبلغ فروش بالا کسر نشده است.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesFiltersCard(
    uiState: SalesReportUiState,
    onMonthSelected: (PersianMonthPeriod) -> Unit,
    onProductSelected: (Int?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortSelected: (SalesReportSortOption) -> Unit
) {
    val productOptions =
        listOf(ReportDropdownOption<Int?>(null, "همه کالاها")) +
                uiState.availableProducts.map { product ->
                    ReportDropdownOption<Int?>(product.productId, product.title)
                }

    val selectedProductTitle =
        productOptions.firstOrNull { option ->
            option.value == uiState.selectedProductId
        }?.title ?: "همه کالاها"

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "فیلتر و مرتب‌سازی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ReportDropdownField(
                label = "ماه",
                selectedTitle = uiState.selectedMonth.title,
                options = uiState.availableMonths.map { month ->
                    ReportDropdownOption(month, month.title)
                },
                onSelected = onMonthSelected
            )

            ReportDropdownField(
                label = "کالا",
                selectedTitle = selectedProductTitle,
                options = productOptions,
                onSelected = onProductSelected
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("جستجوی نام یا مدل کالا") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                singleLine = true
            )

            ReportDropdownField(
                label = "مرتب‌سازی",
                selectedTitle = uiState.sortOption.title,
                options = SalesReportSortOption.entries.map { option ->
                    ReportDropdownOption(option, option.title)
                },
                onSelected = onSortSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ReportDropdownField(
    label: String,
    selectedTitle: String,
    options: List<ReportDropdownOption<T>>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedTitle,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        expanded = false
                        onSelected(option.value)
                    }
                )
            }
        }
    }
}

@Composable
private fun SalesEntryCard(
    entry: SalesReportEntry,
    onDelete: () -> Unit
) {
    val isSale = entry.type == SalesReportEntryType.SALE

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSale) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = entry.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.productModel.isNotBlank()) {
                        Text(
                            text = "مدل: ${entry.productModel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SaleTypeBadge(isSale)
            }

            Text(
                text = formatPersianDateTime(entry.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SummaryLine("تعداد", formatQuantity(entry.quantity))

            if (isSale) {
                SummaryLine(
                    "قیمت واحد",
                    "${entry.unitSalePriceToman.toPriceString()} تومان"
                )
                SummaryLine(
                    "جمع فروش",
                    "${entry.totalSalePriceToman.toPriceString()} تومان"
                )
                entry.priceType?.let { priceType ->
                    SummaryLine("نوع قیمت", priceTypeTitle(priceType))
                }
            } else {
                Text(
                    text = "برای این برگشت فقط تعداد ثبت شده و مبلغ مالی در دسترس نیست.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (entry.note.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = "یادداشت: ${entry.note}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text("حذف این رکورد")
            }
        }
    }
}

@Composable
private fun FirstDeleteSalesEntryDialog(
    entry: SalesReportEntry,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("حذف رکورد")
        },
        text = {
            Text(
                "آیا می‌خواهید رکورد ${entry.productName} حذف شود؟"
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    text = "ادامه",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun FinalDeleteSalesEntryDialog(
    entry: SalesReportEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "آیا کاملاً مطمئن هستید؟",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                if (entry.type == SalesReportEntryType.SALE) {
                    "این عملیات قابل برگشت نیست. رکورد فروش و تراکنش موجودی متصل به آن حذف می‌شوند و پس از Sync از سرور و دستگاه‌های دیگر نیز حذف خواهند شد."
                } else {
                    "این عملیات قابل برگشت نیست. رکورد برگشت از فروش حذف می‌شود و پس از Sync از سرور و دستگاه‌های دیگر نیز حذف خواهد شد."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "حذف قطعی",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun SaleTypeBadge(isSale: Boolean) {
    Surface(
        color = if (isSale) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        contentColor = if (isSale) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onTertiary
        },
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = if (isSale) "فروش" else "برگشت از فروش",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SummaryLine(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SalesReportEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "فروشی مطابق فیلترها پیدا نشد",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "ماه، کالا یا عبارت جستجو را تغییر بده.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class ReportDropdownOption<T>(
    val value: T,
    val title: String
)

// تعداد صحیح را بدون اعشار و تعداد کسری را حداکثر با سه رقم اعشار نمایش می‌دهد.
private fun formatQuantity(quantity: Double): String =
    DecimalFormat("#,##0.###").format(quantity)

// کد ذخیره‌شده نوع قیمت را به عنوان فارسی تبدیل می‌کند.
private fun priceTypeTitle(priceType: String): String =
    when (priceType.lowercase()) {
        "consumer" -> "مصرف‌کننده"
        "colleague" -> "همکار"
        "manual" -> "دستی"
        else -> priceType
    }
