package com.example.solarShop.data.room.tables.orderAll.orderInvoice

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.solarShop.InvoiceType


@Dao
interface InvoiceTemplateDao {

    @Query("SELECT * FROM invoice_templates ORDER BY type, name")
    suspend fun getAllTemplates(): List<InvoiceTemplateEntity>

    @Query("SELECT * FROM invoice_templates WHERE type = :type ORDER BY name")
    suspend fun getTemplatesByType(type: InvoiceType): List<InvoiceTemplateEntity>

    @Query("SELECT * FROM invoice_templates WHERE isDefaultForType = 1 AND type = :type LIMIT 1")
    suspend fun getDefaultTemplateForType(type: InvoiceType): InvoiceTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: InvoiceTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: InvoiceTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: InvoiceTemplateEntity)

    @Query("SELECT * FROM invoice_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Int): InvoiceTemplateEntity?

}