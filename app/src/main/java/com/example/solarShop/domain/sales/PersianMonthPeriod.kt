package com.example.solarShop.domain.sales

import com.example.solarShop.utils.convertToPersianDate
import com.example.solarShop.utils.toPersianDigits
import java.time.Instant
import java.time.ZoneId

data class PersianMonthPeriod(
    val year: Int,
    val month: Int
) : Comparable<PersianMonthPeriod> {

    val key: Int
        get() = year * 100 + month

    val title: String
        get() = "${PERSIAN_MONTH_NAMES[month - 1]} ${year.toString().toPersianDigits()}"

    // بررسی می‌کند یک زمان مشخص داخل همین ماه شمسی قرار دارد یا نه.
    fun contains(epochMillis: Long): Boolean =
        fromEpochMillis(epochMillis) == this

    override fun compareTo(other: PersianMonthPeriod): Int =
        key.compareTo(other.key)

    companion object {
        // زمان Unix را با همان تبدیل تاریخ فعلی پروژه به سال و ماه شمسی تبدیل می‌کند.
        fun fromEpochMillis(
            epochMillis: Long,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): PersianMonthPeriod {
            val localDate =
                Instant.ofEpochMilli(epochMillis)
                    .atZone(zoneId)
                    .toLocalDate()

            val parts =
                convertToPersianDate(localDate)
                    .split("/")

            return PersianMonthPeriod(
                year = parts.getOrNull(0)?.toIntOrNull() ?: localDate.year,
                month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
            )
        }
    }
}

private val PERSIAN_MONTH_NAMES = listOf(
    "فروردین",
    "اردیبهشت",
    "خرداد",
    "تیر",
    "مرداد",
    "شهریور",
    "مهر",
    "آبان",
    "آذر",
    "دی",
    "بهمن",
    "اسفند"
)
