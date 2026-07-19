package com.example.solarShop.feature.backup.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.solarShop.feature.backup.viewmodel.SolarBackupRestoreViewModel
import com.example.solarShop.utils.formatPersianDateTime
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarBackupRestoreScreen(
    onClose: () -> Unit,
    viewModel: SolarBackupRestoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showRestoreConfirm by remember { mutableStateOf(false) }

    var pickedBackupTempFile by remember { mutableStateOf<File?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        viewModel.createBackup { tempFile ->
            context.contentResolver.openOutputStream(uri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()
        }
    }

    val pickBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        val fileName = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }

        val tempFile = File(context.cacheDir, "solar_restore_${System.currentTimeMillis()}.zip")

        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        pickedBackupTempFile = tempFile
        viewModel.previewBackup(
            file = tempFile,
            fileName = fileName
        )
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بکاپ و بازیابی SolarShop") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("گرفتن بکاپ", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = {
                            createBackupLauncher.launch(
                                "SolarShop_Backup_${System.currentTimeMillis()}.zip"
                            )
                        },
                        enabled = !uiState.isCreatingBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isCreatingBackup)
                                "در حال ساخت بکاپ..."
                            else
                                "ساخت فایل بکاپ"
                        )
                    }

                    uiState.backupMessage?.let {
                        Text(it)
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("بازیابی بکاپ", style = MaterialTheme.typography.titleMedium)


                    Button(
                        onClick = {
                            pickBackupLauncher.launch(arrayOf("application/zip"))
                        },
                        enabled = !uiState.isRestoringBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("انتخاب فایل بکاپ")
                    }

                    uiState.selectedBackupFileName?.let {
                        Text("فایل انتخاب‌شده: $it")
                    }

                    uiState.previewInfo?.let { preview ->

                        HorizontalDivider()

                        Text(
                            text = "اطلاعات بکاپ",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("نسخه بکاپ: ${preview.version}")

                        Text(
                            text = "تاریخ ساخت: ${
                                formatPersianDateTime(preview.createdAt)
                            }"
                        )

                        Text(
                            text = "ماژول‌ها: ${
                                preview.modules.joinToString(", ")
                            }"
                        )

                        Text("دسته‌بندی‌ها: ${preview.categoryCount}")
                        Text("برندها: ${preview.brandCount}")
                        Text("کالاها: ${preview.productCount}")
                        Text("عکس‌های کالا: ${preview.imageCount}")
                    }

                    Button(
                        onClick = {
                            showRestoreConfirm = true
                        },
                        enabled = pickedBackupTempFile != null && !uiState.isRestoringBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isRestoringBackup)
                                "در حال بازیابی..."
                            else
                                "شروع بازیابی"
                        )
                    }

                    uiState.restoreMessage?.let {
                        Text(it)
                    }
                }
            }


            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "سینک دستی با سرور",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "این بخش برای کارهای مدیریتی است. در استفاده عادی، سینک باید به‌صورت خودکار انجام شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text =
                        "برای انتقال اولیه به سرور آنلاین، فقط این دکمه را روی یک دستگاه مرجع اجرا کن.",
                        style =
                        MaterialTheme.typography.bodySmall,
                        color =
                        MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            viewModel
                                .fullUploadBusinessDataToServer()
                        },

                        enabled =
                        !uiState.isSyncingWithServer,

                        modifier =
                        Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isSyncingWithServer) {
                                "در حال انتقال اطلاعات فروش..."
                            } else {
                                "انتقال مشتری، سفارش و فاکتور به سرور"
                            }
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.fullUploadAllToServer()
                        },
                        enabled = !uiState.isSyncingWithServer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isSyncingWithServer) {
                                "در حال آپلود کامل..."
                            } else {
                                "آپلود کامل اطلاعات به سرور"
                            }
                        )
                    }

                    uiState.syncMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.pullAllFromServer()
                        },
                        enabled = !uiState.isSyncingWithServer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isSyncingWithServer) {
                                "در حال ارتباط با سرور..."
                            } else {
                                "دریافت اطلاعات از سرور"
                            }
                        )
                    }
                }
            }

        }

    }
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("تأیید بازیابی") },
            text = {
                Text("بازیابی بکاپ ممکن است داده‌های فعلی را جایگزین کند. ادامه می‌دهی؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        pickedBackupTempFile?.let { file ->
                            viewModel.restoreBackup(file)
                        }
                    }
                ) {
                    Text("بله، بازیابی کن")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreConfirm = false }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}