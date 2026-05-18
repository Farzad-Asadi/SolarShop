package com.example.solarShop.ui.questionInfoScreen

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.utils.ConfirmDimmedDialog
import com.example.solarShop.utils.FullscreenImageViewer
import com.example.solarShop.utils.NoteEditorDialog
import com.example.solarShop.utils.TopBarGeneral
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import kotlinx.coroutines.delay
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.io.File

@Composable
fun QuestionInfoScreen(
    vm: QuestionInfoViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onClickOpenQuestionTree: (Int?) -> Unit,
) {
    val ui by vm.uiState.collectAsState()
    val highlightAnswerId by vm.highlightAnswerId.collectAsState()

    val currentQId = ui.currentQuestionEntity?.id

    var latchedDir by remember { mutableStateOf(VisualNavDir.None) }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentQId) {
        // یه مقدار خیلی کوتاه تا transitionSpec ساخته بشه
        // (حتی 0ms هم معمولاً کافیه، ولی این امن‌تره)
        delay(1)
        latchedDir = VisualNavDir.None
    }

    LaunchedEffect(ui.currentQuestionEntity) {
        // اگر سوالی برای نمایش نداریم، یعنی حذف شده/وجود ندارد
        if (ui.isDataLoaded && ui.currentQuestionEntity == null) {
            onBack()   // یا navController.popBackStack()
        }
    }

    LaunchedEffect(currentQId) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }





    Scaffold(
        modifier = Modifier,
        topBar = {
            TopBarGeneral(
                title = "ویرایش سوال و پاسخ های آن",
                onBack = onBack
            )
        },

        ) { innerPadding ->

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

            AnimatedContent(
                modifier = Modifier.padding(innerPadding),
                targetState = currentQId,
                transitionSpec = {
                    val dir = when (latchedDir) {
                        VisualNavDir.ToParentRight -> +1   // از راست بیاد
                        VisualNavDir.ToChildLeft -> -1   // از چپ بیاد
                        else -> 0
                    }

                    if (dir == 0) {
                        (fadeIn(tween(120)) togetherWith androidx.compose.animation.fadeOut(tween(120)))
                            .using(SizeTransform(clip = false))
                    }
                    else {
                        ContentTransform(
                            targetContentEnter =
                            slideInHorizontally(
                                animationSpec = tween(800),
                                initialOffsetX = { full -> dir * full }
                            ) + fadeIn(tween(120)),

                            // 👈 قبلی “نمی‌ره”، همون‌جا می‌مونه
                            initialContentExit = ExitTransition.None,

                            sizeTransform = SizeTransform(clip = false)
                        ).apply {
                            // 👈 جدید روی قبلی
                            targetContentZIndex = 1f
                        }
                    }
                },
                label = "QuestionNav"
            ) { qId ->
                key(qId) {
                    // اینجا دقیقاً همون UI فعلی‌ات، با داده‌های uiState (نه bundle)
                    QuestionContent(
                        answerParent = ui.currentAnswerParentEntity,
                        question = ui.currentQuestionEntity,
                        answersAndAnswerChildren = ui.answersAndAnswerChildren,
                        imagesByAnswerUi = ui.imagesByAnswerUi,
                        isCurrentPage = (qId == currentQId),
                        highlightAnswerId = highlightAnswerId,

                        onImportImage = { aId, src -> vm.importAnswerImage(aId, src) },
                        onQuestionTitleValueChange = { q, t -> vm.updateQuestionTitle(q, t) },
                        onAnswerTitleValueChange = { a, t -> vm.updateAnswerTitle(a, t) },
                        onClickAddAnswer = { q -> vm.addAnswerToQuestion(q?.id, "") },
                        onRemoveImage = { id -> vm.deleteAnswerImageEntity(id) },

                        onClickAnswerParent = { parentAnswer ->
                            latchedDir = VisualNavDir.ToParentRight
                            vm.onClickAnswerParent(parentAnswer)
                        },
                        onClickAnswerChild = { parentAnswerId, childQId ->
                            latchedDir = VisualNavDir.ToChildLeft
                            vm.onClickAnswerChild(parentAnswerId, childQId)
                        },
                        onClickAddChildQuestion = { answerId -> vm.onClickAddChildQuestion(answerId) },

                        onClickDeleteAnswer = { ans -> vm.onClickDeleteAnswer(ans) },
                        onAnswerNoteValueChange = { ans, note -> vm.updateAnswerNote(ans, note) },
                        onReorderImages = { _, newOrder ->
                            vm.onReorderImages(
                                newOrder
                            )
                        },

                        onClickOpenQuestionTree = onClickOpenQuestionTree,
                        onReorderAnswers = { qId, newOrder ->
                            vm.onReorderAnswers(qId, newOrder)
                        },
                        onClickCreateParentForRoot = { rootQId ->
                            latchedDir = VisualNavDir.ToParentRight
                            vm.createParentForRoot(rootQId)
                        },
                        onCreateAnswerCameraTemp = { vm.createAnswerCameraTempUri() },
                        onImportAnswerCameraTemp = { aId, tmpFile -> vm.importAnswerImageFromCameraTemp(aId, tmpFile) },

                        )
                }
                if (qId == null) {
                    // یه جایگزین سبک: لودینگ/صفحه خالی
                    Box(Modifier.fillMaxSize())
                    return@AnimatedContent
                }
            }
        }
    }
}


//region Sheets

@Composable
fun QuestionContent(
    modifier: Modifier = Modifier,
    answerParent: AnswerEntity?,
    imagesByAnswerUi: Map<Int, List<AnswerImageUi>>,
    onImportImage: (answerId: Int, src: Uri) -> Unit,
    question: QuestionEntity?,
    answersAndAnswerChildren: List<Pair<AnswerEntity, QuestionEntity?>>,
    onQuestionTitleValueChange: (questionEntity: QuestionEntity?, title: String) -> Unit,
    onAnswerTitleValueChange: (answerEntity: AnswerEntity?, title: String) -> Unit,
    onClickAddAnswer: (question: QuestionEntity?) -> Unit,
    onRemoveImage: (answerImageEntityId: Int?) -> Unit,
    onClickAnswerParent: (answerParent: AnswerEntity?) -> Unit,
    onClickAnswerChild: (answerId: Int, answerChildId: Int?) -> Unit,
    onClickAddChildQuestion: (answerId: Int) -> Unit,
    onClickDeleteAnswer: (answer: AnswerEntity?) -> Unit,
    onAnswerNoteValueChange: (answerEntity: AnswerEntity?, note: String) -> Unit,
    highlightAnswerId: Int?,
    isCurrentPage: Boolean,
    onClickOpenQuestionTree: (Int?) -> Unit,
    onReorderImages: (answerId: Int, newOrder: List<AnswerImageUi>) -> Unit,
    onReorderAnswers: (questionId: Int, newOrder: List<AnswerEntity>) -> Unit,
    onClickCreateParentForRoot: (rootQuestionId: Int) -> Unit,
    onCreateAnswerCameraTemp: () -> Pair<java.io.File, Uri>,
    onImportAnswerCameraTemp: (answerId: Int, tempFile: java.io.File) -> Unit,
) {
    var fullscreen by remember { mutableStateOf<AnswerFullscreenPayload?>(null) }

    var pendingCameraForAnswerId by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    var pendingTempFile by remember { mutableStateOf<File?>(null) }
    var pendingTempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraController = rememberCameraCaptureLauncher(
        requiredPermissions = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                arrayOf(android.Manifest.permission.CAMERA)
            } else emptyArray()
        },
        createOutputUri = {
            val aId = pendingCameraForAnswerId ?: return@rememberCameraCaptureLauncher null
            val (tmpFile, tmpUri) = onCreateAnswerCameraTemp()
            pendingTempFile = tmpFile
            pendingTempUri = tmpUri
            tmpUri
        },
        onResult = { _, success ->
            val aId = pendingCameraForAnswerId
            val tmp = pendingTempFile

            if (success && aId != null && tmp != null) {
                onImportAnswerCameraTemp(aId, tmp)
            } else {
                tmp?.let { runCatching { it.delete() } }
            }

            pendingCameraForAnswerId = null
            pendingTempFile = null
            pendingTempUri = null
        },
        onMessage = { msg ->
            // همون snackbar/toast خودت
            Log.e("CAMERA", msg)
        }
    )

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            // اگر این صفحه "صفحه فعلی" نیست، هیچ تعاملی نگیرد
            .then(
                if (!isCurrentPage) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                } else Modifier
            )
            // tap روی جای خالی برای clearFocus (همون مرحله قبل)
            .pointerInput(isCurrentPage) {
                if (!isCurrentPage) return@pointerInput
                detectTapGestures(onTap = {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                })
            },
        contentAlignment = Alignment.Center
    ) {

        Surface(
            modifier = modifier
                .fillMaxHeight(0.95f)
                .fillMaxWidth(0.95f),
            tonalElevation = 2.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            shape = CardDefaults.elevatedShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.TopEnd
            ) {
                // ستون اصلی محتوا
                Column(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // ستون اصلی داخل کارت
                    Column(
                        modifier = Modifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // --- ردیف سؤال (بچسب به راست در RTL) ---
                        Row(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            //آنچر سوال
                            ParentQuestionHandle(
                                hasParent = (answerParent != null),
                                onClick = {
                                    val qId = question?.id ?: return@ParentQuestionHandle
                                    if (answerParent != null) onClickAnswerParent(answerParent)
                                    else onClickCreateParentForRoot(qId)   // ✅ ریشه → ساخت والد
                                }
                            )




                            QuestionInfoTab(
                                modifier = Modifier.weight(1f),
                                question = question,
                                onQuestionTitleValueChange = { question, title ->
                                    onQuestionTitleValueChange(
                                        question,
                                        title
                                    )
                                }
                            )

                            // ایکون درخت سوالات
                            val qId = question?.id
                            if (qId != null) {

                                IconButton(
                                    onClick = { onClickOpenQuestionTree(qId) },
                                ) {
                                    Icon(
                                        Icons.Default.AccountTree,
                                        contentDescription = "نمودار درختی"
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            thickness = 2.dp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )

                        //ستون جوابها
                        Row(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width(25.dp))
                            AnswersField(
                                modifier = Modifier.weight(1f),
                                answersAndAnswerChildren = answersAndAnswerChildren,
                                imagesByAnswerUi = imagesByAnswerUi,
                                onImportImage = { answerId, src ->
                                    onImportImage(
                                        answerId,
                                        src
                                    )
                                },
                                onAnswerTitleValueChange = { answerEntity, title ->
                                    onAnswerTitleValueChange(
                                        answerEntity,
                                        title
                                    )
                                },
                                onClickAddAnswer = { onClickAddAnswer(question) },
                                onRemoveImage = { onRemoveImage(it) },
                                onClickAnswerChild = { answerId, childId ->
                                    onClickAnswerChild(
                                        answerId,
                                        childId
                                    )
                                },
                                onClickAddChildQuestion = { onClickAddChildQuestion(it) },
                                onClickDeleteAnswer = { onClickDeleteAnswer(it) },
                                highlightAnswerId = highlightAnswerId,
                                isCurrentPage = isCurrentPage,
                                onOpenImageFullscreen = { answerTitle, images, startIndex ->
                                    fullscreen = AnswerFullscreenPayload(
                                        title = answerTitle,
                                        uris = images.map { it.uri },
                                        startIndex = startIndex
                                    )
                                },
                                onReorderImages = onReorderImages,
                                onAnswerNoteValueChange = { answerEntity, note ->
                                    onAnswerNoteValueChange(answerEntity, note)
                                },
                                onReorderAnswers = { qId, newOrder ->
                                    onReorderAnswers(qId, newOrder)
                                },
                                questionId = question?.id ?: 0,
                                onCreateAnswerCameraTemp = onCreateAnswerCameraTemp,
                                onImportAnswerCameraTemp = onImportAnswerCameraTemp,
                                onRequestCameraForAnswer = { aId ->
                                    pendingCameraForAnswerId = aId
                                    cameraController.launch()
                                }
                            )
                        }


                    }

                }


            }

        }


        // 👇 در انتهای QuestionContent
        fullscreen?.let { payload ->
            FullscreenImageViewer(
                title = payload.title,
                imageUris = payload.uris,
                initialPage = payload.startIndex,
                onClose = { fullscreen = null }
            )
        }

    }

}
//endregion Sheets


//region Components


@Composable
fun QuestionInfoTab(
    modifier: Modifier = Modifier,
    question: QuestionEntity?,
    onQuestionTitleValueChange: (questionEntity: QuestionEntity?, title: String) -> Unit,

    ) {
    var questionTitle by rememberSaveable(question?.id) { mutableStateOf(question?.title.orEmpty()) }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            Modifier
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = questionTitle,
                onValueChange = { questionTitle = it; onQuestionTitleValueChange(question, it) },
                singleLine = true,
                label = { Text("عنوان سوال") },
                placeholder = {
                    Text(
                        "عنوان سوال جدید",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {}
                ),
//                visualTransformation = VisualTransformation.None
            )
        }
    }
}


@Composable
fun AnswersField(
    modifier: Modifier = Modifier,
    answersAndAnswerChildren: List<Pair<AnswerEntity, QuestionEntity?>>,
    imagesByAnswerUi: Map<Int, List<AnswerImageUi>>,
    onImportImage: (answerId: Int, src: Uri) -> Unit,
    onAnswerTitleValueChange: (answerEntity: AnswerEntity?, title: String) -> Unit,
    onClickAddAnswer: () -> Unit,
    onRemoveImage: (answerImageEntityId: Int?) -> Unit,
    onClickAnswerChild: (answerId: Int, answerChildId: Int?) -> Unit,
    onClickAddChildQuestion: (answerId: Int) -> Unit,
    onClickDeleteAnswer: (answer: AnswerEntity?) -> Unit,
    highlightAnswerId: Int?,
    isCurrentPage: Boolean,
    onOpenImageFullscreen: (answerTitle: String, images: List<AnswerImageUi>, startIndex: Int) -> Unit,
    onReorderImages: (answerId: Int, newOrder: List<AnswerImageUi>) -> Unit,
    onReorderAnswers: (questionId: Int, newOrder: List<AnswerEntity>) -> Unit,
    onAnswerNoteValueChange: (answerEntity: AnswerEntity?, note: String) -> Unit,
    questionId: Int?,
    onCreateAnswerCameraTemp: () -> Pair<java.io.File, Uri>,
    onImportAnswerCameraTemp: (answerId: Int, tempFile: java.io.File) -> Unit,
    onRequestCameraForAnswer: (answerId: Int) -> Unit


) {
    val listState = rememberLazyListState()

    //کنترل هایلایت پاسخ
    LaunchedEffect(isCurrentPage, highlightAnswerId, answersAndAnswerChildren) {
        if (!isCurrentPage) return@LaunchedEffect
        val targetId = highlightAnswerId ?: return@LaunchedEffect
        val index = answersAndAnswerChildren.indexOfFirst { it.first.id == targetId }
        if (index < 0) return@LaunchedEffect

        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == index }
        if (!visible) {
            delay(250)
            listState.animateScrollToItem(index)
        }
    }

    // ✅ فقط AnswerEntity ها برای reorder
    val answersOnly =
        remember(answersAndAnswerChildren) { answersAndAnswerChildren.map { it.first } }

    // ✅ لیست لوکال برای UI
    var dragging by remember { mutableStateOf(false) }
    val answersState = remember { mutableStateOf(answersOnly) }

    LaunchedEffect(answersOnly) {
        if (!dragging) answersState.value = answersOnly
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            dragging = true

            val list = answersState.value.toMutableList()
            if (list.isEmpty()) return@rememberReorderableLazyListState

            val fromIdx = from.index
            if (fromIdx !in list.indices) return@rememberReorderableLazyListState  // ✅ جلوی removeAt کرش

            val item = list.removeAt(fromIdx)

            // بعد از remove، size عوض شده؛ پس toIdx باید نسبت به size جدید clamp بشه
            val toIdx = to.index.coerceIn(0, list.size)
            list.add(toIdx, item)

            answersState.value = list
        },
        onDragEnd = { _, _ ->
            dragging = false
            val qId = questionId ?: return@rememberReorderableLazyListState
            onReorderAnswers(qId, answersState.value)
        }
    )



    LazyColumn(
        state = reorderState.listState,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .reorderable(reorderState),
        contentPadding = PaddingValues(2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = answersState.value,
            key = { it.id ?: it.hashCode() }
        ) { answer ->
            val aId = answer.id ?: return@items

            // childQuestion رو از لیست اصلی پیدا کن (چون state فقط answer هاست)
            val childQuestion = remember(answersAndAnswerChildren, aId) {
                answersAndAnswerChildren.firstOrNull { it.first.id == aId }?.second
            }

            val itemsForThisAnswer = remember(imagesByAnswerUi, aId) {
                imagesByAnswerUi[aId] ?: emptyList()
            }

            ReorderableItem(
                state = reorderState,
                key = aId
            ) { isDragging ->

                AnswerWithChildHandleRow(
                    answerAndAnswerChild = answer to childQuestion,
                    imageItems = itemsForThisAnswer,
                    onAnswerTitleValueChange = onAnswerTitleValueChange,
                    onImportImage = onImportImage,
                    onRemoveImage = onRemoveImage,
                    onClickAnswerChild = onClickAnswerChild,
                    onClickAddChildQuestion = onClickAddChildQuestion,
                    onClickDeleteAnswer = onClickDeleteAnswer,
                    isHighlighted = (highlightAnswerId == aId),
                    onOpenImageFullscreen = onOpenImageFullscreen,
                    onReorderImages = { aid, newOrder -> onReorderImages(aid, newOrder) },
                    onAnswerNoteValueChange = onAnswerNoteValueChange,
                    dragHandleModifier = Modifier.detectReorderAfterLongPress(reorderState),
                    onCreateAnswerCameraTemp = onCreateAnswerCameraTemp,
                    onImportAnswerCameraTemp = onImportAnswerCameraTemp,
                    onRequestCameraForAnswer = { aId ->
                        onRequestCameraForAnswer(aId)
                    }

                )
            }
        }

        // افزودن پاسخ (افقی)
        item {
            AddAnswerHorizontalCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClickAddAnswer
            )
        }
    }
}


@Composable
fun AnswerWithChildHandleRow(
    modifier: Modifier = Modifier,
    answerAndAnswerChild: Pair<AnswerEntity, QuestionEntity?>,
    imageItems: List<AnswerImageUi>,
    onAnswerTitleValueChange: (AnswerEntity, String) -> Unit,
    onImportImage: (answerId: Int, src: Uri) -> Unit,
    onRemoveImage: (answerImageEntityId: Int?) -> Unit,
    onClickAnswerChild: (answerId: Int, answerChildId: Int?) -> Unit,
    onClickAddChildQuestion: (answerId: Int) -> Unit,
    onClickDeleteAnswer: (answer: AnswerEntity?) -> Unit,
    isHighlighted: Boolean,
    onOpenImageFullscreen: (answerTitle: String, images: List<AnswerImageUi>, startIndex: Int) -> Unit,
    onReorderImages: (answerId: Int, newOrder: List<AnswerImageUi>) -> Unit,
    onAnswerNoteValueChange: (AnswerEntity, String) -> Unit,
    onCreateAnswerCameraTemp: () -> Pair<java.io.File, Uri>,
    onImportAnswerCameraTemp: (answerId: Int, tempFile: java.io.File) -> Unit,
    onRequestCameraForAnswer: (answerId: Int) -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val answer = answerAndAnswerChild.first
    val childQuestion = answerAndAnswerChild.second
    val aId = answer.id ?: return

    val normalBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.40f)
    var flashPhase by remember(isHighlighted) { mutableStateOf(false) }

    val cardBg by animateColorAsState(
        targetValue = if (flashPhase) highlightBg else normalBg,
        animationSpec = tween(durationMillis = 800),
        label = "answerHighlight"
    )

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            delay(10)
            repeat(4) {
                delay(600)
                flashPhase = !flashPhase
            }
            flashPhase = false
        } else flashPhase = false
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = cardBg
//        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
//            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ✅ محتوا (دو ردیف)
            AnswerContentHorizontal(
                modifier = Modifier.weight(1f),
                answer = answer,
                imageItems = imageItems,
                onAnswerTitleValueChange = onAnswerTitleValueChange,
                onImportImage = onImportImage,
                onRemoveImage = onRemoveImage,
                onClickDeleteAnswer = onClickDeleteAnswer,
                onImageClickFullscreen = { index ->
                    onOpenImageFullscreen(answer.title, imageItems, index)
                },
                onReorderImages = { newOrder -> onReorderImages(aId, newOrder) },
                onAnswerNoteValueChange = onAnswerNoteValueChange,
                onCreateAnswerCameraTemp = onCreateAnswerCameraTemp,
                onImportAnswerCameraTemp = { tempFile -> onImportAnswerCameraTemp(aId, tempFile) },
                onRequestCameraForAnswer = { aId ->
                    onRequestCameraForAnswer(aId)
                }

            )

            // ✅ هدف/هندل سؤال فرزند در «چپ بصری» (RTL => End)
            Column(
                modifier = Modifier
                    .fillMaxHeight(),              // ✅ ستون قد ردیف رو می‌گیره
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // ✅ از بالا بچین
            ) {
                // جابجایی (بالا)
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "جابجایی",
                    modifier = dragHandleModifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(40.dp)) // ✅ فاصله کنترل‌شده

                // هندل پاسخ (پایین‌تر)
                ChildQuestionHandle(
                    filled = (childQuestion != null),
                    onClick = {
                        if (childQuestion != null) onClickAnswerChild(aId, childQuestion.id)
                        else onClickAddChildQuestion(aId)
                    }
                )
            }

        }
    }
}


@Composable
private fun AddAnswerHorizontalCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                drawRoundRect(
                    color = Color.DarkGray,
                    style = Stroke(width = strokeWidth, pathEffect = dash),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "افزودن پاسخ",
                modifier = Modifier.size(42.dp),
                tint = Color.Gray.copy(alpha = 0.45f)
            )
            Spacer(Modifier.width(10.dp))
            Text("افزودن پاسخ", style = MaterialTheme.typography.titleMedium)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerContentHorizontal(
    modifier: Modifier = Modifier,
    answer: AnswerEntity,
    imageItems: List<AnswerImageUi>,
    onAnswerTitleValueChange: (AnswerEntity, String) -> Unit,
    onImportImage: (answerId: Int, src: Uri) -> Unit,
    onRemoveImage: (answerImageEntityId: Int?) -> Unit,
    onClickDeleteAnswer: (answer: AnswerEntity?) -> Unit,
    onImageClickFullscreen: (index: Int) -> Unit,
    onReorderImages: (newOrder: List<AnswerImageUi>) -> Unit,
    onAnswerNoteValueChange: (AnswerEntity, String) -> Unit,
    onCreateAnswerCameraTemp: () -> Pair<java.io.File, Uri>,
    onImportAnswerCameraTemp: (tempFile: java.io.File) -> Unit,
    onRequestCameraForAnswer: (answerId: Int) -> Unit

) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current



    val aId = answer.id ?: return


    var showPickSheet by remember { mutableStateOf(false) }

    val imagesState = remember(aId) { mutableStateOf(imageItems) }  // 👈 از همون اول پر

    var dragging by remember(aId) { mutableStateOf(false) }

    LaunchedEffect(imageItems) {
        if (!dragging) imagesState.value = imageItems
    }


    var answerTitle by rememberSaveable(aId) { mutableStateOf(answer.title ?: "") }

    var noteText by remember(aId, answer.note) {   // فرض: answer.note وجود دارد
        mutableStateOf(answer.note ?: "")
    }
    var showNoteDialog by remember { mutableStateOf(false) }

    // اگر از جایی دیگه note در DB عوض شد، اینجا sync بشه
    LaunchedEffect(answer.note) {
        noteText = answer.note ?: ""
    }

    // 👇 state دیالوگ‌ها
    var showDeleteAnswerDialog by remember { mutableStateOf(false) }
    var showDeleteImageDialog by remember { mutableStateOf(false) }
    var pendingImageToDelete by remember { mutableStateOf<Int?>(null) }
    var showDeleteNoteDialog by remember { mutableStateOf(false) }


    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImportImage(aId, uri)
        }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            dragging = true

            val list = imagesState.value.toMutableList()
            // جابجایی امن
            val item = list.removeAt(from.index)
            list.add(to.index.coerceIn(0, list.size), item)

            imagesState.value = list.toList()
        },
        onDragEnd = { _, _ ->
            // ترتیب نهایی
            onReorderImages(imagesState.value)
            dragging = false
        }
    )



    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // --- ردیف بالا: عنوان + آیکون‌ها ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),   // ✅ هم‌ارتفاع کردن آیتم‌ها
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),              // ✅ ارتفاعش با Row یکی میشه
                contentAlignment = Alignment.CenterStart
            ) {
                OutlinedTextField(
                    value = answerTitle,
                    onValueChange = {
                        answerTitle = it
                        onAnswerTitleValueChange(answer, it)
                    },
                    label = { Text("عنوان پاسخ") },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxSize()  // ✅ کل ارتفاع/عرض این باکس را بگیر
                )

                IconButton(
                    onClick = { showDeleteAnswerDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp)
                        .size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف پاسخ",
                            Modifier.size(22.dp),
                            tint = Color.White
                        )
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف پاسخ",
                            Modifier.size(14.dp),
                            tint = Color.Black
                        )
                    }
                }
            }

            val topAlign = 8.dp

            AnswerNoteSquareBox(
                modifier = Modifier
                    .fillMaxHeight()      // ✅ هم‌ارتفاع با TextField
                    .padding(top = topAlign)
                    .aspectRatio(1f),     // ✅ مربعی
                noteText = noteText,
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                    showNoteDialog = true
                },
                onClickDelete = { showDeleteNoteDialog = true }
            )


        }


        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val list = imagesState.value

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
                contentAlignment = Alignment.CenterEnd // 👈 چسب به راست
            ) {
                LazyRow(
                    state = reorderState.listState,
                    modifier = Modifier
                        .wrapContentWidth(Alignment.End) // 👈 عرض به اندازه محتوا + تراز راست
                        .fillMaxHeight()
                        .reorderable(reorderState)
                        .detectReorderAfterLongPress(reorderState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    reverseLayout = true
                ) {
                    items(
                        items = list,
                        key = { it.id!! }   // ✅ کلید پایدار
                    ) { item ->
                        ReorderableItem(
                            state = reorderState,
                            key = item.id!!
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                if (isDragging) 10.dp else 0.dp,
                                label = "drag-elevation"
                            )

                            // چون itemsIndexed نداریم، index را اینجا حساب می‌کنیم
                            val index = remember(list, item.id) {
                                list.indexOfFirst { it.id == item.id }
                            }

                            AnswerImageThumb(
                                item = item,
                                modifier = Modifier
                                    .shadow(elevation)
                                    .detectReorderAfterLongPress(reorderState),
                                onClick = { if (index >= 0) onImageClickFullscreen(index) },
                                onRequestDelete = {
                                    pendingImageToDelete = item.id
                                    showDeleteImageDialog = true
                                }
                            )
                        }
                    }

//            // باکس افزودن عکس (کوچک)
                    item {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    val dash =
                                        PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                    drawRoundRect(
                                        color = Color.DarkGray,
                                        style = Stroke(width = strokeWidth, pathEffect = dash),
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }
                                .clickable { showPickSheet = true },

                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "افزودن عکس",
                                modifier = Modifier.size(28.dp),
                                tint = Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

        }


    }



    ConfirmDimmedDialog(
        visible = showDeleteAnswerDialog,
        title = "حذف پاسخ",
        message = {
            Column {
                Text("آیا از حذف این پاسخ مطمئن هستید؟")
                Spacer(Modifier.height(4.dp))
                Text("سوالات زیرمجموعه ،بی والد می شوند")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "دقت کنید در سفارشی استفاده نشده باشد ",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        onConfirm = { onClickDeleteAnswer(answer) },
        onDismiss = { showDeleteAnswerDialog = false }
    )



    ConfirmDimmedDialog(
        visible = showDeleteImageDialog,
        title = "حذف عکس",
        message = { Text("آیا از حذف این عکس مطمئن هستید؟") },
        onConfirm = {
            pendingImageToDelete?.let { onRemoveImage(it) }
            pendingImageToDelete = null
        },
        onDismiss = {
            showDeleteImageDialog = false
            pendingImageToDelete = null
        }
    )


    ConfirmDimmedDialog(
        visible = showDeleteNoteDialog,
        title = "حذف توضیحات",
        message = { Text("آیا از حذف توضیحات این پاسخ مطمئن هستید؟") },
        onConfirm = {
            noteText = ""
            onAnswerNoteValueChange(answer, "")
        },
        onDismiss = { showDeleteNoteDialog = false }
    )


    NoteEditorDialog(
        visible = showNoteDialog,
        label = "توضیح در مورد پاسخ",
        initialText = noteText,
        onDismiss = { showNoteDialog = false },
        onSave = { newText ->
            noteText = newText
            onAnswerNoteValueChange(answer, newText)
        }
    )

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
                Text("افزودن تصویر", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = {
                        showPickSheet = false
                        pickImage.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("از گالری") }

                Button(
                    onClick = {
                        showPickSheet = false
                        onRequestCameraForAnswer(aId)

                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("از دوربین") }

                Spacer(Modifier.height(12.dp))
            }
        }
    }


}

@Composable
private fun AnswerNoteSquareBox(
    noteText: String,
    onClick: () -> Unit,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasNote = noteText.isNotBlank()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (hasNote) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val pathEffect =
                    if (hasNote) PathEffect.cornerPathEffect(3f)
                    else PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

                drawRoundRect(
                    color = Color.DarkGray,
                    style = Stroke(width = strokeWidth, pathEffect = pathEffect),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = "توضیحات پاسخ",
            modifier = Modifier
                .size(22.dp),
            tint = Color.Gray.copy(alpha = 0.6f)
        )

        // ❗️آیکون حذف فقط وقتی note داریم
        if (hasNote) {
            IconButton(
                onClick = onClickDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Delete, null, Modifier.size(22.dp), tint = Color.White)
                    Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun ParentQuestionHandle(
    hasParent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (hasParent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.5.dp,
                color = if (hasParent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (hasParent) {
            // فلش به سمت «بالا/والد»
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رفتن به سوال والد",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            // اگر والد ندارد (ریشه)
            Icon(
                imageVector = Icons.Default.Add, // یا Icons.Default.Home
                contentDescription = "ساخت سوال والد جدید",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
private fun ChildQuestionHandle(
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.5.dp,
                color = if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (filled) {
            // ✅ فلشِ آیینه‌ای (به سمت چپ)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward, // یا ArrowForward
                contentDescription = "رفتن به سوال بعدی",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            // ✅ وقتی سوال بعدی ندارد
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "ساخت سوال جدید",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
private fun AnswerImageThumb(
    item: AnswerImageUi,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val req = remember(item.id, item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .size(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = req,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        IconButton(
            onClick = onRequestDelete,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف عکس",
                    Modifier.size(22.dp),
                    tint = Color.White
                )
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف عکس",
                    Modifier.size(14.dp),
                    tint = Color.Black
                )
            }
        }
    }
}





//endregion Components


//region Utils

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    // اگر به سمت جلو می‌رویم، بعد از remove ایندکس‌ها یک پله می‌افتند
    val target = if (to > from) to - 1 else to
    add(target.coerceIn(0, size), item)
}

//endregion Utils
















