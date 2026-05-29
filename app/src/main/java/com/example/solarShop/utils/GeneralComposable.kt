package com.example.solarShop.utils


import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.solarShop.R
import com.example.solarShop.data.room.tables.question_answers.question.QuestionWithAnswers
import com.example.solarShop.ui.theme.BambooTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    rawRes: Int = R.raw.loading,
    play: Boolean = true,
    speed: Float = 1f
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(rawRes)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = play,
        speed = speed
    )

    Box(
        modifier = modifier,              // 👈 از modifier ورودی استفاده کن
        contentAlignment = Alignment.Center
    ) {
        if (composition == null) {
            // 👇 fallback تا وقتی لوتی لود نشده هم چیزی ببینی
            CircularProgressIndicator()
        } else {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp),
                renderMode = RenderMode.HARDWARE // روان‌تر روی دستگاه‌های ضعیف
            )
        }
    }
}



@Composable
fun TopBarGeneral(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp   // فقط گوشه‌های پایین
        ),
        color = BambooTheme.sections.topBarContainer,
        contentColor = BambooTheme.sections.topBarContent,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp

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
                onClick = onBack,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "ArrowBack")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))

            Row(content = actions)

        }
    }


}




@Composable
fun CollectSnackbars(
    messages: Flow<String>,
    hostState: SnackbarHostState,
    duration: SnackbarDuration = SnackbarDuration.Short,
) {
    LaunchedEffect(messages, hostState) {
        messages.collectLatest { text ->
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(
                message = text,
                duration = duration
            )
        }
    }
}


class SnackbarController(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun show(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        job?.cancel()
        job = scope.launch {
            // قطع فوری اسنک‌بار فعلی
            hostState.currentSnackbarData?.dismiss()

            val res = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
                duration = duration
            )

            when (res) {
                SnackbarResult.ActionPerformed -> onAction?.invoke()
                SnackbarResult.Dismissed -> onDismiss?.invoke()
            }
        }
    }

    fun dismissCurrent() {
        job?.cancel()
        hostState.currentSnackbarData?.dismiss()
    }
}



//general alert dialog
@Composable
fun ConfirmDialogRtl(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "تأیید",
    dismissText: String = "انصراف",
    showDismissButton: Boolean = true,
    confirmIsDestructive: Boolean = false,
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showDismissButton) {
                            TextButton(onClick = onDismiss) {
                                Text(dismissText)
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        TextButton(
                            onClick = {
                                onConfirm()
                                onDismiss()
                            },
                            colors = if (confirmIsDestructive)
                                ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            else
                                ButtonDefaults.textButtonColors()
                        ) {
                            Text(confirmText)
                        }
                    }
                }
            }
        }
    }
}







/// تکست‌فیلد برای وارد کردن مبلغ (کانن: تومان)
@Composable
fun MyCurrencyField(
    modifier: Modifier = Modifier,
    label: String = "مبلغ",
    value: Long?,                     // همیشه تومان
    onValueChange: (Long?) -> Unit,   // همیشه تومان
    toman: Boolean = true             // true = نمایش تومان، false = نمایش ریال
) {

    // تومان → رشته رقم‌ها در واحد نمایش (تومان/ریال)
    fun canonicalToDisplayDigits(v: Long?, isToman: Boolean): String {
        val base = v ?: return ""
        val display = if (isToman) base else base * 10L   // تومان → ریال
        return display.toString()
    }

    // مقدار اولیه برای فیلد (فقط بار اول)
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val digits = canonicalToDisplayDigits(value, toman)
        val formatted = if (digits.isEmpty()) "" else formatDigitsGrouped(digits)
        mutableStateOf(
            TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length) // 👉 کرسر آخر
            )
        )
    }

    // اگر واحد نمایش عوض شد (تومان ↔ ریال)، از روی value دوباره بساز
    LaunchedEffect(toman) {
        val digits = canonicalToDisplayDigits(value, toman)
        val formatted = if (digits.isEmpty()) "" else formatDigitsGrouped(digits)
        textFieldValue = TextFieldValue(
            text = formatted,
            selection = TextRange(formatted.length)
        )
    }

    val suffix = if (toman) " (تومان)" else " (ریال)"
    val finalLabel = label + suffix

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->

            // فقط رقم‌ها را نگه‌دار (فارسی/لاتین)
            val digits = newValue.text.filter { it.isDigit() }

            if (digits.isEmpty()) {
                textFieldValue = TextFieldValue(
                    text = "",
                    selection = TextRange(0)
                )
                onValueChange(null)
                return@OutlinedTextField
            }

            // فرمت سه‌رقمی
            val formatted = formatDigitsGrouped(digits)

            // 👉 همیشه کرسر را آخر متن بگذار
            textFieldValue = TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length)
            )

            // تبدیل به تومانی
            val displayLong = digits.toLongOrNull()
            val canonToman: Long? = displayLong?.let { v ->
                if (toman) v else v / 10L
            }

            onValueChange(canonToman)
        },
        label = {
            Text(
                text = finalLabel,
                textAlign = TextAlign.Start
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        maxLines = 1,
        modifier = modifier.heightIn(max = 60.dp),
        shape = RoundedCornerShape(8.dp),
    )
}



// (۲،۷) PhoneInput با LTR داخلی و امکان disable هنگام لودینگ
@Composable
fun MyPhoneField(
    national: String,                 // فقط ۱۰ رقم بعد از ۹ (بدون صفر اول)
    onNationalChange: (String) -> Unit,
    onImeDone: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValidChange: (Boolean) -> Unit = {},
    label: String? = null,            // اگر null باشه لیبل نداره، مثل قبل در SignIn
) {
    val scheme = MaterialTheme.colorScheme
    val lengthError = stringResource(R.string.sign_in_error_phone_length)

    // ❗ استیت خطا داخل خود کامپوننت
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    // تبدیل رقم‌های فارسی → لاتین + حذف صفر اول + محدود کردن به ۱۰ رقم
    fun sanitize(raw: String): String {
        // اگر util خودت رو داری:
        val latin = digitsToLatinInline(raw)          // اگر نداری، این خط رو بردار
        val onlyDigits = latin.filter(Char::isDigit)

        val noLeadingZero = if (onlyDigits.startsWith("0")) {
            onlyDigits.drop(1)
        } else {
            onlyDigits
        }

        return noLeadingZero.take(10)
    }

    // ولیدیشن:
    // - خالی؟ → اوکی، بدون خطا
    // - طول ≠ ۱۰ → خطا
    fun validate(cleaned: String): String? {
        if (cleaned.isEmpty()) return null
        if (cleaned.length != 10) {
            return lengthError
        }
        return null
    }

    // هر بار value عوض شد، ولیدیت کن و نتیجه را به بالا هم بگو
    LaunchedEffect(national) {
        val newError = validate(national)
        errorText = newError
        onValidChange(newError == null)
    }



    fun handleChange(raw: String) {
        val cleaned = sanitize(raw)
        onNationalChange(cleaned)
        // با هر تغییر، دوباره چک کن (اگه خالی شد، خطا هم می‌پره)
        errorText = validate(cleaned)
    }

    fun handleImeDone() {
        val err = validate(national)
        errorText = err
        if (err == null) {
            onImeDone()
        }
    }

    // 🔢 اعداد همیشه LTR حتی وقتی کل اپ RTL است
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = national,
            onValueChange = { raw -> handleChange(raw) },
            singleLine = true,
            enabled = enabled,
            isError = errorText != null,
            modifier = modifier.fillMaxWidth(),
            label ={Text(label ?: stringResource(R.string.profile_field_mobile))},
            supportingText = {
                errorText?.let {
                    Text(
                        it,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { handleImeDone() }),
            textStyle = LocalTextStyle.current.copy(
                textDirection = TextDirection.Ltr
            ),
            prefix = {
                Text(
                    text = "+98 |",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            placeholder = {
                val hintColor = scheme.onSurfaceVariant.copy(alpha = 0.5f)
                Text(
                    "912 345 6789",
                    style = MaterialTheme.typography.bodyLarge,
                    color = hintColor
                )
            },
            visualTransformation = iranMobileVisualTransformation(),
        )
    }
}


@Composable
fun MyStringField(
    value: String,
    onValueChange: (String) -> String,
    onImeDone: () -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 38,
    isError: Boolean = false,
    supportText: String? = null,
    placeholder: String? = null,
    label: String? = null,
    enabled: Boolean = true,
) {

    fun sanitize(input: String): String {
        // حذف اینتر، یکی کردن فاصله‌های پشت سر هم، محدود کردن طول
        var out = input
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")

        if (out.length > maxLength) {
            out = out.take(maxLength)
        }
        return out
    }

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val cleaned = sanitize(raw)
            onValueChange(cleaned)
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        singleLine = true,
        label = {
            Text(label ?: stringResource(R.string.profile_field_full_name))
        },
        supportingText = {
            supportText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        placeholder = {
            val hintColor =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            Text(
                text = (placeholder ?: stringResource(R.string.profile_field_full_name_hint)),
                style = MaterialTheme.typography.bodyLarge,
                color = hintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Start,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeDone() }
        ),
    )
}

@Composable
fun MyLandlineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 11,
    minLengthForValidation: Int = 8,
    onValidChange: (Boolean) -> Unit = {},   // هر وقت نتیجه‌ی ولیدیشن عوض شود
    onImeDone: () -> Unit = {}
) {
    // استرینگ‌ها (چون این‌جا @Composable هستیم، مجازه)
    val labelText = stringResource(R.string.profile_field_landline)
    val hintText = stringResource(R.string.profile_field_landline_hint)
    val errorLeadingZero = stringResource(R.string.profile_field_landline_error_leading_zero)
    val errorTooShort = stringResource(R.string.profile_field_landline_error_too_short)

    // استیت خطا داخل خود کامپوننت
    var errorText by remember { mutableStateOf<String?>(null) }

    fun validate(cleaned: String): String? {
        if (cleaned.isBlank()) return null              // خالی = بدون خطا
        if (!cleaned.startsWith("0")) return errorLeadingZero
        if (cleaned.length < minLengthForValidation) return errorTooShort
        return null
    }

    // هر بار value عوض شد، ولیدیت کن و نتیجه را به بالا هم بگو
    LaunchedEffect(value) {
        val newError = validate(value)
        errorText = newError
        onValidChange(newError == null)
    }

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            // فقط رقم + محدودیت طول
            val cleaned = raw.filter(Char::isDigit).take(maxLength)
            onValueChange(cleaned)

            val newError = validate(cleaned)
            errorText = newError
            onValidChange(newError == null)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(labelText) },
        placeholder = {
            val hintColor =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodyLarge,
                color = hintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        isError = errorText != null,
        supportingText = {
            errorText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                val newError = validate(value)
                errorText = newError
                onValidChange(newError == null)
                if (newError == null) {
                    onImeDone()
                }
            }
        )
    )
}

@Composable
fun MyNationalCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onValidChange: (Boolean) -> Unit = {},  // true/false شدن ولیدیشن را گزارش می‌کند
    onImeDone: () -> Unit = {}
) {
    val labelText = stringResource(R.string.profile_field_national_code)
    val hintText = stringResource(R.string.profile_field_national_code_hint)
    val errorLength = stringResource(R.string.profile_field_national_code_error_length)

    var errorText by remember { mutableStateOf<String?>(null) }

    fun validate(cleaned: String): String? {
        if (cleaned.isBlank()) return null          // خالی → بدون خطا
        return if (cleaned.length == 10) null else errorLength
    }

    // هر بار value بیرونی تغییر کند (مثلاً رستور state)، دوباره ولیدیت
    LaunchedEffect(value) {
        val newError = validate(value)
        errorText = newError
        onValidChange(newError == null)
    }

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            // ۱) تبدیل اعداد فارسی/عربی به لاتین (اختیاری؛ اگر نخواستی، این خط را حذف کن)
            val normalized = digitsToLatinInline(raw)

            // ۲) فقط رقم + محدود به ۱۰ رقم
            val cleaned = normalized.filter(Char::isDigit).take(10)

            onValueChange(cleaned)

            val newError = validate(cleaned)
            errorText = newError
            onValidChange(newError == null)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(labelText) },
        placeholder = {
            val hintColor =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodyLarge,
                color = hintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        isError = errorText != null,
        supportingText = {
            errorText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                val newError = validate(value)
                errorText = newError
                onValidChange(newError == null)
                if (newError == null) {
                    onImeDone()
                }
            }
        )
    )
}






@Composable
fun FullscreenImageViewer(
    title: String,
    imageUris: List<Uri>,
    initialPage: Int = 0,
    onClose: () -> Unit
) {
    val safeInitial = initialPage.coerceIn(0, (imageUris.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { imageUris.size.coerceAtLeast(1) }
    )
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.95f)
        ) {
            Column(Modifier.fillMaxSize()) {

                // نوار بالا
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    if (imageUris.size > 1) {
                        Text(
                            "${pagerState.currentPage + 1}/${imageUris.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Pager
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                ) {
                    HorizontalPager(state = pagerState) { page ->
                        val uri = imageUris.getOrNull(page)

                        // --- Zoom/Pan state per page ---
                        var scale by remember(page) { mutableStateOf(1f) }
                        var offset by remember(page) { mutableStateOf(Offset.Zero) }
                        val minScale = 1f
                        val maxScale = 5f

                        // فقط وقتی زوم شده‌ایم ژستِ transform را فعال کن (درگ افقی با Pager درگیر نشود)
                        val zoomPanModifier = if (scale > 1f) {
                            Modifier.pointerInput(page, scale) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val newScale =
                                        (scale * zoom).coerceIn(minScale, maxScale)
                                    val scaleChange = newScale / scale
                                    offset =
                                        (offset + (centroid - offset) * (1 - scaleChange)) + pan
                                    scale = newScale
                                }
                            }
                        } else {
                            Modifier
                        }

                        // دوبل‌تپ برای زوم / ریست
                        val doubleTapModifier = Modifier.pointerInput(page) {
                            detectTapGestures(
                                onDoubleTap = { tap ->
                                    if (scale < 1.5f) {
                                        val target = 3f
                                        val k = target / scale
                                        offset = offset * k + tap * (1 - k)
                                        scale = target
                                    } else {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        }

                        AsyncImage(
                            model = uri,
                            contentDescription = "image $page",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                                .then(zoomPanModifier)
                                .then(doubleTapModifier)
                        )
                    }

                    // دکمه‌های قبلی/بعدی + نقطه‌ها
                    if (imageUris.size > 1) {
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val hasPrev = pagerState.currentPage > 0
                            TextButton(
                                onClick = {
                                    if (hasPrev) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = hasPrev,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "قبلی",
                                    color = Color.White
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(imageUris.size) { i ->
                                    val active = pagerState.currentPage == i
                                    Box(
                                        Modifier
                                            .size(if (active) 10.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) Color.White
                                                else Color.LightGray
                                            )
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            val hasNext = pagerState.currentPage < imageUris.lastIndex
                            TextButton(
                                onClick = {
                                    if (hasNext) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                enabled = hasNext,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "بعدی",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FullscreenImageViewerForCatalog(
    title: String,
    imageUris: List<Uri>,
    imageIds: List<Int>,               // ✅ جدید
    selectedPhotoId: Int?,             // ✅ جدید
    onSelectPhoto: (Int) -> Unit,
    note: String,
    initialPage: Int = 0,
    onClose: () -> Unit
) {
    val hasNotePage = note.isNotBlank()
    val imagesCount = imageUris.size
    val totalPages = imagesCount + if (hasNotePage) 1 else 0

    if (totalPages == 0) {
        onClose()
        return
    }

    val safeInitial = initialPage.coerceIn(0, totalPages - 1)

    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { totalPages }
    )
    val scope = rememberCoroutineScope()


    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.95f)
        ) {
            Column(Modifier.fillMaxSize()) {

                // نوار بالا
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    if (totalPages > 1) {
                        Text(
                            "${pagerState.currentPage + 1}/$totalPages",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Pager
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                ) {
                    HorizontalPager(state = pagerState) { page ->
                        if (page < imagesCount) {
                            // --- اسلایدهای عکس ---
                            val uri = imageUris.getOrNull(page)

                            var scale by remember(page) { mutableStateOf(1f) }
                            var offset by remember(page) { mutableStateOf(Offset.Zero) }
                            val minScale = 1f
                            val maxScale = 5f

                            val zoomPanModifier = if (scale > 1f) {
                                Modifier.pointerInput(page, scale) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val newScale =
                                            (scale * zoom).coerceIn(minScale, maxScale)
                                        val scaleChange = newScale / scale
                                        offset =
                                            (offset + (centroid - offset) * (1 - scaleChange)) + pan
                                        scale = newScale
                                    }
                                }
                            } else {
                                Modifier
                            }

                            val doubleTapModifier = Modifier.pointerInput(page) {
                                detectTapGestures(
                                    onDoubleTap = { tap ->
                                        if (scale < 1.5f) {
                                            val target = 3f
                                            val k = target / scale
                                            offset = offset * k + tap * (1 - k)
                                            scale = target
                                        } else {
                                            scale = 1f
                                            offset = Offset.Zero
                                        }
                                    }
                                )
                            }

                            Box(Modifier.fillMaxSize()) {

                                AsyncImage(
                                    model = uri,
                                    contentDescription = "image $page",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            translationX = offset.x
                                            translationY = offset.y
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        }
                                        .then(zoomPanModifier)
                                        .then(doubleTapModifier)
                                )

                                // ✅ چک‌باکس کنار هر عکس (فقط صفحات عکس، نه Note)
                                val photoId = imageIds.getOrNull(page)
                                if (photoId != null) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(12.dp)
//                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("طرح مورد پسند", style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.width(6.dp))
                                        Checkbox(
                                            checked = (selectedPhotoId == photoId),
                                            onCheckedChange = { checked ->
                                                if (checked) onSelectPhoto(photoId)
                                                else {
                                                    // اگر خواستی "برداشتن انتخاب" هم داشته باشی:
                                                    // onClearSelection()
                                                    // فعلاً طبق سناریوی تو، فقط یکی انتخاب می‌شود.
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                        } else {
                            // --- اسلاید آخر: Note خود پاسخ ---
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    Text(
                                        text = "توضیح پاسخ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    // دکمه‌های قبلی/بعدی و نقطه‌ها مثل قبل، فقط از totalPages استفاده کن
                    if (totalPages > 1) {
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val hasPrev = pagerState.currentPage > 0
                            TextButton(
                                onClick = {
                                    if (hasPrev) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = hasPrev,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("قبلی", color = Color.White)
                            }

                            Spacer(Modifier.width(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(totalPages) { i ->
                                    val active = pagerState.currentPage == i
                                    Box(
                                        Modifier
                                            .size(if (active) 10.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) Color.White
                                                else Color.LightGray
                                            )
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            val hasNext = pagerState.currentPage < totalPages - 1
                            TextButton(
                                onClick = {
                                    if (hasNext) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                enabled = hasNext,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("بعدی", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// دیالوگ زمینه تیره
@Composable
fun DimmedDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dimAlpha: Float = 0.5f,
    usePlatformDefaultWidth: Boolean = false,
    decorFitsSystemWindows: Boolean = false,
    // اگر true باشد کلیک روی بک‌دراپ دیالوگ را می‌بندد
    dismissOnBackdropClick: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = usePlatformDefaultWidth,
            decorFitsSystemWindows = decorFitsSystemWindows
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .then(
                    if (dismissOnBackdropClick) {
                        Modifier.clickable(
                            onClick = onDismiss,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // کلیک‌ها به بک‌دراپ نشت نکنن
            Box(
                modifier = modifier.clickable(
                    onClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
            ) {
                content()
            }
        }
    }
}

//یک Confirm Dialog عمومی (برای حذف‌ها)
@Composable
fun ConfirmDimmedDialog(
    visible: Boolean,
    title: String,
    message: @Composable () -> Unit,
    confirmText: String = "بله، حذف شود",
    dismissText: String = "انصراف",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    DimmedDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)

                message()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(dismissText) }
                    TextButton(onClick = {
                        onConfirm()
                        onDismiss()
                    }) { Text(confirmText) }
                }
            }
        }
    }
}

@Composable
fun NoteEditorDialog(
    visible: Boolean,
    label: String,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    if (!visible) return

    DimmedDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),

            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            var text by rememberSaveable { mutableStateOf(initialText) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("توضیحات خود را اینجا بنویسید...") },
                    minLines = 6,
                    maxLines = 20
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    TextButton(onClick = {
                        onSave(text)
                        onDismiss()
                    }) { Text("ذخیره") }
                }
            }
        }
    }
}







//جهت انتقال نقطه بالای بکس به کاستوم لییر

class QuestionWithAnswersDataModifier(
    val questionWithAnswers: QuestionWithAnswers,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any {
        return questionWithAnswers
    }
}

fun Modifier.questionWithAnswersData(questionWithAnswers: QuestionWithAnswers) =
    this.then(QuestionWithAnswersDataModifier(questionWithAnswers))





