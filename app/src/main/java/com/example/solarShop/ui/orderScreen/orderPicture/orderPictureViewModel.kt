package com.example.solarShop.ui.orderScreen.orderPicture

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoMetaRepository
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefEntity
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderPictureViewModel @Inject constructor(
    private val repo: OrderPhotoRepository,
    private val repoMeta: OrderPhotoMetaRepository,
    @ApplicationContext private val app: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderIdFlow: StateFlow<Int?> =
        savedStateHandle.getStateFlow("orderId", -1)
            .map { if (it == -1) null else it }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)


    // انتخاب چندتایی
    private val _selected = MutableStateFlow<Set<Int>>(emptySet())
    val selected: StateFlow<Set<Int>> = _selected


    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = orderIdFlow
        .flatMapLatest { oid ->
            if (oid == null) flowOf(UiState()) else
                repo.observe(oid).map { list -> UiState(orderId = oid, photos = list) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())


    private val _uiEvent = MutableSharedFlow<OrderPictureUiEvent>(extraBufferCapacity = 1)
    val uiEvent: SharedFlow<OrderPictureUiEvent> = _uiEvent.asSharedFlow()













    private suspend fun emitMessage(text: String) {
        _uiEvent.emit(OrderPictureUiEvent.Message(text))
    }

    fun togglePinSingle(photoId: Int) = viewModelScope.launch {
        val maxPinned = 2
        val item = uiState.value.photos.firstOrNull { it.id == photoId } ?: return@launch

        if (item.isPinned) {
            // unpin
            repoMeta.unpinOneAndDeleteLocalFile(photoId)
            emitMessage("پین برداشته شد.")
            return@launch
        }else {

            val pinnedCount = uiState.value.photos.count { it.isPinned }
            if (pinnedCount >= maxPinned) {
                emitMessage("حداکثر $maxPinned عکس می‌توان پین کرد. ابتدا یک پین را بردار.")
                return@launch
            }

            runCatching {
                val path = repoMeta.pinOneToLocal(app, item)
                repoMeta.setPinnedMany(listOf(photoId), true, path)
                emitMessage("پین شد.")
            }.onFailure {
                emitMessage("پین کردن ناموفق بود.")
            }
        }
    }

    fun deleteSingle(photoId: Int) = viewModelScope.launch {
        runCatching {
            repoMeta.removeMany(listOf(photoId))
            emitMessage("عکس حذف شد.")
            clearSelection()
        }.onFailure {
            emitMessage("حذف عکس ناموفق بود.")
        }
    }

    private fun onAddFromGallery(uri: Uri) {
        val oid = uiState.value.orderId ?: return
        viewModelScope.launch {
            runCatching { repo.addFromGallery(oid, uri) }
        }
    }

    /** ساخت URI رسانه در MediaStore برای ذخیره عکس دوربین */
    fun createCameraImageUri(): Uri? {
        val oid = uiState.value.orderId ?: return null
        val resolver = app.contentResolver
        val name = "order_${oid}_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // اندروید 10+ (Q): پوشه نمایشی در گالری
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "DCIM/Bambo/Projects/Order_$oid"
                )
            }
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    /** ثبت نتیجهٔ عکس‌گرفتن (اگر موفق بود) */
    private fun onCameraCaptured(uri: Uri, success: Boolean) {
        if (!success) {
            // اگر کاربر کنسل کرد، ورودی MediaStore خالی نماند:
            app.contentResolver.delete(uri, null, null)
            return
        }
        val oid = uiState.value.orderId ?: return
        viewModelScope.launch {
            runCatching { repo.addFromCamera(oid, uri) }
        }
    }

    private fun toggleSelect(id: Int) {
        _selected.update { s -> if (id in s) s - id else s + id }
    }

    private fun clearSelection() = _selected.update { emptySet() }

    // اعمال روی انتخاب‌ها
    private fun removeSelected() = viewModelScope.launch {
        val ids = selected.value.toList()
        if (ids.isNotEmpty()) repoMeta.removeMany(ids)
        clearSelection()
    }

    private fun pinSelected() = viewModelScope.launch {
        val maxPinned = 2
        val selectedIds = selected.value
        if (selectedIds.isEmpty()) return@launch

        val allPhotos = uiState.value.photos
        val currentlyPinnedCount = allPhotos.count { it.isPinned }
        val remaining = (maxPinned - currentlyPinnedCount).coerceAtLeast(0)

        // فقط مواردی که هنوز پین نیستند
        val toPinCandidates = allPhotos
            .filter { it.id in selectedIds && !it.isPinned }

        if (toPinCandidates.isEmpty()) {
            emitMessage("همه‌ی موارد انتخاب‌شده از قبل پین هستند.")
            clearSelection()
            return@launch
        }

        if (remaining == 0) {
            emitMessage("حداکثر $maxPinned عکس می‌توان پین کرد. ابتدا یک پین را بردار.")
            return@launch
        }

        val willPin = toPinCandidates.take(remaining)
        val skipped = toPinCandidates.size - willPin.size

        willPin.forEach { item ->
            runCatching {
                val path = repoMeta.pinOneToLocal(app, item)
                repoMeta.setPinnedMany(listOf(item.id), true, path)
            }.onFailure {
                emitMessage("پین کردن یکی از عکس‌ها ناموفق بود.")
            }
        }

        if (skipped > 0) {
            emitMessage("فقط ${willPin.size} عکس پین شد؛ $skipped مورد به‌خاطر محدودیت پین نشد.")
        } else {
            emitMessage("${willPin.size} عکس پین شد.")
        }

        clearSelection()
    }

    private fun unpinSelected() = viewModelScope.launch {
        val ids = selected.value.toList()
        if (ids.isEmpty()) return@launch

        val pinnedIds = uiState.value.photos
            .filter { it.id in ids && it.isPinned }
            .map { it.id }

        if (pinnedIds.isEmpty()) {
            emitMessage("هیچ‌کدام از موارد انتخاب‌شده پین نیستند.")
            clearSelection()
            return@launch
        }

        repoMeta.unpinManyAndDeleteLocalFiles(pinnedIds)
        emitMessage("${pinnedIds.size} پین برداشته شد.")
        clearSelection()
    }

    private fun onAddFromGalleryMany(uris: List<Uri>) {
        val oid = uiState.value.orderId ?: return
        viewModelScope.launch {
            uris.forEach { uri ->
                runCatching { repo.addFromGallery(oid, uri) }
            }
        }
    }

    fun onEvent(e: OrderPictureEvent) {
        when (e) {
            is OrderPictureEvent.AddFromGalleryMany -> onAddFromGalleryMany(e.uris)
            is OrderPictureEvent.AddFromGalleryOne -> onAddFromGallery(e.uri)
            is OrderPictureEvent.CameraCaptured -> onCameraCaptured(e.uri, e.success)
            is OrderPictureEvent.ToggleSelect -> toggleSelect(e.id)
            is OrderPictureEvent.ClearSelection -> clearSelection()
            is OrderPictureEvent.RemoveSelected -> removeSelected()
            is OrderPictureEvent.PinSelected -> pinSelected()
            is OrderPictureEvent.UnpinSelected -> unpinSelected()
            is OrderPictureEvent.ShowMessage -> viewModelScope.launch { emitMessage(e.text) }
        }
    }


}


data class UiState(
    val orderId: Int? = null,
    val photos: List<OrderPhotoRefEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ====== رویدادهای واحد برای ویو‌مدل ======
sealed interface OrderPictureEvent {
    data class AddFromGalleryMany(val uris: List<Uri>) : OrderPictureEvent
    data class AddFromGalleryOne(val uri: Uri) : OrderPictureEvent
    data class CameraCaptured(val uri: Uri, val success: Boolean) : OrderPictureEvent
    data class ToggleSelect(val id: Int) : OrderPictureEvent
    data class ShowMessage(val text: String) : OrderPictureEvent
    data object ClearSelection : OrderPictureEvent
    data object RemoveSelected : OrderPictureEvent
    data object PinSelected : OrderPictureEvent
    data object UnpinSelected : OrderPictureEvent
}

sealed interface OrderPictureUiEvent {
    data class Message(val text: String) : OrderPictureUiEvent
}