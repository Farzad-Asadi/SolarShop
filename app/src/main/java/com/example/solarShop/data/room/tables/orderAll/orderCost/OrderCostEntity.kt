package com.example.solarShop.data.room.tables.orderAll.orderCost

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity

@Entity(
    tableName = "order_receipts",
    indices = [Index("orderId"), Index("clientId"), Index("dateEpoch")],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class OrderReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    /** اگر null باشد یعنی "برای مشتری (کلی)" */
    val orderId: Int? = null,

    /** همیشه داریم: صاحب اصلی تراکنش */
    val clientId: Int,

    /** مبلغ به تومان */
    val amountToman: Long,
    val dateEpoch: Long,
    val method: String,
    val note: String? = null
)





@Entity(
    tableName = "order_expenses",
    indices = [Index("orderId"), Index("clientId"), Index("dateEpoch"), Index("categoryId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class OrderExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    /** اگر null باشد یعنی "برای مشتری (کلی)" */
    val orderId: Int? = null,

    /** همیشه داریم */
    val clientId: Int,

    val amountToman: Long,
    val dateEpoch: Long,
    val categoryId: Int? = null,
    val note: String? = null
)







@Entity(
    tableName = "expense_catalog",
    indices = [Index(value = ["title"], unique = true)]
)
data class ExpenseCatalogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isActive: Boolean = true
)




@Entity(
    tableName = "receipt_attachments",
    indices = [Index("receiptId"), Index("createdEpoch")],
    foreignKeys = [
        ForeignKey(
            entity = OrderReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE,   // حذف دریافتی ⇒ حذف پیوست‌ها
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class ReceiptAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptId: Int,
    /** نام نسبی فایل (مثلاً: orders/123/receipts/45/img_001.jpg) */
    val fileName: String,
    /** نام نسبی thumbnail (اختیاری) */
    val thumbName: String? = null,
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    /** برای جلوگیری از دوباره‌کاری/تشخیص تکراری‌ها */
    val sha256: String? = null,
    val createdEpoch: Long = System.currentTimeMillis()
)




@Entity(
    tableName = "expense_attachments",
    indices = [Index("expenseId"), Index("createdEpoch")],
    foreignKeys = [
        ForeignKey(
            entity = OrderExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,   // حذف هزینه ⇒ حذف پیوست‌ها
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class ExpenseAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    /** نام نسبی فایل (مثلاً: orders/123/expenses/78/img_002.jpg) */
    val fileName: String,
    val thumbName: String? = null,
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sha256: String? = null,
    val createdEpoch: Long = System.currentTimeMillis()
)





@Entity(
    tableName = "receipt_allocations",
    indices = [Index("receiptId"), Index("orderId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReceiptAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptId: Int,
    val orderId: Int,
    /** مبلغ تخصیص به تومان */
    val amountToman: Long,
    val createdEpoch: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expense_allocations",
    indices = [Index("expenseId"), Index("orderId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpenseAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val orderId: Int,
    val amountToman: Long,
    val createdEpoch: Long = System.currentTimeMillis()
)



@Entity(
    tableName = "expense_category",
    indices = [Index(value = ["title"], unique = true)]
)
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String
)
