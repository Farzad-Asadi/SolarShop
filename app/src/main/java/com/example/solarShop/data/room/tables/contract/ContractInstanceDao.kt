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
interface ContractInstanceDao {

    @Insert
    suspend fun insert(entity: ContractInstanceEntity): Long

    @Upsert
    suspend fun upsert(entity: ContractInstanceEntity) // ← Unit

    @Update
    suspend fun update(entity: ContractInstanceEntity)

    @Delete
    suspend fun delete(entity: ContractInstanceEntity)

    @Query("SELECT * FROM contract_instances WHERE id = :id")
    suspend fun getById(id: Int): ContractInstanceEntity?

    @Query("SELECT * FROM contract_instances WHERE orderId = :orderId ORDER BY updatedAt DESC")
    suspend fun byOrder(orderId: Int): List<ContractInstanceEntity>

    @Query("SELECT * FROM contract_instances WHERE orderId = :orderId ORDER BY updatedAt DESC")
    fun observeByOrder(orderId: Int): Flow<List<ContractInstanceEntity>>

    @Query("UPDATE contract_instances SET renderedHtml = :html, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRenderedHtml(id: Int, html: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE contract_instances SET pdfUri = :pdfUri, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePdfUri(id: Int, pdfUri: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM contract_instances WHERE orderId = :orderId LIMIT 1")
    suspend fun getSingleByOrder(orderId: Int): ContractInstanceEntity?

    @Query("SELECT * FROM contract_instances WHERE orderId = :orderId LIMIT 1")
    fun observeSingleByOrder(orderId: Int): Flow<ContractInstanceEntity?>

    @Query("UPDATE contract_instances SET title = :newTitle WHERE id = :instanceId")
    suspend fun updateTitle(instanceId: Int, newTitle: String): Int

    @Query("DELETE FROM contract_instances WHERE id = :id")
    suspend fun deleteById(id: Int): Int


}

@Dao
interface ContractInstancePartyDao {

    @Insert
    suspend fun insert(entity: ContractInstancePartyEntity): Long

    @Query("DELETE FROM contract_instance_parties WHERE instanceId = :instanceId")
    suspend fun deleteByInstance(instanceId: Int): Int

    @Query("SELECT * FROM contract_instance_parties WHERE instanceId = :instanceId")
    fun observeByInstance(instanceId: Int): Flow<List<ContractInstancePartyEntity>>

    @Query("SELECT * FROM contract_instance_parties WHERE id = :id")
    suspend fun getById(id: Int): ContractInstancePartyEntity?



    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ContractInstancePartyEntity>): List<Long>

    @Query("SELECT * FROM contract_instance_parties WHERE instanceId = :instanceId ORDER BY id")
    suspend fun getByInstance(instanceId: Int): List<ContractInstancePartyEntity>

    @Upsert
    suspend fun upsertAll(items: List<ContractInstancePartyEntity>)


}

@Dao
interface ContractInstanceSectionDao {

    @Insert
    suspend fun insert(entity: ContractInstanceSectionEntity): Long

    @Insert
    suspend fun insertAll(items: List<ContractInstanceSectionEntity>)

    @Upsert
    suspend fun upsert(entity: ContractInstanceSectionEntity)

    @Upsert
    suspend fun upsertAll(items: List<ContractInstanceSectionEntity>)

    @Update
    suspend fun update(entity: ContractInstanceSectionEntity)

    @Delete
    suspend fun delete(entity: ContractInstanceSectionEntity)

    @Query("DELETE FROM contract_instance_sections WHERE instanceId = :instanceId")
    suspend fun deleteByInstance(instanceId: Int): Int




    @Query("""
        SELECT * FROM contract_instance_sections
        WHERE instanceId = :instanceId
        ORDER BY orderNo ASC
    """)
    suspend fun allByInstance(instanceId: Int): List<ContractInstanceSectionEntity>

    @Query("""
        SELECT * FROM contract_instance_sections
        WHERE instanceId = :instanceId
        ORDER BY orderNo ASC
    """)
    fun observeAllByInstance(instanceId: Int): Flow<List<ContractInstanceSectionEntity>>

    // ترتیب
    @Query("SELECT COALESCE(MAX(orderNo), 0) FROM contract_instance_sections WHERE instanceId = :instanceId")
    suspend fun maxOrder(instanceId: Int): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM contract_instance_sections
            WHERE instanceId = :instanceId AND orderNo = :orderNo AND id != :excludeId
        )
    """)
    suspend fun existsOrderNo(instanceId: Int, orderNo: Int, excludeId: Int?): Boolean

    @Query("UPDATE contract_instance_sections SET orderNo = :newOrder, updatedAt = :updatedAt WHERE id = :sectionId")
    suspend fun updateOrder(sectionId: Int, newOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun swapOrders(aId: Int, aOrder: Int, bId: Int, bOrder: Int) {
        val now = System.currentTimeMillis()
        updateOrder(aId, bOrder, now)
        updateOrder(bId, aOrder, now)
    }

    @Query("SELECT * FROM contract_instance_sections WHERE id = :id")
    suspend fun getById(id: Int): ContractInstanceSectionEntity?

}

@Dao
interface ContractInstanceNoteDao {

    @Insert
    suspend fun insert(entity: ContractInstanceNoteEntity): Long

    @Upsert
    suspend fun upsert(entity: ContractInstanceNoteEntity)

    @Update
    suspend fun update(entity: ContractInstanceNoteEntity)

    @Delete
    suspend fun delete(entity: ContractInstanceNoteEntity)

    @Query("""
        SELECT * FROM contract_instance_notes
        WHERE instanceSectionId = :instanceSectionId
        ORDER BY orderNo ASC
    """)
    suspend fun allByInstanceSection(instanceSectionId: Int): List<ContractInstanceNoteEntity>

    @Query("""
        SELECT * FROM contract_instance_notes
        WHERE instanceSectionId = :instanceSectionId
        ORDER BY orderNo ASC
    """)
    fun observeAllByInstanceSection(instanceSectionId: Int): Flow<List<ContractInstanceNoteEntity>>

    @Query("SELECT COALESCE(MAX(orderNo), 0) FROM contract_instance_notes WHERE instanceSectionId = :instanceSectionId")
    suspend fun maxOrder(instanceSectionId: Int): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM contract_instance_notes
            WHERE instanceSectionId = :instanceSectionId AND orderNo = :orderNo AND id != :excludeId
        )
    """)
    suspend fun existsOrderNo(instanceSectionId: Int, orderNo: Int, excludeId: Int?): Boolean

    @Query("UPDATE contract_instance_notes SET orderNo = :newOrder, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateOrder(noteId: Int, newOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun swapOrders(aId: Int, aOrder: Int, bId: Int, bOrder: Int) {
        val now = System.currentTimeMillis()
        updateOrder(aId, bOrder, now)
        updateOrder(bId, aOrder, now)
    }

    @Query("DELETE FROM contract_instance_notes WHERE instanceSectionId IN (:sectionIds)")
    suspend fun deleteByInstanceSections(sectionIds: List<Int>)

    @Query("SELECT * FROM contract_instance_notes WHERE id = :id")
    suspend fun getById(id: Int): ContractInstanceNoteEntity?


    @Query("""
    DELETE FROM contract_instance_notes
    WHERE instanceSectionId IN (
        SELECT id FROM contract_instance_sections WHERE instanceId = :instanceId
    )
""")
    suspend fun deleteByInstance(instanceId: Int): Int

}


@Dao
interface ContractInstanceFullDao {

    @Transaction
    @Query("SELECT * FROM contract_instances WHERE id = :id")
    suspend fun getFullById(id: Int): ContractInstanceFull?

    @Transaction
    @Query("SELECT * FROM contract_instances WHERE orderId = :orderId ORDER BY updatedAt DESC")
    suspend fun getFullByOrder(orderId: Int): List<ContractInstanceFull>

    @Transaction
    @Query("SELECT * FROM contract_instances WHERE id = :id")
    fun observeFullById(id: Int): Flow<ContractInstanceFull?>

    @Transaction
    @Query("SELECT * FROM contract_instances WHERE orderId = :orderId ORDER BY updatedAt DESC")
    fun observeFullByOrder(orderId: Int): Flow<List<ContractInstanceFull>>
}

