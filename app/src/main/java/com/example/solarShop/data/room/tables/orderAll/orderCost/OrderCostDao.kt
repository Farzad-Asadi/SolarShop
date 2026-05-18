package com.example.solarShop.data.room.tables.orderAll.orderCost

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.solarShop.ui.orderScreen.orderCosts.ExpenseRow
import com.example.solarShop.ui.orderScreen.orderCosts.ReceiptRow
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderCostDao {

    // -------------------- Lists (Order scope) --------------------
    @Query("""
    SELECT r.*,
           (SELECT COUNT(*) FROM receipt_attachments a WHERE a.receiptId = r.id) AS attachCount,
           0 AS allocatedSumToman,
           0 AS allocatedToOrderToman,
           0 AS isFromCustomerAllocation
    FROM order_receipts r
    WHERE r.orderId = :orderId
    ORDER BY r.dateEpoch DESC
""")
    fun receiptsForOrder(orderId: Int): Flow<List<ReceiptRow>>


    @Query("""
    SELECT e.*,
           (SELECT COUNT(*) FROM expense_attachments a WHERE a.expenseId = e.id) AS attachCount,
           0 AS allocatedSumToman,
           0 AS allocatedToOrderToman,
           0 AS isFromCustomerAllocation
    FROM order_expenses e
    WHERE e.orderId = :orderId
    ORDER BY e.dateEpoch DESC
""")
    fun expensesForOrder(orderId: Int): Flow<List<ExpenseRow>>


    // -------------------- Lists (Customer scope: orderId IS NULL) --------------------
    @Query("""
    SELECT r.*,
           (SELECT COUNT(*) FROM receipt_attachments a WHERE a.receiptId = r.id) AS attachCount,
           (SELECT COALESCE(SUM(al.amountToman), 0) FROM receipt_allocations al WHERE al.receiptId = r.id) AS allocatedSumToman,
           0 AS allocatedToOrderToman,
           0 AS isFromCustomerAllocation
    FROM order_receipts r
    WHERE r.clientId = :clientId AND r.orderId IS NULL
    ORDER BY r.dateEpoch DESC
""")
    fun receiptsForCustomer(clientId: Int): Flow<List<ReceiptRow>>


    @Query("""
        SELECT e.*,
               (SELECT COUNT(*) FROM expense_attachments a WHERE a.expenseId = e.id) AS attachCount,
               (SELECT COALESCE(SUM(al.amountToman), 0)
                FROM expense_allocations al
                WHERE al.expenseId = e.id) AS allocatedSumToman,
                0 AS allocatedToOrderToman,
           0 AS isFromCustomerAllocation
        FROM order_expenses e
        WHERE e.clientId = :clientId AND e.orderId IS NULL
        ORDER BY e.dateEpoch DESC
    """)
    fun expensesForCustomer(clientId: Int): Flow<List<ExpenseRow>>

    // -------------------- CRUD receipt/expense --------------------
    @Insert
    suspend fun addReceipt(entity: OrderReceiptEntity) : Long

    @Update
    suspend fun updateReceipt(entity: OrderReceiptEntity)

    @Delete
    suspend fun deleteReceipt(entity: OrderReceiptEntity)

    @Insert
    suspend fun addExpense(entity: OrderExpenseEntity) : Long

    @Update
    suspend fun updateExpense(entity: OrderExpenseEntity)

    @Delete
    suspend fun deleteExpense(entity: OrderExpenseEntity)

    // -------------------- Attachments --------------------
    @Query("SELECT * FROM receipt_attachments WHERE receiptId = :receiptId ORDER BY id DESC")
    fun receiptAttachments(receiptId: Int): Flow<List<ReceiptAttachmentEntity>>

    @Query("SELECT * FROM expense_attachments WHERE expenseId = :expenseId ORDER BY id DESC")
    fun expenseAttachments(expenseId: Int): Flow<List<ExpenseAttachmentEntity>>

    @Insert
    suspend fun addReceiptAttachment(entity: ReceiptAttachmentEntity)

    @Delete
    suspend fun deleteReceiptAttachment(entity: ReceiptAttachmentEntity)

    @Insert
    suspend fun addExpenseAttachment(entity: ExpenseAttachmentEntity)

    @Delete
    suspend fun deleteExpenseAttachment(entity: ExpenseAttachmentEntity)

    // -------------------- Sums for Order (direct + allocated share) --------------------
    @Query("""
        SELECT 
        COALESCE((SELECT SUM(amountToman) FROM order_receipts WHERE orderId = :orderId), 0)
        + COALESCE((SELECT SUM(amountToman) FROM receipt_allocations WHERE orderId = :orderId), 0)
    """)
    fun receiptsSumForOrder(orderId: Int): Flow<Long>

    @Query("""
        SELECT 
        COALESCE((SELECT SUM(amountToman) FROM order_expenses WHERE orderId = :orderId), 0)
        + COALESCE((SELECT SUM(amountToman) FROM expense_allocations WHERE orderId = :orderId), 0)
    """)
    fun expensesSumForOrder(orderId: Int): Flow<Long>

    // -------------------- Sums for Customer (customer-level only) --------------------
    @Query("""
        SELECT COALESCE(SUM(amountToman), 0)
        FROM order_receipts
        WHERE clientId = :clientId AND orderId IS NULL
    """)
    fun receiptsSumForCustomer(clientId: Int): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountToman), 0)
        FROM order_expenses
        WHERE clientId = :clientId AND orderId IS NULL
    """)
    fun expensesSumForCustomer(clientId: Int): Flow<Long>

    // -------------------- Allocation replace (Receipt) --------------------
    @Query("DELETE FROM receipt_allocations WHERE receiptId = :receiptId")
    suspend fun deleteReceiptAllocations(receiptId: Int)

    @Insert
    suspend fun insertReceiptAllocations(items: List<ReceiptAllocationEntity>)

    @Transaction
    suspend fun replaceReceiptAllocations(receiptId: Int, items: List<ReceiptAllocationEntity>) {
        deleteReceiptAllocations(receiptId)
        if (items.isNotEmpty()) insertReceiptAllocations(items)
    }

    // -------------------- Allocation replace (Expense) --------------------
    @Query("DELETE FROM expense_allocations WHERE expenseId = :expenseId")
    suspend fun deleteExpenseAllocations(expenseId: Int)

    @Insert
    suspend fun insertExpenseAllocations(items: List<ExpenseAllocationEntity>)

    @Transaction
    suspend fun replaceExpenseAllocations(expenseId: Int, items: List<ExpenseAllocationEntity>) {
        deleteExpenseAllocations(expenseId)
        if (items.isNotEmpty()) insertExpenseAllocations(items)
    }

    @Query("SELECT * FROM receipt_allocations WHERE receiptId = :receiptId")
    suspend fun receiptAllocationsOnce(receiptId: Int): List<ReceiptAllocationEntity>

    @Query("SELECT * FROM expense_allocations WHERE expenseId = :expenseId")
    suspend fun expenseAllocationsOnce(expenseId: Int): List<ExpenseAllocationEntity>


    @Query("""
    SELECT r.*,
           (SELECT COUNT(*) FROM receipt_attachments a WHERE a.receiptId = r.id) AS attachCount,
           (SELECT COALESCE(SUM(al2.amountToman), 0) FROM receipt_allocations al2 WHERE al2.receiptId = r.id) AS allocatedSumToman,
           al.amountToman AS allocatedToOrderToman,
           1 AS isFromCustomerAllocation
    FROM receipt_allocations al
    JOIN order_receipts r ON r.id = al.receiptId
    WHERE al.orderId = :orderId
      AND r.orderId IS NULL
    ORDER BY r.dateEpoch DESC
""")
    fun receiptsAllocatedToOrder(orderId: Int): Flow<List<ReceiptRow>>


    @Query("""
    SELECT e.*,
           (SELECT COUNT(*) FROM expense_attachments a WHERE a.expenseId = e.id) AS attachCount,
           (SELECT COALESCE(SUM(al2.amountToman), 0) FROM expense_allocations al2 WHERE al2.expenseId = e.id) AS allocatedSumToman,
           al.amountToman AS allocatedToOrderToman,
           1 AS isFromCustomerAllocation
    FROM expense_allocations al
    JOIN order_expenses e ON e.id = al.expenseId
    WHERE al.orderId = :orderId
      AND e.orderId IS NULL
    ORDER BY e.dateEpoch DESC
""")
    fun expensesAllocatedToOrder(orderId: Int): Flow<List<ExpenseRow>>



    //دسته هزینه
    @Query("SELECT * FROM expense_category ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT id FROM expense_category WHERE title = :title LIMIT 1")
    suspend fun findIdByTitle(title: String): Int?


    @Query("SELECT COUNT(*) FROM expense_category")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: ExpenseCategoryEntity): Long


    @Query("SELECT * FROM receipt_attachments WHERE receiptId = :receiptId")
    suspend fun receiptAttachmentsOnce(receiptId: Int): List<ReceiptAttachmentEntity>

    @Query("SELECT * FROM expense_attachments WHERE expenseId = :expenseId")
    suspend fun expenseAttachmentsOnce(expenseId: Int): List<ExpenseAttachmentEntity>



    @Query("SELECT * FROM order_receipts WHERE orderId = :orderId")
    suspend fun receiptsOnceForOrder(orderId: Int): List<OrderReceiptEntity>

    @Query("SELECT * FROM order_expenses WHERE orderId = :orderId")
    suspend fun expensesOnceForOrder(orderId: Int): List<OrderExpenseEntity>


    @Query("DELETE FROM receipt_allocations WHERE receiptId IN (SELECT id FROM order_receipts WHERE orderId = :orderId)")
    suspend fun deleteReceiptAllocationsByOrderId(orderId: Int)

    @Query("DELETE FROM expense_allocations WHERE expenseId IN (SELECT id FROM order_expenses WHERE orderId = :orderId)")
    suspend fun deleteExpenseAllocationsByOrderId(orderId: Int)


    @Query("DELETE FROM order_receipts WHERE orderId = :orderId")
    suspend fun deleteReceiptsByOrderId(orderId: Int)

    @Query("DELETE FROM order_expenses WHERE orderId = :orderId")
    suspend fun deleteExpensesByOrderId(orderId: Int)



}



