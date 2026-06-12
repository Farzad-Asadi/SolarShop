package com.example.solarShop.feature.product.ui.category

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.solarShop.feature.product.viewmodel.category.CategoryEditViewModel
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    onClose: () -> Unit,
    onAddAttribute: (Int) -> Unit,
    onEditAttribute: (categoryId: Int, attributeId: Int) -> Unit,
    viewModel: CategoryEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var draggingAttributes by remember { mutableStateOf(false) }

    val attributesState = remember {
        mutableStateOf(uiState.attributes)
    }

    LaunchedEffect(uiState.attributes) {
        if (!draggingAttributes) {
            attributesState.value = uiState.attributes
        }
    }
    val context = LocalContext.current
    var showPickSheet by remember { mutableStateOf(false) }
    var pendingTempFile by remember { mutableStateOf<File?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importCategoryImage(uri)
        }
    }

    val cameraController = rememberCameraCaptureLauncher(
        requiredPermissions = { emptyArray() },
        createOutputUri = {
            val (tmpFile, tmpUri) = viewModel.createCategoryCameraTempUri()
            pendingTempFile = tmpFile
            tmpUri
        },
        onResult = { _, success ->
            val tmp = pendingTempFile
            if (success && tmp != null) {
                viewModel.importCategoryImageFromCameraTemp(tmp)
            } else {
                tmp?.let { runCatching { it.delete() } }
            }
            pendingTempFile = null
        },
        onMessage = {}
    )
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteProductsDialog by remember { mutableStateOf(false) }
    var productCountInCategory by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditMode) {
                            "ویرایش دسته‌بندی"
                        } else {
                            "ایجاد دسته‌بندی"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    if (viewModel.isEditMode) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    productCountInCategory = viewModel.countProductsInThisCategory()

                                    if (productCountInCategory > 0) {
                                        showDeleteProductsDialog = true
                                    } else {
                                        showDeleteDialog = true
                                    }
                                }
                            },
                            modifier = Modifier
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )

                        }
                    }
                }

            )
        },

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                onClick = { showPickSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val fileName = uiState.imageFileName

                    if (fileName != null) {
                        val file = File(File(context.filesDir, "images"), fileName)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("نام دسته‌بندی") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مشخصه‌های این دسته (${uiState.attributes.size})",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (!viewModel.isEditMode) {
                        Text(
                            text = "\"بعد از ذخیره دسته\u200Cبندی، می\u200Cتوانی مشخصه\u200Cهایی مثل توان، ولتاژ، ظرفیت یا مدل را برای آن تعریف کنی.\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (uiState.attributes.isEmpty()) {
                        Text(
                            text = "هنوز مشخصه‌ای برای این دسته تعریف نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val reorderState = rememberReorderableLazyListState(
                            onMove = { from, to ->
                                draggingAttributes = true

                                val list = attributesState.value.toMutableList()
                                if (list.isEmpty()) return@rememberReorderableLazyListState

                                val fromIndex = from.index
                                if (fromIndex !in list.indices) return@rememberReorderableLazyListState

                                val item = list.removeAt(fromIndex)
                                val toIndex = to.index.coerceIn(0, list.size)

                                list.add(toIndex, item)
                                attributesState.value = list
                            },
                            onDragEnd = { _, _ ->
                                draggingAttributes = false
                                viewModel.saveAttributeOrder(attributesState.value)
                            }
                        )

                        LazyColumn(
                            state = reorderState.listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .reorderable(reorderState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = attributesState.value,
                                key = { it.id ?: it.hashCode() }
                            ) { attr ->

                                val attrId = attr.id ?: return@items

                                ReorderableItem(
                                    state = reorderState,
                                    key = attrId
                                ) { isDragging ->

                                    ElevatedCard(
                                        onClick = {
                                            val categoryId = viewModel.currentCategoryId

                                            if (categoryId != null) {
                                                onEditAttribute(categoryId, attrId)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = if (isDragging) 8.dp else 3.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "جابجایی",
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .detectReorderAfterLongPress(reorderState),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = attr.title,
                                                    style = MaterialTheme.typography.titleSmall
                                                )

                                                Text(
                                                    text = listOfNotNull(
                                                        attr.valueType,
                                                        attr.unit
                                                    ).joinToString(" / "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "ویرایش مشخصه",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (viewModel.isEditMode) {
                        OutlinedButton(
                            onClick = {
                                // مرحله فعلی فقط افزودن را به صفحه فعلی AttributeEdit وصل می‌کند
                                // خود navigation را در NavHost می‌نویسیم
                                viewModel.currentCategoryId?.let { categoryId ->
                                    onAddAttribute(categoryId)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("افزودن مشخصه")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val ok = viewModel.save()
                        if (ok) onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (viewModel.isEditMode) {
                        "ذخیره تغییرات"
                    } else {
                        "ایجاد دسته‌بندی"
                    }
                )
            }
        }
        if (showPickSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPickSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("انتخاب تصویر دسته", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = {
                            showPickSheet = false
                            pickImage.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("از گالری")
                    }

                    Button(
                        onClick = {
                            showPickSheet = false
                            cameraController.launch()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("از دوربین")
                    }
                }
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text("حذف دسته‌بندی")
                },
                text = {
                    Text("آیا مطمئنی می‌خواهی این دسته‌بندی حذف شود؟")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false

                            scope.launch {
                                viewModel.deleteThisCategoryWithProducts()
                                onClose()
                            }
                        }
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("انصراف")
                    }
                }
            )
        }

        if (showDeleteProductsDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteProductsDialog = false },
                title = {
                    Text("این دسته کالا دارد")
                },
                text = {
                    Text(
                        "در این دسته $productCountInCategory کالا ثبت شده است. " +
                                "با حذف این دسته، همه کالاهای داخل آن هم حذف می‌شوند. ادامه می‌دهی؟"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteProductsDialog = false

                            scope.launch {
                                viewModel.deleteThisCategoryWithProducts()
                                onClose()
                            }
                        }
                    ) {
                        Text("حذف دسته و کالاها")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteProductsDialog = false }
                    ) {
                        Text("انصراف")
                    }
                }
            )
        }

    }
}