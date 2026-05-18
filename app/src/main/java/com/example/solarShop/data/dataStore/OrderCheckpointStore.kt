package com.example.solarShop.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.solarShop.data.room.tables.orderAll.OrderSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OrderSummaryCheckpoint(
    val priceEstimateTotal: Long = 0L,
    val catalogSelectedCount: Int = 0,
    val hasContract: Boolean = false,
    val hasPreFacture: Boolean = false,
    val hasFacture: Boolean = false,
)


enum class SuggestKey(val systemKey: String, val priority: Int) {
    INVOICE_CREATED("INVOICE_CREATED", 100),
    PRE_INVOICE_CREATED("PRE_INVOICE_CREATED", 90),
    CONTRACT_CREATED("CONTRACT_CREATED", 80),
    PRICE_ESTIMATE_EDITED("PRICE_ESTIMATE_EDITED", 70),
    PRICE_ESTIMATE_CREATED("PRICE_ESTIMATE_CREATED", 60),
    CATALOG_CHANGED("CATALOG_CHANGED", 50),
    CATALOG_SELECTED("CATALOG_SELECTED", 40),
}

data class TimelineSuggestion(
    val key: SuggestKey,
    val message: String
)


private fun checkpointKey(orderId: Int) =
    stringPreferencesKey("order_checkpoint_$orderId")

private fun pendingKey(orderId: Int) =
    stringPreferencesKey("order_pending_suggestion_$orderId")





class OrderCheckpointStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun checkpointFlow(orderId: Int): Flow<OrderSummaryCheckpoint?> =
        dataStore.data.map { prefs ->
            prefs[checkpointKey(orderId)]?.let { raw ->
                runCatching { json.decodeFromString(OrderSummaryCheckpoint.serializer(), raw) }.getOrNull()
            }
        }

    suspend fun setCheckpoint(orderId: Int, checkpoint: OrderSummaryCheckpoint) {
        val raw = json.encodeToString(OrderSummaryCheckpoint.serializer(), checkpoint)
        dataStore.edit { it[checkpointKey(orderId)] = raw }
    }

    fun pendingSuggestionFlow(orderId: Int): Flow<String?> =
        dataStore.data.map { prefs -> prefs[pendingKey(orderId)] }

    suspend fun setPendingSuggestion(orderId: Int, suggestSystemKey: String?) {
        dataStore.edit { prefs ->
            if (suggestSystemKey == null) prefs.remove(pendingKey(orderId))
            else prefs[pendingKey(orderId)] = suggestSystemKey
        }
    }

    suspend fun commitDecision(orderId: Int, checkpoint: OrderSummaryCheckpoint) {
        val raw = json.encodeToString(OrderSummaryCheckpoint.serializer(), checkpoint)
        dataStore.edit { prefs ->
            prefs.remove(pendingKey(orderId))              // ✅ pending پاک
            prefs[checkpointKey(orderId)] = raw            // ✅ checkpoint ست
        }
    }

    suspend fun setPendingSuggestionOnly(orderId: Int, suggestSystemKey: String?) {
        dataStore.edit { prefs ->
            if (suggestSystemKey == null) prefs.remove(pendingKey(orderId))
            else prefs[pendingKey(orderId)] = suggestSystemKey
        }
    }
}



fun OrderSummary.toCheckpoint(): OrderSummaryCheckpoint =
    OrderSummaryCheckpoint(
        priceEstimateTotal = priceEstimateTotal,
        catalogSelectedCount = catalogSelectedCount,
        hasContract = hasContract,
        hasPreFacture = hasPreFacture,
        hasFacture = hasFacture
    )



fun detectSuggestion(
    current: OrderSummaryCheckpoint,
    prev: OrderSummaryCheckpoint
): TimelineSuggestion? {

    val cands = mutableListOf<TimelineSuggestion>()

    if (!prev.hasFacture && current.hasFacture) {
        cands += TimelineSuggestion(SuggestKey.INVOICE_CREATED, "فاکتور ثبت شده. در تایم‌لاین ثبت شود؟")
    }
    if (!prev.hasPreFacture && current.hasPreFacture) {
        cands += TimelineSuggestion(SuggestKey.PRE_INVOICE_CREATED, "پیش‌فاکتور ثبت شده. در تایم‌لاین ثبت شود؟")
    }
    if (!prev.hasContract && current.hasContract) {
        cands += TimelineSuggestion(SuggestKey.CONTRACT_CREATED, "قرارداد ثبت شده. در تایم‌لاین ثبت شود؟")
    }

    val prevPrice = prev.priceEstimateTotal
    val newPrice = current.priceEstimateTotal
    if (prevPrice == 0L && newPrice > 0L) {
        cands += TimelineSuggestion(SuggestKey.PRICE_ESTIMATE_CREATED, "تخمین قیمت ثبت شده. در تایم‌لاین ثبت شود؟")
    } else if (prevPrice > 0L && newPrice != prevPrice) {
        cands += TimelineSuggestion(SuggestKey.PRICE_ESTIMATE_EDITED, "تخمین قیمت ویرایش شده. در تایم‌لاین ثبت شود؟")
    }

    val prevC = prev.catalogSelectedCount
    val newC = current.catalogSelectedCount
    if (prevC == 0 && newC > 0) {
        cands += TimelineSuggestion(SuggestKey.CATALOG_SELECTED, "کاتالوگ انتخاب شده. در تایم‌لاین ثبت شود؟")
    } else if (prevC != newC && prevC > 0 && newC > 0) {
        cands += TimelineSuggestion(SuggestKey.CATALOG_CHANGED, "انتخاب‌های کاتالوگ تغییر کرده. در تایم‌لاین ثبت شود؟")
    }

    return cands.maxByOrNull { it.key.priority }
}


fun suggestKeyFromSystemKey(systemKey: String): SuggestKey? =
    SuggestKey.entries.firstOrNull { it.systemKey == systemKey }

fun defaultMessageFor(key: SuggestKey): String = when (key) {
    SuggestKey.INVOICE_CREATED -> "فاکتور ثبت شده. در تایم‌لاین ثبت شود؟"
    SuggestKey.PRE_INVOICE_CREATED -> "پیش‌فاکتور ثبت شده. در تایم‌لاین ثبت شود؟"
    SuggestKey.CONTRACT_CREATED -> "قرارداد ثبت شده. در تایم‌لاین ثبت شود؟"
    SuggestKey.PRICE_ESTIMATE_EDITED -> "تخمین قیمت ویرایش شده. در تایم‌لاین ثبت شود؟"
    SuggestKey.PRICE_ESTIMATE_CREATED -> "تخمین قیمت ثبت شده. در تایم‌لاین ثبت شود؟"
    SuggestKey.CATALOG_CHANGED -> "انتخاب‌های کاتالوگ تغییر کرده. در تایم‌لاین ثبت شود؟"
    SuggestKey.CATALOG_SELECTED -> "کاتالوگ انتخاب شده. در تایم‌لاین ثبت شود؟"
}
