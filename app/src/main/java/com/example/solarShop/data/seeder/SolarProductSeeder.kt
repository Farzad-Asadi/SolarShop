package com.example.solarShop.data.seeder

import com.example.solarShop.data.local.dao.pricing.PricingDao
import com.example.solarShop.data.local.dao.product.ProductDao
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProfitRuleEntity
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SolarProductSeeder @Inject constructor(
    private val productDao: ProductDao,
    private val pricingDao: PricingDao
) {

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        seedUnits()
        seedCategories()
        seedBrands()
        seedCurrencyRate()
        seedDefaultProfitRule()
    }

    private suspend fun seedUnits() {
        listOf(
            ProductUnitEntity(name = "عدد", symbol = "عدد"),
            ProductUnitEntity(name = "متر", symbol = "m"),
            ProductUnitEntity(name = "حلقه", symbol = "حلقه"),
            ProductUnitEntity(name = "بسته", symbol = "بسته")
        ).forEach {
            productDao.upsertUnit(it)
        }
    }

    private suspend fun seedCategories() {
        listOf(
            ProductCategoryEntity(name = "پنل خورشیدی", sortOrder = 1),
            ProductCategoryEntity(name = "سانورتر", sortOrder = 2),
            ProductCategoryEntity(name = "اینورتر", sortOrder = 3),
            ProductCategoryEntity(name = "باتری", sortOrder = 4),
            ProductCategoryEntity(name = "شارژ کنترلر", sortOrder = 5),
            ProductCategoryEntity(name = "کابل و اتصالات", sortOrder = 6),
            ProductCategoryEntity(name = "استراکچر", sortOrder = 7)
        ).forEach {
            productDao.upsertCategory(it)
        }
    }

    private suspend fun seedBrands() {
        listOf(
            "JA Solar",
            "LONGi",
            "Growatt",
            "Power Solid",
            "Medal Power",
            "Gogo Power"
        ).forEach {
            productDao.upsertBrand(ProductBrandEntity(name = it))
        }
    }

    private suspend fun seedCurrencyRate() {
        pricingDao.insertCurrencyRate(
            CurrencyRateEntity(
                currencyCode = "USD",
                rateToman = 170000,
                source = "seed",
                note = "نرخ اولیه تستی"
            )
        )
    }

    private suspend fun seedDefaultProfitRule() {
        pricingDao.upsertProfitRule(
            ProfitRuleEntity(
                categoryId = null,
                title = "سود پیش‌فرض",
                profitPercent = 15.0,
                fixedProfitToman = 0,
                isDefault = true
            )
        )
    }
}