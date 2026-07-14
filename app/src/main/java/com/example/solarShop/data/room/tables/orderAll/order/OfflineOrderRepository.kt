package com.example.solarShop.data.room.tables.orderAll.order

import androidx.room.withTransaction
import com.example.solarShop.data.backupRestore.AttachmentController
import com.example.solarShop.data.local.database.AppDatabase
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoMetaRepository
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefDao
import com.example.solarShop.ui.orderScreen.orderCosts.OrderMini
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject


class OfflineOrderRepository @Inject constructor(
    private val db: AppDatabase,
    private val orderDao: OrderDao,
    private val orderCostDao: OrderCostDao,
    private val attachmentController: AttachmentController,
    private val orderPhotoDao: OrderPhotoRefDao,
    private val orderPhotoMetaRepo: OrderPhotoMetaRepository
) : OrderRepository {


    override suspend fun insertOrder(
        orderEntity: OrderEntity
    ): Long {
        val now =
            System.currentTimeMillis()

        return orderDao.insertOrder(
            orderEntity.copy(
                createdAt = orderEntity.createdAt
                    .takeIf { it > 0L }
                    ?: now,

                updatedAt = now,
                deletedAt = null,
                isSynced = false
            )
        )
    }

    override suspend fun deleteOrder(
        orderEntity: OrderEntity
    ): Int {
        val orderId =
            orderEntity.id ?: return 0

        return orderDao.softDeleteById(orderId)
    }

    override suspend fun updateOrder(
        orderEntity: OrderEntity
    ): Int {
        return orderDao.updateOrder(
            orderEntity.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
        )
    }
    override suspend fun updateOrderName(orderId: Int, name: String): Int =
        orderDao.updateOrderName(
            orderId = orderId,
            name = name.trim(),
            updatedAt = System.currentTimeMillis()
        )

    override suspend fun getOrderById(orderId: Int): OrderEntity? =
        orderDao.getOrderById(orderId)

    override fun observeAllOrders(): Flow<List<OrderEntity>> =
        orderDao.observeAllOrders().distinctUntilChanged()

    override fun observeOrderById(orderId: Int): Flow<OrderEntity?> =
        orderDao.observeOrderById(orderId).distinctUntilChanged()

    override fun observeAllOrderOfClient(clientId: Int): Flow<List<OrderEntity>> =
        orderDao.observeAllOrderOfClient(clientId).distinctUntilChanged()

    override fun getOrderWithPriceEstimates(orderId: Int): Flow<OrderWithPriceEstimate?> =
        orderDao.observeOrderWithPriceEstimates(orderId).distinctUntilChanged()

    override fun getOrderWithSelectedChoice(orderId: Int): Flow<OrderWithSelectedChoice?> =
        orderDao.observeOrderWithSelectedChoice(orderId).distinctUntilChanged()

    override fun observeOrderWithTimelineItem(orderId: Int): Flow<OrderWithTimelineItem?> =
        orderDao.observeOrderWithTimelineItems(orderId).distinctUntilChanged()

    override suspend fun clientIdOfOrderOnce(orderId: Int): Int =
        orderDao.clientIdOfOrderOnce(orderId)

    override fun clientIdOfOrder(orderId: Int): Flow<Int> =
        orderDao.clientIdOfOrder(orderId)

    override fun ordersOfClientMini(clientId: Int): Flow<List<OrderMini>> =
        orderDao.ordersOfClientMini(clientId)

    override suspend fun updateOrderNote(
        orderId: Int,
        note: String
    ) {
        orderDao.updateOrderNote(
            orderId = orderId,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun deleteOrderWithChildren(orderId: Int) {
        db.withTransaction {

            // 1) receipts/expenses
            val receipts = orderCostDao.receiptsOnceForOrder(orderId)
            val expenses = orderCostDao.expensesOnceForOrder(orderId)

            // 2) allocations (قبل از حذف receipt/expense)
            orderCostDao.deleteReceiptAllocationsByOrderId(orderId)
            orderCostDao.deleteExpenseAllocationsByOrderId(orderId)

            // 3) attachments + فایل‌ها
            receipts.forEach { r ->
                val atts = orderCostDao.receiptAttachmentsOnce(r.id)
                atts.forEach { att ->
                    attachmentController.deletePhysicalOnly(att.fileName, att.thumbName)
                    orderCostDao.deleteReceiptAttachment(att)
                }
            }

            expenses.forEach { e ->
                val atts = orderCostDao.expenseAttachmentsOnce(e.id)
                atts.forEach { att ->
                    attachmentController.deletePhysicalOnly(att.fileName, att.thumbName)
                    orderCostDao.deleteExpenseAttachment(att)
                }
            }

            // 4) receipts/expenses
            orderCostDao.deleteReceiptsByOrderId(orderId)
            orderCostDao.deleteExpensesByOrderId(orderId)

            // 5)  عکس‌های سفارش (پین شده‌ها فایل دارند)
            val photoIds = orderPhotoDao.photoIdsOnceForOrder(orderId)
            if (photoIds.isNotEmpty()) {
                orderPhotoMetaRepo.removeMany(photoIds) // ✅ هم DB را پاک می‌کند هم فایل پین‌شده را
            } else {
                // اگر removeMany خودش deleteMany می‌کند، نیازی به این نیست
                // orderPhotoDao.deletePhotosByOrderId(orderId)
            }


            // 6) cleanupEmptyDir
            attachmentController.cleanupEmptyDirsForOrder(orderId)


            // 7) order
            orderDao.softDeleteById(
                orderId = orderId,
                deletedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun getOrderByUid(
        uid: String
    ): OrderEntity? {
        return orderDao.getOrderByUid(uid)
    }

    override suspend fun getOrderIdByUid(
        uid: String
    ): Int? {
        return orderDao.getOrderIdByUid(uid)
    }

    override suspend fun getUnsyncedOrders():
            List<OrderEntity> {

        return orderDao.getUnsyncedOrders()
    }

    override suspend fun markOrdersSynced(
        uids: List<String>
    ) {
        if (uids.isEmpty()) return

        orderDao.markOrdersSynced(uids)
    }

    override suspend fun softDeleteByUid(
        uid: String
    ): Int {
        return orderDao.softDeleteByUid(uid)
    }

    override suspend fun upsertOrderByUid(
        orderEntity: OrderEntity
    ): Long {
        val existing =
            orderDao.getOrderByUid(orderEntity.uid)

        if (existing == null) {
            return orderDao.insertOrder(orderEntity)
        }

        if (
            existing.deletedAt != null &&
            orderEntity.deletedAt == null
        ) {
            return existing.id?.toLong() ?: 0L
        }

        if (
            !existing.isSynced &&
            existing.updatedAt > orderEntity.updatedAt
        ) {
            return existing.id?.toLong() ?: 0L
        }

        val merged =
            orderEntity.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )

        orderDao.updateOrder(merged)

        return existing.id?.toLong() ?: 0L
    }

}