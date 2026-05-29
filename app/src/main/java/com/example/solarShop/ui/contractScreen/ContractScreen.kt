package com.example.solarShop.ui.contractScreen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solarShop.ARTICLE_PREFIX
import com.example.solarShop.R
import com.example.solarShop.data.room.tables.contract.ContractInstanceEntity
import com.example.solarShop.data.room.tables.contract.ContractInstanceFull
import com.example.solarShop.data.room.tables.contract.ContractInstanceNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractInstancePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractInstanceSectionEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateFull
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplatePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionEntity
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.ConfirmDimmedDialog
import com.example.solarShop.utils.ContractText
import com.example.solarShop.utils.MyNationalCodeField
import com.example.solarShop.utils.MyPhoneField
import com.example.solarShop.utils.MyStringField
import com.example.solarShop.utils.buildFinalTitle
import com.example.solarShop.utils.e164ToNational10
import com.example.solarShop.utils.extractTitleBodyForEdit
import com.example.solarShop.utils.national10ToE164
import kotlinx.coroutines.launch


@SuppressLint("SuspiciousIndentation")
@Composable
fun ContractScreen(                               //صفحه پروفایل

    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    vm: ContractViewModel = hiltViewModel()
){
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val editingParties by vm.editingParties.collectAsStateWithLifecycle()


    val editingSection by vm.editingSection.collectAsStateWithLifecycle()
    val editingNote by vm.editingNote.collectAsStateWithLifecycle()
    val pendingDeleteId by vm.pendingDeleteSectionId.collectAsStateWithLifecycle()
    val pendingDeleteNoteId by vm.pendingDeleteNoteId.collectAsStateWithLifecycle()
    val editingInstanceSection by vm.editingInstanceSection.collectAsStateWithLifecycle()
    val editingInstanceNote by vm.editingInstanceNote.collectAsStateWithLifecycle()
    val pendingDeleteInstanceSectionId by vm.pendingDeleteInstanceSectionId.collectAsStateWithLifecycle()
    val pendingDeleteInstanceNoteId by vm.pendingDeleteInstanceNoteId.collectAsStateWithLifecycle()


    val onEvent = vm::onEvent

        when {
            // 1) اگر نمونهٔ سفارش ساخته/لود شده، اولویت با ادیت Instance است
            ui.currentInstanceId != null && ui.instanceFull != null -> {
                EditInstanceContent(
                    instance = ui.instanceFull!!,
                    parties = editingParties,
                    editingSection = editingInstanceSection,
                    editingNote = editingInstanceNote,
                    pendingDeleteSectionId = pendingDeleteInstanceSectionId,
                    pendingDeleteNoteId = pendingDeleteInstanceNoteId,
                    onEvent = vm::onInstanceEvent,   // 👈 فقط همین!
                    onSave =  { partyId -> vm.onEvent(ContractEvent.SaveSingleInstanceParty(partyId)) } ,
                    onBack = { vm.onEvent(ContractEvent.BackToTemplatePicker) }
                )
            }

            // 2) اگر تمپلیت انتخاب شده (حالت پروفایل)، برو ادیت تمپلیت
            ui.template != null -> {
                Scaffold { innerPadding ->
                    EditContractContent(
                        template = ui.template!!,
                        onEvent = vm::onEvent,
                        editingSection = editingSection,
                        editingNote = editingNote,
                        pendingDeleteSectionId = pendingDeleteId,
                        pendingDeleteNoteId = pendingDeleteNoteId,
                        onClose = onClose,
                        onBackToPicker = { vm.onEvent(ContractEvent.BackToTemplatePicker) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            // 3) در غیر این صورت، صفحهٔ انتخاب تمپلیت
            else -> {
                TemplatePickerScreen(
                    templates = ui.templates,
                    isLoading = ui.isLoadingTemplates,
                    entrySource = ui.entrySource,
                    orderExistingInstance = ui.orderExistingInstance,                // ⬅️
                    onOpenExistingInstance = { id -> onEvent(ContractEvent.OpenExistingInstance(id)) }, // ⬅️
                    onDeleteExistingInstance = { id -> onEvent(ContractEvent.DeleteExistingInstance(id)) },
                    onSelect = { id -> onEvent(ContractEvent.SelectTemplate(id)) },
                    onCopy   = { id, newTitle -> onEvent(ContractEvent.CopyTemplateWithTitle(id, newTitle)) },
                    onDelete = { id -> onEvent(ContractEvent.DeleteTemplate(id)) },
                    onRefresh = { onEvent(ContractEvent.RefreshTemplates) },
                    onBack = onClose
                )
            }
        }


}

@Composable
fun EditContractContent(
    template: ContractTemplateFull,
    onEvent: (ContractEvent) -> Unit,
    editingSection: ContractTemplateSectionEntity?,
    editingNote: ContractTemplateNoteEntity?,
    pendingDeleteSectionId: Int?,
    pendingDeleteNoteId: Int?,
    onClose: () -> Unit,
    onBackToPicker: () -> Unit,
    modifier: Modifier = Modifier
) {

    BackHandler { onBackToPicker() }

    val sectionsSorted = remember(template.sectionsWithNotes) {
        template.sectionsWithNotes.sortedBy { it.section.orderNo }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(Modifier.fillMaxSize()) {

                // ــ هدر: عنوان قرارداد + بستن ــ
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ دکمه بک (برگشت به TemplatePickerScreen)
                    IconButton(onClick = onBackToPicker) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "بازگشت")
                    }

                    Text(
                        text = template.template.title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
//                    IconButton(onClick = onClose) {
//                        Icon(Icons.Default.Close, contentDescription = "بستن")
//                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ــ مشخصات طرفین ــ
                    item {
                        Text(
                            "مشخصات طرفین",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(template.parties) { p -> PartyCardReadOnly(p) }

                    item { Spacer(Modifier.height(8.dp)) }

                    // ــ مواد قرارداد ــ
                    item {
                        Text(
                            "مواد قرارداد",
                            style = MaterialTheme.typography.titleMedium, // اگر تایپو داشت: typography
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(
                        items = sectionsSorted,
                        key = { it.section.id }
                    ) { swn ->
                        val s = swn.section
                        val notes = swn.notes
                        val isEditingThisSection = editingSection?.id == s.id && s.id != 0

                        if (!isEditingThisSection) {
                            // حالت فقط‌خواندنی کارت ماده
                            SectionCardReadOnly(
                                s = s,
                                notes = notes,
                                editingNote = editingNote,

                                onEdit = { onEvent(ContractEvent.EditSection(s)) },
                                onDelete = { onEvent(ContractEvent.RequestDeleteSection(s)) },

                                onAddNote = { onEvent(ContractEvent.AddNewNote(s.id)) },

                                // نوت‌ها: همه از طریق onEvent
                                onEditNote = { note ->
                                    onEvent(ContractEvent.EditNote(note))
                                },
                                onRequestDeleteNote = { note ->
                                    onEvent(ContractEvent.RequestDeleteNote(note))
                                },
                                onChangeEditingNote = { transform ->
                                    onEvent(ContractEvent.ChangeEditingNote(transform))
                                },
                                onCancelEditNote = {
                                    onEvent(ContractEvent.CancelEditNote)
                                },
                                onSaveEditingNote = {
                                    onEvent(ContractEvent.SaveEditingNote)
                                }
                            )

                        } else {
                            // حالت ادیت کارت ماده
                            val usedOrderNos = template.sectionsWithNotes
                                .map { it.section }
                                .filter { it.id != s.id }
                                .map { it.orderNo }
                                .toSet()

                            SectionCardEdit(
                                section = editingSection!!,
                                usedOrderNos = usedOrderNos,

                                onChange = { transform ->
                                    onEvent(ContractEvent.ChangeEditingSection(transform))
                                },
                                onCancel = {
                                    onEvent(ContractEvent.CancelEditSection)
                                },
                                onSave = { newOrderNo, newTitleBody, newVisible, newRequired ->
                                    onEvent(
                                        ContractEvent.ChangeEditingSection { it.copy(
                                            orderNo = newOrderNo,
                                            title = buildFinalTitle(newOrderNo, newTitleBody),
                                            isDefaultVisible = newVisible,
                                            isRequired = newRequired
                                        ) }
                                    )
                                    onEvent(ContractEvent.SaveEditingSection)
                                }
                            )
                        }

                        // اگر تبصرهٔ پیش‌نویس متعلق به همین ماده است، ادیتور inline
                        if (editingNote?.id == 0 && editingNote.sectionId == s.id) {
                            val usedNoteOrders = notes.map { it.orderNo }.toSet()
                            itemNoteEditorInline(
                                note = editingNote,
                                usedOrderNos = usedNoteOrders,
                                onChange = { transform ->
                                    onEvent(ContractEvent.ChangeEditingNote(transform))
                                },
                                onCancel = {
                                    onEvent(ContractEvent.CancelEditNote)
                                },
                                onSave = { newOrder, newTitle, newBody ->
                                    onEvent(
                                        ContractEvent.ChangeEditingNote { it.copy(
                                            orderNo = newOrder,
                                            title = newTitle,
                                            body = newBody
                                        ) }
                                    )
                                    onEvent(ContractEvent.SaveEditingNote)
                                }
                            )
                        }
                    }

                    // ــ پیش‌نویس مادهٔ جدید (id=0) ــ
                    if (editingSection?.id == 0 && editingSection.templateId == template.template.id) {
                        item {
                            val usedOrderNos = template.sectionsWithNotes
                                .map { it.section.orderNo }
                                .toSet()

                            SectionCardEdit(
                                section = editingSection,
                                usedOrderNos = usedOrderNos,
                                onChange = { transform ->
                                    onEvent(ContractEvent.ChangeEditingSection(transform))
                                },
                                onCancel = {
                                    onEvent(ContractEvent.CancelEditSection)
                                },
                                onSave = { newOrderNo, newTitleBody, newVisible, newRequired ->
                                    onEvent(
                                        ContractEvent.ChangeEditingSection { it.copy(
                                            orderNo = newOrderNo,
                                            title = buildFinalTitle(newOrderNo, newTitleBody),
                                            isDefaultVisible = newVisible,
                                            isRequired = newRequired
                                        ) }
                                    )
                                    onEvent(ContractEvent.SaveEditingSection)
                                }
                            )
                        }
                    }

                    // ــ اکشن‌های پایین صفحه ــ
                    item {
                        Spacer(Modifier.height(12.dp))
                        BottomActionsBar(
                            onAddSection = {
                                onEvent(ContractEvent.AddNewSection(template.template.id))
                            },
                            onPreviewPdf = {
                                onEvent(ContractEvent.PreviewTemplatePdf(template.template.id))
                            }
                        )
                        Spacer(Modifier.height(80.dp))
                    }
                }

                // ــ دیالوگ‌های حذف ــ
                if (pendingDeleteSectionId != null) {
                    ConfirmDeleteDialog(
                        title = "حذف ماده",
                        message = "آیا از حذف این ماده مطمئن هستید؟",
                        onDismiss = { onEvent(ContractEvent.DismissDeleteSection) },
                        onConfirm = { onEvent(ContractEvent.ConfirmDeleteSection) }
                    )
                }

                if (pendingDeleteNoteId != null) {
                    ConfirmDeleteDialog(
                        title = "حذف تبصره",
                        message = "آیا از حذف این تبصره مطمئن هستید؟",
                        onDismiss = { onEvent(ContractEvent.DismissDeleteNote) },
                        onConfirm = { onEvent(ContractEvent.ConfirmDeleteNote) }
                    )
                }
            }
        }
    }
}




@Composable
private fun PartyCardReadOnly(p: ContractTemplatePartyEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = p.role,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            DottedLabelValueRow("نام/نام خانوادگی", p.fullName)
            DottedLabelValueRow("نام پدر", p.fatherFullName)
            DottedLabelValueRow("کد/شناسه ملی", p.nationalId)
            DottedLabelValueRow("شرکت", p.companyName)
            DottedLabelValueRow("آدرس", p.address)
            DottedLabelValueRow("تلفن", p.phone)
        }
    }
}

@Composable
private fun SectionCardReadOnly(
    s: ContractTemplateSectionEntity,
    notes: List<ContractTemplateNoteEntity>,
    editingNote: ContractTemplateNoteEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (ContractTemplateNoteEntity) -> Unit,
    onRequestDeleteNote: (ContractTemplateNoteEntity) -> Unit,
    onChangeEditingNote: ((ContractTemplateNoteEntity) -> ContractTemplateNoteEntity) -> Unit,
    onCancelEditNote: () -> Unit,
    onSaveEditingNote: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    displayTitleFor(s),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "ویرایش"
                        )
                    }
                    IconButton(onClick = onDelete, enabled = !s.isRequired) {
                        Icon(Icons.Filled.Close, contentDescription = "حذف")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            ContractText(body = s.body, values = emptyMap())

            if (!s.isDefaultVisible || s.isRequired) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!s.isDefaultVisible) AssistChip(
                        onClick = {},
                        label = { Text("پیش‌فرض: مخفی") })
                    if (s.isRequired) AssistChip(onClick = {}, label = { Text("ضروری") })
                }
            }

            // ---- تبصره‌ها ----
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تبصره‌ها", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onAddNote) { Text("افزودن تبصره") }
            }

            if (notes.isEmpty()) {
                Text(
                    "بدون تبصره",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.sortedBy { it.orderNo }.forEach { n ->
                        val isEditingThisNote = editingNote?.id == n.id && n.id != 0
                        if (!isEditingThisNote) {
                            NoteCardReadOnly(
                                sectionOrderNo = s.orderNo,
                                note = n,
                                onEdit = { onEditNote(n) },
                                onDelete = { onRequestDeleteNote(n) }
                            )
                        } else {
                            val usedNoteOrders =
                                notes.filter { it.id != n.id }.map { it.orderNo }.toSet()
                            NoteCardEdit(
                                note = editingNote!!,
                                usedOrderNos = usedNoteOrders,
                                onChange = onChangeEditingNote,
                                onCancel = onCancelEditNote,
                                onSave = { newOrder, newTitle, newBody ->
                                    onChangeEditingNote {
                                        it.copy(
                                            orderNo = newOrder,
                                            title = newTitle,
                                            body = newBody
                                        )
                                    }
                                    onSaveEditingNote()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SectionCardEdit(
    section: ContractTemplateSectionEntity,
    usedOrderNos: Set<Int>,
    onChange: ((ContractTemplateSectionEntity) -> ContractTemplateSectionEntity) -> Unit,
    onCancel: () -> Unit,
    onSave: (newOrderNo: Int, newTitleBody: String, newVisible: Boolean, newRequired: Boolean) -> Unit
) {
    // state محلی برای ورودی‌ها
    var orderText by remember(section.id) { mutableStateOf(section.orderNo.toString()) }
    var titleBody by remember(section.id) { mutableStateOf(extractTitleBodyForEdit(section.title)) }
    var visible by remember(section.id) { mutableStateOf(section.isDefaultVisible) }
    var required by remember(section.id) { mutableStateOf(section.isRequired) }

    // تبدیل امن orderText به عدد
    val orderNo = orderText.toIntOrNull() ?: section.orderNo

    // تکراری بودن شماره (به‌جز همین بند)
    val isDuplicateOrder = usedOrderNos.contains(orderNo)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // شماره ماده
            OutlinedTextField(
                value = orderText,
                onValueChange = { v ->
                    // فقط ارقام اجازه بده (اختیاری، ولی UX بهتر)
                    val digitsOnly = v.filter { it.isDigit() }
                    orderText = digitsOnly.ifEmpty { "" }
                },
                label = { Text("شماره ماده") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                isError = isDuplicateOrder || orderText.isBlank()
            )

            // عنوان ماده (بدون "ماده N -")
            OutlinedTextField(
                value = titleBody,
                onValueChange = { titleBody = it },
                label = { Text("عنوان ماده") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
            )

            // متن ماده
            OutlinedTextField(
                value = section.body,
                onValueChange = { v -> onChange { it.copy(body = v) } },
                label = { Text("متن ماده") },
                placeholder = { Text("متن ماده را اینجا بنویسید…") },
                singleLine = false,
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
            )

            // سوییچ‌ها (اینجا با AssistChip toggle ساده نگه‌داشتیم)
//            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                AssistChip(
//                    onClick = {
//                        visible = !visible
//                        onChange { it.copy(isDefaultVisible = visible) }
//                    },
//                    label = { Text(if (visible) "نمایش پیش‌فرض: بله" else "نمایش پیش‌فرض: خیر") }
//                )
//                AssistChip(
//                    onClick = {
//                        required = !required
//                        onChange { it.copy(isRequired = required) }
//                    },
//                    label = { Text(if (required) "ضروری: بله" else "ضروری: خیر") }
//                )
//            }

            // اخطار تکراری بودن شماره
            if (isDuplicateOrder) {
                Text(
                    "شمارهٔ ماده تکراری است.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("انصراف") }
                Button(
                    onClick = {
                        val finalOrder = orderText.toIntOrNull() ?: section.orderNo
                        onSave(finalOrder, titleBody, visible, required)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = orderText.isNotBlank() && !isDuplicateOrder && titleBody.isNotBlank()
                ) { Text("ذخیره") }
            }
        }
    }
}


@Composable
private fun itemNoteEditorInline(
    note: ContractTemplateNoteEntity,
    usedOrderNos: Set<Int>,
    onChange: ((ContractTemplateNoteEntity) -> ContractTemplateNoteEntity) -> Unit,
    onCancel: () -> Unit,
    onSave: (newOrderNo: Int, newTitle: String?, newBody: String) -> Unit
) {
    NoteCardEdit(
        note = note,
        usedOrderNos = usedOrderNos,
        onChange = onChange,
        onCancel = onCancel,
        onSave = onSave
    )
}

@Composable
private fun BottomActionsBar(
    onAddSection: () -> Unit,
    onPreviewPdf: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onAddSection
            ) { Text("➕ افزودن ماده جدید") }

            // نیاز به material-icons-extended برای آیکن PDF (نکته آموزشی زیر)
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onPreviewPdf
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("پیش‌ نمایش PDF")
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(6.dp)) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(message)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("خیر") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) { Text("بله، حذف کن") }
                }
            }
        }
    }
}

@SuppressLint("Range")
@Composable
private fun DottedLabelValueRow(
    label: String,
    value: String?,
    dots: String = "……………………………………"
) {
    val shown = value?.takeIf { it.isNotBlank() } ?: dots

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Right,
            modifier = Modifier
                .widthIn(min = 96.dp)
                .weight(1f, fill = false)
        )
        Text(
            shown,
            textAlign = TextAlign.Right,   // ← چون RTL هستیم
            modifier = Modifier.weight(1f)
        )
    }
}

private fun displayTitleFor(section: ContractTemplateSectionEntity): String {
    return if (ARTICLE_PREFIX.containsMatchIn(section.title)) {
        section.title
    } else {
        "ماده ${section.orderNo} - ${section.title}"
    }
}

@Composable
private fun NoteCardEdit(
    note: ContractTemplateNoteEntity,
    usedOrderNos: Set<Int>,
    onChange: ((ContractTemplateNoteEntity) -> ContractTemplateNoteEntity) -> Unit,
    onCancel: () -> Unit,
    onSave: (newOrderNo: Int, newTitle: String?, newBody: String) -> Unit
) {
    var orderText by remember(note.id) { mutableStateOf(note.orderNo.toString()) }
    var title by remember(note.id) { mutableStateOf(note.title.orEmpty()) }
    var body by remember(note.id) { mutableStateOf(note.body) }

    val orderNo = orderText.toIntOrNull() ?: note.orderNo
    val isDuplicateOrder = usedOrderNos.contains(orderNo) && orderNo != note.orderNo

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            OutlinedTextField(
                value = orderText,
                onValueChange = { v -> orderText = v.filter { it.isDigit() } },
                label = { Text("شماره تبصره (برای این ماده)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                isError = orderText.isBlank() || isDuplicateOrder
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان (اختیاری)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("متن تبصره") },
                singleLine = false,
                placeholder = { Text("متن تبصره را اینجا بنویسید…") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
            )

            if (isDuplicateOrder) {
                Text(
                    "شمارهٔ تبصره در این ماده تکراری است.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("انصراف") }
                Button(
                    onClick = {
                        val finalOrder = orderText.toIntOrNull() ?: note.orderNo
                        onSave(finalOrder, title.ifBlank { null }, body)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = orderText.isNotBlank() && body.isNotBlank() && !isDuplicateOrder
                ) { Text("ذخیره") }
            }
        }
    }
}

@Composable
private fun NoteCardReadOnly(
    sectionOrderNo: Int,
    note: ContractTemplateNoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val title = "تبصره ${note.orderNo}–${sectionOrderNo}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "ویرایش تبصره"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "حذف تبصره"
                        )
                    }
                }
            }
            if (!note.title.isNullOrBlank()) {
                Text(
                    note.title!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(note.body, style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable
fun TemplatePickerScreen(
    templates: List<ContractTemplateEntity>,
    isLoading: Boolean,
    entrySource: ContractEntrySource,
    orderExistingInstance: ContractInstanceEntity?,           // ⬅️ جدید
    onOpenExistingInstance: (Int) -> Unit,
    onDeleteExistingInstance: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCopy: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val title = when (entrySource) {
        ContractEntrySource.PROFILE -> "کدام قرارداد را می‌خواهید ویرایش کنید؟"
        ContractEntrySource.ORDER   -> "کدام قرارداد را می‌خواهید برای مشتری ثبت کنید؟"
    }

    Scaffold(
        topBar = {
            BamboTopBar(
                title = title,
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            when {
                isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                templates.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("هیچ تمپلیتی یافت نشد.")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "برای شروع، یک تمپلیت بسازید یا بازخوانی کنید.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onRefresh) { Text("بازخوانی") }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (entrySource == ContractEntrySource.ORDER && orderExistingInstance != null) {

                            item(key = "existing-instance") {

                                var showDeleteDialog by remember(orderExistingInstance.id) { mutableStateOf(false) }

                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("✔️ قرارداد همین سفارش موجود است", style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Text(orderExistingInstance.title, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.height(12.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                                            Button(
                                                modifier = Modifier.weight(1f),
                                                onClick = { onOpenExistingInstance(orderExistingInstance.id) }
                                            ) {
                                                Text("ادامه و ویرایش")
                                            }

                                            OutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                onClick = { showDeleteDialog = true }
                                            ) {
                                                Text("حذف قرارداد")
                                            }
                                        }
                                    }
                                }

                                ConfirmDimmedDialog(
                                    visible = showDeleteDialog,
                                    title = "حذف قرارداد این سفارش",
                                    message = {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("آیا از حذف این قرارداد مطمئن هستید؟")
                                            Text("بعد از حذف، برای این سفارش باید دوباره قرارداد بسازید.")
                                        }
                                    },
                                    confirmText = "بله، حذف شود",
                                    dismissText = "انصراف",
                                    onConfirm = {
                                        // 👇 این callback را در امضای تابع اضافه می‌کنیم
                                        onDeleteExistingInstance(orderExistingInstance.id)
                                    },
                                    onDismiss = { showDeleteDialog = false }
                                )
                            }

                            item { Spacer(Modifier.height(8.dp)) }
                        }

                        items(templates, key = { it.id }) { t ->
                            TemplateRow(
                                title = t.title,
                                subtitle = t.description ?: "بدون توضیح",
                                isProtected = t.isProtected,
                                onClick = { onSelect(t.id) },
                                onCopyConfirm = { newTitle ->
                                    // پاس دادن به ViewModel
                                    onCopy(t.id, newTitle)
                                },
                                onDeleteConfirm = { onDelete(t.id) }    // ⬅️ اینجا حذف را صدا بزن
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(
    title: String,
    subtitle: String,
    isProtected: Boolean,
    onClick: () -> Unit,
    onCopyConfirm: (String) -> Unit,
    onDeleteConfirm: () -> Unit,
) {
    var showCopyDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("کپی - $title") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor =MaterialTheme.colorScheme.primaryContainer,
            disabledContentColor =MaterialTheme.colorScheme.onPrimaryContainer


        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // دکمهٔ کپی
                FilledTonalButton(onClick = { showCopyDialog = true }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("کپی")
                }
                // دکمهٔ حذف
                FilledTonalButton(
                    enabled = !isProtected,
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("حذف")
                }
            }
        }
    }

    // دیالوگ نام جدید برای کپی
    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("کپی قرارداد") },
            text = {
                Column {
                    Text("نام قرارداد جدید را وارد کنید:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCopyDialog = false
                        onCopyConfirm(newTitle)
                    }
                ) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) { Text("انصراف") }
            }
        )
    }

    // دیالوگ تأیید حذف
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف قرارداد") },
            text = { Text("آیا از حذف این قرارداد مطمئن هستید؟ این عمل قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirm()
                    }
                ) { Text("بله، حذف شود") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
fun BamboTopBar(
    title: String,
    onBack: () -> Unit,
) {

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp   // فقط گوشه‌های پایین
        ),
        color = BambooTheme.sections.topBarContainer,
        contentColor = BambooTheme.sections.topBarContent,
        tonalElevation = 0.dp,
//        shadowElevation = 6.dp

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.padding(end = 12.dp),
                onClick = onBack,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "ArrowBack")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))

        }
    }
    
    
}


@Composable
fun PartyCardEdit(
    party: ContractInstancePartyEntity,
    onChange: (ContractInstancePartyEntity) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("نقش: ${party.role}", style = MaterialTheme.typography.titleSmall)

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = party.fullName.orEmpty(),
                onValueChange = { onChange(party.copy(fullName = it)) },
                label = { Text("نام و نام‌خانوادگی") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = party.fatherFullName.orEmpty(),
                onValueChange = { onChange(party.copy(fatherFullName = it)) },
                label = { Text("نام پدر") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = party.nationalId.orEmpty(),
                onValueChange = { onChange(party.copy(nationalId = it)) },
                label = { Text("کد ملی") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = party.companyName.orEmpty(),
                onValueChange = { onChange(party.copy(companyName = it)) },
                label = { Text("نام شرکت") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = party.address.orEmpty(),
                onValueChange = { onChange(party.copy(address = it)) },
                label = { Text("آدرس") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = party.phone.orEmpty(),
                onValueChange = { onChange(party.copy(phone = it)) },
                label = { Text("تلفن") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EditInstanceContent(
    instance: ContractInstanceFull,
    parties: List<ContractInstancePartyEntity>,
    editingSection: ContractInstanceSectionEntity?,
    editingNote: ContractInstanceNoteEntity?,
    pendingDeleteSectionId: Int?,
    pendingDeleteNoteId: Int?,
    onEvent: (ContractInstanceEvent) -> Unit,
    onSave: (partyId: Int) -> Unit,
    onBack: () -> Unit
) {
    val instanceId = instance.instance.id
    val sectionsSorted = remember(instance.sectionsWithNotes) {
        instance.sectionsWithNotes.sortedBy { it.section.orderNo }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isNearTop = listState.firstVisibleItemIndex <= 0

    BackHandler { onBack() }

    Scaffold(
        topBar = { BamboTopBar(title = instance.instance.title, onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        val lastIndex = maxOf(0, listState.layoutInfo.totalItemsCount - 1)
                        if (isNearTop) {
                            // برو پایین
                            listState.animateScrollToItem(lastIndex)
                        } else {
                            // برو بالا
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            ) {
                Text(if (isNearTop) "برو پایین" else "برو بالا")
            }
        }
    ) { innerPadding ->


        LazyColumn(
            state = listState,
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Parties header
            item(key = "hdr_parties") {
                Text("مشخصات طرفین", style = MaterialTheme.typography.titleMedium)
            }

            // Parties list
            items(
                items = parties,
                key = { p -> "party_${p.id.takeIf { it != 0 } ?: ("${p.role}_${p.hashCode()}")}" }
            ) { p ->
                InstancePartyCardEdit(
                    party = p,
                    onChange = { updated ->
                        onEvent(ContractInstanceEvent.ChangeParty(p.id) { _ -> updated })
                    },
                    onSave ={partyId:Int->
                        onSave(partyId)
                    }
                )
            }

            // فاصله‌ی بعد از طرفین
            item(key = "sp_after_parties") { Spacer(Modifier.height(8.dp)) }

            // Sections title
            item(key = "hdr_sections") {
                Text("مواد قرارداد", style = MaterialTheme.typography.titleMedium)
            }

            // Sections list
            items(
                items = sectionsSorted,
                key = { swn -> "sec_${swn.section.id}" }
            ) { swn ->
                val s = swn.section
                val notes = swn.notes
                val isEditingThisSection = editingSection?.id == s.id && s.id != 0

                if (!isEditingThisSection) {
                    SectionCardReadOnlyInstance(
                        s = s,
                        notes = notes,
                        editingNote = editingNote?.takeIf { it.id != 0 && it.instanceSectionId == s.id },
                        onEdit = { onEvent(ContractInstanceEvent.EditSection(s)) },
                        onDelete = { onEvent(ContractInstanceEvent.RequestDeleteSection(s.id)) },
                        onAddNote = { onEvent(ContractInstanceEvent.AddNewNote(s.id)) },
                        onEditNote = { n -> onEvent(ContractInstanceEvent.EditNote(n)) },
                        onRequestDeleteNote = { n -> onEvent(ContractInstanceEvent.RequestDeleteNote(n.id)) },
                        onChangeEditingNote = { tr -> onEvent(ContractInstanceEvent.ChangeEditingNote(tr)) },
                        onCancelEditNote = { onEvent(ContractInstanceEvent.CancelEditNote) },
                        onSaveEditingNote = { secId -> onEvent(ContractInstanceEvent.SaveEditingNote(secId)) }
                    )
                } else {
                    val usedOrderNos = instance.sectionsWithNotes
                        .map { it.section }
                        .filter { it.id != s.id }
                        .map { it.orderNo }
                        .toSet()

                    SectionCardEditInstance(
                        section = editingSection!!,
                        usedOrderNos = usedOrderNos,
                        onChange = { tr -> onEvent(ContractInstanceEvent.ChangeEditingSection(tr)) },
                        onCancel = { onEvent(ContractInstanceEvent.CancelEditSection) },
                        onSave = { newOrderNo, newTitleBody ->
                            onEvent(
                                ContractInstanceEvent.ChangeEditingSection {
                                    it.copy(
                                        orderNo = newOrderNo,
                                        title   = buildFinalTitle(newOrderNo, newTitleBody)
                                    )
                                }
                            )
                            onEvent(ContractInstanceEvent.SaveEditingSection(instanceId))
                        }
                    )
                }

                // Draft note (id=0) for this section
                if (editingNote?.id == 0 && editingNote.instanceSectionId == s.id) {
                    val usedNoteOrders = notes.map { it.orderNo }.toSet()
                    NoteEditorInlineInstance(
                        note = editingNote,
                        usedOrderNos = usedNoteOrders,
                        onChange = { tr -> onEvent(ContractInstanceEvent.ChangeEditingNote(tr)) },
                        onCancel = { onEvent(ContractInstanceEvent.CancelEditNote) },
                        onSave = { newOrder, newTitle, newBody ->
                            onEvent(
                                ContractInstanceEvent.ChangeEditingNote {
                                    it.copy(orderNo = newOrder, title = newTitle, body = newBody)
                                }
                            )
                            onEvent(ContractInstanceEvent.SaveEditingNote(s.id))
                        }
                    )
                }
            }

            // Draft new section (id=0) — کلید یکتا
            if (editingSection?.id == 0 && editingSection.instanceId == instanceId) {
                item(key = "draft_section_$instanceId") {
                    val used = instance.sectionsWithNotes.map { it.section.orderNo }.toSet()
                    SectionCardEditInstance(
                        section = editingSection,
                        usedOrderNos = used,
                        onChange = { tr -> onEvent(ContractInstanceEvent.ChangeEditingSection(tr)) },
                        onCancel = { onEvent(ContractInstanceEvent.CancelEditSection) },
                        onSave = { newOrderNo, newTitleBody ->
                            onEvent(
                                ContractInstanceEvent.ChangeEditingSection {
                                    it.copy(
                                        orderNo = newOrderNo,
                                        title   = buildFinalTitle(newOrderNo, newTitleBody)
                                    )
                                }
                            )
                            onEvent(ContractInstanceEvent.SaveEditingSection(instanceId))
                        }
                    )
                }
            }

            // Bottom actions (کلید یکتا)
            item(key = "actions_bar") {
                Spacer(Modifier.height(12.dp))
                BottomActionsBar(
                    onAddSection = { onEvent(ContractInstanceEvent.AddNewSection(instanceId)) },
                    onPreviewPdf = { onEvent(ContractInstanceEvent.PreviewPdf(instanceId)) }
                )
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Delete dialogs
    if (pendingDeleteSectionId != null) {
        ConfirmDeleteDialog(
            title = "حذف ماده",
            message = "آیا از حذف این ماده مطمئن هستید؟",
            onDismiss = { onEvent(ContractInstanceEvent.DismissDeleteSection) },
            onConfirm = { onEvent(ContractInstanceEvent.ConfirmDeleteSection) }
        )
    }
    if (pendingDeleteNoteId != null) {
        ConfirmDeleteDialog(
            title = "حذف تبصره",
            message = "آیا از حذف این تبصره مطمئن هستید؟",
            onDismiss = { onEvent(ContractInstanceEvent.DismissDeleteNote) },
            onConfirm = { onEvent(ContractInstanceEvent.ConfirmDeleteNote) }
        )
    }
}


@Composable
fun InstancePartyCardEdit(
    party: ContractInstancePartyEntity,
    onChange: (ContractInstancePartyEntity) -> Unit,
    onSave: (partyId: Int) -> Unit
) {
    // ✅ snapshot آخرین "ذخیره موفق" از نگاه UI
    var lastSavedSnapshot by remember(party.id) { mutableStateOf(party) }

    val phoneNational = remember(party.id, party.phone) { e164ToNational10(party.phone) }
    var isPhoneValid by remember(party.id) { mutableStateOf(true) }



    // اگر از DB آپدیت شد (مثلاً ورود دوباره به صفحه)، snapshot هم sync شود
    LaunchedEffect(party.id) {
        lastSavedSnapshot = party
    }

    fun norm(s: String?) = s?.trim()?.takeIf { it.isNotEmpty() }

    val isDirty = remember(party, lastSavedSnapshot) {
        norm(party.fullName)    != norm(lastSavedSnapshot.fullName) ||
                norm(party.nationalId)  != norm(lastSavedSnapshot.nationalId) ||
                norm(party.companyName) != norm(lastSavedSnapshot.companyName) ||
                norm(party.address)     != norm(lastSavedSnapshot.address) ||
                norm(party.phone)       != norm(lastSavedSnapshot.phone)
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {

            Text(party.role, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // ✅ نام و نام خانوادگی
            MyStringField(
                value = party.fullName.orEmpty(),
                onValueChange = { cleaned ->
                    onChange(party.copy(fullName = cleaned.ifBlank { null }))
                    cleaned
                },
                onImeDone = {}
            )

            // ✅ کد ملی
            Spacer(Modifier.height(8.dp))
            MyNationalCodeField(
                value = party.nationalId.orEmpty(),
                onValueChange = { v -> onChange(party.copy(nationalId = v.ifBlank { null })) },
                onValidChange = { /* اگر خواستی وضعیت اعتبار را بالا هم بفرستی */ }
            )

            // ✅ نام شرکت/کارگاه (همون MyStringField)
            Spacer(Modifier.height(8.dp))
            MyStringField(
                value = party.companyName.orEmpty(),
                onValueChange = { cleaned ->
                    onChange(party.copy(companyName = cleaned.ifBlank { null }))
                    cleaned
                },
                label = "نام شرکت (اختیاری)",
                placeholder = "مثلاً کارگاه چوب...",
                onImeDone = {}
            )

            // ✅ آدرس
            Spacer(Modifier.height(8.dp))
            MyStringField(
                value = party.address.orEmpty(),
                onValueChange = { cleaned ->
                    onChange(party.copy(address = cleaned.ifBlank { null }))
                    cleaned
                },
                maxLength = 110,
                label = "نشانی",
                placeholder = "آدرس را وارد کنید",
                onImeDone = {}
            )

            // ✅ تلفن (همراه) - استاندارد
            Spacer(Modifier.height(8.dp))
            MyPhoneField(
                national = phoneNational,
                onNationalChange = { cleanedNational ->
                    // ✅ ذخیره در مدل به شکل استاندارد DB (E.164) تا دوباره 98 دوبل نشه
                    onChange(party.copy(phone = national10ToE164(cleanedNational)))
                },
                onValidChange = { isPhoneValid = it },
                enabled = isBuyerRole(party.role),  // پایین توضیح دادم
                onImeDone = {},
                label = stringResource(R.string.profile_field_mobile)
            )


            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {

                    onChange(
                        party.copy(
                            fullName = party.fullName?.trim()?.ifBlank { null },
                            nationalId = party.nationalId?.trim()?.ifBlank { null },
                            companyName = party.companyName?.trim()?.ifBlank { null },
                            address = party.address?.trim()?.ifBlank { null },
                            phone = party.phone?.trim()?.ifBlank { null },
                        )
                    )

                    // 1) فقط همین کارت را ذخیره کن
                    onSave(party.id)
                    // 2) snapshot را آپدیت کن تا دکمه غیرفعال شود
                    lastSavedSnapshot = party
                },
                enabled = isDirty,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ذخیره مشخصات")
            }
        }
    }
}



@Composable
private fun SectionCardReadOnlyInstance(
    s: ContractInstanceSectionEntity,
    notes: List<ContractInstanceNoteEntity>,
    editingNote: ContractInstanceNoteEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (ContractInstanceNoteEntity) -> Unit,
    onRequestDeleteNote: (ContractInstanceNoteEntity) -> Unit,
    onChangeEditingNote: ((ContractInstanceNoteEntity) -> ContractInstanceNoteEntity) -> Unit,
    onCancelEditNote: () -> Unit,
    onSaveEditingNote: (sectionId: Int) -> Unit
) {
    val tmplSection = remember(s) { s.asTemplateLike() }
    val tmplNotes = remember(notes) { notes.map { it.asTemplateLike(sectionId = s.id) } }

    SectionCardReadOnly(
        s = tmplSection,
        notes = tmplNotes,
        editingNote = editingNote?.let { it.asTemplateLike(sectionId = s.id) },

        onEdit = onEdit,
        onDelete = onDelete,
        onAddNote = onAddNote,

        onEditNote = { tNote ->
            val src = notes.first { it.id == tNote.id }
            onEditNote(src)
        },
        onRequestDeleteNote = { tNote ->
            val src = notes.first { it.id == tNote.id }
            onRequestDeleteNote(src)
        },
        onChangeEditingNote = { tr ->
            onChangeEditingNote { instNote ->
                val tempAsTmpl = instNote.asTemplateLike(sectionId = s.id)
                val changed = tr(tempAsTmpl)
                instNote.copy(
                    orderNo = changed.orderNo,
                    title = changed.title,
                    body = changed.body
                )
            }
        },
        onCancelEditNote = onCancelEditNote,
        onSaveEditingNote = { onSaveEditingNote(s.id) }
    )
}

@Composable
private fun SectionCardEditInstance(
    section: ContractInstanceSectionEntity,
    usedOrderNos: Set<Int>,
    onChange: ((ContractInstanceSectionEntity) -> ContractInstanceSectionEntity) -> Unit,
    onCancel: () -> Unit,
    onSave: (newOrderNo: Int, newTitleBody: String) -> Unit
) {
    // چون SectionCardEdit (نسخه Template) فیلدهای visible/required می‌خواهد، آن‌ها را نادیده می‌گیریم.
    val tLike = remember(section) { section.asTemplateLike() }

    SectionCardEdit(
        section = tLike,
        usedOrderNos = usedOrderNos,
        onChange = { tr ->
            onChange { old ->
                val changed = tr(old.asTemplateLike())
                old.copy(
                    orderNo = changed.orderNo,
                    title = changed.title,
                    body  = changed.body
                )
            }
        },
        onCancel = onCancel,
        onSave = { orderNo, titleBody, _, _ ->
            onSave(orderNo, titleBody)   // فقط همینی که برای Instance داریم
        }
    )
}

@Composable
private fun NoteEditorInlineInstance(
    note: ContractInstanceNoteEntity,
    usedOrderNos: Set<Int>,
    onChange: ((ContractInstanceNoteEntity) -> ContractInstanceNoteEntity) -> Unit,
    onCancel: () -> Unit,
    onSave: (newOrderNo: Int, newTitle: String?, newBody: String) -> Unit
) {
    NoteCardEdit(
        note = note.asTemplateLike(sectionId = note.instanceSectionId),
        usedOrderNos = usedOrderNos,
        onChange = { tr ->
            onChange { old ->
                val changed = tr(old.asTemplateLike(sectionId = old.instanceSectionId))
                old.copy(orderNo = changed.orderNo, title = changed.title, body = changed.body)
            }
        },
        onCancel = onCancel,
        onSave = onSave
    )
}




private fun ContractInstanceSectionEntity.asTemplateLike(): ContractTemplateSectionEntity =
    ContractTemplateSectionEntity(
        id = this.id,
        templateId = 0,              // فقط برای UI اهمیتی ندارد
        orderNo = this.orderNo,
        title = this.title,
        body = this.body,
        isDefaultVisible = true,     // Instance این فیلدها را ندارد → پیش‌فرض
        isRequired = false
    )

private fun ContractInstanceNoteEntity.asTemplateLike(sectionId: Int): ContractTemplateNoteEntity =
    ContractTemplateNoteEntity(
        id = this.id,
        sectionId = sectionId,
        orderNo = this.orderNo,
        title = this.title,
        body = this.body
    )

private fun String.norm() = trim().lowercase()
private fun isContractorRole(role: String) = role.norm().let {
    it.contains("پیمانکار") || it.contains("مجری") || it.contains("contractor") || it.contains("seller") || it.contains("vendor")
}
private fun isBuyerRole(role: String) = role.norm().let {
    it.contains("خریدار") || it.contains("کارفرما") || it.contains("مشتری") || it.contains("employer") || it.contains("buyer") || it.contains("client")
}
