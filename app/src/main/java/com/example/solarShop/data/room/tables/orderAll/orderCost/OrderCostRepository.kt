package com.example.solarShop.data.room.tables.orderAll.orderCost

import com.example.solarShop.data.room.tables.orderAll.order.OrderDao
import com.example.solarShop.ui.orderScreen.orderCosts.CostScope
import com.example.solarShop.ui.orderScreen.orderCosts.ExpenseRow
import com.example.solarShop.ui.orderScreen.orderCosts.OrderMini
import com.example.solarShop.ui.orderScreen.orderCosts.ReceiptRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

interface OrderCostRepository {

    fun receipts(scope: CostScope, orderId: Int, clientId: Int): Flow<List<ReceiptRow>>
    fun expenses(scope: CostScope, orderId: Int, clientId: Int): Flow<List<ExpenseRow>>

    fun receiptsSum(scope: CostScope, orderId: Int, clientId: Int): Flow<Long>
    fun expensesSum(scope: CostScope, orderId: Int, clientId: Int): Flow<Long>

    fun receiptAttachments(receiptId: Int): Flow<List<ReceiptAttachmentEntity>>
    fun expenseAttachments(expenseId: Int): Flow<List<ExpenseAttachmentEntity>>

    suspend fun addReceipt(entity: OrderReceiptEntity): Long
    suspend fun updateReceipt(entity: OrderReceiptEntity)
    suspend fun deleteReceipt(entity: OrderReceiptEntity)

    suspend fun addExpense(entity: OrderExpenseEntity): Long
    suspend fun updateExpense(entity: OrderExpenseEntity)
    suspend fun deleteExpense(entity: OrderExpenseEntity)

    suspend fun addReceiptAttachment(entity: ReceiptAttachmentEntity)
    suspend fun deleteReceiptAttachment(entity: ReceiptAttachmentEntity)

    suspend fun addExpenseAttachment(entity: ExpenseAttachmentEntity)
    suspend fun deleteExpenseAttachment(entity: ExpenseAttachmentEntity)

    suspend fun replaceReceiptAllocations(receiptId: Int, items: List<ReceiptAllocationEntity>)
    suspend fun replaceExpenseAllocations(expenseId: Int, items: List<ExpenseAllocationEntity>)

    fun clientIdOfOrder(orderId: Int): Flow<Int>
    fun ordersOfClientMini(clientId: Int): Flow<List<OrderMini>>

    suspend fun receiptAllocationsOnce(receiptId: Int): List<ReceiptAllocationEntity>
    suspend fun expenseAllocationsOnce(expenseId: Int): List<ExpenseAllocationEntity>

    //دسته هزینه
    fun observeAll(): Flow<List<ExpenseCategoryEntity>>
    suspend fun findIdByTitle(title: String): Int?
    suspend fun insertIgnore(entity: ExpenseCategoryEntity): Long
    suspend fun getOrCreateId(titleRaw: String): Int
    suspend fun countAll(): Int

    suspend fun receiptAttachmentsOnce(receiptId: Int): List<ReceiptAttachmentEntity>
    suspend fun expenseAttachmentsOnce(expenseId: Int): List<ExpenseAttachmentEntity>


}




class OrderCostRepositoryImpl @Inject constructor(
    private val dao: OrderCostDao,
    private val orderDao: OrderDao,

) : OrderCostRepository {

    override fun receipts(scope: CostScope, orderId: Int, clientId: Int): Flow<List<ReceiptRow>> =
        when (scope) {
            CostScope.Order -> combine(
                dao.receiptsForOrder(orderId),
                dao.receiptsAllocatedToOrder(orderId)
            ) { direct, allocated ->
                (direct + allocated).sortedByDescending { it.entity.dateEpoch }
            }

            CostScope.Customer -> dao.receiptsForCustomer(clientId)
        }


    override fun expenses(scope: CostScope, orderId: Int, clientId: Int): Flow<List<ExpenseRow>> =
        when (scope) {
            CostScope.Order -> combine(
                dao.expensesForOrder(orderId),
                dao.expensesAllocatedToOrder(orderId)
            ) { direct, allocated ->
                (direct + allocated).sortedByDescending { it.entity.dateEpoch }
            }

            CostScope.Customer -> dao.expensesForCustomer(clientId)
        }

    override fun receiptsSum(scope: CostScope, orderId: Int, clientId: Int) =
        when (scope) {
            CostScope.Order -> dao.receiptsSumForOrder(orderId)
            CostScope.Customer -> dao.receiptsSumForCustomer(clientId)
        }

    override fun expensesSum(scope: CostScope, orderId: Int, clientId: Int) =
        when (scope) {
            CostScope.Order -> dao.expensesSumForOrder(orderId)
            CostScope.Customer -> dao.expensesSumForCustomer(clientId)
        }

    override fun receiptAttachments(receiptId: Int) = dao.receiptAttachments(receiptId)
    override fun expenseAttachments(expenseId: Int) = dao.expenseAttachments(expenseId)

    override suspend fun addReceipt(entity: OrderReceiptEntity) : Long = dao.addReceipt(entity)
    override suspend fun updateReceipt(entity: OrderReceiptEntity) = dao.updateReceipt(entity)
    override suspend fun deleteReceipt(entity: OrderReceiptEntity) = dao.deleteReceipt(entity)

    override suspend fun addExpense(entity: OrderExpenseEntity) : Long  = dao.addExpense(entity)
    override suspend fun updateExpense(entity: OrderExpenseEntity) = dao.updateExpense(entity)
    override suspend fun deleteExpense(entity: OrderExpenseEntity) = dao.deleteExpense(entity)

    override suspend fun addReceiptAttachment(entity: ReceiptAttachmentEntity) = dao.addReceiptAttachment(entity)
    override suspend fun deleteReceiptAttachment(entity: ReceiptAttachmentEntity) = dao.deleteReceiptAttachment(entity)

    override suspend fun addExpenseAttachment(entity: ExpenseAttachmentEntity) = dao.addExpenseAttachment(entity)
    override suspend fun deleteExpenseAttachment(entity: ExpenseAttachmentEntity) = dao.deleteExpenseAttachment(entity)

    override suspend fun replaceReceiptAllocations(receiptId: Int, items: List<ReceiptAllocationEntity>) =
        dao.replaceReceiptAllocations(receiptId, items)

    override suspend fun replaceExpenseAllocations(expenseId: Int, items: List<ExpenseAllocationEntity>) =
        dao.replaceExpenseAllocations(expenseId, items)

    override fun clientIdOfOrder(orderId: Int) = orderDao.clientIdOfOrder(orderId)
    override fun ordersOfClientMini(clientId: Int) = orderDao.ordersOfClientMini(clientId)

    override suspend fun receiptAllocationsOnce(receiptId: Int) = dao.receiptAllocationsOnce(receiptId)
    override suspend fun expenseAllocationsOnce(expenseId: Int) = dao.expenseAllocationsOnce(expenseId)



    override fun observeAll(): Flow<List<ExpenseCategoryEntity>> =
        dao.observeAll()

    override suspend fun findIdByTitle(title: String): Int? =
        dao.findIdByTitle(title)

    override suspend fun insertIgnore(entity: ExpenseCategoryEntity): Long =
        dao.insertIgnore(entity)

    override suspend fun getOrCreateId(titleRaw: String): Int {
        val title = titleRaw.trim()
        require(title.isNotBlank())

        dao.findIdByTitle(title)?.let { return it }

        val inserted = dao.insertIgnore(ExpenseCategoryEntity(title = title))
        if (inserted != -1L) return inserted.toInt()

        // یعنی قبلاً وجود داشته یا همزمان درج شده → دوباره بخون
        return dao.findIdByTitle(title)
            ?: error("getOrCreateId failed for title=$title")
    }


    override suspend fun countAll(): Int =
        dao.countAll()

    override suspend fun receiptAttachmentsOnce(receiptId: Int): List<ReceiptAttachmentEntity> =
        dao.receiptAttachmentsOnce(receiptId)

    override suspend fun expenseAttachmentsOnce(expenseId: Int): List<ExpenseAttachmentEntity> =
            dao.expenseAttachmentsOnce(expenseId)

}

