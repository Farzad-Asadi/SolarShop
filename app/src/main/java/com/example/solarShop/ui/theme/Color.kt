package com.example.solarShop.ui.theme
import androidx.compose.ui.graphics.Color

//Light (روشن)
val primaryLight = Color(0xFF3FA34D)                // رنگ اصلی برای تأکیدها: دکمه Filled/FAB، سوئیچ/چک‌باکس فعال، لینک‌های مهم
val onPrimaryLight = Color(0xFFFFFFFF)              // رنگ متن/آیکن روی primary (کنتراست مناسب روی زمینهٔ primary)

val primaryContainerLight = Color(0xFFD6F5DD)       // زمینهٔ «کانتینرِ پرایمری» برای نسخه‌های Tonal/Container (مثل Chip/Badge/Section برجسته)
val onPrimaryContainerLight = Color(0xFF153D1C)     // رنگ متن/آیکن روی primaryContainer

val secondaryLight = Color(0xFF6B7F52)              // رنگ ثانویه برای عناصر کم‌اهمیت‌تر: دکمه Filled Tonal، فیلترچیپ‌ها، آیتم‌های انتخابی دوم
val onSecondaryLight = Color(0xFFFFFFFF)            // متن/آیکن روی secondary
val secondaryContainerLight = Color(0xFFE3EAD6)     // زمینهٔ کانتینرِ ثانویه برای حالات Tonal و پس‌زمینه‌های فرعی
val onSecondaryContainerLight = Color(0xFF283319)   // متن/آیکن روی secondaryContainer

val tertiaryLight = Color(0xFF2F8C8C)               // رنگ ثالث برای لهجه‌های بصری/نمودارها/آیکن‌های مکمل
val onTertiaryLight = Color(0xFFFFFFFF)             // متن/آیکن روی tertiary
val tertiaryContainerLight = Color(0xFFCDEBEB)      // زمینهٔ کانتینرِ ثالث برای باکس‌های تأکیدی فرعی
val onTertiaryContainerLight = Color(0xFF113F3F)    // متن/آیکن روی tertiaryContainer

val errorLight = Color(0xFFB3261E)                  // رنگ خطا: پیام خطا، آندرلاین/آیکن خطای TextField، دکمه‌های خطرناک
val onErrorLight = Color(0xFFFFFFFF)                // متن/آیکن روی زمینهٔ error
val errorContainerLight = Color(0xFFF9DEDC)         // زمینهٔ خطا برای کارت/بنر هشدار (Severity کمتر از error خالص)
val onErrorContainerLight = Color(0xFF410E0B)       // متن/آیکن روی errorContainer

val backgroundLight = Color(0xFFFAFAF7)             // پس‌زمینهٔ کل صفحه/Activity (پشت همهٔ سطوح)
val onBackgroundLight = Color(0xFF1A1C18)           // رنگ متن/آیکن روی background

val surfaceLight = Color(0xFFFEFEFC)                // رنگ پیش‌فرض سطوح: Card/Sheet/Dialog/AppBar/Navigation
val onSurfaceLight = Color(0xFF1A1C18)              // متن/آیکن روی surface (متن معمولی، آیکن‌های لیست)

val surfaceVariantLight = Color(0xFFE3E3D5)         // سطح خنثیِ متنوع: لیست‌ها/آیتم‌ها/بخش‌های جداشده با کنتراست ملایم
val onSurfaceVariantLight = Color(0xFF45483E)       // متن/آیکن روی surfaceVariant
val outlineLight = Color(0xFF74796D)                // خط دور و Divider پررنگ‌تر: مرزبندی کارت، فریم TextField، جداکننده‌ها
val outlineVariantLight = Color(0xFFC7CAB9)         // خط دور ملایم‌تر/Divider کم‌جلوه برای مرزبندی ظریف
val scrimLight = Color(0xFF000000)                  // پردهٔ تاریک پشت Dialog/BottomSheet/Drawer (Back-Drop)

val inverseSurfaceLight = Color(0xFF2E3129)         // سطح معکوس برای Snackbar/BottomBar تیره روی زمینهٔ روشن
val inverseOnSurfaceLight = Color(0xFFF0F1EA)       // متن/آیکن روی inverseSurface
val inversePrimaryLight = Color(0xFF9ADFA4)         // رنگ پرایمریِ معکوس برای تأکید روی سطوح معکوس (مثل Snackbar Action)

val surfaceDimLight = Color(0xFFE2E3D9)             // سطح تیره‌تر برای سطوح با ارتفاع/الوییشن پایین (Backgroundِ محتوا)
val surfaceBrightLight = Color(0xFFFEFEFC)          // سطح روشن‌تر برای الوییشن بالا (Card/Sheet برجسته)
val surfaceContainerLowestLight = Color(0xFFFFFFFF) // پایین‌ترین لایهٔ ظرف‌ها: درون کارت‌های خیلی روشن/باکس‌های مینیمال
val surfaceContainerLowLight = Color(0xFFF7F7F2)    // لایهٔ ظرف با الوییشن کم
val surfaceContainerLight = Color(0xFFF1F2EC)       // لایهٔ ظرف استاندارد برای بخش‌بندی محتوا
val surfaceContainerHighLight = Color(0xFFEBECE6)   // لایهٔ ظرف با الوییشن بالاتر (تمایز بیشتر از surface)
val surfaceContainerHighestLight = Color(0xFFE5E6E0)// بالاترین لایهٔ ظرف برای برجسته‌ترین کانتینرها





//Dark (تاریک)
val primaryDark = Color(0xFF9ADFA4)
val onPrimaryDark = Color(0xFF003916)
val primaryContainerDark = Color(0xFF2F6A37)
val onPrimaryContainerDark = Color(0xFFBCF5C3)

val secondaryDark = Color(0xFFC7D2B6)
val onSecondaryDark = Color(0xFF223016)
val secondaryContainerDark = Color(0xFF3A4A28)
val onSecondaryContainerDark = Color(0xFFDDE6CC)

val tertiaryDark = Color(0xFF8FD9D9)
val onTertiaryDark = Color(0xFF003A3A)
val tertiaryContainerDark = Color(0xFF1F5E5E)
val onTertiaryContainerDark = Color(0xFFCAF0F0)

val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)

val backgroundDark = Color(0xFF121412)
val onBackgroundDark = Color(0xFFE7E9E3)
val surfaceDark = Color(0xFF121412)
val onSurfaceDark = Color(0xFFE7E9E3)

val surfaceVariantDark = Color(0xFF45483E)
val onSurfaceVariantDark = Color(0xFFC5C8B5)
val outlineDark = Color(0xFF909487)
val outlineVariantDark = Color(0xFF5F6458)
val scrimDark = Color(0xFF000000)

val inverseSurfaceDark = Color(0xFFE7E9E3)
val inverseOnSurfaceDark = Color(0xFF2A2D26)
val inversePrimaryDark = Color(0xFF2F7A3A)

val surfaceDimDark = Color(0xFF10120F)
val surfaceBrightDark = Color(0xFF363933)
val surfaceContainerLowestDark = Color(0xFF0A0C09)
val surfaceContainerLowDark = Color(0xFF191B17)
val surfaceContainerDark = Color(0xFF1F211D)
val surfaceContainerHighDark = Color(0xFF2A2C27)
val surfaceContainerHighestDark = Color(0xFF343730)






