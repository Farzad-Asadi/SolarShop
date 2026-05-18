package com.example.solarShop.ui.signInScreen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BackupTable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SolarShop.R
import com.example.solarShop.AppLanguage
import com.example.solarShop.utils.MyPhoneField
import com.example.solarShop.utils.bambooAngledBackground
import kotlinx.coroutines.launch

// ------------- SignInScreen (Final) -------------
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    viewModel: SignInScreenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPrivacy by remember { mutableStateOf(false) }

    val context = LocalContext.current



    // رویدادها
    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is SignInUiEvent.NavigateHome -> onNavigateHome()
                is SignInUiEvent.Message -> scope.launch {
                    snackBarHostState.showSnackbar(ev.text)
                }
            }
        }
    }


    Scaffold(snackbarHost = { SnackbarHost(snackBarHostState) }) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .bambooAngledBackground(),
            contentAlignment = Alignment.Center
        ) {
            // لوگو
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = stringResource(R.string.cd_app_logo),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
                    .size(240.dp)
            )
            // ستون اصلی ...
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 120.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // تیتر + زیرتیتر
                Crossfade(targetState = state.step) { step ->
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = if (step == SignInStep.EnterPhone)
                                stringResource(R.string.signin_title_phone)
                            else
                                stringResource(R.string.otp_title),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (step == SignInStep.EnterPhone)
                                stringResource(R.string.signin_subtitle_phone)
                            else
                                stringResource(R.string.otp_subtitle_enter_code),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (state.step == SignInStep.EnterPhone) {
                    MyPhoneField(
                        national = state.phoneNational,
                        onNationalChange = { raw ->
                            if (!state.isLoading) {
                                viewModel.onPhoneNationalChange(raw)
                            }
                        },
                        onImeDone = {
                            if (state.phoneNational.length == 10 && !state.isLoading) {
                                viewModel.onClickSendOtp()
                            }
                        },
                        enabled = !state.isLoading,
                    )

                    Spacer(Modifier.height(12.dp))

                    LoadingButton(
                        loading = state.isLoading,
                        onClick = viewModel::onClickSendOtp,
                        enabled = state.phoneNational.length == 10,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.btn_send_code)) }

                    PrivacyPolicyInlineNotice(
                        onOpenPolicy = { showPrivacy = true },
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    if (showPrivacy) {
                        PrivacyPolicyDialog(
                            onDismiss = { showPrivacy = false }
                        )
                    }

                } else {
                    OtpCodeField(
                        code = state.code,
                        onCodeChange = viewModel::onCodeChange,
                        onImeDone = {
                            if (state.code.length == 6 && !state.isLoading) {
                                viewModel.onClickVerify()
                            }
                        },
                        length = 6,
                        isError = state.codeError != null,
                        modifier = Modifier
                    )

                    Spacer(Modifier.height(6.dp))

                    state.codeError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LoadingButton(
                        loading = state.isLoading,
                        onClick = viewModel::onClickVerify,
                        enabled = state.code.length == 6,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.btn_verify)) }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = viewModel::onClickEditPhone,
                            enabled = !state.isLoading
                        ) { Text(stringResource(R.string.btn_edit_phone)) }

                        Spacer(Modifier.weight(1f))

                        ResendCountdownButton(
                            secondsLeft = state.secondsToResend,
                            totalSeconds = 60,
                            onClick = viewModel::onClickResend,
                            modifier = Modifier
                        )
                    }

                    PrivacyPolicyInlineNotice(
                        onOpenPolicy = { showPrivacy = true },
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    if (showPrivacy) {
                        PrivacyPolicyDialog(
                            onDismiss = { showPrivacy = false }
                        )
                    }

                }
            }

            //db 2.3
//            CreateDumpSeed(onClickCreateDumpSeed = { viewModel.onClickCreateDumpSeed() })


            // 👇 سوییچر زبان: ثابت در پایین صفحه
//            LanguageSwitcher(
//                selected = appLang,
//                onSelect = { lang ->
//                    viewModel.onPickLanguage(lang)
//                    // ⬇️ برای اعمال سریع UI:
//                    context.findActivity()?.recreate()
//                },
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .padding(16.dp)
//            )
        }
    }
}
// ------------------------------------------------




// (۴) OTP با کنتراست بالا + نشانگر فوکوس اسلات بعدی
@Composable
fun OtpCodeField(
    code: String,
    onCodeChange: (String) -> String,
    onImeDone: () -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false
) {
    fun sanitize(input: String) = input.filter(Char::isDigit).take(length)

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() }
        ) {
            BasicTextField(
                value = code,
                onValueChange = { onCodeChange(sanitize(it)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onImeDone()
                        focusManager.clearFocus()
                    }
                ),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .onFocusChanged { fieldFocused = it.isFocused },
                decorationBox = {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val scheme = MaterialTheme.colorScheme
                        val shape = RoundedCornerShape(12.dp)

                        val slots = length
                        val spacing = 8.dp

                        // عرض مفید برای خود اسلات‌ها (بدون فاصله‌ها)
                        val availableForSlots = maxWidth - spacing * (slots - 1)

                        // اندازهٔ داینامیک هر خونه (مین و مکس برای زیبایی)
                        val slotSize = (availableForSlots / slots)
                            .coerceIn(36.dp, 56.dp)

                        // فونت هم بر اساس اندازهٔ خونه کمی تغییر کنه
                        val textStyle =
                            if (slotSize < 44.dp)
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            else
                                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                spacing,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(slots) { index ->
                                val char = code.getOrNull(index)?.toString().orEmpty()
                                val isFilled = char.isNotEmpty()
                                val isCaretSlot = index == code.length && code.length < slots

                                val borderColor = when {
                                    isError -> scheme.error
                                    fieldFocused && isCaretSlot -> scheme.primary
                                    else -> scheme.outline
                                }
                                val borderWidth = when {
                                    isError -> 1.dp
                                    fieldFocused && isCaretSlot -> 2.dp
                                    else -> 1.dp
                                }
                                val bg =
                                    if (isFilled) scheme.surface
                                    else scheme.surfaceVariant.copy(alpha = 0.65f)

                                Surface(
                                    color = bg,
                                    contentColor = scheme.onSurface,
                                    shape = shape,
                                    tonalElevation = if (isFilled) 2.dp else 0.dp,
                                    border = BorderStroke(borderWidth, borderColor),
                                    modifier = Modifier.size(slotSize)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isFilled) {
                                            Text(
                                                text = char,
                                                style = textStyle
                                            )
                                        } else if (fieldFocused && isCaretSlot) {
                                            Box(
                                                Modifier
                                                    .width(3.dp)
                                                    .height(slotSize * 0.55f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}



// (۳،۱۰) دکمهٔ لودینگ: داخل دکمه، نه سراسری
@Composable
fun LoadingButton(
    loading: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
    ) {
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Text("لطفاً صبر کن")
            }
        } else {
            content()
        }
    }
}


@Composable
fun ResendCountdownButton(
    secondsLeft: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    totalSeconds: Int = 60,
    // اگر بخواهی حتی وقتی آماده شد هم غیرفعال باشد، false بده
    enabledWhenReady: Boolean = true
) {
    val ready = secondsLeft <= 0
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)

    // پیشرفت: از 0→1 پر می‌شود
    val rawProgress =
        if (ready) 1f else 1f - (secondsLeft.coerceAtLeast(0) / totalSeconds.toFloat())
    val progress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = tween(300), label = "resend-progress"
    )

    // رنگ‌ها
    val trackColor = scheme.surfaceVariant.copy(alpha = 0.6f)      // نوار زمینه
    val fillColor = scheme.primary.copy(alpha = 0.25f)            // پرشدن
    val textColor = if (ready) scheme.primary else scheme.onSurface

    // خود دکمه (تمام‌عرض) + پس‌زمینه‌ی پرشونده
    TextButton(
        onClick = onClick,
        enabled = ready && enabledWhenReady,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 44.dp)
            .clip(shape),
        shape = shape,
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .background(trackColor)
        ) {
            // لایهٔ پرشونده
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(fillColor)
            )

            // متن (راست‌چین برای RTL)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (ready) {
                        stringResource(R.string.btn_resend)
                    } else {
                        stringResource(R.string.btn_resend_in_seconds, secondsLeft)
                    },

                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}


@Composable
fun LanguageSwitcher(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        color = scheme.surface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Text(
//                text = stringResource(R.string.label_language),
//                color = scheme.onSurfaceVariant,
//                style = MaterialTheme.typography.labelLarge
//            )
//            Spacer(Modifier.width(12.dp))

            // دو چیپ ساده
            FilterChip(
                selected = selected == AppLanguage.FA,
                onClick = { onSelect(AppLanguage.FA) },
                label = { Text(stringResource(R.string.lang_persian)) }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = selected == AppLanguage.EN,
                onClick = { onSelect(AppLanguage.EN) },
                label = { Text(stringResource(R.string.lang_english)) }
            )
        }
    }
}


@Composable
private fun CreateDumpSeed(                            // یک دکمه/اکشن موقتی برای گرفتن خروجی
    modifier: Modifier = Modifier,
    onClickCreateDumpSeed: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.TopCenter,
    ) {
        IconButton(
            modifier = Modifier.padding(end = 12.dp),
            onClick = { onClickCreateDumpSeed() },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Outlined.BackupTable, contentDescription = "BackupTable")
        }
    }
}


@Composable
fun PrivacyPolicyInlineNotice(
    onOpenPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        append("با ادامه دادن، ")
        pushStringAnnotation(tag = "policy", annotation = "policy")
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        ) { append("سیاست حفظ حریم خصوصی") }
        pop()
        append(" را می‌پذیرید.")
    }

    ClickableText(
        text = annotated,
        style = TextStyle(
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("policy", offset, offset)
                .firstOrNull()
                ?.let { onOpenPolicy() }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سیاست حفظ حریم خصوصی") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scroll)
            ) {
                Text(
                    text = PRIVACY_POLICY_FA,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}

// ✅ متن کامل سیاست (همانی که بالا دادم) را اینجا قرار بده
private const val PRIVACY_POLICY_FA = """
سیاست حفظ حریم خصوصی اپلیکیشن بامبو (Bambo)
تاریخ اجرا: 2026-01-24
ناشر/مالک: [نام ناشر/کسب‌وکار]
راه ارتباطی: [ایمیل پشتیبانی] / [شماره تماس اختیاری]

1) مقدمه

اپلیکیشن بامبو برای مدیریت اطلاعات فروش/مشتری/سفارش و خروجی‌هایی مثل پیش‌فاکتور و گزارش طراحی شده است. ما به حریم خصوصی کاربران اهمیت می‌دهیم و در این سند توضیح می‌دهیم چه اطلاعاتی جمع‌آوری می‌شود، چگونه استفاده می‌شود، و چه کنترل‌هایی در اختیار شماست.

با استفاده از اپلیکیشن و تکمیل فرآیند ورود، شما این سیاست را مطالعه کرده و می‌پذیرید.

2) چه اطلاعاتی جمع‌آوری می‌شود؟

بامبو بسته به استفاده شما ممکن است این داده‌ها را دریافت/ذخیره کند:

2-1) اطلاعات ورود و کاربری

شماره موبایل (برای ارسال و تأیید کد یکبارمصرف)

اطلاعات مربوط به نشست کاربر (مثل توکن ورود/شناسه کاربر) برای نگه‌داشتن وضعیت ورود

2-2) اطلاعات مشتریان و سفارش‌ها

اطلاعات مشتری (مثل نام، شماره تماس، توضیحات)

اطلاعات سفارش (اقلام، انتخاب‌ها، هزینه‌ها، برآورد قیمت، یادداشت‌ها، وضعیت‌ها)

فایل‌ها/تصاویر انتخاب‌شده برای سفارش (در صورت استفاده شما)

2-3) اطلاعات فنی حداقلی

اطلاعات فنی لازم برای عملکرد اپ (مانند خطاهای برنامه در سطح دستگاه)

ما در حال حاضر از تبلیغات و ردیاب‌های تبلیغاتی استفاده نمی‌کنیم. اگر در آینده ابزار جدیدی اضافه شود، این سیاست به‌روزرسانی و اطلاع‌رسانی خواهد شد.

3) اطلاعات چگونه استفاده می‌شود؟

اطلاعات شما برای اهداف زیر استفاده می‌شود:

ارائه قابلیت ورود (OTP) و احراز هویت

ساخت، مدیریت و پیگیری سفارش‌ها

تولید خروجی‌ها (مانند PDF/پیش‌فاکتور/گزارش)

بهبود تجربه کاربری و رفع اشکالات برنامه

4) اطلاعات کجا ذخیره می‌شود؟

4-1) ذخیره‌سازی محلی روی دستگاه
بخش عمده اطلاعات (مشتریان، سفارش‌ها، انتخاب‌ها، تصاویر انتخاب‌شده) به‌صورت محلی روی دستگاه شما ذخیره می‌شود.

4-2) تبادل اطلاعات شبکه
برای فرآیند ورود (ارسال و تأیید کد یکبارمصرف) و برخی همگام‌سازی‌های مرتبط با حساب کاربری (در صورت فعال بودن)، اطلاعات لازم از طریق اینترنت با سرویس‌های احراز هویت/سرورهای مرتبط تبادل می‌شود. ما تلاش می‌کنیم حداقل داده لازم تبادل شود.

5) مجوزهای دسترسی (Permissions)

اپ ممکن است برای ارائه امکانات زیر درخواست مجوز کند:

دسترسی به تصاویر/ویدئوهای دستگاه: برای انتخاب عکس و استفاده در سفارش‌ها/خروجی‌ها

اینترنت: برای ورود و ارتباطات ضروری

6) اشتراک‌گذاری با اشخاص ثالث

ما اطلاعات شخصی شما را نمی‌فروشیم.
اطلاعات فقط در این موارد ممکن است با سرویس‌های ضروری به اشتراک گذاشته شود:

سرویس ارسال/تأیید پیامک OTP (در حد لازم برای ورود)

خدمات زیرساختی ضروری (در صورت استفاده)

7) مدت نگه‌داری اطلاعات

اطلاعات محلی تا زمانی که شما آن‌ها را حذف کنید یا برنامه را حذف کنید باقی می‌ماند.

اطلاعات مرتبط با ورود/نشست به‌تناسب نیاز اپ نگهداری و با خروج از حساب/حذف داده قابل پاکسازی است.

8) امنیت اطلاعات

ما اقدامات معقول فنی برای حفاظت از داده‌ها انجام می‌دهیم، اما هیچ روشی در دنیای دیجیتال 100٪ بدون ریسک نیست. توصیه می‌شود:

قفل صفحه گوشی را فعال نگه دارید

دسترسی فیزیکی دیگران به دستگاه را محدود کنید

9) حقوق و کنترل‌های کاربر

شما می‌توانید:

داده‌های مشتری/سفارش را در اپ اصلاح یا حذف کنید

با حذف برنامه، داده‌های محلی را از روی دستگاه پاک کنید

برای درخواست‌های مرتبط با حریم خصوصی با ما تماس بگیرید: [ایمیل]

10) حریم خصوصی کودکان

این اپ برای استفاده کودکان (زیر سن قانونی) طراحی نشده است. در صورت استفاده، مسئولیت بر عهده سرپرست است.

11) تغییرات این سیاست

ممکن است این سیاست به‌روزرسانی شود. نسخه جدید در همین بخش داخل اپ در دسترس خواهد بود و تاریخ اجرا تغییر می‌کند.

12) تماس با ما

اگر سوالی درباره این سیاست دارید:

ایمیل: [ایمیل پشتیبانی]

[آدرس/راه ارتباطی اختیاری]
"""
