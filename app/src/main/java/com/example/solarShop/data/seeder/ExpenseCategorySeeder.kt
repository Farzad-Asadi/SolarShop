package com.example.solarShop.data.seeder

import com.example.solarShop.data.local.database.AppDatabase
import com.example.solarShop.data.room.tables.orderAll.orderCost.ExpenseCategoryEntity
import javax.inject.Inject

class ExpenseCategorySeeder @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun insertDefaultsIfEmpty() {
        val dao = db.orderCostDao()

        // اگر قبلاً چیزی هست، دست نزن
        val count = dao.countAll()
        if (count > 0) return

        val defaults = listOf(
            "یراق",
            "حمل",
            "رنگ",
            "MDF",
            "دستمزد",
            "برش",
            "نصب",
            "متفرقه"
        )

        defaults.forEach { title ->
            dao.insertIgnore(ExpenseCategoryEntity(title = title.trim()))
        }
    }
}
