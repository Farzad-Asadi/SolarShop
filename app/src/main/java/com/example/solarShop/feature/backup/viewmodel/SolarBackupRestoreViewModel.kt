package com.example.solarShop.feature.backup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.backup.core.SolarBackupManager
import com.example.solarShop.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SolarBackupRestoreViewModel @Inject constructor(
    private val backupManager: SolarBackupManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            SolarBackupRestoreUiState()
        )

    val uiState = _uiState.asStateFlow()


    fun createBackup(
        onSuccess: (File) -> Unit
    ) {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isCreatingBackup = true,
                    backupMessage = null
                )
            }

            runCatching {

                backupManager.createBackup()

            }.onSuccess { file ->

                _uiState.update {
                    it.copy(
                        isCreatingBackup = false,
                        backupMessage = "بکاپ با موفقیت ساخته شد."
                    )
                }

                onSuccess(file)

            }.onFailure { error ->

                _uiState.update {
                    it.copy(
                        isCreatingBackup = false,
                        backupMessage = error.message
                    )
                }
            }
        }
    }

    fun restoreBackup(
        file: File,
        onSuccess: () -> Unit = {}
    ) {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isRestoringBackup = true,
                    restoreMessage = null
                )
            }

            runCatching {

                backupManager.restoreBackup(file)

            }.onSuccess {

                _uiState.update {
                    it.copy(
                        isRestoringBackup = false,
                        restoreMessage = "بازیابی با موفقیت انجام شد."
                    )
                }

                onSuccess()

            }.onFailure { error ->

                _uiState.update {
                    it.copy(
                        isRestoringBackup = false,
                        restoreMessage = error.message
                    )
                }
            }
        }
    }

    fun previewBackup(
        file: File,
        fileName: String?
    ) {
        viewModelScope.launch {
            runCatching {
                backupManager.previewBackup(file)
            }.onSuccess { preview ->

                _uiState.update {
                    it.copy(
                        selectedBackupFileName = fileName ?: "فایل انتخاب شده",
                        previewInfo = preview,
                        restoreMessage = null
                    )
                }

            }.onFailure { error ->

                _uiState.update {
                    it.copy(
                        selectedBackupFileName = fileName,
                        previewInfo = null,
                        restoreMessage = "خواندن فایل ناموفق: ${error.message}"
                    )
                }
            }
        }
    }

    /**
     * فقط مشتریان، سفارش‌ها و اسناد فروش را
     * از دستگاه مرجع به سرور منتقل می‌کند.
     */
    fun fullUploadBusinessDataToServer() {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isSyncingWithServer = true,
                    syncMessage = null
                )
            }

            runCatching {
                syncManager
                    .fullUploadBusinessDataToServer()

            }.onSuccess { success ->

                _uiState.update {
                    it.copy(
                        isSyncingWithServer = false,

                        syncMessage =
                        if (success) {
                            "مشتریان، سفارش‌ها و فاکتورها با موفقیت به سرور منتقل شدند."
                        } else {
                            "انتقال اطلاعات فروش کامل نشد. لاگ Sync را بررسی کن."
                        }
                    )
                }

            }.onFailure { error ->

                _uiState.update {
                    it.copy(
                        isSyncingWithServer = false,

                        syncMessage =
                        "انتقال اطلاعات فروش ناموفق بود: " +
                                (
                                        error.message
                                            ?: "خطای نامشخص"
                                        )
                    )
                }
            }
        }
    }

    fun fullUploadAllToServer() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isSyncingWithServer = true,
                    syncMessage = null
                )
            }

            runCatching {
                syncManager.fullUploadAllToServer()
            }.onSuccess { success ->

                _uiState.update {
                    it.copy(
                        isSyncingWithServer = false,
                        syncMessage = if (success) {
                            "آپلود کامل اطلاعات به سرور انجام شد."
                        } else {
                            "آپلود کامل انجام نشد. اتصال، توکن یا پاسخ سرور را بررسی کن."
                        }
                    )
                }

            }.onFailure { error ->

                _uiState.update {
                    it.copy(
                        isSyncingWithServer = false,
                        syncMessage = "آپلود کامل ناموفق بود: ${error.message ?: "خطای نامشخص"}"
                    )
                }
            }
        }
    }

    fun pullAllFromServer() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isSyncingWithServer = true,
                    syncMessage = null
                )
            }

            runCatching {
                syncManager.syncCategoriesOnce()
            }.onSuccess { success ->

                _uiState.update {
                    it.copy(
                        isSyncingWithServer = false,
                        syncMessage = if (success) {
                            "دریافت اطلاعات از سرور انجام شد."
                        } else {
                            "دریافت اطلاعات انجام نشد. اتصال یا پاسخ سرور را بررسی کن."
                        }
                    )
                }

            }.onFailure { error ->

                _uiState.update {
                    it.copy(
                        isSyncingWithServer = false,
                        syncMessage = "دریافت اطلاعات ناموفق بود: ${error.message ?: "خطای نامشخص"}"
                    )
                }
            }
        }
    }
}