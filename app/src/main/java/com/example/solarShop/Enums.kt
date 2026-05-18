package com.example.solarShop

enum class OrderTimeline(val value: String) {
    CREATE_ORDER("create_order"),
    CREATE_PRICE_ESTIMATE("create_price_estimate"),
    CREATE_SELECTED_CHOICE("create_selected_choice");

    companion object {
        fun fromValue(value: String): OrderTimeline? {
            return entries.find { it.value == value }
        }
    }
}

enum class AppLanguage(val languageTag: String, val isRtl: Boolean) {
    FA("fa", true),
    EN("en", false);

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            when (tag) {
                "fa" -> FA
                "en" -> EN
                else -> EN   // پیش‌فرض امن
            }
    }
}


enum class LengthUnit { METER, CENTIMETER }
enum class CurrencyUnit { TOMAN, RIAL }