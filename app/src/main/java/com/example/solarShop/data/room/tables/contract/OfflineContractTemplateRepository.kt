package com.example.solarShop.data.room.tables.contract

import androidx.room.withTransaction
import com.example.solarShop.data.room.appDatabase.AppDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineContractTemplateRepository @Inject constructor(
    private val db: AppDatabase,
    private val templateDao: ContractTemplateDao,
    private val sectionDao: ContractTemplateSectionDao,
    private val noteDao: ContractTemplateNoteDao,
    private val templateFullDao: ContractTemplateFullDao,
    private val instanceDao: ContractInstanceDao,
    private val instanceSectionDao: ContractInstanceSectionDao,
    private val instanceNoteDao: ContractInstanceNoteDao,
) : ContractTemplateRepository {
    override fun observeAll(): Flow<List<ContractTemplateEntity>> =
        templateDao.observeAll()

    override suspend fun insert(entity: ContractTemplateEntity): Long =
        templateDao.insert(entity)





    override suspend fun createInstanceFromTemplate(
        orderId: Int,
        templateId: Int,
        instanceTitle: String,
        instanceDescription: String
    ): Int = db.withTransaction {
        val full = templateFullDao.getFullById(templateId)
            ?: throw IllegalStateException("Template not found: $templateId")

        val instanceId = instanceDao.insert(
            ContractInstanceEntity(
                id = 0,
                orderId = orderId,
                templateId = templateId,
                title = instanceTitle,
                description = instanceDescription,
                renderedHtml = null,
                pdfUri = null
            )
        ).toInt()

        val sectionIdMap = mutableMapOf<Int, Int>()
        full.sectionsWithNotes.map { it.section }.sortedBy { it.orderNo }.forEach { s ->
            val newSectionId = instanceSectionDao.insert(
                ContractInstanceSectionEntity(
                    id = 0,
                    instanceId = instanceId,
                    orderNo = s.orderNo,
                    title = s.title,
                    body = s.body
                )
            ).toInt()
            sectionIdMap[s.id] = newSectionId
        }

        full.sectionsWithNotes.forEach { swn ->
            val newSecId = sectionIdMap[swn.section.id] ?: return@forEach
            swn.notes.sortedBy { it.orderNo }.forEach { n ->
                instanceNoteDao.insert(
                    ContractInstanceNoteEntity(
                        id = 0,
                        instanceSectionId = newSecId,
                        orderNo = n.orderNo,
                        title = n.title,
                        body = n.body
                    )
                )
            }
        }

        instanceId
    }











    override suspend fun deleteById(id: Int): Int =
        templateDao.deleteById(id)


    override fun observeAllTemplatesFull(): Flow<List<ContractTemplateFull>> =
        templateFullDao.observeAllFull()

    override fun observeTemplateFullById(id: Int): Flow<ContractTemplateFull?> =
        templateFullDao.observeFullById(id)

    override suspend fun getTemplateFullById(id: Int) =
        templateFullDao.getFullById(id)

    override suspend fun getTemplateAggregate(templateId: Int): ContractTemplateAggregate? {
        val full = templateFullDao.getFullById(templateId) ?: return null
        val sections = full.sectionsWithNotes.map { it.section }.sortedBy { it.orderNo }
        val notes = full.sectionsWithNotes
            .flatMap { it.notes }
            // نکته: برای پایداریِ مرتب‌سازی، هم شماره ماده هم شماره تبصره را لحاظ کن
            .sortedWith(compareBy(
                { sn -> sections.firstOrNull { it.id == sn.sectionId }?.orderNo ?: Int.MAX_VALUE },
                { sn -> sn.orderNo }
            ))
        return ContractTemplateAggregate(
            template = full.template,
            parties = full.parties,              // roles: EMPLOYER / CONTRACTOR (index یونیک)
            sections = sections,
            notes = notes
        )
    }

//    override suspend fun seedDefaultsIfEmpty() {
//        seeder.insertDefaultsIfEmpty()
//    }

    override suspend fun addSection(
        templateId: Int,
        title: String,
        body: String,
        isRequired: Boolean,
        isDefaultVisible: Boolean
    ): Int = db.withTransaction {
        val next = sectionDao.maxOrder(templateId) + 1
        val entity = ContractTemplateSectionEntity(
            templateId = templateId,
            orderNo = next,
            title = title,
            body = body,
            isRequired = isRequired,
            isDefaultVisible = isDefaultVisible,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        sectionDao.insert(entity).toInt()
    }

    override suspend fun updateSection(
        sectionId: Int,
        title: String,
        body: String,
        isRequired: Boolean,
        isDefaultVisible: Boolean
    ) {
        val now = System.currentTimeMillis()
        // یک کوئری کمکی بگذار تا با id بخوانی؛ یا از full بیاوری
        val all = sectionDao // ساده: لازم داری getById داشته باشی
        // بهتر: یک متد getById در DAO اضافه کن
        val old = db.contractTemplateSectionDao().getById(sectionId) // اضافه‌اش کن در DAO
        if (old != null) {
            sectionDao.update(old.copy(
                title = title,
                body = body,
                isRequired = isRequired,
                isDefaultVisible = isDefaultVisible,
                updatedAt = now
            ))
        }
    }

    override suspend fun deleteSection(sectionId: Int) {
        // ساده: delete مستقیم
        val dao = sectionDao
        val all = db.contractTemplateSectionDao().getById(sectionId) ?: return
        // تبصره‌های زیرش cascade با FK حذف می‌شوند (اگر onDelete=CASCADE گذاشته‌ای)
        sectionDao.delete(all)
    }

    override suspend fun swapSectionOrders(templateId: Int, aId: Int, bId: Int) = db.withTransaction {
        // برای سادگی: orderNo فعلی دو id را بخوان و جابه‌جا کن
        val list = sectionDao.allByTemplate(templateId)
        val a = list.firstOrNull { it.id == aId } ?: return@withTransaction
        val b = list.firstOrNull { it.id == bId } ?: return@withTransaction
        sectionDao.swapOrders(aId, a.orderNo, bId, b.orderNo)
    }

    override suspend fun addNote(sectionId: Int, title: String?, body: String): Int = db.withTransaction {
        val next = noteDao.maxOrder(sectionId) + 1
        val entity = ContractTemplateNoteEntity(
            sectionId = sectionId,
            orderNo = next,
            title = title,
            body = body,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insert(entity).toInt()
    }

    override suspend fun updateNote(noteId: Int, title: String?, body: String) {
        // مشابه updateSection: getById برای Note هم اضافه کن
        val old = db.contractTemplateNoteDao().getById(noteId) // متد کمکی اضافه کن
        if (old != null) {
            noteDao.update(old.copy(
                title = title,
                body = body,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    override suspend fun deleteNote(noteId: Int) {
        val old = db.contractTemplateNoteDao().getById(noteId) ?: return
        noteDao.delete(old)
    }

    override suspend fun deleteNoteAndRenumber(sectionId: Int, noteId: Int) {
        db.withTransaction {
            // 1) حذف
            val note = noteDao.getById(noteId) ?: return@withTransaction
            noteDao.delete(note)

            // 2) لیست باقی‌مانده و رینامبر 1..N
            val remaining = noteDao.bySection(sectionId).sortedBy { it.orderNo }
            remaining.forEachIndexed { index, n ->
                val desired = index + 1
                if (n.orderNo != desired) {
                    noteDao.updateOrder(n.id, desired)
                }
            }
        }
    }

    override fun observeFullById(id: Int): Flow<ContractTemplateFull?> =
        templateFullDao.observeFullById(id)
}
