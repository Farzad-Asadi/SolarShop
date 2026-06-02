package com.example.solarShop.feature.backup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.backup.core.SolarBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SolarBackupRestoreViewModel @Inject constructor(
    private val backupManager: SolarBackupManager
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            SolarBackupRestoreUiState()
        )

    val uiState = _uiState.asStateFlow()

    fun setSelectedBackupFileName(
        fileName: String?
    ) {
        _uiState.update {
            it.copy(
                selectedBackupFileName = fileName
            )
        }
    }

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
}