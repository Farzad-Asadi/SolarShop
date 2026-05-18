package com.example.solarShop.data.room.tables.orderAll.orderInvoice


import com.example.solarShop.data.room.appDatabase.InvoiceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceTemplateSeeder @Inject constructor(
    private val invoiceTemplateDao: InvoiceTemplateDao
) {

    suspend fun insertDefaultsIfEmpty() {
        // اگر قبلاً تمپلیت هست، کاری نکن
        val existing = invoiceTemplateDao.getAllTemplates()
        if (existing.isNotEmpty()) return

        val defaults = createDefaultInvoiceTemplates()
        defaults.forEach { tpl ->
            invoiceTemplateDao.insertTemplate(tpl)
        }
    }

    private fun createDefaultInvoiceTemplates(): List<InvoiceTemplateEntity> = listOf(
        // ۱) پیش‌فاکتور ساده
        InvoiceTemplateEntity(
            name = "پیش‌فاکتور ساده",
            type = InvoiceType.PROFORMA,
            title = "پیش‌فاکتور فروش",
            hasTax = false,
            defaultTaxPercent = null,
            showAmountInWords = false,
            isDefaultForType = true
        ),

        // ۲) فاکتور فروش ساده
        InvoiceTemplateEntity(
            name = "فاکتور فروش ساده",
            type = InvoiceType.INVOICE,
            title = "صورتحساب فروش کالا و خدمات",
            hasTax = true,
            defaultTaxPercent = 9f,        // ۹٪ پیش‌فرض (قابل تغییر بعداً)
            showAmountInWords = true,
            isDefaultForType = true
        )
    )
}
