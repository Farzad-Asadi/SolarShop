package com.example.solarShop.feature.product.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.solarShop.domain.product.AttributeValueType
import com.example.solarShop.feature.product.viewmodel.category.AttributeEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeEditScreen(
    onClose: () -> Unit,
    viewModel: AttributeEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditMode) {
                            "ویرایش مشخصه"
                        } else {
                            "مشخصه جدید"
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
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("عنوان مشخصه") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.key,
                onValueChange = viewModel::onKeyChange,
                label = { Text("کلید داخلی مثلا power") },
                modifier = Modifier.fillMaxWidth()
            )

            var valueTypeMenuExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = valueTypeMenuExpanded,
                onExpandedChange = {
                    valueTypeMenuExpanded = !valueTypeMenuExpanded
                }
            ) {
                OutlinedTextField(
                    value = uiState.valueType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("نوع مقدار") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = valueTypeMenuExpanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = valueTypeMenuExpanded,
                    onDismissRequest = {
                        valueTypeMenuExpanded = false
                    }
                ) {
                    AttributeValueType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                viewModel.onValueTypeChange(type)
                                valueTypeMenuExpanded = false
                            }
                        )
                    }
                }
            }
            if (uiState.valueType == AttributeValueType.ENUM) {
                OutlinedTextField(
                    value = uiState.enumOptions,
                    onValueChange = viewModel::onEnumOptionsChange,
                    label = { Text("گزینه‌ها") },
                    supportingText = {
                        Text("هر گزینه را در یک خط بنویس. مثال: N-Type")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }

            OutlinedTextField(
                value = uiState.unit,
                onValueChange = viewModel::onUnitChange,
                label = { Text("واحد مثلا W یا V") },
                modifier = Modifier.fillMaxWidth()
            )



            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.isRequired,
                    onCheckedChange = viewModel::onRequiredChange
                )
                Text("اجباری باشد")
            }

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
                    text = when {
                        uiState.isSaving -> "در حال ذخیره..."
                        viewModel.isEditMode -> "ذخیره تغییرات"
                        else -> "ایجاد مشخصه"
                    }
                )
            }
        }
    }
}