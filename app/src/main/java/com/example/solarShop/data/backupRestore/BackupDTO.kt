package com.example.solarShop.data.backupRestore

import kotlinx.serialization.Serializable

@Serializable
data class BackupDTO(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val questions: List<QuestionDTO>,
    val answers: List<AnswerDTO>,
    val routes: List<RouteDTO>,
    val answerImages: List<AnswerImageDTO>, // نگاشت id پاسخ → اسامی فایل‌ها
    val contractTemplates: List<ContractTemplateBundleDTO> = emptyList()
)

@Serializable data class QuestionDTO(
    val id: Int?,
    val title: String,
    val userKey:String?=null
)

@Serializable data class AnswerDTO(
    val id: Int?,
    val questionId: Int,
    val title: String
)

@Serializable data class RouteDTO(
    val answerId: Int,
    val nextQuestionId: Int
)

@Serializable data class AnswerImageDTO(
    val answerId: Int,
    val fileName: String
)




//Contract

@Serializable
data class ContractTemplateBundleDTO(
    val template: ContractTemplateDTO,
    val parties: List<ContractTemplatePartyDTO>,
    val sections: List<ContractTemplateSectionDTO>,
    val notes: List<ContractTemplateNoteDTO> = emptyList() // تبصره‌های همهٔ مواد (flat)
)

/* ---------- Template ---------- */

@Serializable
data class ContractTemplateDTO(
    val id: Int? = null,
    val userKey: String?= null,
    val title: String,
    val description: String? = null,
    val isProtected: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ContractTemplatePartyDTO(
    val id: Int? = null,
    val templateId: Int,
    val role: String,                 // "employer" | "contractor"
    val fullName: String? = null,
    val fatherFullName: String? = null,
    val nationalId: String? = null,
    val companyName: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ContractTemplateSectionDTO(
    val id: Int? = null,
    val templateId: Int,
    val orderNo: Int,                 // شماره ماده
    val title: String,
    val body: String,
    val isDefaultVisible: Boolean = true,
    val isRequired: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ContractTemplateNoteDTO(
    val id: Int? = null,
    val sectionId: Int,               // FK به همان Section
    val orderNo: Int,                 // شماره تبصره در همان ماده
    val title: String? = null,        // تبصره ممکن است بی‌عنوان باشد
    val body: String,
    val createdAt: Long,
    val updatedAt: Long
)




