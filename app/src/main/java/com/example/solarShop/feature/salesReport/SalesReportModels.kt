package com.example.solarShop.feature.salesReport

import com.example.solarShop.domain.sales.PersianMonthPeriod

enum class SalesReportEntryType {
    SALE,
    SALE_RETURN
}

enum class SalesReportSortOption(
    val title: String
) {
    DATE_NEWEST("تاریخ؛ جدیدترین"),
    DATE_OLDEST("تاریخ؛ قدیمی‌ترین"),
    AMOUNT_HIGHEST("مبلغ؛ بیشترین"),
    AMOUNT_LOWEST("مبلغ؛ کمترین"),
    QUANTITY_HIGHEST("تعداد؛ بیشترین"),
    QUANTITY_LOWEST("تعداد؛ کمترین")
}

data class SalesReportProductOption(
    val productId: Int,
    val name: String,
    val model: String
) {
    val title: String
        get() = if (model.isBlank()) name else "$name ـ $model"
}

data class SalesReportEntry(
    val stableId: String,
    val sourceUid: String,
    val linkedInventoryTransactionUid: String?,
    val type: SalesReportEntryType,
    val productId: Int,
    val productName: String,
    val productModel: String,
    val quantity: Double,
    val unitSalePriceToman: Long?,
    val totalSalePriceToman: Long?,
    val priceType: String?,
    val occurredAt: Long,
    val note: String
)

data class SalesReportSummary(
    val totalSalesToman: Long = 0L,
    val saleCount: Int = 0,
    val soldQuantity: Double = 0.0,
    val returnCount: Int = 0,
    val returnedQuantity: Double = 0.0
)

data class SalesReportUiState(
    val isLoading: Boolean = true,
    val availableMonths: List<PersianMonthPeriod> = emptyList(),
    val selectedMonth: PersianMonthPeriod =
        PersianMonthPeriod.fromEpochMillis(System.currentTimeMillis()),
    val availableProducts: List<SalesReportProductOption> = emptyList(),
    val selectedProductId: Int? = null,
    val searchQuery: String = "",
    val sortOption: SalesReportSortOption = SalesReportSortOption.DATE_NEWEST,
    val summary: SalesReportSummary = SalesReportSummary(),
    val entries: List<SalesReportEntry> = emptyList()
)
