package com.example.solarShop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.solarShop.R


val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, weight = FontWeight.Normal),
    Font(R.font.vazirmatn_medium,  weight = FontWeight.Medium),
    Font(R.font.vazirmatn_bold,    weight = FontWeight.Bold),
)

val AppTypography = Typography(
    displayLarge  = Typography().displayLarge.copy(fontFamily = Vazirmatn),
    displayMedium = Typography().displayMedium.copy(fontFamily = Vazirmatn),
    displaySmall  = Typography().displaySmall.copy(fontFamily = Vazirmatn),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = Vazirmatn),
    headlineMedium= Typography().headlineMedium.copy(fontFamily = Vazirmatn),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = Vazirmatn),
    titleLarge    = Typography().titleLarge.copy(fontFamily = Vazirmatn),
    titleMedium   = Typography().titleMedium.copy(fontFamily = Vazirmatn),
    titleSmall    = Typography().titleSmall.copy(fontFamily = Vazirmatn),
    bodyLarge     = Typography().bodyLarge.copy(fontFamily = Vazirmatn),
    bodyMedium    = Typography().bodyMedium.copy(fontFamily = Vazirmatn),
    bodySmall     = Typography().bodySmall.copy(fontFamily = Vazirmatn),
    labelLarge    = Typography().labelLarge.copy(fontFamily = Vazirmatn),
    labelMedium   = Typography().labelMedium.copy(fontFamily = Vazirmatn),
    labelSmall    = Typography().labelSmall.copy(fontFamily = Vazirmatn),
)



// Set of Material typography styles to start with
//val Typography = Typography(
//    bodyLarge = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp,
//        lineHeight = 24.sp,
//        letterSpacing = 0.5.sp
//    )
//    /* Other default text styles to override
//    titleLarge = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Normal,
//        fontSize = 22.sp,
//        lineHeight = 28.sp,
//        letterSpacing = 0.sp
//    ),
//    labelSmall = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Medium,
//        fontSize = 11.sp,
//        lineHeight = 16.sp,
//        letterSpacing = 0.5.sp
//    )
//    */
//)