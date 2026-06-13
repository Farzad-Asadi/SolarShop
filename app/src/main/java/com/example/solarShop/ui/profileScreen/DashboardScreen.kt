package com.example.solarShop.ui.profileScreen

import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.solarShop.AppLanguage
import com.example.solarShop.AppLanguageViewModel
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.LengthUnit
import com.example.solarShop.R
import com.example.solarShop.USER_ROLE_OPTIONS
import com.example.solarShop.USER_ROLE_SELLER
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.entitlement.EntitlementState
import com.example.solarShop.data.entitlement.toPremiumUi
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.orderAll.OrderSummary
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.ConfirmDialogRtl
import com.example.solarShop.utils.LoadingScreen
import com.example.solarShop.utils.MediaPictureStore
import com.example.solarShop.utils.MyCurrencyField
import com.example.solarShop.utils.MyLandlineField
import com.example.solarShop.utils.MyNationalCodeField
import com.example.solarShop.utils.MyPhoneField
import com.example.solarShop.utils.MyStringField
import com.example.solarShop.utils.PictureBucket
import com.example.solarShop.utils.bambooAngledBackground
import com.example.solarShop.utils.currency.toPriceString
import com.example.solarShop.utils.dbToLocalDisplay
import com.example.solarShop.utils.digitsToLatinInline
import com.example.solarShop.utils.formatIranMobileForDisplay
import com.example.solarShop.utils.formatPersianDateTime
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import com.example.solarShop.utils.savePickedImageToBucket
import com.example.solarShop.utils.toCurrencyText
import com.example.solarShop.utils.uiToIranE164
import java.io.File

val horizontalPadding = 8.dp
val verticalPadding = 8.dp

@Composable
fun DashboardScreen(                               //صفحه پروفایل
    onClickAddOrder: (Int) -> Unit,
    onClickOrder: (Int) -> Unit,
    onSignedOut: () -> Unit,
    onShowEditeContract: () -> Unit,
    onClickBackUpRestore: () -> Unit,
    onClickProductList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {

    //region stats

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entState by viewModel.entitlement.collectAsStateWithLifecycle(initialValue = EntitlementState.Inactive)
    val prefs by viewModel.displayPrefsState.collectAsStateWithLifecycle()

    val langVm: AppLanguageViewModel = hiltViewModel()
    val appLang by langVm.appLanguage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Snackbar
    val snackBarHostState = remember { SnackbarHostState() }

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    var showEditeProfileSheet by rememberSaveable { mutableStateOf(false) }
    var showAddClientSheet by rememberSaveable { mutableStateOf(false) }
    var showClientSheet by rememberSaveable { mutableStateOf(false) }
    var showEditeClientSheet by rememberSaveable { mutableStateOf(false) }
    var showPremiumSheet by rememberSaveable { mutableStateOf(false) }

    var choiceClientId by rememberSaveable { mutableIntStateOf(1) }

    var indicatorsExpanded by rememberSaveable { mutableStateOf(false) }
    var marketExpanded by rememberSaveable { mutableStateOf(true) }
    var actionsExpanded by rememberSaveable { mutableStateOf(true) }
    var clientsExpanded by rememberSaveable { mutableStateOf(false) }
    var manualDollarText by rememberSaveable {
        mutableStateOf("")
    }

    val clients = uiState.currentClientWithOrders?.map { it.clientEntity } ?: listOf()
    val orderSummaries = uiState.orderSummaries ?: emptyList()

    BackHandler {
        if (
            !showEditeProfileSheet &&
            !showAddClientSheet &&
            !showClientSheet &&
            !showEditeClientSheet &&
            !showPremiumSheet
        ) {
            showExitDialog = true
        } else {
            showEditeProfileSheet = false
            showAddClientSheet = false
            showClientSheet = false
            showEditeClientSheet = false
            showPremiumSheet = false
        }
    }

//endregion stats

    if (uiState.isDataLoaded) {

        // اسکرین نیمه‌شفاف روی همه‌چیز جهت
        Box(Modifier.fillMaxSize()) {

            // محتوای اصلی
            if (
                !showEditeProfileSheet &&
                !showAddClientSheet &&
                !showEditeClientSheet &&
                !showPremiumSheet
            ) {

                var pendingDelete by remember { mutableStateOf<ClientEntity?>(null) }
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                Scaffold(
                    topBar = {
                        uiState.currentUserEntity?.let {
                            ProfileTopBar(
                                user = it,
                                entState = entState,
                                prefs = prefs,
                                appLanguage = appLang,
                                onChangeLanguage = langVm::setLanguage,
                                onClickSignedOut = { showSignOutDialog = true },
                                onClickAvatar = { showEditeProfileSheet = true },
                                onClickPremium = { showPremiumSheet = true },
                                onChangeCurrency = viewModel::onChangeCurrency,
                                onChangeLength = viewModel::onChangeLength
                            )

                        }
                    },
                    modifier = modifier,
                    snackbarHost = { SnackbarHost(snackBarHostState) }
                ) { innerPadding ->


                    Surface (
                        color =Color.Transparent
                    ){
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .bambooAngledBackground()
                        ) {
                            LazyColumn(

                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()

                            ) {

                                //شاخص‌ها
                                item {
                                    Spacer(Modifier.height(12.dp))

                                    DashboardExpandableCard(
                                        title = "شاخص‌ها",
                                        expanded = indicatorsExpanded,
                                        onExpandedChange = { indicatorsExpanded = it }
                                    ) {
                                        Text("اینجا بعداً ارزش انبار، درآمد، سود و شاخص‌های لحظه‌ای قرار می‌گیرد.")
                                    }
                                }

                                //متغیرهای بازار
                                item {
                                    DashboardExpandableCard(
                                        title = "متغیرهای بازار",
                                        expanded = marketExpanded,
                                        onExpandedChange = { marketExpanded = it }
                                    ) {
                                        Text(
                                            text = "نرخ API: ${
                                                uiState.apiDollarRateToman?.toPriceString() ?: "-"
                                            }"
                                        )

                                        Text(
                                            text = "نرخ موثر: ${
                                                uiState.effectiveDollarRateToman?.toPriceString() ?: "-"
                                            }"
                                        )
                                        MyCurrencyField(
                                            value = uiState.manualDollarRateToman,
                                            onValueChange = viewModel::onManualDollarRateChange,
                                            label = "نرخ دلار دستی",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = viewModel::fetchDollarRateFromApi,
                                            enabled = !uiState.isFetchingDollarRate,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                if (uiState.isFetchingDollarRate) {
                                                    "در حال دریافت..."
                                                } else {
                                                    "دریافت نرخ دلار از API"
                                                }
                                            )
                                        }
                                        uiState.dollarRateMessage?.let { message ->
                                            Text(
                                                text = message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                //دسترسی سریع
                                item {
                                    DashboardExpandableCard(
                                        title = "دسترسی سریع",
                                        expanded = actionsExpanded,
                                        onExpandedChange = { actionsExpanded = it }
                                    ) {
                                        QuickAccessRow(
                                            onClickProductList = onClickProductList,
                                            onClickEditContract = onShowEditeContract,
                                            onClickBackup = onClickBackUpRestore,
                                            onClickUserData = {}
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.testCategorySync()
                                        }
                                    ) {
                                        Text("تست سینک دسته ها")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.testInitialUploadAll()
                                        }
                                    ) {
                                        Text("اپلود دیتا در سرور")
                                    }
                                }

                                //مشتریان
                                item {
                                    DashboardExpandableCard(
                                        title = "مشتریان",
                                        expanded = clientsExpanded,
                                        onExpandedChange = { clientsExpanded = it }
                                    ) {
                                        ClientsHeaderRow(
                                            count = clients.size,
                                            onClickAddClient = {
                                                showAddClientSheet = true
                                            }
                                        )

                                        if (clients.isEmpty()) {
                                            Text(
                                                text = "هنوز مشتری ثبت نشده است.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            clients.forEach { client ->
                                                val numberOfOrderOfThisClient =
                                                    uiState.orderEntityList?.count { it.clientId == client.id } ?: 0


                                                // ۱) دیگه از confirmValueChange استفاده نمی‌کنیم
                                                val state = rememberSwipeToDismissBoxState()

                                                // ۲) تعریف تارگت‌ها بر اساس RTL / LTR
                                                val deleteTarget =
                                                    if (isRtl) SwipeToDismissBoxValue.StartToEnd else SwipeToDismissBoxValue.EndToStart
                                                val editTarget =
                                                    if (isRtl) SwipeToDismissBoxValue.EndToStart else SwipeToDismissBoxValue.StartToEnd

                                                // ۳) واکنش به تغییر استیت با LaunchedEffect
                                                LaunchedEffect(state.currentValue) {
                                                    when (state.currentValue) {
                                                        deleteTarget -> {
                                                            // سوایپ جهت حذف
                                                            pendingDelete = client
                                                            // آیتم رو برگردون به حالت اولیه
                                                            state.snapTo(SwipeToDismissBoxValue.Settled)
                                                        }

                                                        editTarget -> {
                                                            // سوایپ جهت ویرایش
                                                            showEditeClientSheet = true
                                                            client.id?.let { choiceClientId = it }
                                                            // آیتم رو برگردون به حالت اولیه
                                                            state.snapTo(SwipeToDismissBoxValue.Settled)
                                                        }

                                                        SwipeToDismissBoxValue.Settled -> Unit
                                                        else -> Unit
                                                    }
                                                }

                                                SwipeToDismissBox(
                                                    state = state,
                                                    backgroundContent = {
                                                        DismissBackground(
                                                            state = state,
                                                            isRtl = isRtl
                                                        )
                                                    },
                                                    content = {
                                                        ClientRowCard(
                                                            client = client,
                                                            numberOfOrderOfThisClient = numberOfOrderOfThisClient,
                                                            onClickClient = {
                                                                showClientSheet = true
                                                                choiceClientId = client.id!!
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }


                                //                        item { //db 2.3
                                //                            CreateDumpSeed(onClickCreateDumpSeed = { vm.onClickCreateDumpSeed() })
                                //                        }

                            }
                            // دیالوگ تأیید حذف

                            if (pendingDelete != null) {
                                AlertDialog(
                                    onDismissRequest = { pendingDelete = null },
                                    title = { Text(stringResource(R.string.profile_delete_client_title)) },
                                    text = {
                                        Text(
                                            stringResource(
                                                R.string.profile_delete_client_message,
                                                pendingDelete!!.name
                                            )
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.onClickDeleteClient(pendingDelete!!)
                                            pendingDelete = null
                                        }) { Text(stringResource(R.string.common_yes)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { pendingDelete = null }) {
                                            Text(stringResource(R.string.common_no))
                                        }
                                    }
                                )

                            }
                        }

                    }
                }
            }

            //شیت های موقت
            if (showEditeProfileSheet) {
                Scaffold(
                    topBar = {
                        uiState.currentUserEntity?.let {
                            AddAndEditeClientTopBar(
                                topBarTitle = stringResource(R.string.profile_edit_title),
                                onClickBack = { showEditeProfileSheet = false },
                            )
                        }
                    },
                    modifier = modifier
                ) { innerPadding ->
                    EditProfileSheet(
                        modifier = Modifier.padding(innerPadding),
                        userEntity = uiState.currentUserEntity,
                        onBackClick = { showEditeProfileSheet = false },
                        onConfirmClick = {
                            showEditeProfileSheet = false;viewModel.onClickConfirmInEditProfile(it)
                        }
                    )
                }
            }
            if (showAddClientSheet) {
                Scaffold(
                    topBar = {
                        uiState.currentUserEntity?.let {
                            AddAndEditeClientTopBar(
                                topBarTitle =  stringResource(R.string.profile_new_client_title),
                                onClickBack = { showAddClientSheet = false },
                            )
                        }
                    },
                    modifier = modifier
                ) { innerPadding ->
                    uiState.currentUserEntity?.userKey?.let { userKey ->
                        AddAndEditeClientSheet(
                            userKey = userKey,
                            modifier = Modifier.padding(innerPadding),
                            clientEntity = clients.find { it.id == choiceClientId },
                            onBackClick = { showAddClientSheet = false },
                            onConfirmClick = {
                                showAddClientSheet = false; viewModel.onClickConfirmInAddClient(
                                it
                            )
                            },
                            inAddMode = true
                        )
                    }
                }
            }
            if (showEditeClientSheet) {
                Scaffold(
                    topBar = {
                        uiState.currentUserEntity?.let {
                            AddAndEditeClientTopBar(
                                topBarTitle = stringResource(R.string.profile_edit_client_title),
                                onClickBack = {
                                    showAddClientSheet = false;showEditeClientSheet = false
                                },
                            )
                        }
                    },
                    modifier = modifier
                ) { innerPadding ->
                    uiState.currentUserEntity?.userKey?.let { userKey ->
                        AddAndEditeClientSheet(
                            userKey = userKey,
                            modifier = Modifier.padding(innerPadding),
                            clientEntity = clients.find { it.id == choiceClientId },
                            onBackClick = { showEditeClientSheet = false },
                            onConfirmClick = {
                                showEditeClientSheet = false; viewModel.onClickConfirmInEditClient(
                                it
                            )
                            },
                        )
                    }
                }
            }
            if (showPremiumSheet) {
                Scaffold(
                    topBar = {
                        uiState.currentUserEntity?.let {
                            EditePremiumTopBar(
                                onClickBack = { showPremiumSheet = false },
                            )
                        }
                    },
                    modifier = modifier
                ) { innerPadding ->
                    PremiumSheet(
                        modifier = Modifier.padding(innerPadding),
                        user = uiState.currentUserEntity,
                        entState = entState
                    )
                }

            }
            if (showClientSheet) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }) { /* بلاک تاچ زیرین */ }
                )
                {
                    clients.find { it.id == choiceClientId }?.let { client ->
                        ClientDetailsSheet(
                            client = client,
                            prefs = prefs,
                            orderSummaries = orderSummaries.filter { it.order.clientId == choiceClientId },
                            onClickOrder = { orderId ->
                                showClientSheet = false; onClickOrder(orderId)
                            },
                            onClickAddOrder = { clientId ->
                                viewModel.onClickAddOrder(clientId) { newOrderId ->
                                    onClickAddOrder(newOrderId)   // وقتی ساخت، بفرست بالا
                                }
                            },
                            onClickEditClient = {
                                showClientSheet = false; showEditeClientSheet = true
                            },
                            onClickDeleteOrder = viewModel::onClickDeleteOrder,
                            onDismiss = { showClientSheet = false }
                        )
                    }

                }
            }


            //دیالوگ ها
            ConfirmDialogRtl(
                visible = showExitDialog,
                onDismiss = { showExitDialog = false },
                onConfirm = { if (context is ComponentActivity) context.finish() },
                title = stringResource(R.string.profile_exit_title),
                message = stringResource(R.string.profile_exit_message),
                confirmText = stringResource(R.string.common_exit),
                dismissText = stringResource(R.string.common_back),
            )
            ConfirmDialogRtl(
                visible = showSignOutDialog,
                onDismiss = { showSignOutDialog = false },
                onConfirm = { viewModel.signOut(onSignedOut) },
                title = stringResource(R.string.profile_signout_title),
                message = stringResource(R.string.profile_signout_message),
                confirmText = stringResource(R.string.common_sign_out),
                dismissText = stringResource(R.string.common_back),
            )


            // — راه‌حل «اسکرین نیمه‌شفاف» پشت دیالوگ —
            // وقتی showClientWindow=true شد، یک Box تمام‌صفحه با پس‌زمینه‌ی نیمه‌شفاف
            // روی محتوای اصلی می‌کشیم تا پس‌زمینه تیره شود و تعاملات زیرین بلاک شوند.
            // محل قرارگیری: داخل همان Box ریشه‌ی ProfileScreen، بعد از محتوای اصلی.
            // شدت تیرگی را با alpha تنظیم کن (مثلاً 0.35f). رنگ هم قابل تغییر است.
            //
            // مثال:
            // if (showClientWindow) {
            // Box(
            // Modifier
            // .fillMaxSize()
            // .background(Color.Black.copy(alpha = 0.35f))
            // .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* لمس زیرین غیرفعال */ }
            // )
            // }
        }

    } else {
        LoadingScreen()
    }

}


//region Sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    userEntity: UserEntity?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onConfirmClick: (UserEntity) -> Unit,
) {

    // --- Form State ---
    val scrollState = rememberScrollState()
    var avatar by rememberSaveable { mutableStateOf(userEntity?.avatar.orEmpty()) }
    var showAvatarSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // گالری
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        val newPath = savePickedImageToBucket(context, uri, PictureBucket.Profile)
        if (newPath != null) {
            MediaPictureStore.deleteOldIfInBucket(context, avatar, PictureBucket.Profile)
            avatar = newPath
        } else {
            // TODO snackbar: "خطا در ذخیره عکس"
        }
    }

    fun createCameraTemp(bucket: PictureBucket): Pair<File, Uri> {
        val dir = File(context.filesDir, "camera_tmp/${bucket.name.lowercase()}").apply { mkdirs() }
        val file = File(dir, "c_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }


    // دوربین
    var pendingTempFile by remember { mutableStateOf<File?>(null) }

    val camera = rememberCameraCaptureLauncher(
        requiredPermissions = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                arrayOf(android.Manifest.permission.CAMERA)
            } else emptyArray()
        },
        createOutputUri = {
            val (f, u) = createCameraTemp(PictureBucket.Profile) // ↓ پایین توضیح میدم
            pendingTempFile = f
            u
        },
        onResult = { uri, success ->
            val tmp = pendingTempFile

            if (success) {
                val newPath = savePickedImageToBucket(context, uri, PictureBucket.Profile)
                if (newPath != null) {
                    MediaPictureStore.deleteOldIfInBucket(context, avatar, PictureBucket.Profile)
                    avatar = newPath
                }
            }

            // فایل موقت رو همیشه جمع کن
            tmp?.let { runCatching { it.delete() } }
            pendingTempFile = null
        },
        onMessage = { msg ->
            // هرچی تو costscreen می‌کردی همینجا هم بکن
            // مثلا snackbar یا vm.onCameraMessage(msg)
        }
    )


    val presetAvatars = remember {
        listOf(
            R.drawable.ic_avatar_carpenter_01,
            R.drawable.ic_avatar_carpenter_02,
            R.drawable.ic_avatar_carpenter_03,
            R.drawable.ic_avatar_carpenter_04,
            R.drawable.ic_avatar_carpenter_05,
            R.drawable.ic_avatar_carpenter_06,
            R.drawable.ic_avatar_carpenter_07,
            R.drawable.ic_avatar_carpenter_08,
        )
    }
    var name by rememberSaveable { mutableStateOf(userEntity?.name.orEmpty()) }
    val initialLocal = remember(userEntity?.mobilePhone.orEmpty()) {
        dbToLocalDisplay(digitsToLatinInline(userEntity?.mobilePhone.orEmpty()))
    }
    val mobilePhone by rememberSaveable { mutableStateOf(initialLocal) }

    var landlinePhone by rememberSaveable { mutableStateOf(userEntity?.landlinePhone.orEmpty()) }
    var isLandlineValid by rememberSaveable { mutableStateOf(true) }


    var nationalCode by rememberSaveable { mutableStateOf(userEntity?.nationalCode.orEmpty()) }
    var isNationalCodeValid by rememberSaveable { mutableStateOf(true) }

    var workshop by rememberSaveable { mutableStateOf(userEntity?.workshop.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(userEntity?.address.orEmpty()) }
    var role by rememberSaveable(userEntity?.id, userEntity?.role) {
        mutableStateOf(
            userEntity?.role ?: USER_ROLE_SELLER
        )
    }


    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),

        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),

        ) {
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp)
                    .verticalScroll(scrollState)
                    .imePadding(), //وقتی کیبورد باز می‌شود، محتوا زیر کیبورد نرود
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                // --- Header: Avatar + Click ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(5.dp)
                            .clickable { showAvatarSheet = true },
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = CardDefaults.elevatedShape
                    ) {
                        if (avatar.isNotBlank() && File(avatar).exists()) {
                            AsyncImage(
                                model = File(avatar),
                                contentDescription = "آواتار",
                                modifier = Modifier.size(82.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // پیش‌فرض از drawable
                            Image(
                                painter = painterResource(R.drawable.ic_avatar_carpenter_05),
                                contentDescription = "آواتار",
                                modifier = Modifier.size(82.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                HorizontalDivider()


                // --- Fields ---

                //نام و نام خانوادگی
                MyStringField(
                    value = name,
                    onValueChange = { cleaned ->
                        name = cleaned
                        cleaned
                    },
                    onImeDone = {
                        // اگر خواستی، مثلا فوکوس را ببندی یا بری روی فیلد بعدی
                    }
                )

                //تلفن همراه
                OutlinedTextField(
                    value = mobilePhone,
                    onValueChange = { /* نادیده؛ چون readOnly است */ },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_field_mobile)) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                    colors = OutlinedTextFieldDefaults.colors(
                        // پس‌زمینه‌ی ملایم برای تمایز
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,

                        // رنگ کادر کمرنگ‌تر
                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,

                        // متن و لیبل خاکستری‌تر
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,

                        // کرسر را پنهان کن تا حس ویرایشی نده
                        cursorColor = Color.Transparent,

                        // آیکن‌ها اگر داشتی
                        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Read-only"
                        )
                    },
                    // برای جلوگیری از اکشن‌های کیبورد
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.None),
                    keyboardActions = KeyboardActions(onAny = { /* هیچ */ })
                )

                //تلفن ثابت
                MyLandlineField(
                    value = landlinePhone,
                    onValueChange = { landlinePhone = it },
                    onValidChange = { isValid -> isLandlineValid = isValid },
                    onImeDone = { /* اگر خواستی فوکوس جابجا کنی یا کاری کنی */ }
                )

                //کد ملی
                MyNationalCodeField(
                    value = nationalCode,
                    onValueChange = { nationalCode = it },
                    onValidChange = { isValid -> isNationalCodeValid = isValid }
                )

                //کارگاه/نمایشگاه
                MyStringField(
                    value = workshop,
                    onValueChange = { cleaned ->
                        workshop = cleaned
                        cleaned
                    },
                    label = stringResource(R.string.profile_field_workshop),
                    placeholder =stringResource(R.string.profile_field_workshop_hint),
                    onImeDone = {
                        // اگر خواستی، مثلا فوکوس را ببندی یا بری روی فیلد بعدی
                    }
                )

                //آدرس
                MyStringField(
                    value = address,
                    onValueChange = { cleaned ->
                        address = cleaned
                        cleaned
                    },
                    maxLength = 110,
                    label = stringResource(R.string.profile_field_address),
                    placeholder =stringResource(R.string.profile_field_address_hint),
                    onImeDone = {
                        // اگر خواستی، مثلا فوکوس را ببندی یا بری روی فیلد بعدی
                    }
                )

                var roleExpanded by rememberSaveable {
                    mutableStateOf(false)
                }

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = {
                        roleExpanded = !roleExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نقش کاربر") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = roleExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = {
                            roleExpanded = false
                        }
                    ) {
                        USER_ROLE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    role = option
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }


            }
            // --- Actions ---
            Surface(tonalElevation = 3.dp) {
                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.common_back), textAlign = TextAlign.Center) }

                    Button(
                        onClick = {
                            if (!isLandlineValid) return@Button
                            if (!isNationalCodeValid) return@Button

                            userEntity?.let {
                                val e164 = uiToIranE164(mobilePhone)
                                onConfirmClick(
                                    it.copy(
                                        name = name.trim(),
                                        mobilePhone = e164,
                                        landlinePhone = landlinePhone,
                                        nationalCode = nationalCode,
                                        workshop = workshop,
                                        address = address,
                                        avatar = avatar, // ← مسیر آواتار ذخیره می‌شود
                                        role = role
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = userEntity != null && isLandlineValid && isNationalCodeValid
                    ) { Text(stringResource(R.string.btn_verify), textAlign = TextAlign.Center) }
                }
            }
        }
//        }
    }

    // BottomSheet انتخاب آواتار
    if (showAvatarSheet) {
        AvatarPickerSheet(
            onPickFromGallery = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                showAvatarSheet = false
            },
            onPickFromCamera = {
                showAvatarSheet = false
                camera.launch()
            },
            onPickDrawable = { resId ->
                val uri = Uri.parse("android.resource://${context.packageName}/$resId")
                val newPath = savePickedImageToBucket(context, uri, PictureBucket.Profile)
                if (newPath != null) {
                    MediaPictureStore.deleteOldIfInBucket(context, avatar, PictureBucket.Profile)
                    avatar = newPath
                }
                showAvatarSheet = false
            }
            ,
            onDismiss = { showAvatarSheet = false },
            presetAvatars = presetAvatars
        )
    }

}

@Composable
fun AddAndEditeClientSheet(
    userKey: String,
    clientEntity: ClientEntity?,          // null = افزودن
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onConfirmClick: (ClientEntity) -> Unit,
    inAddMode: Boolean = false
) {
    val isAdd = inAddMode || clientEntity == null
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // ---- State ها با کلید ----
    var avatar by rememberSaveable(clientEntity?.id, isAdd) {
        mutableStateOf(if (isAdd) "" else clientEntity?.avatar.orEmpty())
    }
    var name by rememberSaveable(clientEntity?.id, isAdd) {
        mutableStateOf(if (isAdd) "" else clientEntity?.name.orEmpty())
    }
    // موبایل به صورت ۱۰ رقم بعد از ۹ (بدون صفر اول) برای UI
    var mobilePhone by rememberSaveable(clientEntity?.id, isAdd) {
        mutableStateOf(
            if (isAdd) "" else {
                val raw = clientEntity?.mobilePhone.orEmpty()   // ممکنه +98912..., یا 0912...
                val digits = raw.filter(Char::isDigit)

                when {
                    digits.startsWith("98") && digits.length >= 12 ->
                        digits.drop(2).take(10)          // 98 + 912xxxxxxx
                    digits.startsWith("0") && digits.length >= 11 ->
                        digits.drop(1).take(10)          // 0 + 912xxxxxxx
                    digits.startsWith("9") && digits.length >= 10 ->
                        digits.take(10)                  // 912xxxxxxx
                    else ->
                        digits.takeLast(10)              // حالت‌های عجیب
                }
            }
        )
    }
    var isMobilePhoneValid by rememberSaveable { mutableStateOf(true) }

    var landlinePhone by rememberSaveable(clientEntity?.id, isAdd) {
        mutableStateOf(if (isAdd) "" else clientEntity?.landlinePhone.orEmpty())
    }
    var isLandlineValid by rememberSaveable { mutableStateOf(true) }

    var nationalCode by rememberSaveable(clientEntity?.id, isAdd) {
        mutableStateOf(if (isAdd) "" else clientEntity?.nationalCode.orEmpty())
    }
    var isNationalCodeValid by rememberSaveable { mutableStateOf(true) }

    var address by rememberSaveable(clientEntity?.id, isAdd) {
        mutableStateOf(if (isAdd) "" else clientEntity?.address.orEmpty())
    }
    var showAvatarSheet by rememberSaveable(clientEntity?.id, isAdd) { mutableStateOf(false) }

    // ---- لانچرگالری ----
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        val newPath = savePickedImageToBucket(context, uri, PictureBucket.Client)
        if (newPath != null) {
            MediaPictureStore.deleteOldIfInBucket(context, avatar, PictureBucket.Client)
            avatar = newPath
        }
    }

    //لانچر دوربین
    fun createCameraTemp(bucket: PictureBucket): Pair<File, Uri> {
        val dir = File(context.filesDir, "camera_tmp/${bucket.name.lowercase()}").apply { mkdirs() }
        val file = File(dir, "c_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }


    // دوربین
    var pendingTempFile by remember { mutableStateOf<File?>(null) }

    val camera = rememberCameraCaptureLauncher(
        requiredPermissions = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                arrayOf(android.Manifest.permission.CAMERA)
            } else emptyArray()
        },
        createOutputUri = {
            val (f, u) = createCameraTemp(PictureBucket.Client) // ↓ پایین توضیح میدم
            pendingTempFile = f
            u
        },
        onResult = { uri, success ->
            val tmp = pendingTempFile

            if (success) {
                val newPath = savePickedImageToBucket(context, uri, PictureBucket.Client)
                if (newPath != null) {
                    MediaPictureStore.deleteOldIfInBucket(context, avatar, PictureBucket.Client)
                    avatar = newPath
                }
            }

            // فایل موقت رو همیشه جمع کن
            tmp?.let { runCatching { it.delete() } }
            pendingTempFile = null
        },
        onMessage = { msg ->
            // هرچی تو costscreen می‌کردی همینجا هم بکن
            // مثلا snackbar یا vm.onCameraMessage(msg)
        }
    )


    val presetAvatars = remember {
        listOf(
            R.drawable.ic_client_avatar_man,
            R.drawable.ic_client_avatar_women,
            R.drawable.ic_client_avatar_couple,
            R.drawable.ic_client_avatar_home,
            R.drawable.ic_client_avatar_bedroom,
            R.drawable.ic_client_avatar_living_room,
            R.drawable.ic_client_avatar_office,
            R.drawable.ic_client_avatar_kitchen,
        )
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp)
                    .verticalScroll(scrollState)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // --- Header: Avatar + Click ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(5.dp)
                            .clickable { showAvatarSheet = true },
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = CardDefaults.elevatedShape
                    ) {
                        if (avatar.isNotBlank() && File(avatar).exists()) {
                            AsyncImage(
                                model = File(avatar),
                                contentDescription = "آواتار",
                                modifier = Modifier.size(82.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_client_avatar_home),
                                contentDescription = "آواتار",
                                modifier = Modifier.size(82.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                HorizontalDivider()

                // --- Fields ---



                //نام و نام خانوادگی
                MyStringField(
                    value = name,
                    onValueChange = { cleaned ->
                        name = cleaned
                        cleaned
                    },
                    onImeDone = {
                        // اگر خواستی، مثلا فوکوس را ببندی یا بری روی فیلد بعدی
                    }
                )

                //تلفن همراه
                MyPhoneField(
                    national = mobilePhone,
                    onNationalChange = { cleaned ->
                        mobilePhone = cleaned
                    },
                    onImeDone = {
                        // اگر خواستی بعد از Done فوکوس بره روی فیلد بعدی یا کار خاصی بکنی
                    },
                    onValidChange = { isValid -> isMobilePhoneValid = isValid },
                    enabled = true,
                    label = stringResource(R.string.profile_field_mobile)  // "تلفن همراه"
                )


                //تلفن ثابت
                MyLandlineField(
                    value = landlinePhone,
                    onValueChange = { landlinePhone = it },
                    onValidChange = { isValid -> isLandlineValid = isValid },
                    onImeDone = { /* اگر خواستی فوکوس جابجا کنی یا کاری کنی */ }
                )

                //کد ملی
                MyNationalCodeField(
                    value = nationalCode,
                    onValueChange = { nationalCode = it },
                    onValidChange = { isValid -> isNationalCodeValid = isValid }
                )

                //آدرس
                MyStringField(
                    value = address,
                    onValueChange = { cleaned ->
                        address = cleaned
                        cleaned
                    },
                    maxLength = 110,
                    label = stringResource(R.string.profile_field_address),
                    placeholder =stringResource(R.string.profile_field_address_hint),
                    onImeDone = {
                        // اگر خواستی، مثلا فوکوس را ببندی یا بری روی فیلد بعدی
                    }
                )
            }

            // --- Actions ---
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.common_back), textAlign = TextAlign.Center) }

                    Button(
                        onClick = {
                            if (!isLandlineValid) return@Button
                            if (!isNationalCodeValid) return@Button
                            if (!isMobilePhoneValid) return@Button


                            if (isAdd) {
                                onConfirmClick(
                                    ClientEntity(
                                        userKey = userKey,
                                        name = name.trim(),
                                        mobilePhone = mobilePhone,
                                        landlinePhone = landlinePhone,
                                        nationalCode = nationalCode,
                                        address = address,
                                        avatar = avatar
                                    )
                                )
                            } else {
                                onConfirmClick(
                                    clientEntity!!.copy(
                                        name = name.trim(),
                                        mobilePhone = mobilePhone,
                                        landlinePhone = landlinePhone,
                                        nationalCode = nationalCode,
                                        address = address,
                                        avatar = avatar
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isLandlineValid && isNationalCodeValid && isMobilePhoneValid
                    ) { Text(if (isAdd) stringResource(R.string.create) else stringResource(R.string.btn_verify), textAlign = TextAlign.Center) }
                }
            }
        }
//        }
    }

    if (showAvatarSheet) {
        AvatarPickerSheet(
            onPickFromGallery = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                showAvatarSheet = false
            },
            onPickFromCamera = {
                showAvatarSheet = false
                camera.launch()
            },
            onPickDrawable = { resId ->
                val uri = Uri.parse("android.resource://${context.packageName}/$resId")
                val newPath = savePickedImageToBucket(context, uri, PictureBucket.Client)
                if (newPath != null) {
                    MediaPictureStore.deleteOldIfInBucket(context, avatar, PictureBucket.Client)
                    avatar = newPath
                }
                showAvatarSheet = false
            },
            onDismiss = { showAvatarSheet = false },
            presetAvatars = presetAvatars
        )
    }
}


@Composable
fun PremiumSheet(
    user: UserEntity?,
    entState: EntitlementState,
    modifier: Modifier = Modifier,
) {

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),

        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),

        )
    {
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {

            Row(
                Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val premiumLabel = when (entState) {
                        is EntitlementState.Active -> "پریمیوم ✓"
                        EntitlementState.Inactive -> "معمولی"
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(premiumLabel) }
                    )

                    Spacer(Modifier.height(16.dp))

                    if (user == null) {
                        Text(
                            text = stringResource(R.string.common_user_not_found),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                        )
                    } else {
                        Text(
                            "شناسه: ${user.id}",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Start
                        )
                        Text("شماره: ${user.mobilePhone}", textAlign = TextAlign.Start)
                        Text(
                            "عضویت از: ${formatPersianDateTime(user.createdAt)}",
                            textAlign = TextAlign.Start
                        )
                    }

                }
            }

        }
//        }
    }

}


@Composable
fun ClientDetailsSheet(
    client: ClientEntity,
    prefs: DisplayPreferences,
    orderSummaries: List<OrderSummary>,
    onClickOrder: (orderId: Int) -> Unit,
    onClickAddOrder: (clientId: Int) -> Unit,
    onClickEditClient: (ClientEntity) -> Unit,
    onClickDeleteOrder: (OrderSummary) -> Unit,
    onDismiss: () -> Unit,
) {

    var avatar by rememberSaveable { mutableStateOf(client.avatar) }
    LaunchedEffect(client.avatar) {
        avatar = client.avatar
    }


//    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(180)) + scaleIn(tween(200), initialScale = 0.98f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.98f)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .fillMaxHeight(0.72f),
                color =MaterialTheme.colorScheme.surfaceContainer ,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                var pendingDeleteOrder by remember { mutableStateOf<OrderSummary?>(null) }


                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    // نوار بالا
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "بستن")
                        }
                        Card(
                            modifier = Modifier
                                .padding(5.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = CardDefaults.elevatedShape
                        ) {

                            if (avatar.isNotBlank()) {
                                AsyncImage(
                                    model = File(avatar),
                                    contentDescription = "آواتار",
                                    modifier = Modifier.size(42.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // پیش‌فرض از drawable
                                Image(
                                    painter = painterResource(R.drawable.ic_client_avatar_home),
                                    contentDescription = "آواتار",
                                    modifier = Modifier.size(42.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Text(
                            client.name ,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onClickEditClient(client) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "ویرایش مشتری")
                        }
                    }

                    HorizontalDivider()

                    // خلاصه
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryChipCard(
                            title = stringResource(R.string.profile_client_orders_count_title),
                            value = orderSummaries.size.toString(),
                            icon = Icons.Outlined.LocalShipping
                        )
                        SummaryChipCard(
                            title = stringResource(R.string.profile_client_orders_total_title),
                            value = orderSummaries
                                .sumOf { (it.priceEstimateTotal ?: 0L) }
                                .toCurrencyText(prefs),
                            icon = Icons.Outlined.Money
                        )

                    }

                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))

                    //لیست سفارشها
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)      // مهم: قد محتوا کنترل شود و اسکرول داخل دیالوگ بماند
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(orderSummaries, key = { it.order.id!! }) { summary ->

                            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                            val swipeState = rememberSwipeToDismissBoxState()

                            // 🔹 جهت درست برای «کشیدن به چپ»
                            val deleteTarget =
                                if (isRtl) SwipeToDismissBoxValue.StartToEnd   // RTL: راست → چپ
                                else SwipeToDismissBoxValue.EndToStart         // LTR: چپ → راست

                            val openTarget =
                                if (isRtl) SwipeToDismissBoxValue.EndToStart else SwipeToDismissBoxValue.StartToEnd


                            LaunchedEffect(swipeState.currentValue) {
                                when (swipeState.currentValue) {
                                    deleteTarget -> {
                                        pendingDeleteOrder = summary
                                        swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                                    }

                                    openTarget -> {
                                        // همون کاری که کلیک انجام میده
                                        summary.order.id?.let(onClickOrder)
                                        swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                                    }

                                    SwipeToDismissBoxValue.Settled -> Unit
                                    else -> Unit
                                }
                            }

                            SwipeToDismissBox(
                                state = swipeState,
                                backgroundContent = {
                                    DismissBackground(
                                        state = swipeState,
                                        isRtl = isRtl
                                    )
                                },
                                content = {
                                    OrderCardPretty(
                                        summary = summary,
                                        onClick = { summary.order.id?.let(onClickOrder) }
                                    )
                                }
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }


                    // مرزبندی با اکشن‌ها
                    HorizontalDivider()

                    // دکمه‌های پایین (همیشه داخل کادر)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.profile_client_close))
                        }
                        Button(
                            onClick = { client.id?.let(onClickAddOrder) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
//                                Icon(Icons.Filled.Add, null)
//                                Spacer(Modifier.width(2.dp))
                            Text(stringResource(R.string.profile_client_new_order))
                        }
                    }

                    if (pendingDeleteOrder != null) {
                        AlertDialog(
                            onDismissRequest = { pendingDeleteOrder = null },
                            title = { Text(stringResource(R.string.profile_delete_order_title)) },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.profile_delete_order_message,
                                        pendingDeleteOrder!!.order.id ?: 0   // یا هر متن دلخواه
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    pendingDeleteOrder?.let(onClickDeleteOrder)
                                    pendingDeleteOrder = null
                                }) {
                                    Text(stringResource(R.string.common_yes))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingDeleteOrder = null }) {
                                    Text(stringResource(R.string.common_no))
                                }
                            }
                        )
                    }

                }
            }
        }
//        }
    }
}

//endregion Sheets


//region Components

@Composable
private fun ProfileTopBar(
    user: UserEntity,
    entState: EntitlementState,
    prefs: DisplayPreferences,
    appLanguage: AppLanguage,
    onChangeLanguage: (AppLanguage) -> Unit,
    onClickSignedOut: () -> Unit,
    onClickAvatar: () -> Unit,
    onClickPremium: () -> Unit = {},
    onChangeLength: (LengthUnit) -> Unit,
    onChangeCurrency: (CurrencyUnit) -> Unit,
)
{
    var avatar by rememberSaveable { mutableStateOf(user.avatar) }
    LaunchedEffect(user.avatar) {
        avatar = user.avatar
    }

    val ui = remember(entState) { entState.toPremiumUi() }

    val gold = Color(0xFFFFC107)
    val grey = MaterialTheme.colorScheme.onSurfaceVariant

    val iconTint = if (ui.isPremium) gold else grey.copy(alpha = 0.5f)
    val iconSize = if (ui.isPremium) 28.dp else 24.dp



    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp
        ),
        color = BambooTheme.sections.topBarContainer,
        contentColor = BambooTheme.sections.topBarContent,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 👈 بخش کاربر: آواتار + نام + نقش (با وزن)
            Row(
                modifier = Modifier
                    .weight(1f)              // این سمت، فضای باقیمانده را می‌گیرد
                    .clickable { onClickAvatar() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.padding(5.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = CardDefaults.elevatedShape
                ) {
                    if (avatar.isNotBlank() && File(avatar).exists()) {
                        AsyncImage(
                            model = File(avatar),
                            contentDescription = "آواتار",
                            modifier = Modifier.size(62.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_avatar_carpenter_05),
                            contentDescription = "آواتار",
                            modifier = Modifier.size(62.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f) // نام/نقش هم در همین فضا مدیریت می‌شوند
                        .padding(end = 2.dp)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                    Text(
                        text = user.role,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // 👉 بخش اکشن‌ها: پریمیوم + تنظیمات + خروج
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Premium
//                Box(
//                    modifier = Modifier.wrapContentSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    IconButton(
//                        onClick = { onClickPremium() },
//                        colors = IconButtonDefaults.iconButtonColors(contentColor = iconTint)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Outlined.Diamond,
//                            contentDescription = if (ui.isPremium)
//                                "پریمیوم؛ ${ui.daysLeft} روز باقی‌مانده"
//                            else
//                                "غیرپریمیوم؛ 0 روز باقی‌مانده",
//                            tint = iconTint,
//                            modifier = Modifier.size(iconSize)
//                        )
//                    }
//
//                    Box(
//                        modifier = Modifier
//                            .align(Alignment.BottomEnd)
//                            .offset(x = (-2).dp, y = (-2).dp)
//                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
//                            .background(MaterialTheme.colorScheme.surface, CircleShape)
//                            .padding(horizontal = 4.dp, vertical = 1.dp)
//                    ) {
//                        Text(
//                            text = ui.daysLeft.toString(),
//                            style = MaterialTheme.typography.labelSmall,
//                            color = if (ui.isPremium) MaterialTheme.colorScheme.onSurface else grey,
//                            textAlign = TextAlign.Center,
//                            modifier = Modifier.widthIn(min = 14.dp)
//                        )
//                    }
//                }

                SettingsButtonWithMenu(
                    prefs = prefs,
                    appLanguage = appLanguage,
                    onChangeLanguage = onChangeLanguage,
                    onChangeLength = onChangeLength,
                    onChangeCurrency = onChangeCurrency,
                )

                IconButton(
                    onClick = { onClickSignedOut() },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Logout")
                }
            }
        }
    }

}

@Composable
private fun AddAndEditeClientTopBar(
    topBarTitle: String,
    onClickBack: () -> Unit = {},
) {

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp   // فقط گوشه‌های پایین
        ),
        color = BambooTheme.sections.topBarContainer,
        contentColor = BambooTheme.sections.topBarContent,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.padding(end = 12.dp),
                onClick = { onClickBack() },
//                colors = IconButtonDefaults.iconButtonColors(
//                    contentColor = MaterialTheme.colorScheme.onPrimary
//                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "ArrowBack")
            }
            Text(
                text = topBarTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))

        }
    }
}

@Composable
private fun EditePremiumTopBar(
    onClickBack: () -> Unit = {},
) {

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp   // فقط گوشه‌های پایین
        ),
        color = BambooTheme.sections.topBarContainer,
        contentColor = BambooTheme.sections.topBarContent,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.padding(end = 12.dp),
                onClick = { onClickBack() },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "Catalog Q&A")
            }
            Text(
                text = "پریمیوم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))

        }
    }
}

@Composable
fun QuickAccessRow(
    modifier: Modifier = Modifier,
    onClickProductList: () -> Unit,
    onClickEditContract: () -> Unit,
    onClickBackup: () -> Unit,
    onClickUserData: () -> Unit,
) {
//    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // عنوان بالا-سمت راست
        Text(
            text = stringResource(R.string.profile_quick_access_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // لیست افقیِ کاشی‌ها (بدون Card/Surface کلی)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            reverseLayout = true
        ) {
            item {
                QuickTile(
                    icon = Icons.Outlined.Inventory2,
                    label = stringResource(R.string.profile_quick_access_questions),
                    onClick = onClickProductList
                )
            }
            item {
                QuickTile(
                    icon = Icons.Outlined.Description,
                    label = stringResource(R.string.profile_quick_access_edit_contract),
                    onClick = onClickEditContract
                )
            }
//            item {
//                QuickTile(
//                    icon = Icons.Outlined.Person,
//                    label = stringResource(R.string.profile_quick_access_user_data),
//                    onClick = onClickUserData
//                )
//            }
            item {
                QuickTile(
                    icon = Icons.Outlined.SettingsBackupRestore,
                    label =  stringResource(R.string.profile_quick_access_backup_restore),
                    onClick = onClickBackup
                )
            }

        }
//        }
    }
}

@Composable
private fun ClientsHeaderRow(
    onClickAddClient: () -> Unit,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 2.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onClickAddClient() }
        )
        Text(
            stringResource(R.string.profile_clients_add_client),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onClickAddClient() }
        )

        Spacer(modifier = Modifier.weight(1f))


        BadgeSmall(
            text = count.toString()
        )
        Text(
            stringResource(R.string.profile_clients_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            Icons.Outlined.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

    }
}

@Composable
private fun ClientRowCard(
    client: ClientEntity,
    numberOfOrderOfThisClient: Int,
    onClickClient: (clientId: Int?) -> Unit,
    modifier: Modifier = Modifier
) {


    Card(
        modifier = modifier

            .clickable { onClickClient(client.id) }
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(
                BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
                RoundedCornerShape(10.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // avatar
            if (client.avatar.isNotBlank() && File(client.avatar).exists()) {
                AsyncImage(
                    model = File(client.avatar),
                    contentDescription = "آواتار",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // پیش‌فرض از drawable
                Image(
                    painter = painterResource(R.drawable.ic_client_avatar_home),
                    contentDescription = "آواتار",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            //name mobile CreatAt
            Column(Modifier.weight(1f)) {
                Text(
                    client.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatIranMobileForDisplay(client.mobilePhone),
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formatPersianDateTime(client.createdAt, true),
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(Modifier.width(24.dp))

            Text(
                text = stringResource(
                    R.string.profile_clients_orders_count,
                    numberOfOrderOfThisClient
                ),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

        }
    }
}

@Composable
private fun SummaryChipCard(
    title: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}



@Composable
fun OrderCardPretty(
    summary: OrderSummary,
    onClick: () -> Unit,
) {
    val order = summary.order
    val title = order.name ?: "بدون عنوان"
    val note = order.note.orEmpty()
    val dateStr = formatPersianDateTime(order.createdAt, noClock = true)


    val progress = (summary.progressPercent ?: 0).coerceIn(0, 100)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color =MaterialTheme.colorScheme.secondaryContainer ,
        contentColor =MaterialTheme.colorScheme.onSecondaryContainer  ,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(12.dp)) {

            // 🟦 ردیف بالا: عنوان سفارش + تاریخ
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.width(8.dp))

                // تاریخ بیاد بالا سمت راست
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 🔹 خط باریک زیر هدر
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // 🟦 ردیف ۱ آیکون‌ها
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ===== ۱) برآورد قیمت =====
                val priceActive = summary.priceEstimateTotal != null && summary.priceEstimateTotal > 0L

                SummaryStatIcon(
                    icon = if (priceActive) Icons.Filled.AttachMoney else Icons.Outlined.AttachMoney,
                    value = null,
                    active = priceActive,
                    label = "برآورد"
                )

                // ===== ۲) انتخاب‌های کاتالوگ =====
                val catalogActive = (summary.catalogSelectedCount ?: 0) > 0

                SummaryStatIcon(
                    icon = if (catalogActive) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
                    value = null,
                    active = catalogActive,
                    label = "انتخاب‌ها"
                )

                // ===== ۳) قرارداد =====
                val contractActive = summary.hasContract == true

                SummaryStatIcon(
                    icon = if (contractActive) Icons.Filled.Contacts else Icons.Outlined.Contacts,
                    value = null,
                    active = contractActive,
                    label = "قرارداد"
                )

                // ===== ۴) هزینه نهایی =====
                val costActive = summary.costResult != null && summary.costResult > 0L

                SummaryStatIcon(
                    icon = if (costActive) Icons.Filled.Money else Icons.Outlined.Money,
                    value = null,
                    active = costActive,
                    label = "هزینه"
                )

                // ===== ۵) عکس‌ها =====
                val photoActive = summary.galleryPhotoCount > 0

                SummaryStatIcon(
                    icon = if (photoActive) Icons.Filled.PictureInPicture else Icons.Outlined.PictureInPicture,
                    value = null,
                    active = photoActive,
                    label = "عکس"
                )

                // ===== ۶) پیش‌فاکتور / فاکتور =====
                val factureActive = summary.hasPreFacture == true || summary.hasFacture == true

                SummaryStatIcon(
                    icon = if (factureActive) Icons.Filled.RequestQuote else Icons.Outlined.RequestQuote,
                    value = null,
                    active = factureActive,
                    label = "پیش‌فاکتور"
                )
            }


//            Spacer(Modifier.height(6.dp))
//
//            // 🟦 ردیف ۲ آیکون‌ها (آیتم‌های کاتالوگ + عکس‌ها)
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(12.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//
//
//
//                // تعداد عکس‌های پین‌شده
//                SummaryStatIcon(
//                    icon = Icons.Outlined.PushPin,
//                    value = summary.pinnedPhotoCount.takeIf { it > 0 }?.toString(),
//                    active = summary.pinnedPhotoCount > 0,
//                    label = "پین"
//                )
//            }

            // 🟦 نوار پیشرفت سفارش (دل‌خواه ولی خیلی کمک می‌کنه)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(Modifier.width(6.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$progress٪",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }
    }
}


@Composable
private fun SummaryStatIcon(
    icon: ImageVector,
    value: String?,
    active: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null   // فقط برای contentDescription / دیباگ
) {
    val color = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        if (value != null) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    onPickFromGallery: () -> Unit,
    onPickFromCamera: () -> Unit,
    onPickDrawable: (Int) -> Unit,
    onDismiss: () -> Unit,
    presetAvatars: List<Int>
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("انتخاب آواتار", style = MaterialTheme.typography.titleMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onPickFromGallery) { Text("از گالری") }
                OutlinedButton(onClick = onPickFromCamera) { Text("دوربین") }
            }

            Text("آواتارهای آماده", style = MaterialTheme.typography.labelLarge)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(presetAvatars.size) { idx ->
                    val resId = presetAvatars[idx]
                    Card(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { onPickDrawable(resId) },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Image(
                            painter = painterResource(resId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}


/** کاشی مربعی زیبا، بدون Card/Surface؛ فقط Box با پس‌زمینهٔ تونال، بوردر و سایهٔ سبک */
@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    size: Dp = 96.dp,
    corner: Dp = 6.dp
) {
    val bg = MaterialTheme.colorScheme.primaryContainer
    val fg = MaterialTheme.colorScheme.onPrimaryContainer
    val brd = MaterialTheme.colorScheme.outlineVariant
    val pr = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(size),
        contentAlignment = AbsoluteAlignment.BottomLeft
    ) {
        Box(
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(corner), clip = true)
                .size(size - (20.dp))
                .background(bg, RoundedCornerShape(corner))
                .border(0.6.dp, brd, RoundedCornerShape(corner))
                .align(AbsoluteAlignment.BottomLeft)
                .clickable(onClick = onClick)

        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp)
                    .align(AbsoluteAlignment.CenterLeft)
            )

        }
        Box(
            modifier = Modifier
//                .size(32.dp)
                .align(AbsoluteAlignment.TopRight)
                .padding(12.dp)
                .size(30.dp)
                .background(bg, RoundedCornerShape(corner))
                .border(0.6.dp, pr, RoundedCornerShape(corner)),
            contentAlignment = Alignment.Center

        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier
                    .size(28.dp)

            )
        }
    }
}

@Composable
private fun DismissBackground(
    state: SwipeToDismissBoxState,
    isRtl: Boolean
) {
    // جهت‌ها مطابق RTL/LTR
    val deleteDir =
        if (isRtl) SwipeToDismissBoxValue.StartToEnd else SwipeToDismissBoxValue.EndToStart
    val editDir =
        if (isRtl) SwipeToDismissBoxValue.EndToStart else SwipeToDismissBoxValue.StartToEnd

    val color = when (state.dismissDirection) {
        deleteDir -> MaterialTheme.colorScheme.errorContainer
        editDir -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val icon = when (state.dismissDirection) {
        deleteDir -> Icons.Outlined.Delete
        editDir -> Icons.Outlined.Edit
        else -> Icons.Outlined.Swipe // هر آیکن دلخواه
    }

    // پس‌زمینه + آیکن در لبه‌ی مقصدِ سوايپ
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(color)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        val align = when (state.dismissDirection) {
            deleteDir -> if (isRtl) Alignment.CenterStart else Alignment.CenterEnd
            editDir -> if (isRtl) Alignment.CenterEnd else Alignment.CenterStart
            else -> Alignment.Center
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(align)
        )
    }
}

@Composable
private fun SettingsButtonWithMenu(
    prefs: DisplayPreferences,
    appLanguage: AppLanguage,
    onChangeLanguage: (AppLanguage) -> Unit,
    onChangeLength: (LengthUnit) -> Unit,
    onChangeCurrency: (CurrencyUnit) -> Unit,
    modifier: Modifier = Modifier
)
{
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.Settings, contentDescription = "تنظیمات")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = 0.dp)
        ) {
//            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(min = 220.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_menu_display_settings),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()

                // --- زبان برنامه ---
//                Text(
//                    text = stringResource(R.string.label_language),
//                    style = MaterialTheme.typography.labelLarge
//                )
//                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    FilterChip(
//                        selected = appLanguage == AppLanguage.FA,
//                        onClick = { onChangeLanguage(AppLanguage.FA) },
//                        label = { Text(stringResource(R.string.lang_persian)) }
//                    )
//                    FilterChip(
//                        selected = appLanguage == AppLanguage.EN,
//                        onClick = { onChangeLanguage(AppLanguage.EN) },
//                        label = { Text(stringResource(R.string.lang_english)) }
//                    )
//                }
//
//                HorizontalDivider()

                // --- واحد طول ---
                Text(
                    text = stringResource(R.string.profile_menu_length_unit),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.lengthUnit == LengthUnit.METER,
                        onClick = { onChangeLength(LengthUnit.METER) },
                        label = { Text(stringResource(R.string.length_unit_meter)) }
                    )
                    FilterChip(
                        selected = prefs.lengthUnit == LengthUnit.CENTIMETER,
                        onClick = { onChangeLength(LengthUnit.CENTIMETER) },
                        label = { Text(stringResource(R.string.length_unit_centimeter)) }
                    )
                }

                HorizontalDivider()

                // --- واحد پول ---
                Text(
                    text = stringResource(R.string.profile_menu_currency_unit),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.currencyUnit == CurrencyUnit.TOMAN,
                        onClick = { onChangeCurrency(CurrencyUnit.TOMAN) },
                        label = { Text(stringResource(R.string.currency_unit_toman)) }
                    )
                    FilterChip(
                        selected = prefs.currencyUnit == CurrencyUnit.RIAL,
                        onClick = { onChangeCurrency(CurrencyUnit.RIAL) },
                        label = { Text(stringResource(R.string.currency_unit_rial)) }
                    )
                }
            }
//            }
        }

    }
}



//endregion Components


//region Utils

@Composable
private fun BadgeSmall(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
//endregion Utils



@Composable
private fun DashboardExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}





















