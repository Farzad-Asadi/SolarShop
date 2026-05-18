package com.example.solarShop.ui.backUpRestore

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.solarShop.data.backupRestore.v2.BackupCategory
import com.example.solarShop.data.backupRestore.v2.ConflictPolicy
import com.example.solarShop.data.backupRestore.v2.UserKeyMapping
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.bambooAngledBackground
import com.example.solarShop.utils.titleFa

@Composable
fun BackUpRestoreScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    vm: BackUpRestoreViewModel = hiltViewModel()
) {
    var tab by remember { mutableIntStateOf(0) } // 0=Backup, 1=Restore

    val context = LocalContext.current

    val backup by vm.backup.collectAsState()
    val restore by vm.restore.collectAsState()


    // لانچر ساخت فایل ZIP
    val createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            vm.exportToUri(uri, context.contentResolver)
        }
    )

    // ⬇️ نگهداری آخرین فایل انتخاب‌شده برای ریستور
    var lastPickedUri by remember { mutableStateOf<Uri?>(null) }

    // لانچر انتخاب فایل ZIP
    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            // نگه‌داشتن دسترسی بلندمدت (در صورت نیاز)
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            vm.previewFromUri(uri)
            lastPickedUri = uri
        }
    )

    Scaffold(
        topBar = {
            BackUpRestoreTopBar(
                topBarTitle = "پشتیبان\u200Cگیری و بازیابی",
                onClickBack = onClose,
            )
        },
        modifier = modifier
    ) { innerPadding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .bambooAngledBackground(),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("بک‌آپ", textAlign = TextAlign.Center) })
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("ریستور", textAlign = TextAlign.Center) })
            }
            Spacer(Modifier.height(12.dp))

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally


                ) {
                when (tab) {


                    //بک‌آپ
                    0 -> BackupTab(backup, onToggle = vm::toggleCategory) {
                        // روی ساخت فایل کلیک شد ⇒ SAF
                        val name = vm.runCatching { }.getOrNull()
                        createLauncher.launch("bambo_backup.zip")
                    }

                    //ریستور
                    else -> RestoreTab(
                        state = restore,
                        onPick = { pickLauncher.launch(arrayOf("application/zip")) },
                        onToggle = vm::setSelectedForRestore,
                        onPolicy = vm::setConflictPolicy,
                        onMapping = vm::setMapping,
                        onRestore = { lastPickedUri?.let(vm::restoreFromLastPicked) }
                    )
                }
            }
        }

    }


}


@Composable
private fun BackupTab(
    state: BackupUiState,
    onToggle: (BackupCategory) -> Unit,
    onExport: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {

                // تیتر سوالی بالا
                Text(
                    text = "کدام بخش‌ها را می‌خواهی پشتیبان‌گیری کنی؟",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // لیست تیک‌ها
                state.categories.forEach { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = c.checked,
                            onCheckedChange = { onToggle(c.id) }
                        )
                        Text(
                            c.title,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // دکمه اصلی بک‌آپ
                Button(
                    onClick = onExport,
                    enabled = !state.working,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.working) "در حال ساخت…" else "ساخت فایل بک‌آپ"
                    )
                }

                state.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
private fun RestoreTab(
    state: RestoreUiState,
    onPick: () -> Unit,
    onToggle: (BackupCategory, Boolean) -> Unit,
    onPolicy: (ConflictPolicy) -> Unit,
    onMapping: (UserKeyMapping) -> Unit,
    onRestore: () -> Unit
) {
    val scrollState = rememberScrollState()

    // وقتی گزارش جدید آمد، تا پایین لیست اسکرول کن
    LaunchedEffect(state.report.size) {
        if (state.report.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ───── کارت ۱: انتخاب فایل و دسته‌ها ─────
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "۱. انتخاب فایل بک‌آپ",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onPick,
                                enabled = !state.working
                            ) { Text("انتخاب فایل") }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = state.pickedName ?: "هنوز فایلی انتخاب نشده",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }

                        state.manifest?.let { m ->
                            Divider(Modifier.padding(vertical = 8.dp))
                            Text(
                                "این فایل شامل این بخش‌هاست:",
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            val orderedCats = defaultUiCategories()
                                .map { it.id }
                                .filter { it in m.categories }    // فقط چیزهایی که در فایل هستند
                            orderedCats.forEach { cat ->
                                val checked = state.selected.contains(cat)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { onToggle(cat, !checked) }
                                    )
                                    Text(
                                        text = cat.titleFa(),          // ✅ حالا مثل تب بک‌آپ فارسی
                                        modifier = Modifier.weight(1f)
                                    )

                                }
                            }
                        }
                    }
                }

                // ───── کارت ۲: سیاست کانفلیکت (با انتخاب واضح) ─────
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "۲. اگر این داده‌ها قبلاً وجود داشته باشند چه کار کنیم؟",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "نوع برخورد با رکوردهای تکراری:",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        // به‌جای Chip، انتخاب تک‌گزینه‌ای با RadioButton
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = state.conflict == ConflictPolicy.Overwrite,
                                    onClick = { onPolicy(ConflictPolicy.Overwrite) }
                                )
                                Text(
                                    "جایگزین کن (Overwrite)",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = state.conflict == ConflictPolicy.Skip,
                                    onClick = { onPolicy(ConflictPolicy.Skip) }
                                )
                                Text(
                                    "رد شو و نگه دار قبلی‌ها (Skip)",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = state.conflict == ConflictPolicy.Merge,
                                    onClick = { onPolicy(ConflictPolicy.Merge) }
                                )
                                Text(
                                    "ترکیب کن؛ فقط جاهای خالی را پر کن (Merge)",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ───── کارت ۳: نقشه userKey (با انتخاب واضح) ─────
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "۳. نسبت‌دادن داده‌ها به کاربر فعلی",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "اگر فایل برای کاربر دیگری ساخته شده باشد:",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = state.mapping == UserKeyMapping.RequireExactMatch,
                                    onClick = { onMapping(UserKeyMapping.RequireExactMatch) }
                                )
                                Text(
                                    "فقط اگر userKey همان باشد (ایمن‌ترین)",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = state.mapping == UserKeyMapping.RemapToCurrent,
                                    onClick = { onMapping(UserKeyMapping.RemapToCurrent) }
                                )
                                Text(
                                    "همه داده‌ها را به کاربر فعلی نسبت بده",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = onRestore,
                            enabled = !state.working && state.manifest != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("شروع ریستور")
                        }
                    }
                }

                // خطا و گزارش
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.report.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "نتیجهٔ ریستور برای هر بخش:",
                        textAlign = TextAlign.Right,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    state.report.forEach { r ->
                        val sectionTitle = when (r.category) {
                            BackupCategory.QNA -> "سؤال و جواب"
                            BackupCategory.CONTRACTS -> "قالب‌های قرارداد"
                            BackupCategory.CUSTOMERS_ACTIVE -> "مشتریان فعال"
                            BackupCategory.CUSTOMERS_ARCHIVED -> "مشتریان آرشیوی"
                        }

                        Text(
                            text = "• $sectionTitle:",
                            textAlign = TextAlign.Right,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "    - ${r.inserted} مورد جدید اضافه شد",
                            textAlign = TextAlign.Right,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "    - ${r.updated} مورد قدیمی به‌روزرسانی شد",
                            textAlign = TextAlign.Right,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "    - ${r.skipped} مورد بدون تغییر رد شد (Skip)",
                            textAlign = TextAlign.Right,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))
                    }
                }

            }
        }
    }
}



/* ----------------------------------------------
* ۴) LocalContentResolver — کمک کوچک برای دسترسی در Compose
* ---------------------------------------------- */

@Composable
private fun BackUpRestoreTopBar(
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
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "Catalog Q&A")
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
