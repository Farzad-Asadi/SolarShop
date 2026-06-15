package com.example.solarShop.feature.backup.viewmodel

import com.example.solarShop.data.backup.core.BackupPreviewInfo

data class SolarBackupRestoreUiState(

    val isCreatingBackup: Boolean = false,
    val isRestoringBackup: Boolean = false,
    val isSyncingWithServer: Boolean = false,

    val backupMessage: String? = null,
    val restoreMessage: String? = null,
    val syncMessage: String? = null,

    val selectedBackupFileName: String? = null,
    val previewInfo: BackupPreviewInfo? = null,
)