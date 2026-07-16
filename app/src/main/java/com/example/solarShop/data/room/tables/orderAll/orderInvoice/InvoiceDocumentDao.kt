package com.example.solarShop.data.room.tables.orderAll.orderInvoice

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.solarShop.InvoiceType
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDocumentDao {

    // =========================================================
    // Invoice documents - UI
    // =========================================================

    /*
     * @Relation معمولی نمی‌تواند deletedAt ردیف‌ها را فیلتر کند.
     * بنابراین سند و ردیف‌های فعال را جداگانه می‌گیریم.
     */
    @Transaction
    suspend fun getInvoiceWithItems(
        id: Int
    ): InvoiceWithItems? {
        val invoice =
            getInvoiceById(id) ?: return null

        val items =
            getItemsForInvoice(id)

        return InvoiceWithItems(
            invoice = invoice,
            items = items
        )
    }

    @Query(
        """
    SELECT *
    FROM invoice_documents
    WHERE id = :invoiceId
    LIMIT 1
    """
    )
    suspend fun getInvoiceByIdForSync(
        invoiceId: Int
    ): InvoiceDocumentEntity?

    @Query(
        """
        SELECT *
        FROM invoice_documents
        WHERE orderId = :orderId
          AND type = :type
          AND deletedAt IS NULL
        ORDER BY createdAt DESC, updatedAt DESC
        """
    )
    suspend fun getInvoicesForOrder(
        orderId: Int,
        type: InvoiceType
    ): List<InvoiceDocumentEntity>

    @Query(
        """
        SELECT *
        FROM invoice_documents
        WHERE orderId = :orderId
          AND type = :type
          AND deletedAt IS NULL
        ORDER BY createdAt DESC, updatedAt DESC
        """
    )
    fun observeInvoicesForOrder(
        orderId: Int,
        type: InvoiceType
    ): Flow<List<InvoiceDocumentEntity>>

    @Query(
        """
        SELECT *
        FROM invoice_documents
        WHERE id = :id
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getInvoiceById(
        id: Int
    ): InvoiceDocumentEntity?

    // =========================================================
    // Invoice documents - Local write
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInvoice(
        invoice: InvoiceDocumentEntity
    ): Long

    @Update
    suspend fun updateInvoice(
        invoice: InvoiceDocumentEntity
    ): Int

    @Upsert
    suspend fun upsertInvoice(
        invoice: InvoiceDocumentEntity
    )

    // =========================================================
    // Invoice documents - UID
    // =========================================================

    /*
     * deletedAt عمداً فیلتر نشده تا tombstone هم پیدا شود.
     */
    @Query(
        """
        SELECT *
        FROM invoice_documents
        WHERE uid = :uid
        LIMIT 1
        """
    )
    suspend fun getInvoiceByUid(
        uid: String
    ): InvoiceDocumentEntity?

    @Query(
        """
        SELECT id
        FROM invoice_documents
        WHERE uid = :uid
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getInvoiceIdByUid(
        uid: String
    ): Int?

    // =========================================================
    // Invoice documents - Sync
    // =========================================================

    @Query(
        """
    SELECT *
    FROM invoice_documents
    ORDER BY updatedAt ASC
    """
    )
    suspend fun getAllInvoicesForSync():
            List<InvoiceDocumentEntity>

    @Query(
        """
        SELECT *
        FROM invoice_documents
        WHERE isSynced = 0
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getUnsyncedInvoices():
            List<InvoiceDocumentEntity>

    @Query(
        """
        UPDATE invoice_documents
        SET isSynced = 1
        WHERE uid IN (:uids)
        """
    )
    suspend fun markInvoicesSynced(
        uids: List<String>
    )

    // =========================================================
    // Invoice documents - Soft delete
    // =========================================================

    @Query(
        """
        UPDATE invoice_documents
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE id = :invoiceId
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteInvoiceById(
        invoiceId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE invoice_documents
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE uid = :uid
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteInvoiceByUid(
        uid: String,
        deletedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE invoice_documents
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE orderId = :orderId
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteInvoicesByOrderId(
        orderId: Int,
        deletedAt: Long
    ): Int

    @Query(
        """
        UPDATE invoice_documents
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE orderId IN (
            SELECT id
            FROM orders
            WHERE clientId = :clientId
        )
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteInvoicesByClientId(
        clientId: Int,
        deletedAt: Long
    ): Int

    // =========================================================
    // Invoice items - UI
    // =========================================================

    @Query(
        """
        SELECT *
        FROM invoice_items
        WHERE invoiceId = :invoiceId
          AND deletedAt IS NULL
        ORDER BY rowIndex ASC
        """
    )
    suspend fun getItemsForInvoice(
        invoiceId: Int
    ): List<InvoiceItemEntity>

    /*
     * برای Sync یا تشخیص tombstone.
     */
    @Query(
        """
        SELECT *
        FROM invoice_items
        WHERE invoiceId = :invoiceId
        ORDER BY rowIndex ASC
        """
    )
    suspend fun getAllItemsForInvoice(
        invoiceId: Int
    ): List<InvoiceItemEntity>

    @Query(
        """
        SELECT *
        FROM invoice_items
        WHERE id = :itemId
        LIMIT 1
        """
    )
    suspend fun getItemById(
        itemId: Int
    ): InvoiceItemEntity?

    @Query(
        """
        SELECT *
        FROM invoice_items
        WHERE uid = :uid
        LIMIT 1
        """
    )
    suspend fun getItemByUid(
        uid: String
    ): InvoiceItemEntity?

    // =========================================================
    // Invoice items - Local write
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(
        items: List<InvoiceItemEntity>
    )

    @Update
    suspend fun updateItem(
        item: InvoiceItemEntity
    ): Int

    @Upsert
    suspend fun upsertItems(
        items: List<InvoiceItemEntity>
    )

    // =========================================================
    // Invoice items - Sync
    // =========================================================

    @Query(
        """
    SELECT *
    FROM invoice_items
    ORDER BY updatedAt ASC
    """
    )
    suspend fun getAllInvoiceItemsForSync():
            List<InvoiceItemEntity>

    @Query(
        """
        SELECT *
        FROM invoice_items
        WHERE isSynced = 0
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getUnsyncedInvoiceItems():
            List<InvoiceItemEntity>

    @Query(
        """
        UPDATE invoice_items
        SET isSynced = 1
        WHERE uid IN (:uids)
        """
    )
    suspend fun markInvoiceItemsSynced(
        uids: List<String>
    )

    // =========================================================
    // Invoice items - Soft delete
    // =========================================================

    @Query(
        """
        UPDATE invoice_items
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE id = :itemId
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteItemById(
        itemId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE invoice_items
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE id IN (:itemIds)
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteItemsByIds(
        itemIds: List<Int>,
        deletedAt: Long
    ): Int

    @Query(
        """
        UPDATE invoice_items
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE invoiceId = :invoiceId
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteItemsForInvoice(
        invoiceId: Int,
        deletedAt: Long
    ): Int

    @Query(
        """
        UPDATE invoice_items
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE invoiceId IN (
            SELECT id
            FROM invoice_documents
            WHERE orderId = :orderId
        )
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteItemsByOrderId(
        orderId: Int,
        deletedAt: Long
    ): Int

    @Query(
        """
        UPDATE invoice_items
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE invoiceId IN (
            SELECT invoice_documents.id
            FROM invoice_documents
            INNER JOIN orders
                ON orders.id = invoice_documents.orderId
            WHERE orders.clientId = :clientId
        )
          AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteItemsByClientId(
        clientId: Int,
        deletedAt: Long
    ): Int

    // =========================================================
    // Atomic operations
    // =========================================================

    @Transaction
    suspend fun saveInvoiceItemsAndTotals(
        updatedInvoice: InvoiceDocumentEntity,
        activeItems: List<InvoiceItemEntity>,
        removedItemIds: List<Int>,
        now: Long
    ) {
        if (removedItemIds.isNotEmpty()) {
            softDeleteItemsByIds(
                itemIds = removedItemIds,
                deletedAt = now
            )
        }

        if (activeItems.isNotEmpty()) {
            upsertItems(activeItems)
        }

        updateInvoice(updatedInvoice)
    }

    @Transaction
    suspend fun softDeleteInvoiceWithItems(
        invoiceId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ): Int {
        softDeleteItemsForInvoice(
            invoiceId = invoiceId,
            deletedAt = deletedAt
        )

        return softDeleteInvoiceById(
            invoiceId = invoiceId,
            deletedAt = deletedAt
        )
    }

    @Transaction
    suspend fun softDeleteInvoicesWithItemsByOrderId(
        orderId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ) {
        softDeleteItemsByOrderId(
            orderId = orderId,
            deletedAt = deletedAt
        )

        softDeleteInvoicesByOrderId(
            orderId = orderId,
            deletedAt = deletedAt
        )
    }

    @Transaction
    suspend fun softDeleteInvoicesWithItemsByClientId(
        clientId: Int,
        deletedAt: Long = System.currentTimeMillis()
    ) {
        softDeleteItemsByClientId(
            clientId = clientId,
            deletedAt = deletedAt
        )

        softDeleteInvoicesByClientId(
            clientId = clientId,
            deletedAt = deletedAt
        )
    }
}