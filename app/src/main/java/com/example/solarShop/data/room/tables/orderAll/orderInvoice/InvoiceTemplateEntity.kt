package com.example.solarShop.data.room.tables.orderAll.orderInvoice

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.InvoiceType
import java.util.UUID

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
        Index(value = ["invoiceId"]),

        Index(value = ["uid"], unique = true),
        Index("updatedAt"),
        Index("deletedAt"),
        Index("isSynced")
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val invoiceId: Int,

    // شماره ردیف برای مرتب‌سازی
    val rowIndex: Int,

    // شرح کالا یا خدمت
    val description: String,

    val quantity: Double,

    // متر، عدد و...
    val unit: String? = null,

    // مبلغ واحد به ریال
    val unitPrice: Long,

    // تخفیف ردیف
    val rowDiscount: Long = 0L,

    // مبلغ قبل از مالیات
    val rowSubtotal: Long,

    val taxPercent: Float? = null,

    val taxAmount: Long = 0L,

    // مبلغ نهایی ردیف
    val rowTotal: Long,

    // ---------- Sync ----------

    @ColumnInfo(defaultValue = "''")
    val uid: String = UUID.randomUUID().toString(),

    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),

    val deletedAt: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false,

    val createdByUserId: Int? = null,
    val updatedByUserId: Int? = null,

    val shopUid: String? = null
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






