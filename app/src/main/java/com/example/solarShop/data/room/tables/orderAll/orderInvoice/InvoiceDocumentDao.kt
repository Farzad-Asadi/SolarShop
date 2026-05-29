package com.example.solarShop.data.room.tables.orderAll.orderInvoice

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.solarShop.InvoiceType
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDocumentDao {

    // --- اسناد ---

    @Transaction
    @Query("SELECT * FROM invoice_documents WHERE id = :id")
    suspend fun getInvoiceWithItems(id: Int): InvoiceWithItems?

    @Query("SELECT * FROM invoice_documents WHERE orderId = :orderId AND type = :type ORDER BY createdAt DESC")
    suspend fun getInvoicesForOrder(
        orderId: Int,
        type: InvoiceType
    ): List<InvoiceDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceDocumentEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceDocumentEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceDocumentEntity)

    @Query("SELECT * FROM invoice_documents WHERE id = :id")
    suspend fun getInvoiceById(id: Int): InvoiceDocumentEntity?

    // --- ردیف‌ها ---

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY rowIndex ASC")
    suspend fun getItemsForInvoice(invoiceId: Int): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Update
    suspend fun updateItem(item: InvoiceItemEntity)

    @Delete
    suspend fun deleteItem(item: InvoiceItemEntity)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Int)

    @Query("SELECT * FROM invoice_documents WHERE orderId = :orderId AND type = :type ORDER BY createdAt DESC")
    fun observeInvoicesForOrder(
        orderId: Int,
        type: InvoiceType
    ): Flow<List<InvoiceDocumentEntity>>

}