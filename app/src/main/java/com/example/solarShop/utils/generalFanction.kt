package com.example.solarShop.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.solarShop.ARTICLE_PREFIX
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.OrderTimeline
import com.example.solarShop.data.backupRestore.v2.BackupCategory
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionEntity
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionWithAnswers
import com.example.solarShop.ui.questionTreeScreen.Edge
import com.example.solarShop.ui.questionTreeScreen.LayoutPlan
import com.example.solarShop.ui.questionTreeScreen.Slot
import com.example.solarShop.ui.theme.BambooTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


@Singleton
class FirstRunManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    private companion object { const val KEY = "first_run_done" }

    fun shouldRunOnce(): Boolean = !prefs.getBoolean(KEY, false)

    fun markDone() {
        prefs.edit().putBoolean(KEY, true).apply()
    }
}



fun e164ToNational10(raw: String?): String {
    val digits = raw.orEmpty().filter(Char::isDigit)
    return when {
        digits.startsWith("98") && digits.length >= 12 -> digits.drop(2).take(10) // 98 + 912xxxxxxx
        digits.startsWith("0")  && digits.length >= 11 -> digits.drop(1).take(10) // 0 + 912xxxxxxx
        digits.startsWith("9")  && digits.length >= 10 -> digits.take(10)         // 912xxxxxxx
        else -> digits.takeLast(10)
    }
}

fun national10ToE164(national10: String): String =
    uiToIranE164(national10) // همون util خودت




// تبدیل اعداد فارسی به لاتین و بلعکس
// اعداد عربی-هندی (U+0660..U+0669) و فارسی (U+06F0..U+06F9) → لاتین 0..9
fun digitsToLatinInline(input: String): String = buildString(input.length) {
    input.forEach { ch ->
        append(when (ch) {
            '٠','۰' -> '0'; '١','۱' -> '1'; '٢','۲' -> '2'; '٣','۳' -> '3'; '٤','۴' -> '4'
            '٥','۵' -> '5'; '٦','۶' -> '6'; '٧','۷' -> '7'; '٨','۸' -> '8'; '٩','۹' -> '9'
            else -> ch
        })
    }
}

/** تصمیم‌گیری دربارهٔ اسکریپتِ نمایش.
 * اگر فقط لاتین دید → لاتین؛ اگر فقط فارسی/عربی دید → فارسی؛
 * اگر مخلوط یا بدون رقم بود → مقدار قبلی را نگه می‌داریم.
 */

fun dbToLocalDisplay(dbPhoneLatin: String): String {
    val raw = dbPhoneLatin.trim()
    val digits = raw.filter(Char::isDigit)
    // فقط اگر واقعاً +98 یا 0098 داشت، به ۰۹… برگردان
    return if (raw.startsWith("+98") || digits.startsWith("0098") || digits.startsWith("98")) {
        val last10 = digits.takeLast(10)          // 10 رقم انتهایی
        if (last10.length == 10) "0$last10" else raw
    } else raw
}

// ورودی UI (هر فرمتی) → ذخیره به +98xxxxxxxxxx (بدون صفر اضافی)
fun uiToIranE164(ui: String): String {
    val latin = digitsToLatinInline(ui)
    val digits = latin.filter(Char::isDigit)

    // حذف همه‌ی پیشوندهای شناخته‌شده به ترتیبِ دقیق
    val core = when {
        digits.startsWith("0098") -> digits.removePrefix("0098")
        digits.startsWith("98")   -> digits.removePrefix("98")
        digits.startsWith("0")    -> digits.removePrefix("0")
        else -> digits
    }

    // در نهایت باید 10 رقم موبایل بماند (مثلاً 919…)
    // اگر بیشتر بود، 10 رقم آخر را می‌گیریم؛ اگر کمتر بود همان را ذخیره می‌کنیم (یا خطا بده)
    val mobile10 = if (core.length >= 10) core.takeLast(10) else core

    return "+98$mobile10"
}





fun createTimelineItemEntityForOrder(
    orderId: Int,
    orderTimeline: OrderTimeline,
    description: String? = null,                 // توضیح اختیاری از بیرون
    now: Long = System.currentTimeMillis()
): TimelineItemEntity {
    val (title, defaultDesc, completed) = when (orderTimeline) {
        OrderTimeline.CREATE_ORDER -> Triple(
            "ثبت سفارش",
            "",
            true
        )
        OrderTimeline.CREATE_PRICE_ESTIMATE -> Triple(
            "ثبت پیش‌برآورد قیمت",
            "",
            false
        )
        OrderTimeline.CREATE_SELECTED_CHOICE -> Triple(
            "ثبت انتخاب‌های مشتری",
            "",
            false
        )
    }

    return TimelineItemEntity(
        orderId = orderId,
        title = title,
        description = description ?: defaultDesc,
        date = now,
        completed = completed,

    )
}




//تابع کمکی برای کوتاه‌کردن متن
fun String.ellipsize(maxChars: Int = 17): String {
    if (this.length <= maxChars) return this
    return this.take(maxChars) + "…"
}



//region تبدیل و مدیریت تاریخ


//تبدیل میلادی به شمسی
fun formatPersianDateTime(millis: Long,noClock:Boolean=false): String {
    val instant = Instant.ofEpochMilli(millis)
    val zoneId = ZoneId.systemDefault()
    val localDateTime = LocalDateTime.ofInstant(instant, zoneId)

    // تاریخ میلادی به شمسی
    val persianDate = convertToPersianDate(localDateTime.toLocalDate())

    // ساعت و دقیقه
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')

    return if (noClock) persianDate else "$persianDate $hour:$minute"



}
fun convertToPersianDate(localDate: LocalDate): String {

    val year = localDate.year
    val month = localDate.monthValue
    val day = localDate.dayOfMonth

    // آرایه تعداد روزهای هر ماه میلادی (سال کبیسه بررسی شده)
    val monthDays = arrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    if (isGregorianLeapYear(year)) monthDays[1] = 29

    // محاسبه تعداد روزهای سپری‌شده از ابتدای سال میلادی
    var dayOfYear = day
    for (i in 0 until (month - 1)) {
        dayOfYear += monthDays[i]
    }

    // تبدیل سال میلادی به شمسی
    val persianYear = if (dayOfYear <= 79) year - 622 else year - 621

    // محاسبه روزهای سپری‌شده از ابتدای سال شمسی
    val persianDayOfYear = if (dayOfYear > 79) {
        dayOfYear - 79
    } else {
        if (isGregorianLeapYear(year - 1)) dayOfYear + 287 else dayOfYear + 286
    }

    // محاسبه ماه و روز شمسی
    var persianMonth = 1
    var persianDay = persianDayOfYear
    while (persianDay > getPersianMonthDays(persianYear, persianMonth)) {
        persianDay -= getPersianMonthDays(persianYear, persianMonth)
        persianMonth++
    }

    return "$persianYear/$persianMonth/$persianDay"
}
// بررسی سال کبیسه میلادی
fun isGregorianLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
// تعداد روزهای هر ماه شمسی
fun getPersianMonthDays(year: Int, month: Int): Int {
    return when {
        month <= 6 -> 31
        month <= 11 -> 30
        else -> if (isPersianLeapYear(year)) 30 else 29
    }
}
// بررسی سال کبیسه شمسی
fun isPersianLeapYear(year: Int): Boolean {
    return (((year + 2346) * 683) % 2820) < 683
}



interface DateUi {
    fun format(epochMs: Long): String
    @Composable fun Picker(currentEpochMs: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit)
}

// ===== استفاده از توابع موجود پروژه =====
// formatPersianDateTime(millis: Long): String
// convertToPersianDate(localDate: LocalDate): String
// isPersianLeapYear(year: Int): Boolean
// getPersianMonthDays(year: Int, month: Int): Int

object  PersianDateUiAdapter : DateUi {
    override fun format(epochMs: Long): String = formatPersianDateTime(epochMs)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Picker(currentEpochMs: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
        val instant = Instant.ofEpochMilli(currentEpochMs)
        val zoneId = ZoneId.systemDefault()
        val curDate = LocalDateTime.ofInstant(instant, zoneId).toLocalDate()
        val (startY, startM, startD) = parsePersianYMD(convertToPersianDate(curDate))

        var y by remember { mutableStateOf(startY) }
        var m by remember { mutableStateOf(startM) }
        var d by remember { mutableStateOf(startD) }

        val years = remember { (1380..1450).toList() }
        val months = remember { (1..12).toList() }
        val daysInThisMonth = remember(y, m) { getPersianMonthDays(y, m) }
        var dayList by remember(y, m) { mutableStateOf((1..daysInThisMonth).toList()) }
        LaunchedEffect(y, m) {
            val dim = getPersianMonthDays(y, m)
            if (d > dim) d = dim
            dayList = (1..dim).toList()
        }

        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("انتخاب تاریخ (جلالی)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // سال
                    SelectField(
                        label = "سال",
                        valueText = y.toString(),
                        options = years.map { it.toString() },
                        onSelectIndex = { idx -> y = years[idx] },
                        modifier = Modifier.weight(1f)
                    )
                    // ماه
                    val persianMonths = listOf(
                        "فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور",
                        "مهر","آبان","آذر","دی","بهمن","اسفند"
                    )
                    SelectField(
                        label = "ماه",
                        valueText = persianMonths[m - 1],
                        options = persianMonths,
                        onSelectIndex = { idx -> m = idx + 1 },
                        modifier = Modifier.weight(1f)
                    )
                    // روز
                    SelectField(
                        label = "روز",
                        valueText = d.toString(),
                        options = dayList.map { it.toString() },
                        onSelectIndex = { idx -> d = dayList[idx] },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val epoch = jalaliToEpochMillis(y, m, d, zoneId)
                        onPick(epoch); onDismiss()
                    }) { Text("تأیید") }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }


}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    label: String,
    valueText: String,
    options: List<String>,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = valueText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, txt ->
                DropdownMenuItem(
                    text = { Text(txt) },
                    onClick = { onSelectIndex(index); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun NumberPickerText(
    label: String,
    value: Int,
    range: IntRange,
    formatter: (Int) -> String,     // خروجی نمایشی (مثلاً ارقام فارسی، نام ماه)
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label)
        AndroidView(
            modifier = Modifier.width(96.dp).height(160.dp),
            factory = { ctx ->
                android.widget.NumberPicker(ctx).apply {
                    descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    wrapSelectorWheel = false
                    // مقداردهی اولیه (ترتیب مهم است)
                    minValue = range.first
                    maxValue = range.last
                    setFormatter { v -> formatter(v) }
                    setDisplayedValues(Array(maxValue - minValue + 1) { i ->
                        formatter(minValue + i)
                    })
                    val clamped = value.coerceIn(minValue, maxValue)
                    setOnValueChangedListener { _, _, newVal -> onChange(newVal) }
                    this.value = clamped
                }
            },
            update = { np ->
                // 1) همیشه اول displayedValues را null کن
                np.displayedValues = null
                // 2) سپس min/max را آپدیت کن
                if (np.minValue != range.first) np.minValue = range.first
                if (np.maxValue != range.last)  np.maxValue = range.last
                // 3) بعد آرایه‌ی نمایش جدید را ست کن
                np.setDisplayedValues(Array(np.maxValue - np.minValue + 1) { i ->
                    formatter(np.minValue + i)
                })
                // 4) و در نهایت مقدار را داخل رنج clamp کن
                val clamped = value.coerceIn(np.minValue, np.maxValue)
                if (np.value != clamped) np.value = clamped
            }
        )
    }
}
private fun Int.fa() = toString().map {
    if (it in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it
}.joinToString("")

private val persianMonths = arrayOf(
    "فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور",
    "مهر","آبان","آذر","دی","بهمن","اسفند"
)
fun formatPersianDateOnly(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val zoneId = ZoneId.systemDefault()
    val localDate = LocalDateTime.ofInstant(instant, zoneId).toLocalDate()
    return convertToPersianDate(localDate) // خروجی مثل 1404/8/18
}


// NumberPicker ساده
@Composable
private fun NP(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label)
        AndroidView(factory = { ctx ->
            NumberPicker(ctx).apply {
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = false
                setOnValueChangedListener { _, _, new -> onChange(new) }
                this.value = value
            }
        }, update = { np ->
            if (np.minValue != range.first) np.minValue = range.first
            if (np.maxValue != range.last) np.maxValue = range.last
            if (np.value != value) np.value = value
        }, modifier = Modifier.width(96.dp).height(160.dp))
    }
}

// کمک: "1404/8/18" -> Triple(1404,8,18)
private fun parsePersianYMD(s: String): Triple<Int,Int,Int> {
    val parts = s.trim().split("/", "-")
    val y = parts.getOrNull(0)?.toIntOrNull() ?: 1400
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
    return Triple(y, m, d)
}

// از منطق خودت برای تعداد روزهای ماه‌ها و کبیسه استفاده می‌کنیم
private fun jalaliToEpochMillis(py: Int, pm: Int, pd: Int, zoneId: ZoneId): Long {
    // 1) روز از ابتدای سال شمسی
    var persianDayOfYear = pd
    var m = 1
    while (m < pm) {
        persianDayOfYear += getPersianMonthDays(py, m)
        m++
    }

    // 2) تبدیل سال/روزِ شمسی به سال/روزِ میلادی
    // معکوس همان تبدیل تو:
    val gy: Int
    val dayOfYearG: Int
    // روزِ 1 فروردین شمسی در تقویم میلادی:
    // در منطق تو، اگر dayOfYear <= 79 => در سال میلادی قبلی هستیم
    // برعکس: اگر در سال شمسی py باشیم، شروعش در میلادی یا سال py+621 یا py+622 می‌افتد
    // از همان فرمول تفکیک استفاده می‌کنیم:
    // ابتدا یک حدس از سال میلادی:
    val guessGy = py + 621
    // حالا باید dayOfYear میلادی را بیابیم:
    // برای پیدا کردن mapping، از تبدیل رو به جلو استفاده می‌کنیم:
    // راه ساده و مطمئن: بگذاریم "اول فروردین py" را بسازیم و persianDayOfYear-1 روز به آن اضافه کنیم.

    val (gStartYear, gStartMonth, gStartDay) = jalaliNewYearToGregorian(py) // 1 فروردین py
    var gLocal = LocalDate.of(gStartYear, gStartMonth, gStartDay).plusDays((persianDayOfYear - 1).toLong())
    // 3) LocalDate → epochMillis ابتدای روزِ همان منطقه
    return gLocal.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

// محاسبهٔ تاریخ میلادیِ «۱ فروردینِ سال شمسی py» با همان قواعد تو:
private fun jalaliNewYearToGregorian(py: Int): Triple<Int, Int, Int> {
    // راه مطمئن: از تبدیل برعکس موجود استفاده می‌کنیم:
    // یک نقطهٔ مرجع: 1 فروردین py در سیستم تو چه زمانی به میلادی می‌افتد؟
    // با جستجوی کوچک حول نوروز (مارس) حل می‌کنیم (سریع است؛ سالی یک‌بار):
    // از 19 مارس تا 22 مارس را چک می‌کنیم تا اولین روزی که convertToPersianDate == "py/1/1" شود.
    val zoneId = ZoneId.systemDefault()
    for (day in 19..22) {
        val candidate = LocalDate.of(py + 621, 3, day)
        val persian = convertToPersianDate(candidate)
        if (persian.startsWith("$py/1/1")) {
            return Triple(candidate.year, candidate.monthValue, candidate.dayOfMonth)
        }
    }
    // اگر به هر دلیل پیدا نشد، fallback محافظه‌کارانه:
    val fallback = LocalDate.of(py + 621, 3, 21)
    return Triple(fallback.year, fallback.monthValue, fallback.dayOfMonth)
}


//endregion تبدیل و مدیریت تاریخ



//region توابع مربوط به QuestionTree


// کلیدهای رجیستری مختصات
sealed interface AnchorKey
@JvmInline value class AnswerAnchor(val answerId: Int) : AnchorKey
@JvmInline value class QuestionAnchor(val questionId: Int) : AnchorKey

private fun Float.safeCoerceIn(a: Float, b: Float): Float {
    return if (a <= b) {
        this.coerceIn(a, b)
    } else {
        // اگر بازه برعکس بود، جابه‌جاشون می‌کنیم تا کرش نکنه
        this.coerceIn(b, a)
    }
}



//جهت رسم خطوط ارتباطی بین سوالات
fun DrawScope.drawEdgesOrthogonalStable(
    edges: List<Edge>,
    anchors: Map<AnchorKey, Offset>,
    questionRects: Map<Int, Rect>,
    isInteracting: Boolean,
    isRtl: Boolean,
    longEdges: Set<Edge>,
    questionLayer: Map<Int, Int>,
    answerOwnerQuestionId: Map<Int, Int>,   // 👈 سوالِ والدِ هر پاسخ
    lowDetail: Boolean,
    highlightEdges: Set<Edge> = emptySet(),   // 👈 یال‌های در مسیر انتخاب‌ها
    dimNonHighlighted: Boolean = false        // 👈 بقیه کم‌رنگ شوند؟
) {
    // --- تنظیمات ظاهری کلی ---
    val minDropPx = if (isInteracting) 4.dp.toPx() else 10.dp.toPx()
    val topMarginPx = if (isInteracting) 2.dp.toPx() else 8.dp.toPx()
    val strokeWidth = if (isInteracting) 1.dp.toPx() else 2.dp.toPx()

    val longEdgeBaseColor = Color(0xFF1565C0)
    val shortEdgeBaseColor = Color(0xFF777777)

    fun edgeColorFor(edge: Edge, isLong: Boolean): Color {
        var c = if (isLong) longEdgeBaseColor else shortEdgeBaseColor
        if (dimNonHighlighted && edge !in highlightEdges) {
            c = c.copy(alpha = 0.15f)
        }
        return c
    }

    // helper برای coerce امن (وقتی max < min است، ارور نده)
    fun safeCoerce(value: Float, minV: Float, maxV: Float): Float {
        return if (maxV >= minV + 0.5f) {
            value.coerceIn(minV, maxV)
        } else {
            (minV + maxV) / 2f
        }
    }

    // --- حالت ساده برای زوم کم (lowDetail) ---
    if (lowDetail) {
        edges.forEach { e ->
            val from = anchors[AnswerAnchor(e.fromAnswerId)] ?: return@forEach
            val to = anchors[QuestionAnchor(e.toQuestionId)] ?: return@forEach

            val color = edgeColorFor(e, isLong = e in longEdges)

            drawLine(
                color = color,
                start = from,
                end = to,
                strokeWidth = strokeWidth
            )
        }
        return
    }

    // --- حالت کامل (Orthogonal + fan-out + long edges) ---

    val parallelGapPx = 8.dp.toPx()
    val halfParallelGap = parallelGapPx / 2f

    // برای fallback یال‌های بلند
    val globalMinX = questionRects.values.minOfOrNull { it.left } ?: 0f
    val globalMaxX = questionRects.values.maxOfOrNull { it.right } ?: size.width
    val outerMarginPx = 64.dp.toPx()

    data class EdgeInfo(
        val edge: Edge,
        val from: Offset,
        val toQuestionId: Int,
        val childCenterX: Float,
        val childTopY: Float,
        val dx: Float,
        val isLong: Boolean,
        val fromLayer: Int?,      // لایه‌ی سوال والد
        val toLayer: Int?,        // لایه‌ی سوال فرزند
        var dropY: Float = 0f,            // (برای کوتاه‌ها عملاً استفاده نمی‌کنیم)
        var childOffsetX: Float = 0f,     // فن‌اوت افقی نزدیک مقصد
        var extraDropOffset: Float = 0f,  // افست عمودی اضافه برای یال‌های بلند
        var extraXOffset: Float = 0f,     // افست افقی اضافه برای یال‌های بلند
        var midX: Float? = null,           // ستون عمودی اختصاصی این یال (برای کوتاه‌ها)
        var childOffsetY: Float = 0f
    )

    val allInfos = mutableListOf<EdgeInfo>()

    // ۱) پر کردن allInfos از روی anchors و rectها
    for (e in edges) {
        val fromAnchor = anchors[AnswerAnchor(e.fromAnswerId)] ?: continue
        val toRect = questionRects[e.toQuestionId] ?: continue

        val qAnchor = anchors[QuestionAnchor(e.toQuestionId)]
        val childCenterX = qAnchor?.x ?: toRect.center.x
        val childTopY = qAnchor?.y ?: toRect.top

        val fromQId = answerOwnerQuestionId[e.fromAnswerId]
        val fromLayer = fromQId?.let { questionLayer[it] }
        val toLayer = questionLayer[e.toQuestionId]

        val dx = childCenterX - fromAnchor.x
        val isLong = e in longEdges

        allInfos += EdgeInfo(
            edge = e,
            from = fromAnchor,
            toQuestionId = e.toQuestionId,
            childCenterX = childCenterX,
            childTopY = childTopY,
            dx = dx,
            isLong = isLong,
            fromLayer = fromLayer,
            toLayer = toLayer
        )
    }

    // ۲) گروه‌بندی یال‌های کوتاه فقط بر اساس جفت ستون (fromLayer, toLayer)
    data class ShortBandKey(val fromLayer: Int?, val toLayer: Int?)

    val bandGroups = mutableMapOf<ShortBandKey, MutableList<EdgeInfo>>()

    for (info in allInfos) {
        if (info.isLong) continue
        val key = ShortBandKey(info.fromLayer, info.toLayer)
        bandGroups.getOrPut(key) { mutableListOf() }.add(info)
    }

    // ۳) fan-out مقصد مشترک + ستون خطوط عمودی برای هر گروه ستون
    bandGroups.forEach { (_, groupInfos) ->
        if (groupInfos.isEmpty()) return@forEach

        // --- ۳.۱: fan-out نزدیک مقصد (childOffsetX) برای مقصد مشترک ---
        val byTarget = groupInfos.groupBy { it.toQuestionId }

        byTarget.forEach { (_, listForTarget) ->
            if (listForTarget.isEmpty()) return@forEach

            when (listForTarget.size) {
                1 -> {
                    val info = listForTarget[0]
                    info.childOffsetX = 0f
                }

                2 -> {
                    val sorted = listForTarget.sortedBy { it.from.x }
                    val first = sorted[0]
                    val second = sorted[1]

                    first.childOffsetX = -halfParallelGap
                    second.childOffsetX = +halfParallelGap
                }

                else -> {
                    val sorted = listForTarget.sortedBy { it.from.x }
                    val n = sorted.size
                    val centerIndex = (n - 1) / 2f

                    sorted.forEachIndexed { index, info ->
                        val k = (index - centerIndex)   // ... -2, -1, 0, +1, +2 ...
                        info.childOffsetX = k * halfParallelGap
                    }
                }

            }
            // --- ۲) fan-out عمودی انتهای یال برای مقصد مشترک ---
            if (listForTarget.size > 1) {
                // بر اساس ارتفاع مبدأ (from.y) از بالا به پایین مرتب می‌کنیم
                val sortedByFromY = listForTarget.sortedBy { it.from.y }
                val n = sortedByFromY.size
                val centerIndex = (n - 1) / 2f

                sortedByFromY.forEachIndexed { index, info ->
                    val k = (index - centerIndex)     // ... -2, -1, 0, +1, +2 ...
                    info.childOffsetY = k * halfParallelGap
                }
            }
        }

        // --- ۳.۲: ستون خطوط عمودی بین این دو ستون سؤال ---

        val groupLeft = groupInfos.minOf { min(it.from.x, it.childCenterX) }
        val groupRight = groupInfos.maxOf { max(it.from.x, it.childCenterX) }
        val groupWidth = (groupRight - groupLeft).coerceAtLeast(1f)

        // دو سوم وسط فضا را به عنوان "ستون خطوط عمودی" در نظر می‌گیریم
        val colLeft = groupLeft + groupWidth / 6f
        val colRight = groupRight - groupWidth / 6f
        val colWidth = (colRight - colLeft).coerceAtLeast(1f)

        // تقسیم به دو گروه: بالا (مقصد بالاتر از مبدا) و پایین (مقصد پایین‌تر یا مساوی)
        val upGroup = groupInfos.filter { it.childTopY < it.from.y }
        val downGroup = groupInfos.filter { it.childTopY >= it.from.y }

        fun assignLanesUp(subList: List<EdgeInfo>) {
            if (subList.isEmpty()) return
            val n = subList.size
            // به ترتیب مقصد از بالا به پایین
            val byDest = subList.sortedBy { it.childTopY }

            byDest.forEachIndexed { index, info ->
                // بالاترین مقصد → laneIndex بزرگ‌تر → ستون راست‌تر
                val laneIndex = index
                val laneWidth = colWidth / n.toFloat()

                val centerX = if (isRtl) {
                    colRight - laneWidth * (laneIndex + 0.5f)
                } else {
                    colLeft + laneWidth * (laneIndex + 0.5f)
                }

                info.midX = centerX
            }
        }

        fun assignLanesDown(subList: List<EdgeInfo>) {
            if (subList.isEmpty()) return
            val n = subList.size
            // از پایین‌ترین مقصد به بالا
            val byDest = subList.sortedByDescending { it.childTopY }

            byDest.forEachIndexed { index, info ->
                val laneIndex = index
                val laneWidth = colWidth / n.toFloat()

                val centerX = if (isRtl) {
                    colRight - laneWidth * (laneIndex + 0.5f)
                } else {
                    colLeft + laneWidth * (laneIndex + 0.5f)
                }

                info.midX = centerX
            }
        }

        // اول گروه بالا (طبق تعریف تو)
        assignLanesUp(upGroup)
        // بعد گروه پایین
        assignLanesDown(downGroup)
    }

    // ۴) افست موازی برای یال‌های بلند در یک بازه‌ی لایه‌ای (مثل قبل)
    val longBandGroups = allInfos
        .filter { it.isLong && it.fromLayer != null && it.toLayer != null }
        .groupBy {
            val low = minOf(it.fromLayer!!, it.toLayer!!)
            val high = maxOf(it.fromLayer!!, it.toLayer!!)
            low to high
        }

    longBandGroups.forEach { (_, list) ->
        if (list.size <= 1) return@forEach

        val sorted = list.sortedBy { it.from.x }
        val n = sorted.size
        val center = (n - 1) / 2f

        sorted.forEachIndexed { index, info ->
            val k = (index - center) // ... -2, -1, 0, +1, +2 ...
            info.extraDropOffset = k * halfParallelGap
            info.extraXOffset = k * halfParallelGap
        }
    }

    // برای جلوگیری از هم‌پوشانی long با short و long با long
    val shortInfos = allInfos.filter { !it.isLong }
    val longSecondYMap = mutableMapOf<Edge, Float>()

    // ۵) رسم نهایی
    for (info in allInfos) {
        val start = info.from
        val childCenterX = info.childCenterX
        val childTopY = info.childTopY

        val path = Path()

        if (info.isLong) {
            // ⭐ یال‌های بلند
            val fromLayer = info.fromLayer
            val toLayer = info.toLayer

            val corridorX: Float = if (fromLayer != null && toLayer != null) {
                val low = minOf(fromLayer, toLayer)
                val high = maxOf(fromLayer, toLayer)

                val rectsInBand = questionRects.filterKeys { qId ->
                    val layer = questionLayer[qId] ?: return@filterKeys false
                    layer in low..high
                }.values

                if (rectsInBand.isNotEmpty()) {
                    val minX = rectsInBand.minOf { it.left }
                    val maxX = rectsInBand.maxOf { it.right }
                    if (isRtl) maxX + outerMarginPx else minX - outerMarginPx
                } else {
                    if (isRtl) globalMaxX + outerMarginPx else globalMinX - outerMarginPx
                }
            } else {
                if (isRtl) globalMaxX + outerMarginPx else globalMinX - outerMarginPx
            }

            val minY = start.y + minDropPx
            val maxY = childTopY - topMarginPx

            var usedGapLogic = false

            if (fromLayer != null && toLayer != null) {
                val low = minOf(fromLayer, toLayer)
                val high = maxOf(fromLayer, toLayer)

                val middleRects = questionRects.filterKeys { qId ->
                    val layer = questionLayer[qId] ?: return@filterKeys false
                    layer in (low + 1) until high
                }.values

                if (middleRects.isNotEmpty()) {
                    val middleTop = middleRects.minOf { it.top }
                    val middleBottom = middleRects.maxOf { it.bottom }

                    val gap1Min = start.y + minDropPx
                    val gap1Max = middleTop - topMarginPx

                    val gap2Min = middleBottom + topMarginPx
                    val gap2Max = childTopY - topMarginPx

                    if (gap1Max > gap1Min + 2f && gap2Max > gap2Min + 2f) {
                        val baseY1 = (gap1Min + gap1Max) / 2f
                        val baseY2 = (gap2Min + gap2Max) / 2f

                        val y1 = safeCoerce(baseY1 + info.extraDropOffset, gap1Min, gap1Max)
                        var y2 = safeCoerce(baseY2 + info.extraDropOffset, gap2Min, gap2Max)

                        val x1 = corridorX + info.extraXOffset
                        val x2 = childCenterX + info.extraXOffset

                        // جلوگیری از هم‌پوشانی افقی دوم با یال‌های کوتاه
                        run {
                            val overlapThreshold = halfParallelGap / 2f

                            for (s in shortInfos) {
                                val syDefault = (s.from.y + s.childTopY) / 2f
                                val sy = if (s.dropY != 0f) s.dropY else syDefault

                                if (abs(sy - y2) < overlapThreshold) {
                                    val sx0 = s.from.x
                                    val sx1 = s.childCenterX + s.childOffsetX

                                    val lx0 = x1
                                    val lx1 = x2

                                    val shortMinX = minOf(sx0, sx1)
                                    val shortMaxX = maxOf(sx0, sx1)
                                    val longMinX = minOf(lx0, lx1)
                                    val longMaxX = maxOf(lx0, lx1)

                                    val overlapMin = maxOf(shortMinX, longMinX)
                                    val overlapMax = minOf(shortMaxX, longMaxX)

                                    if (overlapMax > overlapMin) {
                                        y2 += halfParallelGap
                                    }
                                }
                            }
                        }

                        // جلوگیری از هم‌پوشانی افقی دوم با سایر یال‌های بلند در همان باند
                        run {
                            val overlapThreshold = halfParallelGap / 2f

                            val bandKey: Pair<Int, Int>? =
                                if (fromLayer != null && toLayer != null) {
                                    val lowB = minOf(fromLayer, toLayer)
                                    val highB = maxOf(fromLayer, toLayer)
                                    lowB to highB
                                } else null

                            if (bandKey != null) {
                                longSecondYMap.forEach { (otherEdge, otherY2) ->
                                    val otherInfo =
                                        allInfos.firstOrNull { it.edge == otherEdge }
                                            ?: return@forEach
                                    val of = otherInfo.fromLayer
                                    val ot = otherInfo.toLayer
                                    val otherBand: Pair<Int, Int>? =
                                        if (of != null && ot != null) {
                                            val lowB2 = minOf(of, ot)
                                            val highB2 = maxOf(of, ot)
                                            lowB2 to highB2
                                        } else null

                                    if (otherBand == bandKey) {
                                        if (abs(otherY2 - y2) < overlapThreshold) {
                                            val dir = if (info.dx >= 0f) 1f else -1f
                                            y2 += dir * halfParallelGap
                                        }
                                    }
                                }

                                longSecondYMap[info.edge] = y2
                            }
                        }

                        path.moveTo(start.x, start.y)
                        path.lineTo(start.x, y1)
                        path.lineTo(x1, y1)
                        path.lineTo(x1, y2)
                        path.lineTo(x2, y2)
                        path.lineTo(childCenterX, childTopY)

                        usedGapLogic = true
                    }
                }
            }

            if (!usedGapLogic) {
                if (maxY <= minY + 4f) {
                    val midY = (start.y + childTopY) / 2f
                    val x1 = corridorX + info.extraXOffset

                    path.moveTo(start.x, start.y)
                    path.lineTo(start.x, midY)
                    path.lineTo(x1, midY)
                    path.lineTo(x1, childTopY)
                    path.lineTo(childCenterX, childTopY)
                } else {
                    val midTotal = (start.y + childTopY) / 2f
                    val drop1 = safeCoerce(midTotal, minY, maxY)

                    val midLower = (midTotal + maxY) / 2f
                    val drop2 = safeCoerce(midLower, minY, maxY)

                    val x1 = corridorX + info.extraXOffset
                    val x2 = childCenterX + info.extraXOffset

                    val y1 = drop1 + info.extraDropOffset
                    val y2 = drop2 + info.extraDropOffset

                    path.moveTo(start.x, start.y)
                    path.lineTo(start.x, y1)
                    path.lineTo(x1, y1)
                    path.lineTo(x1, y2)
                    path.lineTo(x2, y2)
                    path.lineTo(childCenterX, childTopY)
                }
            }
        } else {
            // ⭐ یال‌های کوتاه – الگوی استاندارد جدید:
            // افقی (تا وسط فاصله) → عمودی → افقی در ارتفاع کمی جابه‌جا شده → عمودی کوچک تا خود سؤال

            val finalX = childCenterX + info.childOffsetX
            val midXBase = (start.x + childCenterX) / 2f
            val midX = midXBase + info.childOffsetX

            // ارتفاع نهاییِ افقی قبل از رسیدن به خود سؤال
            val targetY = childTopY + info.childOffsetY

            path.moveTo(start.x, start.y)
            // ۱) افقی از پاسخ تا وسط فاصله
            path.lineTo(midX, start.y)
            // ۲) عمودی تا ارتفاع targetY (نه دقیقاً خود childTopY)
            path.lineTo(midX, targetY)
            // ۳) افقی تا کنار سؤال (در همان targetY)
            path.lineTo(finalX, targetY)
            // ۴) عمودی کوچک تا خود مرکز سؤال
            path.lineTo(childCenterX, childTopY)
        }


        val edgeColor = edgeColorFor(info.edge, info.isLong)

        drawPath(
            path = path,
            color = edgeColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}


fun DrawScope.myDrawEdgesOrthogonalStable(
    edges: List<Edge>,
    anchors: Map<AnchorKey, Offset>,
    questionRects: Map<Int, Rect>,
    isInteracting: Boolean,
    isRtl: Boolean,
    longEdges: Set<Edge>,
    questionLayer: Map<Int, Int>,
    answerOwnerQuestionId: Map<Int, Int>,   // سوالِ والدِ هر پاسخ
    lowDetail: Boolean,
    highlightEdges: Set<Edge> = emptySet(),
    dimNonHighlighted: Boolean = false
) {
    val strokeWidth = if (isInteracting) 1.dp.toPx() else 2.dp.toPx()

    val longEdgeBaseColor = Color(0xFF1565C0)
    val shortEdgeBaseColor = Color(0xFF777777)

    fun edgeColorFor(edge: Edge, isLong: Boolean): Color {
        var c = if (isLong) longEdgeBaseColor else shortEdgeBaseColor
        if (dimNonHighlighted && edge !in highlightEdges) {
            c = c.copy(alpha = 0.15f)
        }
        return c
    }

    // ───── ۱) محاسبهٔ gap افقی معمولی بین دو ستونِ مجاور ─────
    val normalGapX: Float = run {
        val gaps = mutableListOf<Float>()

        edges.forEach { e ->
            if (e in longEdges) return@forEach

            val fromQ = answerOwnerQuestionId[e.fromAnswerId] ?: return@forEach
            val toQ = e.toQuestionId
            val fromLayer = questionLayer[fromQ] ?: return@forEach
            val toLayer = questionLayer[toQ] ?: return@forEach

            // فقط یال‌هایی که یک لایه جلوترند (ستون مجاور)
            if (toLayer == fromLayer + 1) {
                val parentRect = questionRects[fromQ] ?: return@forEach
                val childRect = questionRects[toQ] ?: return@forEach

                // لایوت ما از راست به چپ است → gap = فاصله بین سمت چپ ستون والد و سمت راست ستون فرزند
                val gap = parentRect.left - childRect.right
                if (gap > 0f) gaps += gap
            }
        }

        if (gaps.isNotEmpty()) {
            gaps.average().toFloat()
        } else {
            // اگر چیزی پیدا نشد یک مقدار پیش‌فرض
            64.dp.toPx()
        }
    }

    // نصف فاصلهٔ معمولی → midSpace
    val midSpaceBase = normalGapX / 2f
    val verticalMargin = 24.dp.toPx() // "کمی بالاتر" از بالاترین کارت وسط مسیر

    // ───── ۲) رسم یال‌ها ─────
    edges.forEach { e ->
        val from = anchors[AnswerAnchor(e.fromAnswerId)] ?: return@forEach

        val targetRect = questionRects[e.toQuestionId] ?: return@forEach
        val targetAnchor = anchors[QuestionAnchor(e.toQuestionId)]

        val to = targetAnchor ?: Offset(targetRect.center.x, targetRect.top)
        val childCenterX = to.x
        val childTopY = to.y

        val isLong = e in longEdges
        val color = edgeColorFor(e, isLong)

        if (!isLong) {
            // ── یال‌های کوتاه: همان خط ساده مستقیم ──
            drawLine(
                color = color,
                start = from,
                end = to,
                strokeWidth = strokeWidth
            )
        } else {
            // ── یال بلند: خم دور زدن ستون‌ها ──

            val fromQ = answerOwnerQuestionId[e.fromAnswerId]
            val toQ = e.toQuestionId
            val fromLayer = fromQ?.let { questionLayer[it] }
            val toLayer = questionLayer[toQ]

            // اگر لایه معلوم نبود → fallback سادهٔ سه تکه‌ای
            if (fromLayer == null || toLayer == null) {
                val midX = (from.x + childCenterX) / 2f
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    lineTo(midX, from.y)
                    lineTo(midX, childTopY)
                    lineTo(childCenterX, childTopY)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                return@forEach
            }

            // فاصله افقی کل بین مبدا و مقصد
            val totalDx = from.x - childCenterX   // در لایوت RTL باید مثبت باشد
            if (totalDx <= 0f) {
                // حالت عجیب → همان fallback سه‌تکه‌ای
                val midX = (from.x + childCenterX) / 2f
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    lineTo(midX, from.y)
                    lineTo(midX, childTopY)
                    lineTo(childCenterX, childTopY)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                return@forEach
            }

            // midSpace را طوری محدود می‌کنیم که از نصف فاصله بیشتر نشود
            val midSpace = min(midSpaceBase, totalDx / 3f)

            // xِ ستون خطوط عمودی سمت مبدا و سمت مقصد
            val midX1 = from.x - midSpace  -40f        // نزدیک ستون مبدا
            val midX2 = childCenterX + midSpace +40f    // نزدیک ستون مقصد

            // لایه‌هایی که بین مبدا و مقصد هستند
            val lowLayer = min(fromLayer, toLayer)
            val highLayer = max(fromLayer, toLayer)

            val middleRects = questionRects.filterKeys { qId ->
                val l = questionLayer[qId] ?: return@filterKeys false
                l in (lowLayer + 1) until highLayer
            }.values

            // بالاترین نقطه بین کارت‌های وسط مسیر
            val yUp = if (middleRects.isNotEmpty()) {
                middleRects.minOf { it.top } - verticalMargin
            } else {
                min(from.y, childTopY) - verticalMargin
            }

            val path = Path().apply {
                moveTo(from.x, from.y)

                // ۱) افقی از مبدا تا midX1
                lineTo(midX1, from.y)

                // ۲) عمودی تا بالای همه کارت‌های بین راه (yUp)
                lineTo(midX1, yUp)

                // ۳) افقی روی ستون خطوط عمودی تا نزدیک مقصد (midX2)
                lineTo(midX2, yUp)

                // ۴) عمودی پایین تا ارتفاع مقصد
                lineTo(midX2, childTopY)

                // ۵) افقی تا خود مقصد
                lineTo(childCenterX, childTopY)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}











//برای محاسبهٔ LayoutPlan
fun computeLayoutPlan(
    questionWithAnswersList: List<QuestionWithAnswers>,
    childrenByQuestionId: Map<Int, List<QuestionEntity>>,
    edges: List<Edge>,
    answerOwnerQuestionId: Map<Int, Int>
): LayoutPlan {

    // ۱) نودها و والدها
    val nodeIds = questionWithAnswersList.mapNotNull { it.question.id }

    val parentsByQuestionId = mutableMapOf<Int, MutableList<Int>>()
    for ((p, children) in childrenByQuestionId) {
        children.mapNotNull { it.id }.forEach { c ->
            parentsByQuestionId.getOrPut(c) { mutableListOf() }.add(p)
        }
    }

    // ۲) لایه‌بندی با Kahn (BFS لایه‌ای)
    val inDeg = nodeIds.associateWith { id ->
        parentsByQuestionId[id]?.size ?: 0
    }.toMutableMap()

    val layers = mutableListOf<MutableList<Int>>()
    val q = ArrayDeque<Int>()

    // ریشه‌ها را (برای لایه‌ی صفر) می‌گذاریم داخل صف – اینجا sort اشکال ندارد
    nodeIds.filter { inDeg[it] == 0 }.sorted().forEach { q.addLast(it) }

    val visited = mutableSetOf<Int>()
    while (q.isNotEmpty()) {
        val layer = mutableListOf<Int>()
        val size = q.size
        repeat(size) {
            val u = q.removeFirst()
            if (!visited.add(u)) return@repeat
            layer.add(u)

            val childs = childrenByQuestionId[u].orEmpty().mapNotNull { it.id }
            for (v in childs) {
                inDeg[v] = (inDeg[v] ?: 0) - 1
                if (inDeg[v] == 0) q.addLast(v)
            }
        }
        if (layer.isNotEmpty()) layers += layer
    }

    // اگر (به هر دلیل) نودی بدون لایه ماند (گراف ناقص/چند جزیره‌ای)
    val leftovers = nodeIds.filterNot { visited.contains(it) }
    if (leftovers.isNotEmpty()) {
        // همین ترتیب خام را هم نگه می‌داریم (بدون sort)
        layers += leftovers.toMutableList()
    }

    // ❗ ۳) این‌جا دیگر sort نمی‌کنیم؛ همان ترتیب BFS / ترتیب جواب‌ها را نگه می‌داریم
    val orderedLayers: List<List<Int>> = layers.map { it.toList() }

    // ۴) questionLayer: هر سؤال در کدام لایه است؟
    val questionLayer = buildMap {
        orderedLayers.forEachIndexed { li, layer ->
            layer.forEach { qId -> put(qId, li) }
        }
    }

    // ۵) اندیس هر سؤال داخل لایه‌ی خودش
    val indexInLayer = mutableMapOf<Int, MutableMap<Int, Int>>() // layerIndex → (qId → index)
    orderedLayers.forEachIndexed { li, layer ->
        val map = mutableMapOf<Int, Int>()
        layer.forEachIndexed { idx, qId ->
            map[qId] = idx
        }
        indexInLayer[li] = map
    }

    // ۶) تشخیص یال‌های بلند (بیش از یک لایه فاصله)
    val longEdgesSet = mutableSetOf<Edge>()
    data class LongEdgeSlot(
        val fromQuestionId: Int,
        val toQuestionId: Int,
        val fromLayer: Int,
        val toLayer: Int
    )
    val longEdgeSlots = mutableListOf<LongEdgeSlot>()

    for (e in edges) {
        val fromQ = answerOwnerQuestionId[e.fromAnswerId] ?: continue
        val toQ = e.toQuestionId

        val fromLayer = questionLayer[fromQ] ?: continue
        val toLayer = questionLayer[toQ] ?: continue

        if (toLayer > fromLayer + 1) {
            longEdgesSet += e
            longEdgeSlots += LongEdgeSlot(
                fromQuestionId = fromQ,
                toQuestionId = toQ,
                fromLayer = fromLayer,
                toLayer = toLayer
            )
        }
    }

    // ۷) ساخت Slotها (سؤال + گپ‌ها) برای هر لایه
    val slotsPerLayer: List<MutableList<Slot>> =
        MutableList(orderedLayers.size) { mutableListOf() }

    for (li in orderedLayers.indices) {
        val orig = orderedLayers[li]
        val gapPositions = mutableListOf<Int>()

        // تمام یال‌های بلند که از روی این لایه عبور می‌کنند
        longEdgeSlots.forEach { le ->
            if (li in (le.fromLayer + 1) until le.toLayer) {
                val origSize = orig.size

                val srcIndexMap = indexInLayer[le.fromLayer] ?: return@forEach
                val srcIdx = srcIndexMap[le.fromQuestionId] ?: 0

                val destIndexMap = indexInLayer[le.toLayer] ?: return@forEach
                val destIdx = destIndexMap[le.toQuestionId] ?: 0

                // نسبت پیشروی بین لایه مبدأ و مقصد
                val t = (li - le.fromLayer).toFloat() /
                        (le.toLayer - le.fromLayer).toFloat()

                val posFloat = srcIdx + (destIdx - srcIdx) * t
                val pos = posFloat
                    .toInt()
                    .coerceIn(0, origSize)

                gapPositions += pos
            }
        }

        gapPositions.sort()

        val layerSlots = slotsPerLayer[li]
        var slotIndex = 0
        var gIdx = 0

        for (qi in orig.indices) {
            while (gIdx < gapPositions.size && gapPositions[gIdx] == slotIndex) {
                layerSlots += Slot(questionId = null, isGap = true)
                slotIndex++
                gIdx++
            }
            layerSlots += Slot(questionId = orig[qi], isGap = false)
            slotIndex++
        }

        while (gIdx < gapPositions.size) {
            layerSlots += Slot(questionId = null, isGap = true)
            slotIndex++
            gIdx++
        }
    }

    return LayoutPlan(
        orderedLayers = orderedLayers,
        slotsPerLayer = slotsPerLayer,
        questionLayer = questionLayer,
        longEdges = longEdgesSet
    )
}




//endregion توابع مربوط به QuestionTree




//لانچر دوربین
@Stable
class CameraCaptureController internal constructor(
    private val launchInternal: () -> Unit
) {
    fun launch() = launchInternal()
}
@Composable
fun rememberCameraCaptureLauncher(
    requiredPermissions: () -> Array<String>,
    createOutputUri: () -> Uri?,
    onResult: (uri: Uri, success: Boolean) -> Unit,
    onMessage: (String) -> Unit,
): CameraCaptureController {
    val context = LocalContext.current

    // ✅ اینجا نگه می‌داریم تا با recomposition از بین نره
    val pendingUriState = remember { mutableStateOf<Uri?>(null) }

    // ✅ latest lambdas (تا controller ثابت بمونه ولی همیشه آخرین callbackها اجرا بشن)
    val latestRequiredPermissions by rememberUpdatedState(requiredPermissions)
    val latestCreateOutputUri by rememberUpdatedState(createOutputUri)
    val latestOnResult by rememberUpdatedState(onResult)
    val latestOnMessage by rememberUpdatedState(onMessage)

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingUriState.value
        if (uri == null) {
            latestOnMessage("خطا: مسیر عکس مشخص نیست.")
            return@rememberLauncherForActivityResult
        }
        latestOnResult(uri, success)
        pendingUriState.value = null
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val perms = latestRequiredPermissions()
        val denied = perms.filter { result[it] != true }

        if (denied.isEmpty()) {
            // دوباره launch کن
            // (این بار باید permها ok باشن و uri ساخته بشه)
            val outUri = runCatching { latestCreateOutputUri() }.getOrNull()
            if (outUri == null) {
                latestOnMessage("ساخت مسیر ذخیره عکس ناموفق بود.")
                return@rememberLauncherForActivityResult
            }
            pendingUriState.value = outUri
            takePictureLauncher.launch(outUri)
            return@rememberLauncherForActivityResult
        }

        val activity = context.findActivityOrNull()
        val permanentlyDenied = if (activity != null) {
            denied.any { p ->
                ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
            }
        } else false

        if (permanentlyDenied) latestOnMessage("دسترسی لازم برای دوربین برای همیشه رد شده. از تنظیمات فعالش کن.")
        else latestOnMessage("بدون مجوزهای لازم امکان عکس‌گرفتن نیست.")
    }

    val controller = remember(context) {
        CameraCaptureController(
            launchInternal = {
                val perms = latestRequiredPermissions()
                if (perms.isNotEmpty()) {
                    val allGranted = perms.all { p ->
                        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
                    }
                    if (!allGranted) {
                        requestPermissionsLauncher.launch(perms)
                        return@CameraCaptureController
                    }
                }

                val outUri = runCatching { latestCreateOutputUri() }.getOrNull()
                if (outUri == null) {
                    latestOnMessage("ساخت مسیر ذخیره عکس ناموفق بود.")
                    return@CameraCaptureController
                }
                pendingUriState.value = outUri
                takePictureLauncher.launch(outUri)
            }
        )
    }

    return controller
}

private tailrec fun Context.findActivityOrNull(): Activity? {
    return when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivityOrNull()
        else -> null
    }
}












// ساخت مسیرهای عکسهای پروفایل و مشتری
enum class PictureBucket { Profile, Client }

object MediaPictureStore {

    private fun bucketDir(context: Context, bucket: PictureBucket): File {
        val sub = when (bucket) {
            PictureBucket.Profile -> "profilePicture"
            PictureBucket.Client -> "clientPicture"
        }
        return File(context.filesDir, "media/$sub").apply { mkdirs() }
    }

    fun createDestFile(
        context: Context,
        bucket: PictureBucket,
        prefix: String = "img",
        ext: String = "jpg"
    ): File {
        val dir = bucketDir(context, bucket)
        val name = "${prefix}_${System.currentTimeMillis()}.$ext"
        return File(dir, name)
    }

    fun fileToUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun isInsideBucket(context: Context, path: String?, bucket: PictureBucket): Boolean {
        if (path.isNullOrBlank()) return false
        val dir = bucketDir(context, bucket).absolutePath
        return path.startsWith(dir)
    }

    fun deleteOldIfInBucket(context: Context, oldPath: String?, bucket: PictureBucket) {
        if (oldPath.isNullOrBlank()) return
        if (!isInsideBucket(context, oldPath, bucket)) return
        runCatching { File(oldPath).delete() }
    }
}

fun savePickedGalleryImage(
    context: Context,
    fromUri: Uri,
    bucket: PictureBucket,
    quality: Int = 85,
    maxSidePx: Int = 1600
): String? {
    val dest = MediaPictureStore.createDestFile(context, bucket, prefix = "g")
    val ok = copyAndCompressImageToFile(
        context = context,
        uri = fromUri,
        outFile = dest,
        quality = quality,
        maxSidePx = maxSidePx
    )
    return if (ok) dest.absolutePath else null
}











fun myFormatNumber(value: Long): String {
    // هزارگان ساده، بدون Locale (برای سریع‌بودن در میدان مشتری)
    val s = value.toString()
    val sb = StringBuilder()
    var count = 0
    for (i in s.length - 1 downTo 0) {
        sb.append(s[i])
        count++
        if (count % 3 == 0 && i != 0) sb.append(',')
    }
    return sb.reverse().toString()
}



//جهت تعیین طول نقطه چین یک متن

private val PLACEHOLDER_REGEX = Regex("\\{\\{\\s*([\\w.]+)\\s*\\}\\}")
/** طول تقریبی هر placeholder برحسب تعداد کاراکتر (em) */
private val defaultLengthByKey = mapOf(
    // تاریخ‌ها
    "order.startDate" to 10, "order.endDate" to 10, "date.today" to 10,
    // مبالغ
    "order.totalAmount" to 14, "order.advanceAmount" to 14,
    // درصد/اعداد
    "order.advancePercent" to 4, "penalty.perDay" to 8, "warranty.months" to 3,
    // آدرس/نام‌ها
    "order.address" to 20, "employer.fullName" to 14, "contractor.fullName" to 14,
)
/** اگر کلید ناشناخته بود، این طول را بده */
private const val DEFAULT_EM_LEN = 12
@Composable
fun ContractText(
    body: String,
    values: Map<String, String> = emptyMap(), // اگر مقداری داری، اینجا بده
    textAlign: TextAlign = TextAlign.Right
) {
    // 1) متن را به قطعات تبدیل می‌کنیم (متن معمولی + placeholder ها)
    val matches = remember(body) { PLACEHOLDER_REGEX.findAll(body).toList() }

    // 2) AnnotatedString + inline map می‌سازیم
    val (annotated, inlineContent) = remember(body, values) {
        buildAnnotatedWithInline(body, matches, values)
    }

    // 3) راست‌به‌چپ برای فارسی
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = annotated,
            inlineContent = inlineContent,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            textAlign = textAlign
        )
    }
}
/** خروجی: متن با inlineContent برای placeholder های بدون مقدار */
private fun buildAnnotatedWithInline(
    body: String,
    matches: List<MatchResult>,
    values: Map<String, String>
): Pair<AnnotatedString, Map<String, InlineTextContent>> {

    val builder = AnnotatedString.Builder()
    val inlineMap = mutableMapOf<String, InlineTextContent>()

    var lastIndex = 0
    var placeholderCounter = 0

    for (m in matches) {
        // متن قبل از placeholder
        if (m.range.first > lastIndex) {
            builder.append(body.substring(lastIndex, m.range.first))
        }

        val key = m.groupValues[1] // مثلا order.startDate
        val value = values[key]?.takeIf { it.isNotBlank() }

        if (value != null) {
            // اگر مقدار داریم، خودش را با استایل متمایز چاپ کن
            builder.withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(value)
            }
        } else {
            // مقدار نداریم → inline placeholder با عرض n.em
            val id = "ph_$placeholderCounter"
            placeholderCounter++

            val emLen = estimateEmForKey(key)
            // رزرو فضا در متن
            builder.appendInlineContent(id, alternateText = "…")

            // تعریف محتوای درون placeholder: خط‌چین
            inlineMap[id] = InlineTextContent(
                Placeholder(
                    width = emLen.em,
                    height = 1.5.em, // قدِ خط (اندازهٔ ارتفاع خط)
                    placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                )
            ) {
                DottedLineBox()
            }
        }

        lastIndex = m.range.last + 1
    }

    // متنِ بعد از آخرین placeholder
    if (lastIndex < body.length) {
        builder.append(body.substring(lastIndex))
    }

    return builder.toAnnotatedString() to inlineMap
}
private fun estimateEmForKey(key: String): Float {
    // اگر کلید دقیق نیست (مثلا material.hingeBrand)، کمی هوشمندی:
    val exact = defaultLengthByKey[key]
    if (exact != null) return exact.toFloat()

    return when {
        key.contains("date", true) || key.contains("Day", true) -> 10f
        key.contains("amount", true) || key.contains("price", true) -> 14f
        key.contains("percent", true) || key.contains("months", true) -> 4f
        key.contains("address", true) -> 20f
        key.contains("name", true) -> 14f
        else -> DEFAULT_EM_LEN.toFloat()
    }
}
@Composable
private fun DottedLineBox() {
    // یک Canvas ساده که خط‌چین افقی می‌کشد
    Canvas(Modifier.fillMaxSize()) {
        val y = size.height * 0.75f
        drawLine(
            brush = SolidColor(Color.Blue.copy(alpha = 0.6f)),
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width, y),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) // 10 on, 8 off
        )
    }
}




//جهت اصلاح نام در قرارداد


fun displayTitleFor(section: ContractTemplateSectionEntity): String {
    // اگر کاربر خودش "ماده N -" تایپ کرده، همون رو نشان بده؛
    // وگرنه با orderNo بساز
    return if (ARTICLE_PREFIX.containsMatchIn(section.title)) {
        section.title.trim()
    } else {
        "ماده ${section.orderNo} - ${section.title.trim()}"
    }
}

fun extractTitleBodyForEdit(title: String): String {
    // برای فیلدِ «عنوان ماده» فقط متنِ بعد از پیشوند را نشان بده
    return title.replace(ARTICLE_PREFIX, "").trim()
}

fun buildFinalTitle(orderNo: Int, titleBody: String): String {
    return "ماده $orderNo - ${titleBody.trim()}"
}



//توابع کمکی: ساخت فایل، کپی/فشرده‌سازی


fun avatarsDir(context: Context): File =
    File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }

fun createTempImageFile(context: Context, prefix: String = "avatar_"): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    return File.createTempFile("${prefix}${time}_", ".jpg", avatarsDir(context))
}

/** ورودی: Uri از گالری/دوربین. خروجی: مسیر فایل فشرده‌شده. */
fun copyAndCompressImage(context: Context, source: Uri, quality: Int = 80): String? {
    val dest = createTempImageFile(context)
    context.contentResolver.openInputStream(source)?.use { inStream ->
        // decode با نمونه‌گیری مناسب
        val bmp = decodeSampledBitmapFromStream(inStream, 1024, 1024) ?: return null
        FileOutputStream(dest).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return dest.absolutePath
    }
    return null
}




fun copyAndCompressImageToFile(
    context: Context,
    uri: Uri,
    outFile: File,
    quality: Int = 85,
    maxSidePx: Int = 1600
): Boolean {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.isEmpty()) return false

            // 1) فقط برای خواندن سایز
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) {
                // اگر نتونست سایز بده، همون raw copy
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { it.write(bytes) }
                return true
            }

            // 2) محاسبه نمونه‌برداری
            val largest = max(w, h)
            val sample = (largest / maxSidePx).coerceAtLeast(1)

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return false

            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 95), out)
            }
            bmp.recycle()
            true
        } ?: false
    }.getOrDefault(false)
}

fun savePickedImageToBucket(
    context: Context,
    fromUri: Uri,
    bucket: PictureBucket,
    quality: Int = 85,
    maxSidePx: Int = 1600
): String? {
    val dest = MediaPictureStore.createDestFile(context, bucket, prefix = "g")
    val ok = copyAndCompressImageToFile(
        context = context,
        uri = fromUri,
        outFile = dest,
        quality = quality,
        maxSidePx = maxSidePx
    )
    return if (ok) dest.absolutePath else null
}


/** Decode با کمترین مصرف حافظه متناسب با حداکثر ابعاد */
fun decodeSampledBitmapFromStream(input: InputStream, reqW: Int, reqH: Int): Bitmap? {
    val data = input.readBytes()
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, options)
    options.inSampleSize = calculateInSampleSize(options, reqW, reqH)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(data, 0, data.size, options)
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
    val (h, w) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (h > reqH || w > reqW) {
        val halfH = h / 2
        val halfW = w / 2
        while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}






fun formatDigitsGrouped(input: String): String {
    val digits = input.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val value = digits.toLongOrNull() ?: return ""
    return "%,d".format(value)
}




// --- ارقام فارسی ---
private val faDigits = listOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
fun String.toPersianDigits(): String = buildString(length) {
    for (ch in this@toPersianDigits) {
        append(
            when (ch) {
                in '0'..'9' -> faDigits[ch - '0']
                else -> ch
            }
        )
    }
}

// --- جداکنندهٔ هزارگان ---
@SuppressLint("DefaultLocale")
fun Long.withThousandsSep(): String = String.format("%,d", this)



fun Long.toCurrencyText(prefs: DisplayPreferences): String {
    val isToman = prefs.currencyUnit == CurrencyUnit.TOMAN

    // مقدار اصلی در دیتابیس همیشه "تومان" است
    val valueForDisplay: Long = if (isToman) this else this * 10L

    val absVal = abs(valueForDisplay)
    val formatted = "%,d".format(absVal)
    val sign = if (valueForDisplay < 0) "−" else ""

    val suffix = if (isToman) "تومان" else "ریال"

    // همون استایل قبلی خودت: رقم + علامت + فاصله + واحد
    return "$formatted$sign $suffix"
}




//برای نمایش با فاصله‌گذاری (۳-۳-۴)     شماره موبایل
fun iranMobileVisualTransformation(): VisualTransformation = VisualTransformation { raw ->
    // raw: مثل "9123456789"
    val digits = raw.text.filter(Char::isDigit)
    val sb = StringBuilder()
    var outToIn = IntArray(digits.length + 3) { 0 } // حدودی، کمی اضافه

    var inIdx = 0
    var outIdx = 0

    fun appendChunk(n: Int) {
        repeat(n.coerceAtMost(digits.length - inIdx)) {
            sb.append(digits[inIdx++])
            outIdx++
            if (outIdx < outToIn.size) outToIn[outIdx] = inIdx
        }
    }

    // 3-3-4
    appendChunk(3)
    if (inIdx < digits.length) { sb.append(' '); outIdx++ }
    appendChunk(3)
    if (inIdx < digits.length) { sb.append(' '); outIdx++ }
    appendChunk(4)

    val outText = sb.toString()

    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            // تعداد فاصله‌ها را حساب کن
            return when {
                offset <= 3 -> offset
                offset <= 6 -> offset + 1
                else -> (offset + 2).coerceAtMost(outText.length)
            }
        }
        override fun transformedToOriginal(offset: Int): Int {
            return when {
                offset <= 3 -> offset
                offset <= 7 -> (offset - 1).coerceAtLeast(0)
                else -> (offset - 2).coerceAtLeast(0)
            }.coerceAtMost(digits.length)
        }
    }

    TransformedText(AnnotatedString(outText), mapping)
}

//تبدیل  0 → +98     برای تکست فیلد
//visualTransformation = iranMobileVisualTransformationNew(),
fun iranMobileVisualTransformationNew(): VisualTransformation =
    VisualTransformation { raw ->
        val original = raw.text

        // اگر با +98 شروع نمی‌شود، اصلاً دست نزن
        if (!original.startsWith("+98")) {
            return@VisualTransformation TransformedText(
                raw,
                OffsetMapping.Identity
            )
        }

        // +98...  →  0...
        val rest = original.removePrefix("+98")   // "9199910369"
        val transformedText = "0$rest"           // "09199910369"

        val transformedLength = transformedText.length
        val originalLength = original.length

        val mapping = object : OffsetMapping {
            // از متن اصلی (+98...) به متن تبدیل‌شده (0...)
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    // قبل از +  (offset=0)  → قبل از 0
                    offset <= 0 -> 0

                    // داخل ناحیه "+98" (offset=1,2,3) → بعد از 0
                    offset in 1..3 -> 1

                    // بعد از آن (۴ به بعد) → دو کاراکتر کمتر
                    else -> (offset - 2).coerceIn(0, transformedLength)
                }
            }

            // از متن تبدیل‌شده (0...) به متن اصلی (+98...)
            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    // قبل از 0 → قبل از '+'
                    offset <= 0 -> 0

                    // بعد از 0، ولی نزدیک ابتدای شماره
                    offset == 1 -> 3 // بعد از "0" ≈ بعد از "98"

                    // بقیه‌ی موقعیت‌ها → دو کاراکتر بیشتر
                    else -> (offset + 2).coerceIn(0, originalLength)
                }
            }
        }

        TransformedText(
            AnnotatedString(transformedText),
            mapping
        )
    }

// تبدیل "+98..." → "0..."
fun formatIranMobileForDisplay(original: String): String {
    if (!original.startsWith("+98")) return original

    val rest = original.removePrefix("+98") // مثلاً "9199910369"
    return "0$rest"                         // "09199910369"
}











//کمک‌تابع برای پیدا کردن Activity
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}



// عنوان فارسی برای هر دستهٔ بک‌آپ
fun BackupCategory.titleFa(): String = when (this) {
    BackupCategory.QNA               -> "سؤال و جواب"
    BackupCategory.CONTRACTS        -> "قالب‌های قرارداد"
    BackupCategory.CUSTOMERS_ACTIVE -> "مشتریان فعال"
    BackupCategory.CUSTOMERS_ARCHIVED -> "مشتریان آرشیوی"
}



//گرادیان دو رنگ زاویه‌دار
@Composable
fun Modifier.bambooAngledBackground(): Modifier {
    val s = BambooTheme.sections

    val brush = remember(s.pageBackground, s.pageBackgroundAlt) {
        Brush.linearGradient(
            colors = listOf(s.pageBackground, s.pageBackgroundAlt)
        )
    }

    return this.background(brush)
}

