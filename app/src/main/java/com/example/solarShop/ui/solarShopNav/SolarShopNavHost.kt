package com.example.solarShop.ui.solarShopNav

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
import com.example.solarShop.feature.backup.ui.SolarBackupRestoreScreen
import com.example.solarShop.feature.product.ui.ProductByCategoryScreen
import com.example.solarShop.feature.product.ui.ProductListScreen
import com.example.solarShop.feature.product.ui.brand.BrandEditScreen
import com.example.solarShop.feature.product.ui.brand.BrandListScreen
import com.example.solarShop.feature.product.ui.category.AttributeEditScreen
import com.example.solarShop.feature.product.ui.category.CategoryEditScreen
import com.example.solarShop.feature.product.ui.inventory.InventoryTransactionEditScreen
import com.example.solarShop.feature.product.ui.pricing.PurchasePriceEditScreen
import com.example.solarShop.feature.product.ui.product.ProductDetailScreen
import com.example.solarShop.feature.product.ui.product.ProductEditScreen
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
fun SolarShopNavHost(
    modifier: Modifier = Modifier,
    vm: SolarShopNavViewModel = hiltViewModel()
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
        startDestination = SolarRoute.Splash.name,
        modifier = modifier
    ) {
        composable(SolarRoute.Splash.name) {
            SplashGate(
                onResolved = { target ->
                    nav.navigate(target)
                    { popUpTo(SolarRoute.Splash.name) { inclusive = true } }
                }
            )
        }
        composable(SolarRoute.SignIn.name) {
            SignInScreen(
                onNavigateHome = {
                    nav.navigate(SolarRoute.Profile.name ) {
                        popUpTo(
                            SolarRoute.SignIn.name
                        ) { inclusive = true }
                    }
                })

        }
        composable(
            route = SolarRoute.Profile.name + "?userId={userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) {

            ProfileScreen(
                onClickAddOrder = { orderId ->
                    nav.navigate(SolarRoute.Order.name + "?orderId=$orderId") {
                        popUpTo(SolarRoute.Profile.name) { inclusive = false }
                    }
                },
                onClickOrder = { orderId ->
                    nav.navigate(SolarRoute.Order.name + "?orderId=$orderId") {
                        popUpTo(SolarRoute.Profile.name) { inclusive = false }
                    }
                },
                onShowEditeContract = {
                    nav.navigate(SolarRoute.Contract.name + "?entrySource=profile")
                    {
                        popUpTo(SolarRoute.Profile.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Profile، آن را از نو لود کن
                },
                onSignedOut = {
                    nav.navigate(SolarRoute.SignIn.name) {
                        popUpTo(SolarRoute.Profile.name) { inclusive = true }
                    }
                },
                onClickBackUpRestore = {
                    nav.navigate(SolarRoute.BackUpRestore.name) {
                        popUpTo(SolarRoute.Profile.name) { inclusive = false }
                    }
                },
                onClickProductList = {
                    nav.navigate(SolarRoute.ProductList.name) {
                        popUpTo(SolarRoute.Profile.name) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = SolarRoute.Order.name + "?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) {
            OrderScreen(
                onClickPriceEstimate = {orderId ->
                    nav.navigate(SolarRoute.OrderPriceEstimate.name + "?orderId=$orderId")
                    {
                        popUpTo(SolarRoute.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickContract = { orderId ->
                    nav.navigate(SolarRoute.Contract.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(SolarRoute.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickCost = { orderId ->
                    nav.navigate(SolarRoute.OrderCost.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(SolarRoute.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickPicture = { orderId ->
                    nav.navigate(SolarRoute.OrderPicture.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(SolarRoute.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickCatalog = { orderId ->
                    nav.navigate(SolarRoute.OrderCatalog.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(SolarRoute.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickInvoice = { orderId ->
                    nav.navigate(SolarRoute.OrderInvoice.name + "?entrySource=order&orderId=$orderId")
                    {
                        popUpTo(SolarRoute.Order.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                },
                onClickBack = {
                    nav.navigate(SolarRoute.Profile.name)
                    {
                        popUpTo(SolarRoute.Contract.name) {
                            inclusive = false
                        }
                    }    // هنگام برگشت به صفحه Order، آن را از نو لود کن
                }
            )
        }
        composable(
            route = SolarRoute.QuestionTree.name + "/{questionId}?orderId={orderId}",
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
                    nav.navigate(SolarRoute.QuestionInfo.name + "?questionId=$id") {
                        popUpTo(SolarRoute.QuestionTree.name) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { nav.navigateUp() },
                onCloseToProfile = {nav.navigate(SolarRoute.Profile.name )}
            )
        }

        composable(
            route = SolarRoute.QuestionInfo.name + "?questionId={questionId}",
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
                    nav.navigate("${SolarRoute.QuestionTree.name}/$id?orderId=-1")
                },
                onBack = { nav.navigateUp() }
            )
        }

        composable(
            route =SolarRoute.OrderPriceEstimate.name + "?orderId={orderId}",
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
            route = SolarRoute.Contract.name + "?entrySource={entrySource}&orderId={orderId}",
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
            route = SolarRoute.OrderCost.name + "?orderId={orderId}",
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
            route = SolarRoute.OrderPicture.name + "?orderId={orderId}",
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
            route = SolarRoute.OrderCatalog.name + "?orderId={orderId}",
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
                    nav.navigate(SolarRoute.QuestionInfo.name + "?questionId=$id")
                },
                onOpenQuestionTree = { qId, orderId ->
                    val qIdNonNull = qId ?: -1
                    val orderIdNonNull = orderId ?: -1
                    nav.navigate("${SolarRoute.QuestionTree.name}/$qIdNonNull?orderId=$orderIdNonNull")
                }
            )
        }
        composable(
            route = SolarRoute.OrderInvoice.name + "?orderId={orderId}",
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
        composable(route = SolarRoute.BackUpRestore.name) {
            SolarBackupRestoreScreen(
                onClose = { nav.navigateUp() }
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








        composable(SolarRoute.ProductList.name) {
            ProductListScreen(
                onAddCategoryClick = {
                    nav.navigate(SolarRoute.CategoryEdit.name + "?categoryId=-1")
                },
                onCategoryClick = { categoryId ->
                    nav.navigate(
                        SolarRoute.ProductByCategory.name + "/$categoryId"
                    )
                },
                onEditCategoryClick = { categoryId ->
                    nav.navigate(SolarRoute.CategoryEdit.name + "?categoryId=$categoryId")
                },
                onOpenBrands = {
                    nav.navigate(
                        SolarRoute.BrandList.name
                    )
                },
                onHomeClick = {
                    nav.navigate(
                        SolarRoute.Profile.name
                    )
                },
            )
        }
        composable(
            route = SolarRoute.CategoryEdit.name + "?categoryId={categoryId}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {

            CategoryEditScreen(
                onClose = {
                    nav.navigateUp()
                },
                onAddAttribute = { categoryId ->
                    nav.navigate(
                        SolarRoute.AttributeEdit.name + "/$categoryId?attributeId=-1"
                    )
                }
                ,
                onEditAttribute = { categoryId, attributeId ->
                    nav.navigate(
                        SolarRoute.AttributeEdit.name + "/$categoryId?attributeId=$attributeId"
                    )
                }
            )
        }

        composable(
            route = SolarRoute.ProductByCategory.name + "/{categoryId}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.IntType
                }
            )
        ) {

            ProductByCategoryScreen(
                onBack = {
                    nav.navigateUp()
                },
                onAddProduct = { categoryId ->
                    nav.navigate(SolarRoute.ProductEdit.name + "/$categoryId?productId=-1")
                },
                onProductClick = { productId ->
                    nav.navigate(SolarRoute.ProductDetail.name + "/$productId")
                }
            )
        }


        composable(
            route = SolarRoute.AttributeEdit.name + "/{categoryId}?attributeId={attributeId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.IntType },
                navArgument("attributeId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            AttributeEditScreen(
                onClose = { nav.navigateUp() }
            )
        }

        composable(
            route = SolarRoute.ProductEdit.name + "/{categoryId}?productId={productId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.IntType },
                navArgument("productId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            ProductEditScreen(
                onClose = { nav.navigateUp() }
            )
        }

        composable(
            route = SolarRoute.ProductDetail.name + "/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.IntType
                }
            )
        ) {
            ProductDetailScreen(
                onBack = { nav.navigateUp() },

                onEditPurchasePrice = { productId ->
                    nav.navigate(
                        SolarRoute.PurchasePriceEdit.name + "/$productId"
                    )
                },

                onEditProduct = { productId ->
                    nav.navigate(
                        SolarRoute.ProductEdit.name + "/-1?productId=$productId"
                    )
                },
                onAddInventoryTransaction = { productId ->
                    nav.navigate(SolarRoute.InventoryTransactionEdit.name + "/$productId")
                },
                onAddImageClick = {}
            )
        }

        composable(
            route = SolarRoute.PurchasePriceEdit.name + "/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.IntType
                }
            )
        ) {
            PurchasePriceEditScreen(
                onClose = {
                    nav.navigateUp()
                }
            )
        }

        composable(
            route = SolarRoute.InventoryTransactionEdit.name + "/{productId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.IntType }
            )
        ) {
            InventoryTransactionEditScreen(
                onClose = { nav.navigateUp() }
            )
        }

        composable(SolarRoute.BrandList.name) {
            BrandListScreen(
                onBack = { nav.navigateUp() },
                onAddBrand = {
                    nav.navigate(SolarRoute.BrandEdit.name + "?brandId=-1")
                },
                onEditBrand = { brandId ->
                    nav.navigate(SolarRoute.BrandEdit.name + "?brandId=$brandId")
                }
            )
        }

        composable(
            route = SolarRoute.BrandEdit.name + "?brandId={brandId}",
            arguments = listOf(
                navArgument("brandId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            BrandEditScreen(
                onClose = { nav.navigateUp() }
            )
        }

    }

}




enum class SolarRoute {
    Splash,
    SignIn,
    Profile,



    //new for solarShop
    ProductList,
    CategoryEdit,
    ProductByCategory,
    AttributeEdit,
    ProductEdit,
    ProductDetail,
    PurchasePriceEdit,
    InventoryTransactionEdit,
    BrandList,
    BrandEdit,





    Contract,
    BackUpRestore,

    Order,
    OrderPriceEstimate,
    OrderCost,
    OrderPicture,
    OrderCatalog,
    OrderInvoice,

    QuestionTree,
    QuestionInfo
}

