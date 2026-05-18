package com.example.solarShop.data.room.tables.contract

import androidx.room.Embedded
import androidx.room.Relation

//Template
data class SectionWithNotes(
    @Embedded val section: ContractTemplateSectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sectionId",
        entity = ContractTemplateNoteEntity::class
    )
    val notes: List<ContractTemplateNoteEntity>
)

data class ContractTemplateFull(
    @Embedded val template: ContractTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId",
        entity = ContractTemplatePartyEntity::class
    )
    val parties: List<ContractTemplatePartyEntity>,
    @Relation(
        entity = ContractTemplateSectionEntity::class,
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val sectionsWithNotes: List<SectionWithNotes>
)





//Instance
data class InstanceSectionWithNotes(
    @Embedded val section: ContractInstanceSectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "instanceSectionId",
        entity = ContractInstanceNoteEntity::class
    )
    val notes: List<ContractInstanceNoteEntity>
)

data class ContractInstanceFull(
    @Embedded val instance: ContractInstanceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "instanceId",
        entity = ContractInstancePartyEntity::class
    )
    val parties: List<ContractInstancePartyEntity>,
    @Relation(
        entity = ContractInstanceSectionEntity::class,
        parentColumn = "id",
        entityColumn = "instanceId"
    )
    val sectionsWithNotes: List<InstanceSectionWithNotes>
)


