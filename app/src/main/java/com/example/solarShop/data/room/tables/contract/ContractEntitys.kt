package com.example.solarShop.data.room.tables.contract

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


/* =============== TEMPLATE =============== */

@Entity(tableName = "contract_templates")
data class ContractTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userKey: String?,
    val title: String,
    val description: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isProtected: Boolean = false
)

@Entity(
    tableName = "contract_template_parties",
    foreignKeys = [ForeignKey(
        entity = ContractTemplateEntity::class,
        parentColumns = ["id"],
        childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["templateId"], unique = false),
        Index(value = ["templateId", "role"], unique = true) // یک کارفرما/پیمانکار
    ]
)
data class ContractTemplatePartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: Int,
    val role: String, // پیمانکار / خریدار
    val fullName: String?,
    val fatherFullName: String?,
    val nationalId: String?,
    val companyName: String?,
    val address: String?,
    val phone: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contract_template_sections",
    foreignKeys = [ForeignKey(
        entity = ContractTemplateEntity::class,
        parentColumns = ["id"],
        childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["templateId"], unique = false),
        Index(value = ["templateId","orderNo"], unique = true) // شماره یکتا در هر تمپلیت
    ]
)
data class ContractTemplateSectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: Int,
    val orderNo: Int,                 // 👈 برگشت داده شد
    val title: String,
    val body: String,
    val isDefaultVisible: Boolean = true,
    val isRequired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contract_template_notes",
    foreignKeys = [ForeignKey(
        entity = ContractTemplateSectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["sectionId"], unique = false),
        Index(value = ["sectionId","orderNo"], unique = true) // تبصره‌های ماده؛ شماره یکتا
    ]
)
data class ContractTemplateNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sectionId: Int,
    val orderNo: Int,                 // 👈 ترتیب تبصره‌ها
    val title: String?,               // ممکن است بدون عنوان باشد
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


data class ContractTemplateAggregate(
    val template: ContractTemplateEntity,
    val parties: List<ContractTemplatePartyEntity>,
    val sections: List<ContractTemplateSectionEntity>,
    val notes: List<ContractTemplateNoteEntity>
)



/* =============== INSTANCE =============== */

@Entity(
    tableName = "contract_instances",
    indices = [Index(value = ["orderId"], unique = true)]
)
data class ContractInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val templateId: Int?,
    val title: String,
    val description: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val renderedHtml: String?,
    val pdfUri: String?
)

@Entity(
    tableName = "contract_instance_parties",
    foreignKeys = [ForeignKey(
        entity = ContractInstanceEntity::class,
        parentColumns = ["id"],
        childColumns = ["instanceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["instanceId"], unique = false),
        Index(value = ["instanceId","role"], unique = true)
    ]
)
data class ContractInstancePartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val instanceId: Int,
    val role: String,
    val fullName: String?,
    val fatherFullName: String?,
    val nationalId: String?,
    val companyName: String?,
    val address: String?,
    val phone: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contract_instance_sections",
    foreignKeys = [ForeignKey(
        entity = ContractInstanceEntity::class,
        parentColumns = ["id"],
        childColumns = ["instanceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["instanceId"], unique = false),
        Index(value = ["instanceId","orderNo"], unique = true)
    ]
)
data class ContractInstanceSectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val instanceId: Int,
    val orderNo: Int,                 // 👈 لازم
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contract_instance_notes",
    foreignKeys = [ForeignKey(
        entity = ContractInstanceSectionEntity::class,  // 👈 اصلاح FK
        parentColumns = ["id"],
        childColumns = ["instanceSectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["instanceSectionId"], unique = false),
        Index(value = ["instanceSectionId","orderNo"], unique = true)
    ]
)
data class ContractInstanceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val instanceSectionId: Int,       // 👈 به InstanceSection وصل است
    val orderNo: Int,
    val title: String?,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
