package com.example.solarShop.data.seeder

import androidx.room.withTransaction
import com.example.solarShop.data.backupRestore.DefaultContractTemplates
import com.example.solarShop.data.local.database.AppDatabase
import com.example.solarShop.data.room.tables.contract.ContractTemplateDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyDao
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractsTemplateSeeder @Inject constructor(
    private val db: AppDatabase,
    private val templateDao: ContractTemplateDao,
    private val partyDao: ContractTemplatePartyDao,
    private val sectionDao: ContractTemplateSectionDao,
    private val noteDao: ContractTemplateNoteDao,
) {

    /**
     * فقط وقتی چیزی در جدول template نیست، قالب‌های پیش‌فرض رو وارد می‌کنه.
     */
    suspend fun insertDefaultsIfEmpty() {
        val count = templateDao.count()
        if (count > 0) return

        db.withTransaction {
            DefaultContractTemplates.all.forEach { spec ->
                // 1) Template
                val templateId = templateDao.insert(
                    ContractTemplateEntity(
                        userKey = null,
                        title = spec.title,
                        description = spec.description,
                        isProtected = true
                        // اگر فیلدهای زمانی داری، اضافه کن:
                        // createdAt = System.currentTimeMillis(),
                        // updatedAt = System.currentTimeMillis()
                    )
                ).toInt()

                // 2) Parties
                val parties = spec.parties.map { p ->
                    ContractTemplatePartyEntity(
                        templateId = templateId,
                        role = p.role,
                        fullName = p.fullName,
                        fatherFullName = p.fatherFullName,
                        nationalId = p.nationalId,
                        companyName = p.companyName,
                        address = p.address,
                        phone = p.phone
                        // اگر فیلدهای زمانی داری، اضافه کن
                    )
                }
                if (parties.isNotEmpty()) {
                    partyDao.insertAll(parties)
                }

                // 3) Sections (به ترتیب orderNo)
                val sections = spec.sections
                    .sortedBy { it.orderNo }
                    .map { s ->
                        ContractTemplateSectionEntity(
                            templateId = templateId,
                            orderNo = s.orderNo,
                            title = s.title,
                            body = s.body,
                            isDefaultVisible = s.isDefaultVisible,
                            isRequired = s.isRequired
                            // اگر فیلدهای زمانی داری، اضافه کن
                        )
                    }
                if (sections.isNotEmpty()) {
                    sectionDao.insertAll(sections)
                }

                // لازم داریم map از orderNo → sectionId برای وصل‌کردن تبصره‌ها
                val insertedSections = sectionDao.allByTemplate(templateId)
                val sectionIdByOrderNo = insertedSections.associate { it.orderNo to it.id }

                // 4) Notes (اختیاری)
                if (spec.notes.isNotEmpty()) {
                    val notes = spec.notes.mapNotNull { n ->
                        val sectionId = sectionIdByOrderNo[n.sectionOrderNo]
                            ?: return@mapNotNull null // اگر سکشن پیدا نشد، ردش کن (یا throw کن)
                        ContractTemplateNoteEntity(
                            sectionId = sectionId,
                            orderNo = n.orderNo,
                            title = n.title,
                            body = n.body
                            // اگر فیلدهای زمانی داری، اضافه کن
                        )
                    }.sortedWith(
                        compareBy<ContractTemplateNoteEntity>({ it.sectionId }).thenBy { it.orderNo }
                    )

                    if (notes.isNotEmpty()) {
                        // اگر insertAll نداری، از حلقه زیر استفاده کن
                        noteDao.insertAll(notes)
                        // یا:
                        // notes.forEach { noteDao.insert(it) }
                    }
                }
            }
        }
    }
}
