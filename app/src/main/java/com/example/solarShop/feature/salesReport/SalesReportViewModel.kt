package com.example.solarShop.feature.salesReport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.sales.ProductSaleTransactionEntity
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.sales.ProductSaleTransactionRepository
import com.example.solarShop.data.sync.SyncManager
import com.example.solarShop.domain.sales.PersianMonthPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SalesReportViewModel @Inject constructor(
    productRepository: ProductRepository,
    private val productSaleTransactionRepository: ProductSaleTransactionRepository,
    private val inventoryRepository: InventoryRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val filters =
        MutableStateFlow(SalesReportFilters())

    private val rawData =
        combine(
            productRepository.observeProductsForReports(),
            productSaleTransactionRepository.observeAllSaleTransactions(),
            inventoryRepository.observeAllTransactions()
        ) { products, sales, inventoryTransactions ->
            SalesReportRawData(
                products = products,
                sales = sales,
                saleReturns = inventoryTransactions.filter { transaction ->
                    transaction.transactionType == InventoryTransactionType.SALE_RETURN
                }
            )
        }

    val uiState: StateFlow<SalesReportUiState> =
        combine(
            rawData,
            filters
        ) { data, currentFilters ->
            buildUiState(
                data = data,
                filters = currentFilters
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SalesReportUiState()
            )

    // ماه انتخاب‌شده را تغییر می‌دهد و فیلتر کالای قبلی را پاک می‌کند.
    fun onMonthSelected(month: PersianMonthPeriod) {
        filters.update {
            it.copy(
                selectedMonth = month,
                selectedProductId = null
            )
        }
    }

    // گزارش را به یک کالای مشخص محدود می‌کند؛ null یعنی همه کالاها.
    fun onProductSelected(productId: Int?) {
        filters.update {
            it.copy(selectedProductId = productId)
        }
    }

    // عبارت جستجوی نام یا مدل کالا را به‌روزرسانی می‌کند.
    fun onSearchQueryChanged(query: String) {
        filters.update {
            it.copy(searchQuery = query)
        }
    }

    // ترتیب نمایش ردیف‌های گزارش را تغییر می‌دهد.
    fun onSortOptionSelected(option: SalesReportSortOption) {
        filters.update {
            it.copy(sortOption = option)
        }
    }

    // رکورد گزارش و تراکنش موجودی متصل به آن را Soft Delete و برای Sync آماده می‌کند.
    fun deleteEntry(entry: SalesReportEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            when (entry.type) {
                SalesReportEntryType.SALE -> {
                    productSaleTransactionRepository.softDeleteByUid(
                        entry.sourceUid
                    )

                    entry.linkedInventoryTransactionUid?.let { inventoryUid ->
                        inventoryRepository.softDeleteByUid(inventoryUid)
                    }
                }

                SalesReportEntryType.SALE_RETURN -> {
                    inventoryRepository.softDeleteByUid(
                        entry.sourceUid
                    )
                }
            }

            syncManager.autoSyncInBackground(
                reason = "sales_report_entry_deleted"
            )
        }
    }

    // داده خام فروش، برگشت و کالا را به وضعیت نهایی قابل نمایش تبدیل می‌کند.
    private fun buildUiState(
        data: SalesReportRawData,
        filters: SalesReportFilters
    ): SalesReportUiState {
        val productsById =
            data.products
                .mapNotNull { product ->
                    product.id?.let { id -> id to product }
                }
                .toMap()

        val allEntries =
            buildList {
                data.sales.forEach { sale ->
                    add(
                        sale.toReportEntry(
                            product = productsById[sale.productId]
                        )
                    )
                }

                data.saleReturns.forEach { saleReturn ->
                    add(
                        saleReturn.toReturnReportEntry(
                            product = productsById[saleReturn.productId]
                        )
                    )
                }
            }

        val currentMonth =
            PersianMonthPeriod.fromEpochMillis(
                System.currentTimeMillis()
            )

        val availableMonths =
            (allEntries.map { entry ->
                PersianMonthPeriod.fromEpochMillis(entry.occurredAt)
            } + currentMonth)
                .distinct()
                .sortedDescending()

        val selectedMonth =
            filters.selectedMonth
                .takeIf { it in availableMonths }
                ?: currentMonth

        val entriesInMonth =
            allEntries.filter { entry ->
                selectedMonth.contains(entry.occurredAt)
            }

        val availableProducts =
            entriesInMonth
                .distinctBy { entry -> entry.productId }
                .map { entry ->
                    SalesReportProductOption(
                        productId = entry.productId,
                        name = entry.productName,
                        model = entry.productModel
                    )
                }
                .sortedBy { option -> option.title.normalizedForSearch() }

        val normalizedQuery =
            filters.searchQuery.normalizedForSearch()

        val selectedProductId =
            filters.selectedProductId?.takeIf { productId ->
                availableProducts.any { option ->
                    option.productId == productId
                }
            }

        val filteredEntries =
            entriesInMonth.filter { entry ->
                val productMatches =
                    selectedProductId == null ||
                            entry.productId == selectedProductId

                val searchMatches =
                    normalizedQuery.isBlank() ||
                            entry.productName.normalizedForSearch()
                                .contains(normalizedQuery) ||
                            entry.productModel.normalizedForSearch()
                                .contains(normalizedQuery)

                productMatches && searchMatches
            }

        val sortedEntries =
            filteredEntries.sortedByOption(
                option = filters.sortOption
            )

        val sales =
            filteredEntries.filter { entry ->
                entry.type == SalesReportEntryType.SALE
            }

        val saleReturns =
            filteredEntries.filter { entry ->
                entry.type == SalesReportEntryType.SALE_RETURN
            }

        return SalesReportUiState(
            isLoading = false,
            availableMonths = availableMonths,
            selectedMonth = selectedMonth,
            availableProducts = availableProducts,
            selectedProductId = selectedProductId,
            searchQuery = filters.searchQuery,
            sortOption = filters.sortOption,
            summary = SalesReportSummary(
                totalSalesToman = sales.sumOf { entry ->
                    entry.totalSalePriceToman ?: 0L
                },
                totalProfitToman = sales.sumOf { entry ->
                    entry.totalProfitToman ?: 0L
                },
                isProfitComplete = sales.all { entry ->
                    entry.totalProfitToman != null
                },
                saleCount = sales.size,
                soldQuantity = sales.sumOf { entry -> entry.quantity },
                returnCount = saleReturns.size,
                returnedQuantity = saleReturns.sumOf { entry -> entry.quantity }
            ),
            entries = sortedEntries
        )
    }
}

private data class SalesReportFilters(
    val selectedMonth: PersianMonthPeriod =
        PersianMonthPeriod.fromEpochMillis(System.currentTimeMillis()),
    val selectedProductId: Int? = null,
    val searchQuery: String = "",
    val sortOption: SalesReportSortOption = SalesReportSortOption.DATE_NEWEST
)

private data class SalesReportRawData(
    val products: List<ProductEntity>,
    val sales: List<ProductSaleTransactionEntity>,
    val saleReturns: List<InventoryTransactionEntity>
)

// Snapshot فروش را همراه با مشخصات نمایشی کالا به ردیف گزارش تبدیل می‌کند.
private fun ProductSaleTransactionEntity.toReportEntry(
    product: ProductEntity?
): SalesReportEntry =
    SalesReportEntry(
        stableId = "sale:$uid",
        sourceUid = uid,
        linkedInventoryTransactionUid = inventoryTransactionUid,
        type = SalesReportEntryType.SALE,
        productId = productId,
        productName = product?.name ?: "کالای حذف‌شده",
        productModel = product?.model.orEmpty(),
        quantity = quantity,
        unitSalePriceToman = unitSalePriceToman,
        totalSalePriceToman = totalSalePriceToman,
        totalProfitToman = totalProfitToman,
        priceType = priceType,
        occurredAt = soldAt,
        note = note
    )

// برگشت موجودی را بدون ساختن مبلغ مالی غیرواقعی به ردیف گزارش تبدیل می‌کند.
private fun InventoryTransactionEntity.toReturnReportEntry(
    product: ProductEntity?
): SalesReportEntry =
    SalesReportEntry(
        stableId = "return:$uid",
        sourceUid = uid,
        linkedInventoryTransactionUid = null,
        type = SalesReportEntryType.SALE_RETURN,
        productId = productId,
        productName = product?.name ?: "کالای حذف‌شده",
        productModel = product?.model.orEmpty(),
        quantity = quantity,
        unitSalePriceToman = null,
        totalSalePriceToman = null,
        totalProfitToman = null,
        priceType = null,
        occurredAt = createdAt,
        note = note
    )

// متن فارسی و عربی را برای جستجوی پایدار نام و مدل یکدست می‌کند.
private fun String.normalizedForSearch(): String =
    trim()
        .lowercase(Locale.ROOT)
        .replace('ي', 'ی')
        .replace('ك', 'ک')

// لیست گزارش را براساس انتخاب کاربر مرتب می‌کند و مبالغ نامشخص را آخر نگه می‌دارد.
private fun List<SalesReportEntry>.sortedByOption(
    option: SalesReportSortOption
): List<SalesReportEntry> =
    when (option) {
        SalesReportSortOption.DATE_NEWEST ->
            sortedByDescending { entry -> entry.occurredAt }

        SalesReportSortOption.DATE_OLDEST ->
            sortedBy { entry -> entry.occurredAt }

        SalesReportSortOption.AMOUNT_HIGHEST ->
            sortedWith(
                compareBy<SalesReportEntry> { entry ->
                    entry.totalSalePriceToman == null
                }.thenByDescending { entry ->
                    entry.totalSalePriceToman ?: Long.MIN_VALUE
                }
            )

        SalesReportSortOption.AMOUNT_LOWEST ->
            sortedWith(
                compareBy<SalesReportEntry> { entry ->
                    entry.totalSalePriceToman == null
                }.thenBy { entry ->
                    entry.totalSalePriceToman ?: Long.MAX_VALUE
                }
            )

        SalesReportSortOption.PROFIT_HIGHEST ->
            sortedWith(
                compareBy<SalesReportEntry> { entry ->
                    entry.totalProfitToman == null
                }.thenByDescending { entry ->
                    entry.totalProfitToman ?: Long.MIN_VALUE
                }
            )

        SalesReportSortOption.PROFIT_LOWEST ->
            sortedWith(
                compareBy<SalesReportEntry> { entry ->
                    entry.totalProfitToman == null
                }.thenBy { entry ->
                    entry.totalProfitToman ?: Long.MAX_VALUE
                }
            )

        SalesReportSortOption.QUANTITY_HIGHEST ->
            sortedByDescending { entry -> entry.quantity }

        SalesReportSortOption.QUANTITY_LOWEST ->
            sortedBy { entry -> entry.quantity }
    }
