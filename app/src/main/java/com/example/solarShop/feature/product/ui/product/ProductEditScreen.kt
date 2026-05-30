package com.example.solarShop.feature.product.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.solarShop.feature.product.viewmodel.product.ProductEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onClose: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("کالای جدید") },
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
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("نام کالا") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.model,
                    onValueChange = viewModel::onModelChange,
                    label = { Text("مدل") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Divider()
                Text(
                    text = "مشخصات فنی",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (uiState.attributes.isEmpty()) {
                item {
                    Text("برای این دسته هنوز مشخصه‌ای تعریف نشده است.")
                }
            } else {
                items(uiState.attributes) { attr ->
                    OutlinedTextField(
                        value = uiState.attributeValues[attr.attributeDefinitionId].orEmpty(),
                        onValueChange = {
                            viewModel.onAttributeValueChange(
                                attributeDefinitionId = attr.attributeDefinitionId,
                                value = it
                            )
                        },
                        label = {
                            Text(
                                if (attr.unit.isNullOrBlank())
                                    attr.title
                                else
                                    "${attr.title} (${attr.unit})"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.save(
                            onSuccess = onClose
                        )
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isSaving) "در حال ذخیره..." else "ذخیره کالا"
                    )
                }
            }
        }
    }
}