package com.example.solarShop.feature.product.ui.category

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    onClose: () -> Unit,
    viewModel: CategoryEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("دسته‌بندی جدید") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("توضیحات") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    scope.launch {
                        val ok = viewModel.save()
                        if (ok) onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ذخیره")
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
    }
}