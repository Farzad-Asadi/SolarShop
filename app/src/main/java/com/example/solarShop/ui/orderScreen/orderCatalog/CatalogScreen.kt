package com.example.solarShop.ui.orderScreen.orderCatalog

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.ui.orderScreen.QuestionHeaderCard
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.FullscreenImageViewerForCatalog
import kotlinx.coroutines.flow.Flow

@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onOpenQuestionInfo: (Int?) -> Unit,
    onOpenQuestionTree: (qid:Int? ,orderId:Int?) -> Unit,
    vm: CatalogViewModel = hiltViewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()





    CatalogContent(
        modifier = modifier,
        question = ui.currentQuestion,
        answers = ui.currentAnswerItemUnit,
        multiselectAllowed = true, // اگر برای سوال‌های خاص تک‌انتخاب است، false کنید
        isLoading = false,
        totalAnswered = ui.totalAnswered,
        answeredOrder = ui.answeredOrder,
        onBack = onClose,
        onToggleSelect = { answerId, selected ->
            vm.onToggleSelect(
                answerId,
                selected
            )
        },
        onToggleLike = {},
        onNoteChange = { answerId: Int, note: String -> vm.onNoteChange(answerId, note) },
        onClickNext = { vm.onClickNext() },
        onClickPrevious = { vm.onClickPrevious() },
        onOpenQuestionInfo = onOpenQuestionInfo,
        onOpenQuestionTree = {qId ->  onOpenQuestionTree(qId,ui.currentOrderEntity?.id) },
        onClickFirst = { vm.onClickFirst() },   // 👈 جدید
        onClickLast = { vm.onClickLast() },
        onSelectCatalogPhoto = { answerId, selectedPhotoId ->
            vm.onSelectCatalogPhoto(answerId, selectedPhotoId)
        },
        selectedPhotoIdFlow = { answerId ->
            vm.selectedPhotoIdFlow(answerId)
        },
        onClickExportPdf = { vm.exportPdf() }


    )



}



@Composable
fun CatalogContent(
    modifier: Modifier = Modifier,
    question: QuestionEntity?,
    answers: List<AnswerItemUnit>,
    multiselectAllowed: Boolean = true, // اگر برای سوال‌های خاص تک‌انتخاب است، false کنید
    isLoading: Boolean = false,
    totalAnswered: Int?,
    answeredOrder: Int?,
    onBack: () -> Unit,
    onToggleSelect: (answerId: Int, selected: Boolean) -> Unit,
    onToggleLike: (answerId: Int) -> Unit,
    onNoteChange: (answerId: Int, note: String) -> Unit,
    onClickNext: () -> Unit,
    onClickPrevious: () -> Unit,
    onOpenQuestionInfo: (Int?) -> Unit,
    onOpenQuestionTree: (Int?) -> Unit,
    onClickFirst: () -> Unit,   // 👈 جدید
    onClickLast: () -> Unit,
    onSelectCatalogPhoto :(answerId: Int, selectedPhotoId: Int) -> Unit,
    selectedPhotoIdFlow :(answerId: Int) -> Flow<Int?>,
    onClickExportPdf: () -> Unit,

    ) {
    var fullscreen by remember { mutableStateOf<FullscreenPayload?>(null) }
    val hasSelection by remember(answers) { derivedStateOf { answers.any { it.selected } } }
    val hasPrevious by remember(answers) { derivedStateOf { (totalAnswered ?: 1) > 1 } }

    Scaffold(
        topBar = {
            CatalogTopBar(
                totalAnswered = totalAnswered,
                answeredOrder = answeredOrder,
                onClickBack = { onBack() },
                onClickExportPdf = {onClickExportPdf() }
            )
        },
        bottomBar = {
            CatalogBottomBar(
                enabledNext = hasSelection && !isLoading,
                enabledPrevious = hasPrevious,
                totalAnswered = totalAnswered,
                onClickFirst = onClickFirst,
                onClickPrevious = onClickPrevious,
                onClickNext = onClickNext,
                onClickLast = onClickLast,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stickyHeader {

                    // اگر کارت با گوشه‌های گرد می‌خواهی، خود کارت را اینجا صدا بزن
                    QuestionHeaderCard(
                        title = question?.title.orEmpty(),
                        onClickEdit = { onOpenQuestionInfo(question?.id) },
                        onClickOpenTree = { onOpenQuestionTree(question?.id) },
                        modifier = Modifier.zIndex(1f)
                    )

                }
                items(answers, key = { it.answerId }) { item ->
                    AnswerCard(
                        item = item,
                        multiselectAllowed = multiselectAllowed,

                        // 👇 کلیک روی خود عکس → فول‌اسکرین با عکس‌ها + اسلاید آخر = AnswerNote
                        onImageClick = { index ->
                            val uris = item.imageUris.map { Uri.parse(it) }
                            fullscreen = FullscreenPayload(
                                title = item.title,
                                answerId = item.answerId,
                                questionId = question?.id ?: -1,
                                uris = uris,
                                imageIds = item.imageIds,
                                note = item.answerNote,
                                startIndex = index
                            )
                        }
                        ,

                        onToggleSelect = { selected ->
                            onToggleSelect(item.answerId, selected)
                        },
                        onToggleLike = { onToggleLike(item.answerId) },

                        // 👇 این همچنان Note سفارش را ذخیره می‌کند
                        onNoteChange = { noteText ->
                            onNoteChange(item.answerId, noteText)
                        },

                        // 👇 کلیک روی آیکون Note → همان فول‌اسکرین، ولی از صفحه‌ی آخر (نوت) شروع شود
                        onClickOpenFullscreenNote = {
                            val uris = item.imageUris.map { Uri.parse(it) }
                            val noteIndex = uris.size

                            fullscreen = FullscreenPayload(
                                title = item.title,
                                answerId = item.answerId,
                                questionId = question?.id ?: -1,
                                uris = uris,
                                imageIds = item.imageIds,
                                note = item.answerNote,
                                startIndex = noteIndex
                            )
                        }

                    )

                }

                item { Spacer(Modifier.height(64.dp)) } // فضای پایین برای BottomBar
            }
        }
    }

    // Fullscreen Image Viewer
    fullscreen?.let { payload ->
        val selectedPhotoId by selectedPhotoIdFlow(payload.answerId)
            .collectAsStateWithLifecycle(initialValue = null)

        FullscreenImageViewerForCatalog(
            title = payload.title,
            imageUris = payload.uris,
            imageIds = payload.imageIds,
            selectedPhotoId = selectedPhotoId,
            onSelectPhoto = { photoId ->
                onSelectCatalogPhoto(payload.answerId, photoId)
            },
            note = payload.note,
            initialPage = payload.startIndex,
            onClose = { fullscreen = null }
        )
    }



}


@Composable
private fun CatalogTopBar(
    totalAnswered: Int?,
    answeredOrder: Int?,
    onClickBack: () -> Unit,
    onClickExportPdf: () -> Unit,
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
                .padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.padding(end = 22.dp),
                onClick = { onClickBack() },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = "Catalog Q&A")
            }

            Text(
                text = "پرسش و پاسخ ها",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onClickExportPdf,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = "خروجی PDF"
                )
            }

            NumberBadge(
                text = answeredOrder.toString(),
                modifier = Modifier.padding(end = 8.dp)
            )


        }
    }
}




@Composable
private fun CatalogBottomBar(
    enabledNext: Boolean,
    enabledPrevious: Boolean,
    totalAnswered: Int?,
    onClickFirst: () -> Unit,
    onClickPrevious: () -> Unit,
    onClickNext: () -> Unit,
    onClickLast: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        color = BambooTheme.sections.bottomBarContainer,
        contentColor = BambooTheme.sections.bottomBarContent,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(maxHeight = 80.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = onClickFirst,
                enabled = enabledPrevious,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(0.7f),   // 👈 عرض مساوی
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp) // 👈 پدینگ کمتر
            ) {
                Text("اولین")
            }

            Button(
                onClick = onClickPrevious,
                enabled = enabledPrevious,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                if ((totalAnswered)?.minus(1) == 0) {
                    Text("سؤال قبلی")
                } else {
                    Text("سؤال قبلی ${(totalAnswered)?.minus(1)}")
                }
            }

            Button(
                onClick = onClickNext,
                enabled = enabledNext,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("سؤال بعدی")
            }

            Button(
                onClick = onClickLast,
                enabled = enabledNext,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(0.7f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("آخرین")
            }
        }

    }
}


@Composable
private fun NumberBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}


@Composable
private fun AnswerCard(
    item: AnswerItemUnit,
    multiselectAllowed: Boolean,
    onImageClick: (Int) -> Unit,
    onToggleSelect: (Boolean) -> Unit,
    onToggleLike: () -> Unit,
    onNoteChange: (String) -> Unit,
    onClickOpenFullscreenNote: () -> Unit,
) {
    var notesExpanded by remember { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf(item.note) }

    // اگر کارت از حالت انتخاب خارج شد، یادداشت بسته شود
    LaunchedEffect(item.selected) {
        if (!item.selected) notesExpanded = false
    }

    val focusRequester = remember { FocusRequester() }

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
                width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
                ) {

                //چک باکس
                SelectControl(
                    selected = item.selected,
                    multiselectAllowed = multiselectAllowed,
                    onToggle = onToggleSelect
                )

                //عنوان پاسخ
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )



            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ){
                //note
                Box(
                    modifier = Modifier,
                    contentAlignment = Alignment.Center // مشکلی با alignِ داخلی ندارد
                ) {

                    // 👇 آیکون نوت فقط برای «دیدن» نوت
                    val hasAnswerNote = item.answerNote.isNotBlank()
                    IconButton(
                        onClick = onClickOpenFullscreenNote,
                        enabled = hasAnswerNote            // فقط وقتی نوت هست
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description, // یا هر آیکون Note که دوست داری
                            contentDescription = "توضیحات پاسخ",
                            tint = if (hasAnswerNote)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

//                    if (item.answerNote.isNotBlank()) {
//                        Box(
//                            modifier = Modifier
//                                .align(Alignment.CenterEnd)
//                                .padding(end = 4.dp)
//                                .size(10.dp)
//                                .background(
//                                    color = MaterialTheme.colorScheme.error,
//                                    shape = CircleShape
//                                )
//                                .border(
//                                    width = 2.dp,
//                                    color = MaterialTheme.colorScheme.background, // هاله‌ی سفید دور نقطه
//                                    shape = CircleShape
//                                )
//                        )
//                    }
                }


                Spacer(Modifier.width(2.dp))

                //یادداشت
                Box(
                    modifier = Modifier,
                    contentAlignment = Alignment.Center // مشکلی با alignِ داخلی ندارد
                ) {
                    FilterChip(
                        selected = notesExpanded,
                        onClick = { if (item.selected) notesExpanded = !notesExpanded },
                        enabled = item.selected,
                        label = { Text("یادداشت", maxLines = 1) },
                        modifier = Modifier.height(28.dp)
                            .padding(horizontal = 8.dp, vertical = 0.dp),
                    )


                    if (item.note.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(10.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.background, // هاله‌ی سفید دور نقطه
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            // وقتی باز شد، فوکوس بگیرد
            LaunchedEffect(notesExpanded) {
                if (notesExpanded) focusRequester.requestFocus()
            }

            AnimatedVisibility(visible = notesExpanded) {
                NotesArea(
                    note = note,
                    onNoteChange = { note = it;onNoteChange(it) },
                    enabled = item.selected,
                    notesExpanded = notesExpanded,
                    onCloseNotes = { notesExpanded = false },
                    focusRequester = focusRequester
                )
            }

            AnswerImage(
                uri = item.imageUris.firstOrNull(),
                onClick = { onImageClick(0) }
            )


        }
    }
}

@Composable
private fun AssistChipsRow(
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            enabled = enabled,                  // ← کنترل فعال/غیرفعال
            label = { Text("  یادداشت") }
        )
        // رزرو برای آینده: فیلترها/برچسب‌ها
    }
}

@Composable
private fun NotesArea(
    note: String,
    onNoteChange: (String) -> Unit,
    enabled: Boolean,
    notesExpanded: Boolean,
    onCloseNotes: () -> Unit, // بستن و پایان ویرایش
    focusRequester: FocusRequester
) {
    val keyboard = LocalSoftwareKeyboardController.current

    // کانتینر برای راست‌به‌چپ شدن تکست‌فیلد (و placeholder)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // تکست‌فیلد
            OutlinedTextField(
                value = note,
                onValueChange = { onNoteChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    // اینترِ فیزیکی (کیبورد سخت‌افزاری) → بستن
                    .onPreviewKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyUp &&
                            (ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                        ) {
                            keyboard?.hide()
                            onCloseNotes()
                            true
                        } else false
                    },
                placeholder = { Text("یادداشت برای این گزینه…") },
                singleLine = false,
                maxLines = 4,
                enabled = enabled,
                readOnly = !enabled,
                // آی‌اِم‌ای Done (کیبورد لمسی) → بستن
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        onCloseNotes()
                    }
                ),
                // استایل راست‌به‌چپ برای متن
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
                // دکمه تایید داخل خود تکست‌فیلد (سمت راست)
                trailingIcon = {
                    IconButton(
                        onClick = {
                            keyboard?.hide()
                            onCloseNotes()
                        },
                        enabled = enabled
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "تأیید یادداشت")
                    }
                }
            )
        }
    }
}



@Composable
private fun SelectControl(
    selected: Boolean,
    multiselectAllowed: Boolean,
    onToggle: (Boolean) -> Unit
) {
    if (multiselectAllowed) {
        Checkbox(checked = selected, onCheckedChange = onToggle)
    } else {
        RadioButton(selected = selected, onClick = { onToggle(!selected) })
    }
}


@Composable
private fun AnswerImage(uri: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    // داخل HorizontalPager:
    val model = ImageRequest.Builder(LocalContext.current)
        .data(uri) // همین content://
        .crossfade(true)
        .build()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = uri != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}





