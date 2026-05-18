package com.example.solarShop.data.entitlement


import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.repo.EntitlementRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.time.*
import java.time.temporal.ChronoUnit

sealed interface EntitlementState {
    data class Active(val plan: String?, val expiresAt: Long?) : EntitlementState
    data object Inactive : EntitlementState
}

@Singleton
class EntitlementCenter @Inject constructor(
    private val session: SessionDataStore,
    private val repo: EntitlementRepository
) {
    /** جریان واحد وضعیت پریمیوم برای کل اپ */
    val state: Flow<EntitlementState> =
        combine(
            repo.observeCurrent(),          // از Room (دقیق‌تر)
            session.isPremiumFlow           // کش سریع برای استارت
        ) { room, ds ->
            when {
                room != null && room.isActive -> EntitlementState.Active(room.plan, room.expiresAt)
                room != null && !room.isActive -> EntitlementState.Inactive
                room == null && ds -> EntitlementState.Active(null, null)
                else -> EntitlementState.Inactive
            }
        }.distinctUntilChanged()

    /** سنک دستی؛ در استارت اپ و بعد از ورود صدا بزن */
    suspend fun refresh() {
        repo.syncForCurrentUser().onFailure { /* لاگ/نادیده */ }
    }
}


fun daysUntil(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val diff = ChronoUnit.DAYS.between(today, target).toInt()
    return diff.coerceAtLeast(0)
}

data class PremiumUi(val isPremium: Boolean, val daysLeft: Int)

fun EntitlementState.toPremiumUi(zone: ZoneId = ZoneId.systemDefault()): PremiumUi = when (this) {
    is EntitlementState.Active -> {
        val days = expiresAt?.let { daysUntil(it, zone) } ?: 0
        PremiumUi(isPremium = true, daysLeft = days)
    }
    EntitlementState.Inactive -> PremiumUi(isPremium = false, daysLeft = 0)
}
