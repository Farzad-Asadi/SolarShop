package com.example.solarShop.ui.backUpRestore

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.backupRestore.v2.BackupCategory
import com.example.solarShop.data.backupRestore.v2.BackupContext
import com.example.solarShop.data.backupRestore.v2.BackupManagerV2
import com.example.solarShop.data.backupRestore.v2.BackupPlan
import com.example.solarShop.data.backupRestore.v2.ConflictPolicy
import com.example.solarShop.data.backupRestore.v2.Manifest
import com.example.solarShop.data.backupRestore.v2.RestoreContext
import com.example.solarShop.data.backupRestore.v2.RestoreManagerV2
import com.example.solarShop.data.backupRestore.v2.RestoreOptions
import com.example.solarShop.data.backupRestore.v2.RestoreReport
import com.example.solarShop.data.backupRestore.v2.UserKeyMapping
import com.example.solarShop.utils.titleFa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class BackUpRestoreViewModel @Inject constructor(
    app: Application,
    private val backupMgr: BackupManagerV2,
    private val restoreMgr: RestoreManagerV2,
    private val bCtx: BackupContext,
    private val rCtx: RestoreContext
) : AndroidViewModel(app) {

    private val _backup = MutableStateFlow(BackupUiState())
    val backup: StateFlow<BackupUiState> = _backup.asStateFlow()


    private val _restore = MutableStateFlow(RestoreUiState())
    val restore: StateFlow<RestoreUiState> = _restore.asStateFlow()


    fun toggleCategory(cat: BackupCategory) {
        val cur = _backup.value
        val next = cur.categories.map { if (it.id == cat) it.copy(checked = !it.checked) else it }
        _backup.value = cur.copy(categories = next)
    }


    /** ساخت نام فایل پیش‌فرض دوستانه */
    private fun suggestFileName(): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        return "bambo_backup_$now.zip"
    }


    /** فراخوانی از UI بعد از گرفتن Uri خروجی از SAF */
    fun exportToUri(outUri: Uri, cr: ContentResolver) = viewModelScope.launch(Dispatchers.IO) {
        val selected = _backup.value.categories.filter { it.checked }.map { it.id }.toSet()
        if (selected.isEmpty()) {
            _backup.update { it.copy(message = "هیچ دسته‌ای انتخاب نشده است") }
            return@launch
        }
        _backup.update { it.copy(working = true, message = null) }
        try {
// 1) ساخت فایل موقت با Orchestrator
            val temp = File(getApplication<Application>().cacheDir, "out_${System.currentTimeMillis()}.zip")
            backupMgr.export(BackupPlan(selected), bCtx, temp)


// 2) کپی به SAF Uri (محل انتخاب‌شده توسط کاربر)
            cr.openOutputStream(outUri)?.use { outs -> temp.inputStream().use { ins -> ins.copyTo(outs) } }
            temp.delete()


            _backup.update { it.copy(working = false, message = "بک‌آپ ساخته شد ✅") }
        } catch (t: Throwable) {
            _backup.update { it.copy(working = false, message = "خطا در بک‌آپ: ${t.message}") }
        }
    }

    /** پیش‌نمایش مانیفست از فایل انتخاب‌شده */
    fun previewFromUri(inUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _restore.update { it.copy(working = true, error = null, report = emptyList()) }
        try {
            val temp = copyUriToTemp(inUri, "in_${System.currentTimeMillis()}.zip")
            val mani = restoreMgr.previewManifest(temp, rCtx)

            val cr = getApplication<Application>().contentResolver
            val displayName = cr.query(inUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }

            _restore.update {
                it.copy(
                    working = false,
                    pickedName = displayName ?: "فایل انتخاب شده",
                    manifest = mani,
                    selected = mani.categories.toSet()
                )
            }
        } catch (t: Throwable) {
            _restore.update { it.copy(working = false, error = "خواندن فایل ناموفق: ${t.message}") }
        }
    }


    fun setSelectedForRestore(cat: BackupCategory, checked: Boolean) {
        val cur = _restore.value
        val next = if (checked) cur.selected + cat else cur.selected - cat
        _restore.value = cur.copy(selected = next)
    }

    fun setConflictPolicy(p: ConflictPolicy) { _restore.update { it.copy(conflict = p) } }
    fun setMapping(m: UserKeyMapping) { _restore.update { it.copy(mapping = m) } }


    /** اجرای ریستور تفکیکی */
    fun restoreFromLastPicked(inUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val cur = _restore.value
        val sel = cur.selected
        if (sel.isEmpty()) {
            _restore.update { it.copy(error = "هیچ دسته‌ای انتخاب نشده است") }
            return@launch
        }
        _restore.update { it.copy(working = true, error = null, report = emptyList()) }
        try {
            val temp = copyUriToTemp(inUri, "in_${System.currentTimeMillis()}.zip")
            val reports = restoreMgr.restoreSelected(temp, RestoreOptions(sel, cur.conflict, cur.mapping), rCtx)
            _restore.update { it.copy(working = false, report = reports) }
        } catch (t: Throwable) {
            _restore.update { it.copy(working = false, error = "خطا در ریستور: ${t.message}") }
        }
    }

    private fun copyUriToTemp(uri: Uri, name: String): File {
        val f = File(getApplication<Application>().cacheDir, name)
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { ins ->
            f.outputStream().use { outs -> ins.copyTo(outs) }
        } ?: error("Cannot open uri")
        return f
    }




}


data class UiCategory(
    val id: BackupCategory,
    val title: String,
    val checked: Boolean
)

/* Backup tab state */
data class BackupUiState(
    val categories: List<UiCategory> = defaultUiCategories(),
    val working: Boolean = false,
    val message: String? = null
)


/* Restore tab state */
data class RestoreUiState(
    val pickedName: String? = null,
    val manifest: Manifest? = null,
    val selected: Set<BackupCategory> = emptySet(),
    val conflict: ConflictPolicy = ConflictPolicy.Overwrite,
    val mapping: UserKeyMapping = UserKeyMapping.RequireExactMatch,
    val working: Boolean = false,
    val report: List<RestoreReport> = emptyList(),
    val error: String? = null
)


fun defaultUiCategories() = listOf(
    UiCategory(BackupCategory.QNA, BackupCategory.QNA.titleFa(), true),
    UiCategory(BackupCategory.CONTRACTS, BackupCategory.CONTRACTS.titleFa(), true),
    UiCategory(BackupCategory.CUSTOMERS_ACTIVE, BackupCategory.CUSTOMERS_ACTIVE.titleFa(), false),
    UiCategory(BackupCategory.CUSTOMERS_ARCHIVED, BackupCategory.CUSTOMERS_ARCHIVED.titleFa(), false)
)






