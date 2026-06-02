package com.example.solarShop.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.backup.core.SolarBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SolarBackupTestViewModel @Inject constructor(
    private val backupManager: SolarBackupManager
) : ViewModel() {

    fun createBackup(
        onDone: (File) -> Unit
    ) {
        viewModelScope.launch {
            val file = backupManager.createBackup()
            onDone(file)
        }
    }

    fun restoreBackup(
        file: File,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            backupManager.restoreBackup(file)
            onDone()
        }
    }
}