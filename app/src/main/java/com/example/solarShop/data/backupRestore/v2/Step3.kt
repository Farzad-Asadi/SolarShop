package com.example.solarShop.data.backupRestore.v2



// ============================================================================
// مرحلهٔ ۳ — ContractsBackupProvider (فقط کدهای این مرحله)
// فایل: contracts/ContractsBackupProvider.kt
// ----------------------------------------------------------------------------
// نکته‌ها:
// - snapshot(): contracts/data.json + کپی فایل‌های پوشهٔ internal files/contracts → contracts/files/*
// - restore(): فعلاً سیاست Overwrite (پاک‌سازی جداول قرارداد و درج مجدد)
// - در مرحلهٔ ۴ سیاست‌های Skip/Merge اضافه می‌شود
// ============================================================================

import com.example.solarShop.data.room.tables.contract.ContractTemplateEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateFullDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionEntity
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject

/* -------------------------------
* DTO های قرارداد برای V2 (contracts/data.json)
* — از DTO های قدیمی جدا تا مهاجرت ساده بماند
* ------------------------------- */
@Serializable
data class ContractsDTO(
    val version: Int = 1,
    val templates: List<TemplateBundleDTO>
)


@Serializable
data class TemplateBundleDTO(
    val template: TemplateDTO,
    val parties: List<PartyDTO>,
    val sections: List<SectionDTO>,
    val notes: List<NoteDTO> = emptyList()
)


@Serializable data class TemplateDTO(
    val id: Int? = null,
    val userKey: String? = null,
    val title: String,
    val description: String? = null,
    val isProtected: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable data class PartyDTO(
    val id: Int? = null,
    val templateId: Int,
    val role: String,
    val fullName: String? = null,
    val fatherFullName: String? = null,
    val nationalId: String? = null,
    val companyName: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)


@Serializable data class SectionDTO(
    val id: Int? = null,
    val templateId: Int,
    val orderNo: Int,
    val title: String,
    val body: String,
    val isDefaultVisible: Boolean = true,
    val isRequired: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)


@Serializable data class NoteDTO(
    val id: Int? = null,
    val sectionId: Int,
    val orderNo: Int,
    val title: String? = null,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long
)

/* -------------------------------
* ContractsBackupProvider — Provider قراردادها
* ------------------------------- */

class ContractsBackupProvider @Inject constructor(
    private val fullDao: ContractTemplateFullDao
) : BackupProvider {


    override val category: BackupCategory = BackupCategory.CONTRACTS


    override suspend fun snapshot(ctx: BackupContext): ProviderSnapshot {
        // 1) خواندن ساختار کامل قالب‌ها (Template + Parties + Sections + Notes)
        val full = fullDao.getAllFull()

        // 2) نگاشت به DTO های V2
        val bundles = full.map { f ->
            TemplateBundleDTO(
                template = TemplateDTO(
                    id = f.template.id,
                    userKey = f.template.userKey,
                    title = f.template.title,
                    description = f.template.description,
                    isProtected = f.template.isProtected,
                    createdAt = f.template.createdAt,
                    updatedAt = f.template.updatedAt
                ),
                parties = f.parties.map { p ->
                    PartyDTO(
                        id = p.id,
                        templateId = p.templateId,
                        role = p.role,
                        fullName = p.fullName,
                        fatherFullName = p.fatherFullName,
                        nationalId = p.nationalId,
                        companyName = p.companyName,
                        address = p.address,
                        phone = p.phone,
                        createdAt = p.createdAt,
                        updatedAt = p.updatedAt
                    )
                },
                sections = f.sectionsWithNotes.map { swn ->
                    SectionDTO(
                        id = swn.section.id,
                        templateId = swn.section.templateId,
                        orderNo = swn.section.orderNo,
                        title = swn.section.title,
                        body = swn.section.body,
                        isDefaultVisible = swn.section.isDefaultVisible,
                        isRequired = swn.section.isRequired,
                        createdAt = swn.section.createdAt,
                        updatedAt = swn.section.updatedAt
                    )
                },
                notes = f.sectionsWithNotes.flatMap { swn ->
                    swn.notes.map { n ->
                        NoteDTO(
                            id = n.id,
                            sectionId = n.sectionId,
                            orderNo = n.orderNo,
                            title = n.title,
                            body = n.body,
                            createdAt = n.createdAt,
                            updatedAt = n.updatedAt
                        )
                    }
                }
            )
        }
        val dto = ContractsDTO(version = 1, templates = bundles)
        val jsonStr = ctx.json.encodeToString(ContractsDTO.serializer(), dto)


        // 3) فایل‌های فیزیکی قرارداد (اختیاری) → contracts/files/*
        val contractsDir = File(ctx.appContext.filesDir, "contracts")
        val files: List<FileRef> = contractsDir.listFiles()
            ?.filter { it.isFile }
            ?.map { f -> FileRef(f, zipPath = ZipLayout.Contracts.FILES + f.name) }
            ?: emptyList()


        return ProviderSnapshot(
            category = category,
            zipFolder = ZipLayout.Contracts.FOLDER,
            jsonFileName = ZipLayout.Contracts.DATA,
            jsonPayload = jsonStr,
            extraFiles = files
        )
    }
    override suspend fun restore(
        snapshot: ProviderSnapshot,
        options: RestoreOptions,
        ctx: RestoreContext
    ): RestoreReport {
        if (!options.selected.contains(category)) {
            return RestoreReport(category, inserted = 0, updated = 0, skipped = 0)
        }


        val data = ctx.json.decodeFromString(ContractsDTO.serializer(), snapshot.jsonPayload)


        // سیاست فعلی: Overwrite — جداول مرتبط را پاک و دوباره درج می‌کنیم
        when (options.conflictPolicy) {
            ConflictPolicy.Overwrite -> {
                ctx.db.contractTemplateSectionDao().clearSections()
                ctx.db.contractTemplatePartyDao().clearParties()
                ctx.db.contractTemplateDao().clearTemplates()
                ctx.db.contractTemplateNoteDao().clearAll() // اگر Dao جداگانه برای Note داری
            }
            ConflictPolicy.Skip, ConflictPolicy.Merge -> {
                // TODO(step4): Upsert هوشمند برای پرهیز از از دست دادن داده
                ctx.db.contractTemplateSectionDao().clearSections()
                ctx.db.contractTemplatePartyDao().clearParties()
                ctx.db.contractTemplateDao().clearTemplates()
                ctx.db.contractTemplateNoteDao().clearAll()
            }
        }


        // درج داده‌ها
        val templates = data.templates.map { it.template.toEntity() }
        val parties = data.templates.flatMap { it.parties.map(PartyDTO::toEntity) }
        val sections = data.templates.flatMap { it.sections.map(SectionDTO::toEntity) }
        val notes = data.templates.flatMap { it.notes.map(NoteDTO::toEntity) }


        ctx.db.contractTemplateDao().insertTemplates(templates)
        ctx.db.contractTemplatePartyDao().insertParties(parties)
        ctx.db.contractTemplateSectionDao().insertSections(sections)
        ctx.db.contractTemplateNoteDao().insertAll(notes)


        // کپی فایل‌های فیزیکی به internal files/contracts
        val destContractsDir = File(ctx.appContext.filesDir, "contracts").apply { mkdirs() }
        var copied = 0
        snapshot.extraFiles.forEach { ref ->
            val dst = File(destContractsDir, File(ref.zipPath).name)
            ref.file.inputStream().use { ins -> dst.outputStream().use { outs -> ins.copyTo(outs) } }
            copied++
        }


        val insertedCount = templates.size + parties.size + sections.size + notes.size
        return RestoreReport(
            category = category,
            inserted = insertedCount,
            updated = 0,
            skipped = 0,
            errors = emptyList()
        )
    }
}


/* -------------------------------
* مبدل‌های DTO ↔ Entity
* ------------------------------- */

private fun TemplateDTO.toEntity() = ContractTemplateEntity(
    id = this.id ?: 0,
    userKey  = this.userKey ,
    title = this.title,
    description = this.description,
    isProtected = this.isProtected,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)


private fun PartyDTO.toEntity() = ContractTemplatePartyEntity(
    id = this.id ?: 0,
    templateId = this.templateId,
    role = this.role,
    fullName = this.fullName,
    fatherFullName = this.fatherFullName,
    nationalId = this.nationalId,
    companyName = this.companyName,
    address = this.address,
    phone = this.phone,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)


private fun SectionDTO.toEntity() = ContractTemplateSectionEntity(
    id = this.id ?: 0,
    templateId = this.templateId,
    orderNo = this.orderNo,
    title = this.title,
    body = this.body,
    isDefaultVisible = this.isDefaultVisible,
    isRequired = this.isRequired
)


private fun NoteDTO.toEntity() = ContractTemplateNoteEntity(
    id = this.id ?: 0,
    sectionId = this.sectionId,
    orderNo = this.orderNo,
    title = this.title,
    body = this.body,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)