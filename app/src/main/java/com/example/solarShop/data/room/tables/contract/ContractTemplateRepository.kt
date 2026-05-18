package com.example.solarShop.data.room.tables.contract

import kotlinx.coroutines.flow.Flow

interface ContractTemplateRepository {

    fun observeAll(): Flow<List<ContractTemplateEntity>>
    suspend fun insert(entity: ContractTemplateEntity): Long
    suspend fun deleteById(id: Int): Int

    // Read
    fun observeAllTemplatesFull(): Flow<List<ContractTemplateFull>>
    fun observeTemplateFullById(id: Int): Flow<ContractTemplateFull?>
    suspend fun getTemplateFullById(id: Int): ContractTemplateFull?
    suspend fun getTemplateAggregate(templateId: Int): ContractTemplateAggregate?

    // Seed
//    suspend fun seedDefaultsIfEmpty()

    // Sections
    suspend fun addSection(templateId: Int, title: String, body: String, isRequired: Boolean = false, isDefaultVisible: Boolean = true): Int
    suspend fun updateSection(sectionId: Int, title: String, body: String, isRequired: Boolean, isDefaultVisible: Boolean)
    suspend fun deleteSection(sectionId: Int)
    suspend fun swapSectionOrders(templateId: Int, aId: Int, bId: Int)

    // Notes
    suspend fun addNote(sectionId: Int, title: String?, body: String): Int
    suspend fun updateNote(noteId: Int, title: String?, body: String)
    suspend fun deleteNote(noteId: Int)
    suspend fun deleteNoteAndRenumber(sectionId: Int, noteId: Int)

    fun observeFullById(id: Int): Flow<ContractTemplateFull?>



    suspend fun createInstanceFromTemplate(
        orderId: Int,
        templateId: Int,
        instanceTitle: String,          // مثلا "قرارداد فروش - سفارش #123"
        instanceDescription: String = ""
    ): Int




}


