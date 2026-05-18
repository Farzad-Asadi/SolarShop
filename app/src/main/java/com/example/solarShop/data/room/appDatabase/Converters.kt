package com.example.solarShop.data.room.appDatabase

import androidx.room.TypeConverter
import com.example.solarShop.utils.EstimateApproach
import com.example.solarShop.utils.EstimateCategory


class EstimateConverters {
    @TypeConverter
    fun categoryToString(v: EstimateCategory?): String? = v?.name
    @TypeConverter
    fun stringToCategory(s: String?): EstimateCategory? = s?.let { EstimateCategory.valueOf(it) }
    @TypeConverter
    fun approachToString(v: EstimateApproach?): String? = v?.name
    @TypeConverter
    fun stringToApproach(s: String?): EstimateApproach? = s?.let { EstimateApproach.valueOf(it) }

}


class InvoiceTypeConverters {

    @TypeConverter
    fun fromInvoiceType(value: InvoiceType?): String? = value?.name

    @TypeConverter
    fun toInvoiceType(value: String?): InvoiceType? =
        value?.let { InvoiceType.valueOf(it) }

    @TypeConverter
    fun fromInvoiceStatus(value: InvoiceStatus?): String? = value?.name

    @TypeConverter
    fun toInvoiceStatus(value: String?): InvoiceStatus? =
        value?.let { InvoiceStatus.valueOf(it) }
}


enum class InvoiceType {
    PROFORMA,   // پیش‌فاکتور
    INVOICE     // فاکتور
}

enum class InvoiceStatus {
    DRAFT,      // پیش‌نویس
    FINAL,      // نهایی شده
    CANCELLED   // باطل‌شده (برای آینده)
}