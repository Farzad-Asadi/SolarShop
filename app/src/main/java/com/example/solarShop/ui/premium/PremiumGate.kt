package com.example.solarShop.ui.premium

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import com.example.solarShop.data.entitlement.EntitlementState

@Composable
fun PremiumGate(
    entitlementState: Flow<EntitlementState>,
    onClickGoPremium: () -> Unit,
    content: @Composable () -> Unit
) {
    val state by entitlementState.collectAsState(initial = EntitlementState.Inactive)
    if (state is EntitlementState.Active) {
        content()
    } else {
        DefaultPaywall(onClickGoPremium)
    }
}

@Composable
fun DefaultPaywall(onClickGoPremium: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "این بخش ویژهٔ اعضای پریمیوم است.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClickGoPremium) { Text("ارتقا به پریمیوم") }
        }
    }
}



        //مصرف در صفحهٔ پریمیوم (نمونه)


//@Composable
//fun CatalogScreen(
//    entitlementCenter: EntitlementCenter = hiltViewModelRoot() // توضیح پایین را ببین
//) {
//    PremiumGate(
//        entitlementState = entitlementCenter.state,
//        onClickGoPremium = { /* TODO: ناوبری به Paywall/Checkout (فعلاً Mock) */ }
//    ) {
//        // محتوای پریمیوم
//        Text("کاتالوگ پیشرفته (پریمیوم)")
//    }
//}