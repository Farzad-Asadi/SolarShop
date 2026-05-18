package com.example.solarShop.data.backupRestore.v2


/*
 * مرحله ۱ — مدل‌ها و اسکلتِ Provider ها
 * ---------------------------------------------------------------------------
 * این فایل فقط قراردادها (interfaces + data models) را تعریف می‌کند
 * تا در مراحل بعدی، پیاده‌سازی هر دسته (QNA/Contracts/Customers...) را جداگانه بسازیم.
 * تلاش شده همه‌چیز «واضح، قابل تست و قابل توسعه» باشد.
 */

import android.content.Context
import com.example.solarShop.data.room.appDatabase.AppDatabase
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/* ----------------------------------
 * ۱) دسته‌های بک‌آپ/ریستور
 *   - «مشتریان فعال» و «مشتریان آرشیوی» جدا هستند
 *   - اگر در UI گزینه «همه» داشته باشیم، فقط به‌صورت Set همهٔ موارد را پاس می‌دهیم
 * ---------------------------------- */

enum class BackupCategory {
    QNA,                // سؤال/پاسخ/مسیر/تصویر پاسخ‌ها
    CONTRACTS,          // قالب قرارداد + طرفین + مواد + تبصره‌ها + فایل‌های مرتبط
    CUSTOMERS_ACTIVE,   // مشتریان و سفارشات غیرآرشیو (archive=false)
    CUSTOMERS_ARCHIVED  // مشتریان و سفارشات آرشیوی (archive=true)
}

/** طرحِ بک‌آپ: کاربر کدام دسته‌ها را انتخاب کرده؟ */
data class BackupPlan(
    val categories: Set<BackupCategory>
) {
    /** هلپر برای UI: آیا همهٔ دسته‌ها انتخاب شده‌اند؟ */
    fun isAllSelected(all: Set<BackupCategory>) = categories.containsAll(all)
}

/* ----------------------------------
 * ۲) سیاست‌ها و گزینه‌های ریستور
 * ---------------------------------- */

enum class ConflictPolicy { // وقتی رکورد مشابه وجود دارد چه کنیم؟
    Overwrite,  // همهٔ فیلدها را با نسخهٔ فایل بک‌آپ جایگزین کن
    Skip,       // از رویش بگذر؛ مقدار موجود را نگه‌دار
    Merge       // فقط فیلدهای خالی/Null را پر کن (Upsert محتاط)
}

enum class UserKeyMapping {
    RequireExactMatch, // فقط اگر userKey فایل == userKey فعلی بود اجازهٔ ریستور بده
    RemapToCurrent     // همهٔ داده‌ها را با userKey فعلی بنویس (ایمن برای انتقال بین دیوایس‌های کاربر)
}

/** گزینه‌های ریستور که از UI می‌آیند */
data class RestoreOptions(
    val selected: Set<BackupCategory>,
    val conflictPolicy: ConflictPolicy,
    val userKeyMapping: UserKeyMapping
)

/* ----------------------------------
 * ۳) مانیفست فایل بک‌آپ (manifest.json در ریشهٔ ZIP)
 *   - برای پیش‌نمایش در UI و اعتبارسنجی هنگام ریستور
 * ---------------------------------- */

@Serializable
data class Manifest(
    val version: Int = 1,            // نسخهٔ فرمت بک‌آپ
    val createdAt: Long,             // زمان ساخت بک‌آپ (epoch millis)
    val sourceUserKey: String,       // userKey منبع (برای تطبیق یا Remap)
    val categories: List<BackupCategory>,
    val moduleVersions: Map<String, Int> = mapOf(
        // ماژول‌ها می‌توانند نسخهٔ مستقل داشته باشند تا مهاجرت آسان شود
        "qna" to 1,
        "contracts" to 1,
        "customers" to 1
    ),
    val counts: List<CategoryCount> = emptyList() // شمارش‌های خلاصه برای هر دسته
)

@Serializable
data class CategoryCount(
    val category: BackupCategory,
    val items: Int,                  // تعداد رکورد اصلی (مثلاً تعداد Clients یا تعداد QNA nodes)
    val attachments: Int? = null     // (اختیاری) تعداد فایل‌های جانبی مثل images/contracts files
)

/* ----------------------------------
 * ۴) کانتکست‌های زمان اجرا (وابستگی‌ها را تزریق می‌کنیم)
 * ---------------------------------- */

data class BackupContext(
    val appContext: Context,
    val db: AppDatabase,
    val json: Json,
    val currentUserKey: String
)

data class RestoreContext(
    val appContext: Context,
    val db: AppDatabase,
    val json: Json,
    val currentUserKey: String
)

/* ----------------------------------
 * ۵) قرارداد خروجی Snapshot هر Provider
 *   - jsonPayload: محتوای سریال‌شدهٔ دیتای آن دسته (برای قرار گرفتن در ZIP)
 *   - extraFiles: لیست فایل‌هایی که باید در ZIP کپی شوند (مثلاً تصاویر)
 *   - zipFolder: فولدر مقصد داخل ZIP (مثلاً "customers/active" یا "qna")
 * ---------------------------------- */

data class ProviderSnapshot(
    val category: BackupCategory,
    val zipFolder: String,                 // مثال: "customers/active" یا "customers/archived" یا "qna"
    val jsonFileName: String,              // مثال: "data.json"
    val jsonPayload: String,               // خروجی kxJson.encodeToString(...)
    val extraFiles: List<FileRef> = emptyList() // فایل‌های فیزیکی که باید کنار JSON زیپ شوند
)

/** مرجع فایل برای قرار دادن در ZIP */
data class FileRef(
    @Contextual val file: java.io.File,    // سورس فیزیکی روی دیسک داخلی اپ
    val zipPath: String                    // مسیر مقصد داخل ZIP (مثلاً "images/abc.jpg")
)

/* ----------------------------------
 * ۶) گزارش نتیجهٔ ریستور برای هر Provider
 * ---------------------------------- */

data class RestoreReport(
    val category: BackupCategory,
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
    val errors: List<String> = emptyList()
)

/* ----------------------------------
 * ۷) قرارداد Providerها (اسکلت — مرحلهٔ بعد پیاده‌سازی می‌شود)
 *   - هر Provider دقیقاً یک دسته را پوشش می‌دهد
 *   - snapshot: فقط دادهٔ همان دسته را برمی‌گرداند
 *   - restore: فقط دادهٔ همان دسته را از snapshot برمی‌گرداند
 * ---------------------------------- */

interface BackupProvider {
    val category: BackupCategory

    /**
     * ساخت خروجی بک‌آپ برای همین دسته
     * - خروجی شامل JSON سریال‌شده + هر فایل جانبی لازم است
     * - orchestrator بعداً این خروجی‌ها را در ZIP می‌چیند
     */
    suspend fun snapshot(ctx: BackupContext): ProviderSnapshot

    /**
     * برگردانی دادهٔ همین دسته از روی snapshot
     * - باید سیاست کانفلیکت و userKeyMapping را رعایت کند
     * - گزارش تعداد insert/update/skip برگردانده شود
     */
    suspend fun restore(snapshot: ProviderSnapshot, options: RestoreOptions, ctx: RestoreContext): RestoreReport
}

/* ----------------------------------
 * ۸) مسیرهای استاندارد داخل ZIP (پیشنهادی و شفاف برای UI)
 * ---------------------------------- */

object ZipLayout {
    const val ROOT_MANIFEST = "manifest.json"         // ریشهٔ ZIP

    object Qna {
        const val FOLDER = "qna"                      // qna/
        const val DATA = "data.json"                  // qna/data.json
        const val IMAGES = "images/"                  // qna/images/*
    }

    object Contracts {
        const val FOLDER = "contracts"                // contracts/
        const val DATA = "data.json"                  // contracts/data.json
        const val FILES = "files/"                    // contracts/files/* (اختیاری)
    }

    object Customers {
        object Active {
            const val FOLDER = "customers/active"     // customers/active/
            const val DATA = "data.json"              // customers/active/data.json
        }
        object Archived {
            const val FOLDER = "customers/archived"   // customers/archived/
            const val DATA = "data.json"              // customers/archived/data.json
        }
    }
}

/* ----------------------------------
 * ۹) نوت‌های مهم طراحی:
 *  - Manifest همیشه در ریشهٔ ZIP است و قبل از ریستور برای پیش‌نمایش خوانده می‌شود
 *  - Provider ها هیچ دانشی از دسته‌های دیگر ندارند (loosely coupled)
 *  - mapping بر اساس userKey در لایهٔ Provider انجام می‌شود نه orchestrator
 *  - در مرحلهٔ بعد، یک Orchestrator می‌نویسیم (BackupManagerV2/RestoreManagerV2)
 * ---------------------------------- */
