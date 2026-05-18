package com.example.solarShop.ui.bamboApp

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.solarShop.ui.backUpRestore.BackUpRestoreScreen
import com.example.solarShop.ui.contractScreen.ContractScreen
import com.example.solarShop.ui.orderScreen.OrderScreen
import com.example.solarShop.ui.orderScreen.orderCatalog.CatalogScreen
import com.example.solarShop.ui.orderScreen.orderCosts.CostScreen
import com.example.solarShop.ui.orderScreen.orderInvoice.OrderInvoiceScreen
import com.example.solarShop.ui.orderScreen.orderPicture.OrderPictureScreen
import com.example.solarShop.ui.orderScreen.orderPriceEstimate.PriceEstimateScreen
import com.example.solarShop.ui.profileScreen.ProfileScreen
import com.example.solarShop.ui.questionInfoScreen.QuestionInfoScreen
import com.example.solarShop.ui.questionTreeScreen.QuestionTreeScreen
import com.example.solarShop.ui.signInScreen.SignInScreen
import com.example.solarShop.ui.ui_02_splash.SplashGate
import kotlinx.coroutines.flow.collectLatest


@SuppressLint("SuspiciousIndentation")
@Composable
fun BamboApp(
    modifier: Modifier = Modifier,
    vm: BamboAppViewModel = hiltViewModel()
) {

    val nav = rememberNavController()

    LaunchedEffect(nav) {
        nav.currentBackStackEntryFlow.collectLatest { entry ->
            val keys = entry.savedStateHandle.keys()
            Log.d("TTLE", "dest=${entry.destination.route} savedStateKeys=$keys")

            // اگر deepLinkIntent بود، لاگش کن
            if ("android-support-nav:controller:deepLinkIntent" in keys) {
                Log.d("TTLE", "⚠️ deepLinkIntent key EXISTS on ${entry.destination.route}")
            }
        }
    }



    NavHost(
        navController = nav,
        startDestination = BamboScreen.Splash.name,
        modifier = modifier
    ) {
        composable(BamboScreen.Splash.name) {
            SplashGate(
                onResolved = { target ->
                    nav.navigate(target)
                    { popUpTo(BamboScreen.Splash.name) { inclusive = true } }
                }
            )
        }
        composable(BamboScreen.SignIn.name) {
            SignInScreen(
                onNavigateHome = {
                    nav.navigate(BamboScreen.Profile.name ) {
                        popUpTo(
                            BamboScreen.SignIn.name
                        ) { inclusive = true }
                    }
                })

        }
        composable(
            route = BamboScreen.Profile.name + "?userId={userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) {

            ProfileScreen(
                onClickAddOrder = { orderId ->
                    nav.navigate(BamboScreen.Order.name + "?orderId=$orderId") {
                        popUpTo(BamboScreen.Profile.name) { inclusive = false }
                    }
                },
                onClickOrder = { orderId ->
                    nav.navigate(BamboScreen.Order.name + "?orderId=$orderId") {
                        popUpTo(BamboScreen.Profile.name) { inclusive = false }
                    }
                },
                onClickQuestionEdite = {
                    nav.navigate(BamboScreen.QuestionTree.name + "/-1") {
                        popUpTo(BamboScreen.Profile.name) {
                            inclusive = false
                        }
                    }
                }
                ,
                onShowEditeContract = {
                    nav.navigate(BamboScreen.Contract.name + "?entrySource=profile")
                    {
                        popUpTo(BamboScreen.Profile.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Profile، آن را از نو لود کن
                },
                onSignedOut = {
                    nav.navigate(BamboScreen.SignIn.name) {
                        popUpTo(BamboScreen.Profile.name) { inclusive = true }
                    }
                },
                onClickBackUpRestore = {
                    nav.navigate(BamboScreen.BackUpRestore.name) {
                        popUpTo(BamboScreen.Profile.name) { inclusive = false }
                    }
                },
            )
        }
        composable(
            route = BamboScreen.Order.name + "?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) {
            OrderScreen(
                onClickPriceEstimate = {orderId ->
                    nav.navigate(BamboScreen.OrderPriceEstimate.name + "?orderId=$orderId")
                    {
                        popUpTo(BamboScreen.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickContract = { orderId ->
                    nav.navigate(BamboScreen.Contract.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(BamboScreen.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickCost = { orderId ->
                    nav.navigate(BamboScreen.OrderCost.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(BamboScreen.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickPicture = { orderId ->
                    nav.navigate(BamboScreen.OrderPicture.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(BamboScreen.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickCatalog = { orderId ->
                    nav.navigate(BamboScreen.OrderCatalog.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(BamboScreen.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickInvoice = { orderId ->
                    nav.navigate(BamboScreen.OrderInvoice.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(BamboScreen.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickBack = {
                    nav.navigate(BamboScreen.Profile.name)
                    {
                        popUpTo(BamboScreen.Contract.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                }
            )
        }
        composable(
            route = BamboScreen.QuestionTree.name + "/{questionId}?orderId={orderId}",
            arguments = listOf(
                navArgument("questionId") { type = NavType.IntType; defaultValue = -1 },
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->

            val qIdArg = backStackEntry.arguments?.getInt("questionId") ?: -1
            val selectedQuestionId: Int? = if (qIdArg == -1) null else qIdArg

            val orderIdArg = backStackEntry.arguments?.getInt("orderId") ?: -1
            val filterOrderId: Int? = if (orderIdArg == -1) null else orderIdArg

            QuestionTreeScreen(
                selectedQuestionId = selectedQuestionId,
                filterOrderId = filterOrderId,
                onClickEditeQuestion = { qId ->
                    val id = qId ?: -1
                    nav.navigate(BamboScreen.QuestionInfo.name + "?questionId=$id") {
                        popUpTo(BamboScreen.QuestionTree.name) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { nav.navigateUp() },
                onCloseToProfile = {nav.navigate(BamboScreen.Profile.name )}
            )
        }

        composable(
            route = BamboScreen.QuestionInfo.name + "?questionId={questionId}",
            arguments = listOf(
                navArgument("questionId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            QuestionInfoScreen(
                onClickOpenQuestionTree = { qId ->
                    val id = qId ?: -1
                    nav.navigate("${BamboScreen.QuestionTree.name}/$id?orderId=-1")
                },
                onBack = { nav.navigateUp() }
            )
        }

        composable(
            route =BamboScreen.OrderPriceEstimate.name + "?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) {
            PriceEstimateScreen(
                onBack = {
                    nav.navigateUp()
                }
            )
        }
        composable(
            route = BamboScreen.Contract.name + "?entrySource={entrySource}&orderId={orderId}",
            arguments = listOf(
                navArgument("entrySource") { type = NavType.StringType; defaultValue = "profile" },
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )

        ) {
            ContractScreen(
                onClose = {
                    nav.navigateUp()
                }
            )
        }
        composable(
            route = BamboScreen.OrderCost.name + "?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )

        ) {
            CostScreen(
                onClose = {
                    nav.navigateUp()
                }
            )
        }
        composable(
            route = BamboScreen.OrderPicture.name + "?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )

        ) {
            OrderPictureScreen(
                onClose = {
                    nav.navigateUp()
                }
            )
        }
        composable(
            route = BamboScreen.OrderCatalog.name + "?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )

        ) {
            CatalogScreen(
                onClose = {
                    nav.navigateUp()
                },
                onOpenQuestionInfo = { qId ->
                    val id = qId ?: -1
                    nav.navigate(BamboScreen.QuestionInfo.name + "?questionId=$id")
                },
                onOpenQuestionTree = { qId, orderId ->
                    val qIdNonNull = qId ?: -1
                    val orderIdNonNull = orderId ?: -1
                    nav.navigate("${BamboScreen.QuestionTree.name}/$qIdNonNull?orderId=$orderIdNonNull")
                }
            )
        }
        composable(
            route = BamboScreen.OrderInvoice.name + "?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )

        ) {
            OrderInvoiceScreen(
                onClose = {
                    nav.navigateUp()
                }
            )
        }
        composable(route = BamboScreen.BackUpRestore.name) {
            BackUpRestoreScreen(
                onClose = { nav.navigateUp() } // ✅ همین
            )
        }


        // توضیح آموزشی: پاس‌دادن پارامتر در Navigation (نمونهٔ entrySource)
        // 1) در NavHost مقصد را با پارامتر کوئری تعریف کن: route="contract?entrySource={entrySource}" + navArgument("entrySource"){ defaultValue="profile" }.
        // 2) از مبدا، مقدار را در آدرس بفرست: nav.navigate("contract?entrySource=order") یا "profile".
        // 3) در ContractViewModel مقدار را از SavedStateHandle بگیر: savedStateHandle.getStateFlow("entrySource","profile").
        // 4) مقدار رشته‌ای را به enum تبدیل کن (PROFILE/ORDER) و در uiState.entrySource بگذار.
        // 5) در TemplatePickerScreen عنوان را بر اساس entrySource نمایش بده: «…ویرایش کنید» یا «…برای مشتری ثبت کنید».
        // 6) مزیت: بدون state global، مسیر ورود به صفحه شفاف و testable است.
        // 7) نکته: اگر پارامتر اختیاری است، defaultValue بگذار تا نبودنِ آن کرش ندهد.
        // 8) نمونهٔ استفاده در پروژه: ContractScreen با مسیر "Contract?entrySource=order" از Order می‌آید و UI پیام مناسب نشان می‌دهد.

    }

}


enum class BamboScreen {       //مسیرهای برنامه
    Splash,
    SignIn,
    Profile,
    QuestionTree,
    QuestionInfo,
    Contract,
    BackUpRestore,

    Order,
    OrderPriceEstimate,
    OrderCost,
    OrderPicture,
    OrderCatalog,
    OrderInvoice
}

