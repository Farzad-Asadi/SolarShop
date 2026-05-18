package com.example.solarShop.data.room.tables.contract

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractTemplateDao {

    @Insert
    suspend fun insert(entity: ContractTemplateEntity): Long

    @Upsert
    suspend fun upsert(entity: ContractTemplateEntity) // ← Unit (بدون برگرداندن مقدار)

    @Update
    suspend fun update(entity: ContractTemplateEntity)

    @Delete
    suspend fun delete(entity: ContractTemplateEntity)

    @Query("SELECT * FROM contract_templates WHERE id = :id")
    suspend fun getById(id: Int): ContractTemplateEntity?

    @Query("DELETE FROM contract_templates WHERE id = :id AND isProtected = 0")
    suspend fun deleteById(id: Int): Int

    @Query("SELECT * FROM contract_templates ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ContractTemplateEntity>

    @Query("SELECT * FROM contract_templates ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ContractTemplateEntity>>

    @Query("SELECT COUNT(*) FROM contract_templates")
    suspend fun count(): Int

    @Query("DELETE FROM contract_templates")
    suspend fun clearTemplates()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(items: List<ContractTemplateEntity>)


}

@Dao
interface ContractTemplatePartyDao {

    @Insert
    suspend fun insertAll(items: List<ContractTemplatePartyEntity>)

    @Upsert
    suspend fun upsertAll(items: List<ContractTemplatePartyEntity>)

    @Query("DELETE FROM contract_template_parties WHERE templateId = :templateId")
    suspend fun deleteByTemplate(templateId: Int)

    @Query("SELECT * FROM contract_template_parties WHERE templateId = :templateId")
    suspend fun getByTemplate(templateId: Int): List<ContractTemplatePartyEntity>

    @Query("SELECT * FROM contract_template_parties WHERE templateId = :templateId")
    fun observeByTemplate(templateId: Int): Flow<List<ContractTemplatePartyEntity>>

    @Query("DELETE FROM contract_template_parties")
    suspend fun clearParties()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParties(items: List<ContractTemplatePartyEntity>)

    @Query("SELECT * FROM contract_template_parties WHERE id = :id")
    suspend fun getById(id: Int): ContractTemplatePartyEntity?
}

@Dao
interface ContractTemplateSectionDao {

    @Insert
    suspend fun insert(entity: ContractTemplateSectionEntity): Long

    @Insert
    suspend fun insertAll(items: List<ContractTemplateSectionEntity>)

    @Upsert
    suspend fun upsert(entity: ContractTemplateSectionEntity)

    @Upsert
    suspend fun upsertAll(items: List<ContractTemplateSectionEntity>)

    @Update
    suspend fun update(entity: ContractTemplateSectionEntity)

    @Delete
    suspend fun delete(entity: ContractTemplateSectionEntity)

    @Query("DELETE FROM contract_template_sections WHERE templateId = :templateId")
    suspend fun deleteByTemplate(templateId: Int)

    @Query("""
        SELECT * FROM contract_template_sections
        WHERE templateId = :templateId AND isDefaultVisible = 1
        ORDER BY orderNo ASC
    """)
    suspend fun visibleByTemplate(templateId: Int): List<ContractTemplateSectionEntity>

    @Query("""
        SELECT * FROM contract_template_sections
        WHERE templateId = :templateId
        ORDER BY orderNo ASC
    """)
    suspend fun allByTemplate(templateId: Int): List<ContractTemplateSectionEntity>

    @Query("""
        SELECT * FROM contract_template_sections
        WHERE templateId = :templateId
        ORDER BY orderNo ASC
    """)
    fun observeAllByTemplate(templateId: Int): Flow<List<ContractTemplateSectionEntity>>

    // کمکی‌ها برای ترتیب
    @Query("SELECT COALESCE(MAX(orderNo), 0) FROM contract_template_sections WHERE templateId = :templateId")
    suspend fun maxOrder(templateId: Int): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM contract_template_sections
            WHERE templateId = :templateId AND orderNo = :orderNo AND id != :excludeId
        )
    """)
    suspend fun existsOrderNo(templateId: Int, orderNo: Int, excludeId: Int?): Boolean

    @Query("UPDATE contract_template_sections SET orderNo = :newOrder, updatedAt = :updatedAt WHERE id = :sectionId")
    suspend fun updateOrder(sectionId: Int, newOrder: Int, updatedAt: Long = System.currentTimeMillis())

    // جابه‌جایی دو بند (↑/↓) — ترجیحاً در @Transaction سرویس/ریپو انجام بده
    @Transaction
    suspend fun swapOrders(aId: Int, aOrder: Int, bId: Int, bOrder: Int) {
        val now = System.currentTimeMillis()
        updateOrder(aId, bOrder, now)
        updateOrder(bId, aOrder, now)
    }

    @Query("DELETE FROM contract_template_sections")
    suspend fun clearSections()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(items: List<ContractTemplateSectionEntity>)

    @Query("SELECT * FROM contract_template_sections WHERE id = :id")
    suspend fun getById(id: Int): ContractTemplateSectionEntity?
}

@Dao
interface ContractTemplateNoteDao {

    @Insert
    suspend fun insert(entity: ContractTemplateNoteEntity): Long

    @Upsert
    suspend fun upsert(entity: ContractTemplateNoteEntity)

    @Update
    suspend fun update(entity: ContractTemplateNoteEntity)

    @Delete
    suspend fun delete(entity: ContractTemplateNoteEntity)

    @Query("""
        SELECT * FROM contract_template_notes
        WHERE sectionId = :sectionId
        ORDER BY orderNo ASC
    """)
    suspend fun allBySection(sectionId: Int): List<ContractTemplateNoteEntity>

    @Query("""
        SELECT * FROM contract_template_notes
        WHERE sectionId = :sectionId
        ORDER BY orderNo ASC
    """)
    fun observeAllBySection(sectionId: Int): Flow<List<ContractTemplateNoteEntity>>

    @Query("SELECT COALESCE(MAX(orderNo), 0) FROM contract_template_notes WHERE sectionId = :sectionId")
    suspend fun maxOrder(sectionId: Int): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM contract_template_notes
            WHERE sectionId = :sectionId AND orderNo = :orderNo AND id != :excludeId
        )
    """)
    suspend fun existsOrderNo(sectionId: Int, orderNo: Int, excludeId: Int?): Boolean

    @Query("UPDATE contract_template_notes SET orderNo = :newOrder, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateOrder(noteId: Int, newOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun swapOrders(aId: Int, aOrder: Int, bId: Int, bOrder: Int) {
        val now = System.currentTimeMillis()
        updateOrder(aId, bOrder, now)
        updateOrder(bId, aOrder, now)
    }

    @Query("DELETE FROM contract_template_notes WHERE sectionId IN (:sectionIds)")
    suspend fun deleteBySections(sectionIds: List<Int>)

    @Query("SELECT * FROM contract_template_notes WHERE id = :id")
    suspend fun getById(id: Int): ContractTemplateNoteEntity?

    @Insert
    suspend fun insertAll(items: List<ContractTemplateNoteEntity>)

    @Query("SELECT * FROM contract_template_notes WHERE sectionId = :sectionId ORDER BY orderNo ASC")
    suspend fun bySection(sectionId: Int): List<ContractTemplateNoteEntity>

    @Query("DELETE FROM contract_template_notes")
    suspend fun clearAll()


    @Query("UPDATE contract_template_notes SET orderNo = :newOrder WHERE id = :noteId")
    suspend fun updateOrder(noteId: Int, newOrder: Int)


    @Query("DELETE FROM contract_template_notes")
    suspend fun clearNotes()



}


@Dao
interface ContractTemplateFullDao {

    @Transaction
    @Query("SELECT * FROM contract_templates WHERE id = :id")
    suspend fun getFullById(id: Int): ContractTemplateFull?

    @Transaction
    @Query("SELECT * FROM contract_templates ORDER BY updatedAt DESC")
    suspend fun getAllFull(): List<ContractTemplateFull>

    @Transaction
    @Query("SELECT * FROM contract_templates ORDER BY updatedAt DESC")
    fun observeAllFull(): Flow<List<ContractTemplateFull>>

    @Transaction
    @Query("SELECT * FROM contract_templates WHERE id = :id")
    fun observeFullById(id: Int): Flow<ContractTemplateFull?>

}







