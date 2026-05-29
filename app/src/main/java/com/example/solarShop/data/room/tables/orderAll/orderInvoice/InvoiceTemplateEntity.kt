package com.example.solarShop.data.room.tables.orderAll.orderInvoice

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.InvoiceType

@Entity(tableName = "invoice_templates")
data class InvoiceTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /// نام تمپلیت، مثلا: "پیش‌فاکتور ساده"
    val name: String,

    /// نوع سند: پیش‌فاکتور یا فاکتور
    val type: InvoiceType,

    /// عنوانی که بالای سند نمایش داده می‌شود، مثلا: "پیش‌فاکتور فروش"
    val title: String,

    /// آیا ستون/محاسبه‌ی مالیات فعال باشد؟
    val hasTax: Boolean,

    /// درصد پیش‌فرض مالیات (اگر hasTax = true)
    val defaultTaxPercent: Float? = null,

    /// آیا فیلد مبلغ به حروف نمایش داده شود؟ (بیشتر برای فاکتور)
    val showAmountInWords: Boolean = false,

    /// آیا این تمپلیت، پیش‌فرض برای این نوع سند است؟
    val isDefaultForType: Boolean = false
)



@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["invoiceId"])
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val invoiceId: Int,

    /// شماره ردیف (برای مرتب‌سازی)
    val rowIndex: Int,

    /// شرح کالا / خدمت
    val description: String,

    /// تعداد
    val quantity: Double,

    /// واحد (متر، عدد، ...)
    val unit: String? = null,

    /// مبلغ واحد (ریال)
    val unitPrice: Long,

    /// تخفیف ردیف (ریال)
    val rowDiscount: Long = 0L,

    /// مبلغ قبل از مالیات برای این ردیف (unitPrice * qty - rowDiscount)
    val rowSubtotal: Long,

    /// درصد مالیات ردیف (اگر hasTax=false باشد می‌تواند صفر یا null باشد)
    val taxPercent: Float? = null,

    /// مبلغ مالیات ردیف
    val taxAmount: Long = 0L,

    /// مبلغ نهایی ردیف (rowSubtotal + taxAmount)
    val rowTotal: Long
)







data class InvoiceWithItems(
    @Embedded
    val invoice: InvoiceDocumentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
)






