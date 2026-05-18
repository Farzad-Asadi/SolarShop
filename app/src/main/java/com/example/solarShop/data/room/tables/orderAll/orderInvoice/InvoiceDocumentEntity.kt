package com.example.solarShop.data.room.tables.orderAll.orderInvoice

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.data.room.appDatabase.InvoiceStatus
import com.example.solarShop.data.room.appDatabase.InvoiceType

@Entity(
    tableName = "invoice_documents",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
        // اگر جدول OrderEntity داری می‌تونی اینجا هم FK تعریف کنی
        // فعلاً جنرال نگه می‌دارم
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["orderId"])
    ]
)
data class InvoiceDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /// لینک به سفارش
    val orderId: Int,

    /// لینک به تمپلیت (ممکن است بعداً null شود)
    val templateId: Int? = null,

    /// نوع سند: پیش‌فاکتور یا فاکتور
    val type: InvoiceType,

    /// شماره فاکتور/پیش‌فاکتور (رشته تا بتوانی فرمت دلخواه بسازی)
    val number: String,

    /// زمان ساخت (timestamp millis)
    val createdAt: Long,

    /// زمان آخرین ویرایش
    val updatedAt: Long,

    /// وضعیت سند
    val status: InvoiceStatus = InvoiceStatus.DRAFT,

    // --- اسنپ‌شات اطلاعات فروشنده (برای ثابت ماندن فاکتور بعداً) ---

    val sellerLabel: String? = null,
    val sellerName: String,
    val sellerPhone: String? = null,
    val sellerAddress: String? = null,
    val sellerNationalId: String? = null,
    val sellerEconomicCode: String? = null,

    // --- اسنپ‌شات اطلاعات خریدار ---

    val buyerLabel: String? = null,
    val buyerName: String,
    val buyerPhone: String? = null,
    val buyerAddress: String? = null,
    val buyerNationalId: String? = null,

    // --- مبالغ کل (به ریال، نوع Long) ---

    val subtotalBeforeDiscount: Long,  // جمع جزء قبل تخفیف کل
    val totalDiscount: Long,          // تخفیف کل
    val totalBeforeTax: Long,         // بعد از تخفیف، قبل مالیات
    val totalTax: Long,               // جمع مالیات
    val totalFinal: Long,             // مبلغ قابل پرداخت نهایی

    /// متن توضیحات پایین فاکتور
    val notes: String? = null,

    /// لینک به پیوست PDF در جدول Attachment (اگر داری)
    val pdfAttachmentId: Int? = null
)
