package com.example.solarShop.ui.ui_02_splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SplashGate(
    onResolved: (String) -> Unit,
    vm: SplashViewModel = hiltViewModel()
) {
    val dest by vm.destination.collectAsStateWithLifecycle()

    LaunchedEffect(dest) { dest?.let(onResolved) }

    // تا وقتی مقصد معلوم نشده تا ۹۰٪ پر می‌شود؛ وقتی معلوم شد تا ۱۰۰٪ کامل می‌شود
    val progress by animateFloatAsState(
        targetValue = if (dest == null) 0.9f else 1f,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "splash-progress"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {


        //TODO به جای پر شدن خطی زیر یک انیمیشن از رنگ زمینه اسپلاش به رنگ زمینه اپ

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.66f)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
        )
    }
}
