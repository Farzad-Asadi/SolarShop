package com.example.solarShop.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BambooSectionColors(
    val topBarContainer: Color,
    val topBarContent: Color,

    val bottomBarContainer: Color,
    val bottomBarContent: Color,

    val pageBackground: Color,
    val pageBackgroundAlt: Color,   // 👈 رنگ دوم گرادین
)

val LocalBambooSectionColors = staticCompositionLocalOf<BambooSectionColors> {
    error("BambooSectionColors not provided")
}

object BambooTheme {
    val sections: BambooSectionColors
        @Composable get() = LocalBambooSectionColors.current
}
