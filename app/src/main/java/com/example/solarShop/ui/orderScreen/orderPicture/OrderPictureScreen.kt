package com.example.solarShop.ui.orderScreen.orderPicture

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefEntity
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.CollectSnackbars
import com.example.solarShop.utils.ConfirmDimmedDialog
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.io.File


// ====== صفحهٔ اصلی ======
@Composable
fun OrderPictureScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    vm: OrderPictureViewModel = hiltViewModel()
) {

    val state by vm.uiState.collectAsState()
    val selected by vm.selected.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    // کنترل ویوِر فول‌اسکرین
    var showViewer by remember { mutableStateOf(false) }
    var startIndex by remember { mutableIntStateOf(0) }


    if (!showViewer) {
        val snackbarMessages = remember(vm.uiEvent) {
            vm.uiEvent.mapNotNull { (it as? OrderPictureUiEvent.Message)?.text }
        }

        CollectSnackbars(
            messages = snackbarMessages,
            hostState = snackbarHostState
        )
    }


    val pinned = state.photos.filter { it.isPinned }.take(2)
    val others = state.photos.filter { !it.isPinned }


    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }


    // لانچر چند‌انتخابهٔ گالری
    val galleryMultiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        vm.onEvent(OrderPictureEvent.AddFromGalleryMany(uris.take(30)))
    }

    // دوربین
    val camera = rememberCameraCaptureLauncher(
        requiredPermissions = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } else emptyArray()
        },
        createOutputUri = { vm.createCameraImageUri() },
        onResult = { uri, success -> vm.onEvent(OrderPictureEvent.CameraCaptured(uri, success)) },
        onMessage = { vm.onEvent(OrderPictureEvent.ShowMessage(it)) }
    )



    val ratios = remember { mutableStateMapOf<Int, Float>() }

    // برای ثابت ماندن جای پین‌ها (چپ/راست)
    var pinnedLeftId by remember { mutableStateOf<Int?>(null) }
    var pinnedRightId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pinned.map { it.id }) {
        val current = pinned.map { it.id }.toSet()

        // 1) هر اسلاتی که دیگر در pinned نیست را خالی کن
        if (pinnedLeftId != null && pinnedLeftId !in current) pinnedLeftId = null
        if (pinnedRightId != null && pinnedRightId !in current) pinnedRightId = null

        // 2) اگر هر دو اسلات خالی‌اند، تا 2تا را پر کن (پیش‌فرض: left بعد right)
        if (pinnedLeftId == null && pinnedRightId == null) {
            val list = pinned.map { it.id }.take(2)
            pinnedLeftId = list.getOrNull(0)
            pinnedRightId = list.getOrNull(1)
            return@LaunchedEffect
        }

        // 3) آیتم‌های جدیدی که هنوز در هیچ اسلاتی نیستند
        val used = setOfNotNull(pinnedLeftId, pinnedRightId)
        val newcomers = pinned.map { it.id }.filter { it !in used }

        // 4) newcomers را فقط در اسلات‌های خالی قرار بده (بدون جابه‌جا کردن قبلی‌ها)
        var i = 0
        if (pinnedLeftId == null && i < newcomers.size) {
            pinnedLeftId = newcomers[i]; i++
        }
        if (pinnedRightId == null && i < newcomers.size) {
            pinnedRightId = newcomers[i]; i++
        }

        // 5) اگر به هر دلیل هر دو اسلات پرند ولی یکی از pinnedها با اسلات‌ها نمی‌خواند،
        // (مثلاً reorder عجیب)، با حداقل جابه‌جایی درستش کن:
        val finalUsed = setOfNotNull(pinnedLeftId, pinnedRightId)
        val missing = pinned.map { it.id }.filter { it !in finalUsed }
        if (missing.isNotEmpty()) {
            // یکی را جایگزین اسلات خالی یا کم‌اهمیت‌تر کن؛
            // چون max=2 است، این حالت نادر است.
            if (pinnedLeftId == null) pinnedLeftId = missing.first()
            else if (pinnedRightId == null) pinnedRightId = missing.first()
        }
    }


    val pinnedLeft = state.photos.firstOrNull { it.id == pinnedLeftId }
    val pinnedRight = state.photos.firstOrNull { it.id == pinnedRightId }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            OrderPhotosTopBar(
                selectedCount = selected.size,
                onBack = onClose,
                onClearSelection = { vm.onEvent(OrderPictureEvent.ClearSelection) },
                onAddFromGallery = {
                    galleryMultiPicker.launch(arrayOf("image/*"))

                },
                onTakePhoto = {
                    camera.launch()
                },
                onDeleteSelected = { showDeleteSelectedConfirm = true },
                onPinSelected = { vm.onEvent(OrderPictureEvent.PinSelected) },
                onUnpinSelected = { vm.onEvent(OrderPictureEvent.UnpinSelected) }
            )
        },
        modifier = modifier
    ) { pad ->
        if (state.orderId == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad), contentAlignment = Alignment.Center
            ) {
                Text("شناسه سفارش نامعتبر است")
            }
            return@Scaffold
        }

        if (state.photos.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad), contentAlignment = Alignment.Center
            ) {
                Text(
                    "هنوز عکسی اضافه نشده.\n\n" +
                            "عکس‌ها رو می‌تونی از گالری یا دوربین اضافه کنی.\n" +
                            "حداکثر ۲ عکس قابل پین شدن هستن و فقط داخل برنامه ذخیره می‌شن.\n" +
                            "بقیه عکس‌ها در گالری گوشی باقی می‌مونن.",
                    textAlign = TextAlign.Center
                )

            }
        } else {


            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(120.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {

                // --- Header: pinned row (Full span) ---
                if (pinned.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        PinnedRowFixedSlots(
                            left = pinnedLeft,
                            right = pinnedRight,
                            selectedIds = selected,
                            onClick = { item ->
                                if (selected.isNotEmpty()) vm.onEvent(
                                    OrderPictureEvent.ToggleSelect(
                                        item.id
                                    )
                                )
                                else {
                                    startIndex = state.photos.indexOfFirst { it.id == item.id }
                                        .coerceAtLeast(0)
                                    showViewer = true
                                }
                            },
                            onLongClick = { item -> vm.onEvent(OrderPictureEvent.ToggleSelect(item.id)) }
                        )
                    }

                    // فاصله کوچک بین پین‌ها و گالری
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(4.dp))
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                }


                // --- Gallery items (staggered) ---
                items(others, key = { it.id }) { item ->
                    GalleryPhotoCard(
                        e = item,
                        isSelected = item.id in selected,
                        selectionMode = selected.isNotEmpty(),
                        aspectRatio = ratios[item.id],
                        onRatioKnown = { r -> ratios[item.id] = r },
                        onClick = {
                            if (selected.isNotEmpty()) vm.onEvent(
                                OrderPictureEvent.ToggleSelect(
                                    item.id
                                )
                            )
                            else {
                                startIndex =
                                    state.photos.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                showViewer = true
                            }
                        },
                        onLongClick = { vm.onEvent(OrderPictureEvent.ToggleSelect(item.id)) }
                    )
                }
            }


        }
    }

    // ویوِر فول‌اسکرین با نوار اکشن پایین
    if (showViewer) {
        val imageUris: List<Uri> = state.photos.map { e ->
            val path = e.localCopyPath
            if (e.isPinned && !path.isNullOrBlank() && File(path).exists()) {
                File(path).toUri()
            } else {
                e.contentUri.toUri()
            }
        }
        val photoIds = state.photos.map { it.id }


        FullscreenOrderPhotosViewer(
            title = "نمایش عکس‌ها",
            photoIds = photoIds,
            imageUris = imageUris,
            initialPage = startIndex,
            isPinned = { id -> state.photos.firstOrNull { it.id == id }?.isPinned == true },
            onTogglePin = { id -> vm.togglePinSingle(id) },
            onDelete = { id -> vm.deleteSingle(id) },
            uiEvents = vm.uiEvent,
            onClose = { showViewer = false }
        )
    }
    ConfirmDimmedDialog(
        visible = showDeleteSelectedConfirm,
        title = "حذف عکس‌ها",
        message = { Text("عکس‌های انتخاب‌شده از این لیست پاک شوند؟") },
        onConfirm = { vm.onEvent(OrderPictureEvent.RemoveSelected) },
        onDismiss = { showDeleteSelectedConfirm = false }
    )


}

@Composable
private fun OrderPhotosTopBar(
    titleNormal: String = "عکس‌های پروژه",
    selectedCount: Int,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onAddFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onDeleteSelected: () -> Unit,
    onPinSelected: () -> Unit,
    onUnpinSelected: () -> Unit,
) {
    val inSelection = selectedCount > 0

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp
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
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Left icon: back OR clear selection
            IconButton(
                modifier = Modifier.padding(end = 12.dp),
                onClick = { if (inSelection) onClearSelection() else onBack() }
            ) {
                Icon(
                    imageVector = if (inSelection) Icons.Default.Close
                    else Icons.AutoMirrored.Outlined.ArrowBackIos,
                    contentDescription = if (inSelection) "ClearSelection" else "Back"
                )
            }

            Text(
                text = if (inSelection) "انتخاب شده $selectedCount" else titleNormal,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.weight(1f))

            // Right actions
            if (inSelection) {
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف انتخاب‌شده‌ها")
                }
                IconButton(onClick = onPinSelected) {
                    Icon(Icons.Filled.PushPin, contentDescription = "پین انتخاب‌شده‌ها")
                }
                IconButton(onClick = onUnpinSelected) {
                    Icon(Icons.Outlined.PushPin, contentDescription = "برداشتن پین")
                }
            } else {
                IconButton(onClick = onAddFromGallery) {
                    Icon(Icons.Default.Collections, contentDescription = "افزودن چندعکس از گالری")
                }
                IconButton(onClick = onTakePhoto) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "عکس با دوربین")
                }
            }
        }
    }
}


@Composable
private fun PhotoCardSelectable(
    e: OrderPhotoRefEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoModel(e))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            if (e.isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PushPin, contentDescription = null,
                        modifier = Modifier
                            .size(32.dp),
                        tint = Color.White
                    )
                    Icon(
                        Icons.Default.PushPin, contentDescription = null,
                        modifier = Modifier
                            .size(24.dp),
                        tint = Color.Black
                    )


                }
            }

            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // اختیاری: اگر می‌خوای تیک هم داشته باشه
                    if (isSelected) Icon(
                        Icons.Default.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

        }
    }
}

// ====== ویوِر فول‌اسکرین با نوار اکشن پایین ======

@Composable
fun FullscreenOrderPhotosViewer(
    title: String,
    photoIds: List<Int>,
    imageUris: List<Uri>,
    initialPage: Int = 0,
    isPinned: (Int) -> Boolean,
    onTogglePin: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    uiEvents: kotlinx.coroutines.flow.SharedFlow<OrderPictureUiEvent>,
    onClose: () -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarMessages = remember(uiEvents) {
        uiEvents.mapNotNull { ev ->
            (ev as? OrderPictureUiEvent.Message)?.text
        }
    }

    CollectSnackbars(
        messages = snackbarMessages,
        hostState = snackbarHostState
    )

    require(photoIds.size == imageUris.size) { "photoIds and imageUris must have same size" }

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

                // نوار بالا (همان استاندارد)
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

                // Pager (همان استاندارد + یک‌تپ برای انتخاب)
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                ) {
                    val currentId = photoIds.getOrNull(pagerState.currentPage)

                    var showDeleteConfirm by remember { mutableStateOf(false) }


                    HorizontalPager(state = pagerState) { page ->
                        val uri = imageUris.getOrNull(page)
                        val id = photoIds.getOrNull(page) ?: return@HorizontalPager

                        var scale by remember(page) { mutableFloatStateOf(1f) }
                        var offset by remember(page) { mutableStateOf(Offset.Zero) }
                        val minScale = 1f
                        val maxScale = 5f

                        val zoomPanModifier = if (scale > 1f) {
                            Modifier.pointerInput(page, scale) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                                    val scaleChange = newScale / scale
                                    offset =
                                        (offset + (centroid - offset) * (1 - scaleChange)) + pan
                                    scale = newScale
                                }
                            }
                        } else Modifier

                        // دوبل‌تپ برای زوم/ریست + تک‌تپ برای انتخاب
                        val tapModifier = Modifier.pointerInput(page, scale) {
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
                                },
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
                                .then(tapModifier)
                        )
                    }

                    // --- Bottom controls container (یک مجموعه، دو ردیف) ---
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ردیف اکشن‌ها
                        if (currentId != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pinned = isPinned(currentId)

                                IconButton(
                                    onClick = { onTogglePin(currentId) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = if (pinned) "برداشتن پین" else "پین"
                                    )
                                }

                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                                }
                            }
                        }

                        // ردیف ناوبری (قبلی/نقطه‌ها/بعدی)
                        if (imageUris.size > 1) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val hasPrev = pagerState.currentPage > 0
                                TextButton(
                                    onClick = {
                                        if (hasPrev) scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                    enabled = hasPrev,
                                    contentPadding = PaddingValues(
                                        horizontal = 4.dp,
                                        vertical = 0.dp
                                    )
                                ) { Text("قبلی", color = Color.White) }

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
                                                .background(if (active) Color.White else Color.LightGray)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                val hasNext = pagerState.currentPage < imageUris.lastIndex
                                TextButton(
                                    onClick = {
                                        if (hasNext) scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    },
                                    enabled = hasNext,
                                    contentPadding = PaddingValues(
                                        horizontal = 4.dp,
                                        vertical = 0.dp
                                    )
                                ) { Text("بعدی", color = Color.White) }
                            }
                        }
                    }

                    // Snackbar داخل خود Dialog
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )

                    ConfirmDimmedDialog(
                        visible = showDeleteConfirm,
                        title = "حذف عکس",
                        message = { Text("عکس از این لیست پاک شود؟") },
                        onConfirm = { currentId?.let(onDelete) },
                        onDismiss = { showDeleteConfirm = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedSlot(
    item: OrderPhotoRefEntity?,
    selectedIds: Set<Int>,
    selectionMode: Boolean,
    onClick: (OrderPhotoRefEntity) -> Unit,
    onLongClick: (OrderPhotoRefEntity) -> Unit,
    modifier: Modifier,
    isPlaceholder: Boolean = false
) {
    val base = modifier.aspectRatio(1f)

    if (item == null) {
        if (!isPlaceholder) {
            Spacer(base)
            return
        }

        Box(
            modifier = base
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        )
        return
    }

    PhotoCardSelectable(
        e = item,
        isSelected = item.id in selectedIds,
        selectionMode = selectionMode,
        onClick = { onClick(item) },
        onLongClick = { onLongClick(item) },
        modifier = base
    )

}


@Composable
private fun GalleryPhotoCard(
    e: OrderPhotoRefEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    aspectRatio: Float?,
    onRatioKnown: (Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val ratio = (aspectRatio ?: 1f).coerceIn(0.9f, 1.6f)


    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box {
            SubcomposeAsyncImage(
                model = photoModel(e),
                contentDescription = null
            ) {
                val state = painter.state
                if (state is coil.compose.AsyncImagePainter.State.Success) {
                    val d = state.result.drawable
                    val w = d.intrinsicWidth
                    val h = d.intrinsicHeight
                    if (w > 0 && h > 0) {
                        onRatioKnown(w.toFloat() / h.toFloat())
                    }
                }
                SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (e.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }

            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // اختیاری: اگر می‌خوای تیک هم داشته باشه
                    if (isSelected) Icon(
                        Icons.Default.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

        }
    }
}


@Composable
private fun PinnedRowFixedSlots(
    left: OrderPhotoRefEntity?,
    right: OrderPhotoRefEntity?,
    selectedIds: Set<Int>,
    onClick: (OrderPhotoRefEntity) -> Unit,
    onLongClick: (OrderPhotoRefEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionMode = selectedIds.isNotEmpty()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PinnedSlot(
            item = left,
            selectedIds = selectedIds,
            selectionMode = selectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.weight(1f),
            isPlaceholder = true // همیشه اسلات داریم
        )

        PinnedSlot(
            item = right,
            selectedIds = selectedIds,
            selectionMode = selectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.weight(1f),
            isPlaceholder = true
        )
    }
}


private fun photoModel(e: OrderPhotoRefEntity): Any {
    val path = e.localCopyPath
    return if (e.isPinned && !path.isNullOrBlank() && File(path).exists()) {
        File(path) // ✅ از فایل داخلی
    } else {
        Uri.parse(e.contentUri) // ✅ از گالری/مدیاستور
    }
}