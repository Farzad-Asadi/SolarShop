package com.example.solarShop.ui.questionTreeScreen


import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.solarShop.data.room.tables.question_answers.AddChildResult
import com.example.solarShop.data.room.tables.question_answers.DeleteLinkResult
import com.example.solarShop.data.room.tables.question_answers.JoinChildResult
import com.example.solarShop.data.room.tables.question_answers.PromoteRootResult
import com.example.solarShop.data.room.tables.question_answers.SmartDeleteResult
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.question_answers.question.AnswerNextQuestionCrossRef
import com.example.solarShop.data.room.tables.question_answers.question.AnswerWithNextQuestions
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionWithAnswers
import com.example.solarShop.utils.AnchorKey
import com.example.solarShop.utils.AnswerAnchor
import com.example.solarShop.utils.ConfirmDimmedDialog
import com.example.solarShop.utils.LoadingScreen
import com.example.solarShop.utils.QuestionAnchor
import com.example.solarShop.utils.SnackbarController
import com.example.solarShop.utils.TopBarGeneral
import com.example.solarShop.utils.computeLayoutPlan
import com.example.solarShop.utils.ellipsize
import com.example.solarShop.utils.myDrawEdgesOrthogonalStable
import com.example.solarShop.utils.questionWithAnswersData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun QuestionTreeScreen(
    selectedQuestionId: Int?,
    filterOrderId: Int?,
    onClickEditeQuestion: (Int?) -> Unit,
    onBack: () -> Unit,
    onCloseToProfile: () -> Unit,
    vm: QuestionTreeViewModel = hiltViewModel()
) {

    val ui by vm.uiState.collectAsState()
    val viewPrefs by vm.viewPrefs.collectAsState()
    val filterState by vm.filterState.collectAsState()

    val filterLock = filterState != null

    LaunchedEffect(ui.isDataLoaded) {
        if (ui.isDataLoaded) vm.ensureMainRootExists()
    }


    LaunchedEffect(filterOrderId) {
        vm.setFilterOrder(filterOrderId)
    }





    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snack = remember(snackbarHostState, scope) { SnackbarController(snackbarHostState, scope) }



    // دیالوگِ «نیاز به تصمیم» را با این state کنترل می‌کنیم
    var blocked by remember { mutableStateOf<SmartDeleteResult.BlockedMultipleChildren?>(null) }

    // دیالوگ تأیید حذف سؤال
    var pendingDeleteQuestion by remember { mutableStateOf<QuestionEntity?>(null) }


    LaunchedEffect(filterState?.orderId) {
        val orderId = filterState?.orderId ?: return@LaunchedEffect

        snack.show(
            message = "فیلتر انتخاب‌های این سفارش فعال است.",
            actionLabel = "لغو فیلتر",
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite,
            onAction = { vm.clearFilter() }
        )
    }

    var pendingCopy by rememberSaveable {
        mutableStateOf<Pair<Int, Int>?>(null) // (srcQId, newQId)
    }

    // مودهای انتخاب
    var pickingAnswerIdToTargetQuestion by rememberSaveable { mutableStateOf<Int?>(null) }   // انتخاب "سوال مقصد" برای یک پاسخ
    var pickingQuestionIdForFreeAnswer by rememberSaveable { mutableStateOf<Int?>(null) }   // انتخاب "پاسخ آزاد" برای یک سوال
    var pickingQuestionIdToChangeParent by rememberSaveable { mutableStateOf<Int?>(null) }  // انتخاب "پاسخ جدید" برای تغییر والد

    fun clearPickingModes(snackbarHostState: SnackbarHostState) {
        pickingAnswerIdToTargetQuestion = null
        pickingQuestionIdForFreeAnswer = null
        pickingQuestionIdToChangeParent = null
        snackbarHostState.currentSnackbarData?.dismiss()
        Log.i("QTreePick", "clearPickingModes()")
    }

    val isPickingMode =
        pickingAnswerIdToTargetQuestion != null ||
                pickingQuestionIdForFreeAnswer != null ||
                pickingQuestionIdToChangeParent != null

    BackHandler {
        if (isPickingMode) {
            clearPickingModes(snackbarHostState)
        } else {
            onBack()
        }
    }


    // جمع‌کنندهٔ رویدادها از ViewModel
    LaunchedEffect(Unit) {
        launch {
            vm.deleteNeedsResolution.collect { event ->
                if (filterLock) return@collect

                blocked = event
            }
        }

        launch {
            vm.deleteDone.collect { result ->
                if (filterLock) return@collect

                when (result) {
                    SmartDeleteResult.DeletedDirectly -> snack.show(
                        message = "سؤال حذف شد.",
                        actionLabel = "برگرداندن",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                        onAction = { vm.undoLastChange() }
                    )

                    is SmartDeleteResult.RewiredAndDeleted -> snack.show(
                        message = "سوال حذف و والد به سوال فرزند متصل گردید.",
                        actionLabel = "برگرداندن",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                        onAction = { vm.undoLastChange() }
                    )

                    SmartDeleteResult.NoOp -> Unit
                    is SmartDeleteResult.BlockedMultipleChildren -> blocked = result
                }
            }
        }

        launch {
            vm.addChildResult.collect { res ->
                if (filterLock) return@collect

                when (res) {
                    is AddChildResult.BlockedAlreadyHasChild -> snack.show(
                        message = "ابتدا سوال برآمده از این جواب را پاک یا تغییر والد دهید.",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )

                    is AddChildResult.Created -> snack.show(
                        message = "سوال جدید ساخته شد.",
                        actionLabel = "برگرداندن",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                        onAction = { vm.undoLastChange() }
                    )
                }
            }
        }

        launch {
            vm.joinChildResult.collect { res ->
                if (filterLock) return@collect

                when (res) {
                    is JoinChildResult.Joined -> snack.show(
                        message = "اتصال جدید برقرار شد.",
                        actionLabel = "برگرداندن",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                        onAction = { vm.undo(res.undo) }
                    )

                    is JoinChildResult.InvalidSelfParent ->
                        snack.show("نمی‌توان یک پاسخ را والد سؤال خودش کرد.", withDismissAction = true)

                    is JoinChildResult.AlreadyConnected ->
                        snack.show("اتصال قبلاً ایجاد شده.", withDismissAction = true)

                    is JoinChildResult.WouldCreateCycle ->
                        snack.show("این اتصال باعث حلقه (چرخه) در گراف می‌شود.", withDismissAction = true)

                    is JoinChildResult.AnswerAlreadyHasChild ->
                        snack.show("نمی توان یک پاسخ والد چند سوال باشد", withDismissAction = true)
                }
            }
        }

        launch {
            vm.deleteLinkResult.collect { res ->
                if (filterLock) return@collect

                when (res) {
                    is DeleteLinkResult.Deleted -> snack.show(
                        message = "ارتباط با موفقیت پاک شد.",
                        actionLabel = "برگرداندن",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                        onAction = { vm.undoLastChange() }
                    )

                    is DeleteLinkResult.NotFound ->
                        snack.show("این پاسخ هیچ فرزندی ندارد.", withDismissAction = true)

                    is DeleteLinkResult.NeedSelection -> {
                        // اینجا هنوز تصمیم‌گیری UI لازم دارد
                    }
                }
            }
        }

        launch {
            vm.promoteRootResult.collect { res ->
                if (filterLock) return@collect

                when (res) {
                    is PromoteRootResult.Success -> snack.show(
                        message = "سؤال ریشهٔ جدید ساخته شد.",
                        actionLabel = "برگرداندن",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                        onAction = { vm.undoLastChange() }
                    )

                    is PromoteRootResult.AlreadyHasParent ->
                        snack.show("ابتدا والد فعلی این سؤال را حذف یا تغییر دهید.", withDismissAction = true)

                    is PromoteRootResult.Error ->
                        snack.show(res.message, withDismissAction = true)
                }
            }
        }

        launch {
            vm.mainRootEvent.collect { ev ->
                if (filterLock) return@collect

                when (ev) {
                    is MainRootEvent.ChangedByUser -> {
                        snack.show(
                            message = "ریشهٔ اصلی تغییر کرد. از این به بعد سؤال اول کاتالوگ همین سؤال است.",
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite
                        )
                    }
                    is MainRootEvent.Initialized -> {
                        // ❌ هیچ Snackbar نشان نده
                    }
                }
            }
        }

        launch {
            vm.insertIntermediateResult.collect {
                if (filterLock) return@collect

                snack.show(
                    message = "سؤال واسط ساخته شد.",
                    actionLabel = "برگرداندن",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                    onAction = { vm.undoLastChange() }
                )
            }
        }



        launch {
            vm.copyCreated.collect { (newQId, srcQId) ->
                if (filterLock) return@collect
                pendingCopy = srcQId to newQId

                // دقیقا مثل مود اتصال به پاسخ آزاد:
                // این تابع الان داخل QuestionContent تعریف شده.
                // بهتره تبدیلش کنی به یک callback یا state که از بالا کنترل بشه.
            }
        }




    }







    Scaffold(
        modifier = Modifier,
        topBar = {
            TopBarGeneral(
                title = "نمودار درختی سوال و جوابها",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            if (isPickingMode) {
                                clearPickingModes(snackbarHostState)
                            } else {
                                onCloseToProfile()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "بستن")
                    }

                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }

    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopEnd
        ) {

            if (ui.isDataLoaded) {
                QuestionContent(
                    questionWithAnswersList = ui.questionWithAnswersList,
                    answerWithNextQuestionsList = ui.answerWithNextQuestionsList,
                    selectedQuestionId = selectedQuestionId,
                    onClickEditeQuestion = { onClickEditeQuestion(it) },
                    onClickDeleteQuestion = { q ->
                        if (filterLock) return@QuestionContent
                        pendingDeleteQuestion = q
                                            },
                    onClickAnswerAnchorToCreateNewQuestion = {
                        vm.onClickAnswerAnchorToCreateNewQuestion(
                            it
                        )
                    },
                    onClickQuestionAnchorToLinkToExistingQuestion = { pickingTargetForAnswerId, question ->
                        vm.onClickQuestionAnchorToLinkToExistingQuestion(
                            pickingTargetForAnswerId,
                            question
                        )
                    },
                    onClickAnswerAnchorToDeleteExistingRelation = {
                        vm.onClickAnswerAnchorToDeleteExistingRelation(
                            it
                        )
                    },
                    onClickQuestionAnchorToCreateNewQuestion = {
                        vm.onClickQuestionAnchorToCreateNewQuestion(
                            it
                        )
                    },
                    snackbarHostState = snackbarHostState,
                    initialScale = viewPrefs.scale,
                    filterState = filterState,
                    filterLock = filterLock,
                    allAnswerEdges=ui.allAnswerEdges,
                    initialOffset = Offset(viewPrefs.offsetX, viewPrefs.offsetY),
                    onViewportChanged = { s, off -> vm.saveViewPrefs(s, off) },
                    onConnectQuestionToFreeAnswer = { qId, aId ->
                        vm.onClickFreeAnswerToAttachQuestion(
                            freeAnswerId = aId,
                            childQuestionId = qId
                        )
                    },
                    onChangeQuestionParentToExistingAnswer = { qId, aId ->
                        vm.changeQuestionParentToExistingAnswer(qId, aId)
                    },
                    onClickSetAsMainRoot = { vm.setAsMainRoot(it) },
                    onInsertIntermediateAfterAnswer = { answerId ->
                        vm.insertIntermediateAfterAnswer(answerId)
                    },
                    onInsertIntermediateBeforeQuestion = { questionId ->
                        vm.insertIntermediateBeforeQuestion(questionId)
                    },
                    onClickCopyQuestion = { qId -> vm.copyQuestionDetached(qId) },
                    pickingAnswerIdToTargetQuestion = pickingAnswerIdToTargetQuestion,
                    onPickingAnswerIdToTargetQuestionChange = { pickingAnswerIdToTargetQuestion = it },
                    pickingQuestionIdForFreeAnswer = pickingQuestionIdForFreeAnswer,
                    onPickingQuestionIdForFreeAnswerChange = { pickingQuestionIdForFreeAnswer = it },
                    pickingQuestionIdToChangeParent = pickingQuestionIdToChangeParent,
                    onPickingQuestionIdToChangeParentChange = { pickingQuestionIdToChangeParent = it },
                    pendingCopy = pendingCopy,
                    clearPickingModes = { clearPickingModes(it) },
                    onPendingCopyChange = { pendingCopy = it },


                    )
            }
            // TODO {اضافه کردن زمان انیمیشن}
            AnimatedVisibility(
                visible = !ui.isDataLoaded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                ) {
                    LoadingScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }

        // دیالوگ سناریوی ۳ (چند فرزند)
        if (blocked != null) {

            ConfirmDimmedDialog(
                visible = true,
                title = "اتصالات چندگانه",
                message = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("این سؤال به ${blocked!!.childIds.size} سؤال دیگر متصل است:")
                        Text("لطفاً ابتدا در گراف مشخص کنید کدام اتصالات باقی بماند/تغییر کند.")
                    }
                },
                confirmText = "",
                dismissText = "برگشت",
                onConfirm = {
                },
                onDismiss = { blocked = null }
            )

        }

        // دیالوگ تأیید حذف سؤال
        pendingDeleteQuestion.let {  q ->
            if (q != null) {
                ConfirmDimmedDialog(
                    visible = true,
                    title = "حذف سؤال",
                    message = {
                        Column {
                            Text(
                                text = if (q.title.isBlank())
                                    "این سؤال حذف شود؟"
                                else
                                    "آیا از حذف این سؤال مطمئن هستید؟\n«${q.title}»\n"
                            )
                            Text(
                                text = "دقت کنید در سفارشی استفاده نشده باشد ",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onConfirm = {
                        pendingDeleteQuestion = null
                            vm.deleteQuestionSmart(q)
                    },
                    onDismiss = { pendingDeleteQuestion = null }
                )
            }
        }
    }
}


@Composable
private fun QuestionContent(
    questionWithAnswersList: List<QuestionWithAnswers>,
    answerWithNextQuestionsList: List<AnswerWithNextQuestions>,
    selectedQuestionId: Int?,
    onClickEditeQuestion: (qid: Int) -> Unit,
    onClickAnswerAnchorToCreateNewQuestion: (fromAnswerId: Int) -> Unit,
    onClickAnswerAnchorToDeleteExistingRelation: (answer: AnswerEntity) -> Unit,
    onClickQuestionAnchorToLinkToExistingQuestion: (aId: Int?, question: QuestionEntity) -> Unit,
    onClickQuestionAnchorToCreateNewQuestion: (question: QuestionEntity) -> Unit,
    onClickDeleteQuestion: (question: QuestionEntity) -> Unit,
    snackbarHostState: SnackbarHostState,
    filterState: QuestionTreeFilterState?,
    filterLock: Boolean,
    initialScale: Float,
    initialOffset: Offset,
    allAnswerEdges: List<AnswerNextQuestionCrossRef>,
    onViewportChanged: (Float, Offset) -> Unit,
    onConnectQuestionToFreeAnswer: (questionId: Int, answerId: Int) -> Unit,
    onChangeQuestionParentToExistingAnswer: (questionId: Int, answerId: Int) -> Unit,
    onClickSetAsMainRoot: (questionId: Int) -> Unit,
    onInsertIntermediateAfterAnswer: (answerId: Int) -> Unit,
    onInsertIntermediateBeforeQuestion: (questionId: Int) -> Unit,
    onClickCopyQuestion: (questionId: Int) -> Unit,
    pickingAnswerIdToTargetQuestion: Int?,
    onPickingAnswerIdToTargetQuestionChange: (Int?) -> Unit,
    pickingQuestionIdForFreeAnswer: Int?,
    onPickingQuestionIdForFreeAnswerChange: (Int?) -> Unit,
    pickingQuestionIdToChangeParent: Int?,
    onPickingQuestionIdToChangeParentChange: (Int?) -> Unit,
    pendingCopy: Pair<Int, Int>?,                // ✅ جدید
    onPendingCopyChange: (Pair<Int, Int>?) -> Unit,
    clearPickingModes :(snackbarHostState: SnackbarHostState)->Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val filterActive = filterState != null
    val questionsInPath = filterState?.questionIdsInPath ?: emptySet()
    val answersInPath = filterState?.answerIdsInPath ?: emptySet()


    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    // ---- Zoom/Pan  ----
    val scaleEps = 0.0005f
    val offsetEps = 0.5f
    var scale by rememberSaveable { mutableFloatStateOf(initialScale) }
    var offset by remember {
        mutableStateOf(initialOffset)
    }
    val minScale = 0.1f
    val maxScale = 5f


    // فقط یک بار روی سؤال انتخاب‌شده فوکوس کنیم
    var focusApplied by remember { mutableStateOf(false) }


    // ---- anchors  ----
    val anchors = remember { mutableStateMapOf<AnchorKey, Offset>() }
    fun putAnchor(key: AnchorKey, pos: Offset) {
        val old = anchors[key]
        if (old == null || (old - pos).getDistance() > 0.5f) { // کاری که isClose می‌کرد
            anchors[key] = pos
        }
    }

    val registerQuestionAnchor: (Int, Offset) -> Unit = { qId, posInContainer ->
        putAnchor(QuestionAnchor(qId), posInContainer)
    }
    val registerAnswerAnchor: (Int, Offset) -> Unit = { aId, posInContainer ->
        putAnchor(AnswerAnchor(aId), posInContainer)
    }

    // ایدی سوال آیتمی که در حالت ویرایش است
    val editingId by rememberSaveable { mutableStateOf<Int?>(null) }

    // ⬇️ جدید: سوالی که می‌خواهیم به یک "جواب آزاد" وصلش کنیم



    // یال‌ها را از فلو/لیست می‌سازیم
    val edges = remember(allAnswerEdges) {
        allAnswerEdges
            .map { Edge(fromAnswerId = it.answerId, toQuestionId = it.nextQuestionId) }
            .distinct()
    }

    // برای پر/خالی بودن دایره‌ی بالای سؤال
    val questionsWithIncoming = remember(edges) {
        edges.map { it.toQuestionId }.toSet()
    }


    val allAnswerIds = remember(questionWithAnswersList) {
        questionWithAnswersList
            .flatMap { it.answers }
            .mapNotNull { it.id }
            .toSet()
    }

    val answersWithOutgoing = remember(edges) {
        edges.map { it.fromAnswerId }.toSet()
    }

    val freeAnswerIds = remember(allAnswerIds, answersWithOutgoing) {
        allAnswerIds - answersWithOutgoing
    }

    var containerCords by remember { mutableStateOf<LayoutCoordinates?>(null) }


    // 1) برای شمارش، یال‌های تکراری را حذف کن
    val uniqueEdgesForCount = remember(edges) { edges.distinct() }

// 2) parentCount = تعداد Answerهای متمایز که به هر سؤال می‌رسند
    val parentCountByQuestionId: Map<Int, Int> = remember(edges) {
        edges
            .groupBy { it.toQuestionId }
            .mapValues { (_, list) -> list.map { it.fromAnswerId }.toSet().size }
    }

    val describedAnswerIds = filterState?.answerIdsWithChoiceDescription ?: emptySet()



    // برای ساخت زیر‌درخت هر سؤال
    val nextByAnswerId = remember(answerWithNextQuestionsList) {
        answerWithNextQuestionsList.associate { it.answer.id!! to it.nextQuestions }
    }
    val childrenByQuestionId = remember(
        answerWithNextQuestionsList,
        questionWithAnswersList
    ) {
        buildMap {
            for (qwa in questionWithAnswersList) {
                val qId = qwa.question.id ?: continue

                // 👈 دقیقا همان ترتیبی که در UI ستون پاسخ‌ها استفاده می‌کنی
                val answersInVisualOrder = qwa.answers
                    .sortedBy { it.sortOrder }


                val children = answersInVisualOrder.flatMap { a ->
                    nextByAnswerId[a.id] ?: emptyList()
                }

                put(qId, children)
            }
        }
    }


    // مالک هر پاسخ (سؤال والد) مثل قبل
    val answerOwnerQuestionId: Map<Int, Int> = remember(questionWithAnswersList) {
        buildMap {
            questionWithAnswersList.forEach { qwa ->
                val qid = qwa.question.id ?: return@forEach

                qwa.answers.forEach { ans ->
                    val aid = ans.id ?: return@forEach
                    put(aid, qid)
                }
            }
        }
    }

    // 🔹 پلان کلی لایه‌ها و گپ‌ها (خارج از Layout محاسبه می‌شود)
    val layoutPlan = remember(
        questionWithAnswersList,
        childrenByQuestionId,
        edges,
        answerOwnerQuestionId
    ) {
        computeLayoutPlan(
            questionWithAnswersList = questionWithAnswersList,
            childrenByQuestionId = childrenByQuestionId,
            edges = edges,
            answerOwnerQuestionId = answerOwnerQuestionId
        )
    }

    val questionLayer: Map<Int, Int> = layoutPlan.questionLayer
    val longEdges: Set<Edge> = layoutPlan.longEdges


    //ایدی سوالی که برای ایجادلینک جدید پاسخ به سوال انتخاب شده


    // سوالی که می‌خواهیم پاسخ والدش را عوض کنیم


    LaunchedEffect(filterLock) {
        if (filterLock) {
            onPickingAnswerIdToTargetQuestionChange(null)
            onPickingQuestionIdForFreeAnswerChange (null)
            onPickingQuestionIdToChangeParentChange (null)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }


    fun startPickingTargetForAnswer(aId: Int?) {
        if (filterLock) return

        // اگر null شد یعنی باید مود انتخاب رو ببندیم
        if (aId == null) {
            onPickingAnswerIdToTargetQuestionChange(null)
            // اگر اسنک‌باری در حال نمایش است، ببندش
            snackbarHostState.currentSnackbarData?.dismiss()
            return
        }

        // مود انتخاب را فعال کن
        onPickingAnswerIdToTargetQuestionChange(aId)


        // اسنک‌بار راهنما
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "سؤال مقصد را انتخاب کنید",
                actionLabel = "لغو",
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )

            // اگر کاربر «لغو» را زد یا اسنک‌بار dismiss شد → مود انتخاب را ببند
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onPickingAnswerIdToTargetQuestionChange(null)
            }
        }
    }

    fun startPickingFreeAnswerForQuestion(qId: Int?) {
        if (filterLock) return

        if (qId == null) {
            onPickingQuestionIdForFreeAnswerChange (null)
            snackbarHostState.currentSnackbarData?.dismiss()
            return
        }

        onPickingQuestionIdToChangeParentChange (null)
        onPickingAnswerIdToTargetQuestionChange (null)

        onPickingQuestionIdForFreeAnswerChange (qId)

        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "جواب آزاد را انتخاب کنید",
                actionLabel = "لغو",
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )

            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onPickingQuestionIdForFreeAnswerChange  (null)
            }
        }
    }

    fun startChangeParentForQuestion(qId: Int?) {
        if (filterLock) return

        if (qId == null) {
            onPickingQuestionIdToChangeParentChange  (null)
            snackbarHostState.currentSnackbarData?.dismiss()
            return
        }

        // اگر قبلاً در مود انتخاب مقصدِ پاسخ بودیم، ببند
        onPickingAnswerIdToTargetQuestionChange (null)
        onPickingQuestionIdForFreeAnswerChange (null)


        onPickingQuestionIdToChangeParentChange (qId)

        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "پاسخ جدید را انتخاب کنید",
                actionLabel = "لغو",
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )

            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onPickingQuestionIdToChangeParentChange (null)
            }
        }
    }


    // برای فهمیدن اینکه صفحه در حال زوم یا پن هست
    var isInteracting by remember { mutableStateOf(false) }

    // 👇 حالت جزئیات کم: یا در حال ژست هستیم، یا خیلی زوم بیرونیم
    val lowDetail = isInteracting || scale < 0.35f


    LaunchedEffect(scale, offset) {
        isInteracting = true
        delay(150L)         // اگر 60Hz هستی ~7 فریم
        isInteracting = false
    }

    LaunchedEffect(scale, offset, isInteracting) {
        // صبر کن تا کمی از آخرین تغییر بگذرد
        delay(800L)
        // اگر در این فاصله دوباره ژست جدید شروع نشده باشد:
        if (!isInteracting) {
            onViewportChanged(scale, offset)   // 👈 این می‌رود به DataStore
        }
    }


    val questionReacts = remember { mutableStateMapOf<Int, Rect>() }
    val registerQuestionBounds: (Int, Rect) -> Unit = { id, rect -> questionReacts[id] = rect }


    LaunchedEffect(selectedQuestionId, questionReacts.size, containerCords) {
        val qId = selectedQuestionId
        val container = containerCords
        if (qId == null || container == null || focusApplied) return@LaunchedEffect

        val rect = questionReacts[qId] ?: return@LaunchedEffect

        val viewportWidth = container.size.width.toFloat()
        val viewportHeight = container.size.height.toFloat()
        if (viewportWidth == 0f || viewportHeight == 0f) return@LaunchedEffect

        val cardWidth = rect.width
        val cardHeight = rect.height
        if (cardWidth == 0f || cardHeight == 0f) return@LaunchedEffect

        // ۱) فرض می‌گیریم کارت کمی عریض‌تر از چیزی هست که Rect می‌گه
        //    این ضریب عملاً کارت + حواشی (بوردر، آنچر، خط‌ها) رو پوشش می‌ده
        val safetyFactor = 1.10f          // ← اگر باز هم تنگ بود بکنش 1.3f
        val effectiveCardWidth = cardWidth * safetyFactor

        // ۲) چه‌قدر از عرض صفحه رو کارت بگیره؟
        //    مثلاً ۶۵٪ → حدود ۱۷٪ مارجین هر طرف
        val visibleWidthFraction = 0.65f  // ← قبلاً 0.8f بود، خیلی تنگ بود

        // فقط بر اساس عرض حساب می‌کنیم
        val rawTargetScale = (viewportWidth * visibleWidthFraction) / effectiveCardWidth

        val targetScale = rawTargetScale
            .coerceAtMost(maxScale)   // فقط سقف، حداقل نذار

        // مرکز ویوپورت
        val viewportCenter = Offset(
            x = viewportWidth / 2f,
            y = viewportHeight / 2f
        )

        // مرکز کارت در مختصات کانتینر
        val cardCenter = rect.center

        scale = targetScale
        offset = viewportCenter - cardCenter * targetScale

        focusApplied = true
    }



    LaunchedEffect(pendingCopy) {
        val pc = pendingCopy ?: return@LaunchedEffect
        val (_, newQId) = pc

        // ✅ هر اسنک‌بار قبلی را فوراً ببند تا صف نشود
        snackbarHostState.currentSnackbarData?.dismiss()

        // مود انتخاب پاسخ آزاد برای سوال جدید
        startPickingFreeAnswerForQuestion(newQId)

        // اسنک‌بار دائمی
        val res = snackbarHostState.showSnackbar(
            message = "این سوال جدید را فرزند کدام پاسخ می‌کنید؟",
            actionLabel = "لغو",
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite
        )

        // لغو شد
        if (res == SnackbarResult.ActionPerformed || res == SnackbarResult.Dismissed) {
            onPendingCopyChange(null)
            clearPickingModes(snackbarHostState) // همون تابعی که در قدم ۲ ساختی
            Log.i("QTreeCopy", "copy attach canceled")
        }
    }


    val freeAnswerIdsForPicking = remember(freeAnswerIds, pickingQuestionIdForFreeAnswer, answerOwnerQuestionId) {
        val pickingQId = pickingQuestionIdForFreeAnswer
        if (pickingQId == null) freeAnswerIds
        else freeAnswerIds.filter { aid -> answerOwnerQuestionId[aid] != pickingQId }.toSet()
    }



    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, _ ->
                    val old = scale
                    val new = (old * zoom).coerceIn(minScale, maxScale)
                    val appliedZoom = new / old
                    val newOffset = centroid - (centroid - offset) * appliedZoom
                    val finalOffset = newOffset + pan

                    if (abs(new - scale) > scaleEps) scale = new
                    if ((finalOffset - offset).getDistance() > offsetEps) offset = finalOffset
                }
            }
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f) // 👈 خیلی مهم
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .onGloballyPositioned { cords ->
                containerCords = cords
            }

    ) {

        Layout(
            content = {
                questionWithAnswersList.forEach { qwa ->
                    val qId = qwa.question.id ?: return@forEach

                    val parentCount = parentCountByQuestionId[qId] ?: 0
                    val canChangeSingleParent = parentCount == 1

                    Log.i("TEST","parents${qwa.question.title}=${parentCountByQuestionId[qId] ?: 0}")

                    val editeMode = editingId == qId

                    key(qId) {
                        Box(modifier = Modifier.questionWithAnswersData(qwa)) {
                            QuestionWithAnswersUnit(
                                questionEntity = qwa.question,
                                answerEntityList = qwa.answers,
                                onClickEditeQuestion = { onClickEditeQuestion(it) },
                                isHighlighted = (selectedQuestionId != null && selectedQuestionId == qId),
                                editeMode = editeMode,
                                registerQuestionBounds = registerQuestionBounds,
                                containerCords = containerCords,
                                lowDetail = lowDetail,
                                questionFilled = qId in questionsWithIncoming,
                                answersFilled = answersWithOutgoing,
                                registerQuestionAnchor = registerQuestionAnchor,
                                onClickAnswerAnchorToPickingAnswerId = { aId ->
                                    startPickingTargetForAnswer(aId)
                                },
                                pickedAnswerIdToTargetQuestion = pickingAnswerIdToTargetQuestion,
                                registerAnswerAnchor = registerAnswerAnchor,
                                onClickAnswerAnchorToCreateNewQuestion = {
                                    it.id?.let { it1 ->
                                        onClickAnswerAnchorToCreateNewQuestion(it1)
                                    }
                                },
                                onClickQuestionAnchorToLinkToExistingQuestion = { question ->
                                    onClickQuestionAnchorToLinkToExistingQuestion(
                                        pickingAnswerIdToTargetQuestion,
                                        question
                                    )
                                },
                                onClickAnswerAnchorToDeleteExistingRelation = {
                                    onClickAnswerAnchorToDeleteExistingRelation(it)
                                },
                                onClickQuestionAnchorToCreateNewQuestion = {
                                    onClickQuestionAnchorToCreateNewQuestion(it)
                                },
                                onClickDeleteQuestion = { onClickDeleteQuestion(it) },
                                filterActive = filterActive,
                                filterLock = filterLock,
                                inFilterPath = (!filterActive || qId in questionsInPath),
                                answersInPath = answersInPath,
                                onClickQuestionAnchorToStartPickingFreeAnswer = { _ ->
                                    startPickingFreeAnswerForQuestion(qId)
                                },
                                pickingQuestionIdForFreeAnswer = pickingQuestionIdForFreeAnswer,
                                freeAnswerIds = freeAnswerIdsForPicking,
                                onFreeAnswerPicked = { ans ->
                                    val childQId = pickingQuestionIdForFreeAnswer
                                        ?: return@QuestionWithAnswersUnit
                                    onPickingQuestionIdForFreeAnswerChange (null)
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    ans.id?.let { aId ->
                                        onConnectQuestionToFreeAnswer(childQId, aId)
                                    }
                                    onPendingCopyChange(null)
                                },
                                isRootQuestion = qId !in questionsWithIncoming,
                                onClickQuestionAnchorToChangeParentStart = { qIdToChange ->
                                    startChangeParentForQuestion(qIdToChange)
                                },
                                pickingQuestionIdToChangeParent = pickingQuestionIdToChangeParent,
                                onAnswerPickedAsNewParent = { ans ->
                                    val childQId = pickingQuestionIdToChangeParent ?: return@QuestionWithAnswersUnit

                                    onPickingQuestionIdToChangeParentChange (null)
                                    snackbarHostState.currentSnackbarData?.dismiss()

                                    ans.id?.let { aId ->
                                        onChangeQuestionParentToExistingAnswer(childQId, aId)
                                    }
                                },
                                canChangeSingleParent = canChangeSingleParent,
                                describedAnswerIds=describedAnswerIds,
                                isMainRoot=qwa.question.isRoot,
                                onClickSetAsMainRoot= onClickSetAsMainRoot,
                                onInsertIntermediateAfterAnswer = onInsertIntermediateAfterAnswer,
                                onInsertIntermediateBeforeQuestion = onInsertIntermediateBeforeQuestion,
                                onClickCopyQuestion=onClickCopyQuestion,

                            )
                        }
                    }
                }
            },
            modifier = Modifier
        ) { measurables, constraints ->

            // ۱) اندازه‌گیری همه‌ی سوال‌ها
            val placeables: Map<Int, Pair<Placeable, QuestionWithAnswers>> =
                buildMap(measurables.size) {
                    for (m in measurables) {
                        val qwa = m.parentData as QuestionWithAnswers
                        val id = qwa.question.id ?: continue
                        put(id, m.measure(constraints) to qwa)
                    }
                }

            // لایه‌ها از پلان موجود
            val allQuestionIds = placeables.keys.toSet()
            val finalLayers: List<List<Int>> =
                layoutPlan.orderedLayers
                    .map { layer ->
                        layer.filter { it in allQuestionIds }.distinct()
                    }
                    .filter { it.isNotEmpty() }
                    .ifEmpty {
                        listOf(allQuestionIds.toList().sorted())
                    }

            val finalLayers2: List<List<Int>> = run {
                val pc = pendingCopy ?: return@run finalLayers
                val (srcId, newId) = pc

                // اگر هنوز توی placeables نیست، بیخیال
                if (!allQuestionIds.contains(srcId) || !allQuestionIds.contains(newId)) return@run finalLayers

                // 1) newId را از هر لایه‌ای حذف کن
                val removed = finalLayers.map { layer -> layer.filterNot { it == newId } }.toMutableList()

                // 2) newId را “همان لایه‌ی srcId” و “بعد از srcId” قرار بده
                val li = removed.indexOfFirst { it.contains(srcId) }
                if (li == -1) return@run finalLayers

                val layer = removed[li].toMutableList()
                val idx = layer.indexOf(srcId)
                layer.add(idx + 1, newId)
                removed[li] = layer

                removed
            }


            // فاصله‌ها
            val vSpacing = 64   // فاصله عمودی بین سوال‌های یک ستون
            val hSpacing = 164  // فاصله افقی بین ستون‌ها (لایه‌ها)

            // عرض هر ستون = بیشترین عرض کارت‌های آن لایه
            val columnWidths = finalLayers2.map { layerIds ->
                if (layerIds.isEmpty()) 0
                else layerIds.maxOf { id -> placeables[id]!!.first.width }
            }

            // ارتفاع هر ستون = مجموع ارتفاع کارت‌ها + فاصله‌ها
            val columnHeights = finalLayers2.map { layerIds ->
                if (layerIds.isEmpty()) 0
                else {
                    val totalCardsHeight = layerIds.sumOf { id -> placeables[id]!!.first.height }
                    val gapsHeight = vSpacing * (layerIds.size - 1)
                    totalCardsHeight + gapsHeight
                }
            }

            // اندازه نهایی لایه‌بندی
            val layoutHeight = columnHeights.maxOrNull() ?: 0
            val totalColumnsWidth = if (finalLayers2.isEmpty()) 0 else {
                val sumWidths = columnWidths.sum()
                val gaps = hSpacing * (finalLayers2.size - 1)
                sumWidths + gaps
            }
            val layoutWidth = totalColumnsWidth

            // محاسبه موقعیت‌ها (درخت از راست → چپ)
            val positions = mutableMapOf<Int, IntOffset>()

            // xRight از انتهای راست شروع می‌کنیم و به چپ می‌آییم
            var xRight = layoutWidth

            finalLayers2.forEachIndexed { li, layerIds ->
                val colWidth = columnWidths[li]
                val colHeight = columnHeights[li]

                // این ستون کجای عمود صفحه باشد → وسط عمودی
                var yTop = ((layoutHeight - colHeight) / 2).coerceAtLeast(0)

                // x ستون: از راست به چپ
                val colLeft = xRight - colWidth

                layerIds.forEach { id ->
                    val p = placeables[id]!!.first
                    val x = colLeft + (colWidth - p.width) / 2  // کارت در ستون وسط‌چین افقی
                    val y = yTop

                    positions[id] = IntOffset(x, y)

                    yTop += p.height + vSpacing
                }

                xRight = colLeft - hSpacing
            }

            // رسم همه کارت‌ها
            layout(layoutWidth, layoutHeight) {
                positions.forEach { (id, offset) ->
                    placeables[id]!!.first.place(offset)
                }
            }
        }


        // یال‌هایی که در مسیر انتخاب‌های سفارش هستند
        val highlightedEdges: Set<Edge> =
            if (!filterActive) emptySet()
            else edges.filter { e -> e.fromAnswerId in answersInPath }.toSet()


        val lowDetailEdges = scale < 0.35f
        // 2) اورلی خط‌ها: بالاتر از همه (zIndex بالا)
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .zIndex(10f)    // 👈 تضمینِ روی همه بودن
        ) {
            myDrawEdgesOrthogonalStable(
                edges = edges,
                anchors = anchors,
                questionRects = questionReacts,
                isInteracting = isInteracting,
                isRtl = isRtl,
                longEdges = longEdges,
                questionLayer = questionLayer,
                answerOwnerQuestionId = answerOwnerQuestionId,
                lowDetail = lowDetailEdges,
                highlightEdges = highlightedEdges,          // 👈 جدید
                dimNonHighlighted = filterActive
            )

        }


    }


}



@Composable
fun QuestionWithAnswersUnit(
    questionEntity: QuestionEntity,
    answerEntityList: List<AnswerEntity>,
    onClickEditeQuestion: (qid: Int) -> Unit,
    onClickAnswerAnchorToCreateNewQuestion: (answer: AnswerEntity) -> Unit,
    onClickAnswerAnchorToDeleteExistingRelation: (answer: AnswerEntity) -> Unit,
    onClickQuestionAnchorToLinkToExistingQuestion: (question: QuestionEntity) -> Unit,
    onClickQuestionAnchorToCreateNewQuestion: (question: QuestionEntity) -> Unit,
    onClickAnswerAnchorToPickingAnswerId: (aId: Int?) -> Unit,
    pickedAnswerIdToTargetQuestion: Int?,
    isHighlighted: Boolean,
    editeMode: Boolean,
    registerQuestionBounds: (Int, Rect) -> Unit,
    containerCords: LayoutCoordinates?,
    questionFilled: Boolean,
    filterActive: Boolean,
    filterLock: Boolean,
    inFilterPath: Boolean,
    answersInPath: Set<Int?>,
    lowDetail: Boolean,
    canChangeSingleParent: Boolean,
    answersFilled: Set<Int>,
    registerQuestionAnchor: (questionId: Int, center: Offset) -> Unit,
    registerAnswerAnchor: (answerId: Int, center: Offset) -> Unit,
    onClickDeleteQuestion: (question: QuestionEntity) -> Unit,
    onClickQuestionAnchorToStartPickingFreeAnswer: (questionId: Int) -> Unit,
    pickingQuestionIdForFreeAnswer: Int?,
    freeAnswerIds: Set<Int>,
    onFreeAnswerPicked: (AnswerEntity) -> Unit,
    isRootQuestion: Boolean,
    onClickQuestionAnchorToChangeParentStart: (questionId: Int) -> Unit,
    pickingQuestionIdToChangeParent: Int?,
    describedAnswerIds:Set<Int>,
    onAnswerPickedAsNewParent: (AnswerEntity) -> Unit,
    onInsertIntermediateAfterAnswer: (answerId: Int) -> Unit,
    onInsertIntermediateBeforeQuestion: (questionId: Int) -> Unit,
    isMainRoot: Boolean,
    onClickSetAsMainRoot: (questionId: Int) -> Unit,
    onClickCopyQuestion: (questionId: Int) -> Unit,

    modifier: Modifier = Modifier
) {
    val qId = questionEntity.id ?: return
    val sortedAnswers = remember(answerEntityList) {
        answerEntityList.sortedBy { it.sortOrder }
    }


    val highlightAnim = remember { Animatable(0f) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            highlightAnim.snapTo(0f)
            repeat(4) {
                highlightAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 280)
                )
                highlightAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 280)
                )
            }
        } else {
            highlightAnim.snapTo(0f)
        }
    }

    val dimFactor = if (!filterActive || inFilterPath) 1f else 0.18f
    val cardElevation = if (lowDetail) 0.dp else 8.dp

    val baseBorderColor: Color =
        if (editeMode) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant

    val flashColor = MaterialTheme.colorScheme.tertiary
    val t = highlightAnim.value

    val animatedColor =
        if (lowDetail) Color.Transparent
        else if (t > 0f) {
            Color(
                red = baseBorderColor.red * (1f - t) + flashColor.red * t,
                green = baseBorderColor.green * (1f - t) + flashColor.green * t,
                blue = baseBorderColor.blue * (1f - t) + flashColor.blue * t,
                alpha = 1f
            )
        } else {
            baseBorderColor
        }

    val baseWidth = if (editeMode) 4.dp else 1.dp
    val pulseExtra = if (lowDetail) 0.dp else (2.dp * t)

    val cardBorder = if (lowDetail) {
        BorderStroke(0.dp, Color.Transparent)
    } else {
        BorderStroke(width = baseWidth + pulseExtra, color = animatedColor)
    }

    var unitMenuExpanded by remember { mutableStateOf(false) }
    var questionMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .onGloballyPositioned { childCords ->
                containerCords?.let { cont ->
                    val r = cont.localBoundingBoxOf(childCords, clipBounds = false)
                    registerQuestionBounds(qId, r)
                }
            }
            // دیگه wrapContentSize(unbounded = true) نداریم
            .animateContentSize(),
        tonalElevation = cardElevation,
        border = cardBorder,
        shape = CardDefaults.elevatedShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = dimFactor),
        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = dimFactor)
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


                // Box(
                //     modifier = Modifier
                //         .width(120.dp)
                //         .height(0.dp)
                // )

                // ستون اصلی داخل کارت
                Column(
                    modifier = Modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {

                    // --- ردیف سؤال  ---
                    Row(
                        modifier = Modifier
                            .sizeIn(maxWidth = 220.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // هندل سؤال (در RTL به‌صورت طبیعی سمت راست می‌ایستد)
                        Box {
                            AnchorHandle(
                                enabled = !filterLock,
                                filled = questionFilled,
                                onClick = {
                                    if (filterLock) return@AnchorHandle
                                    if (pickedAnswerIdToTargetQuestion != null) {
                                        onClickQuestionAnchorToLinkToExistingQuestion(questionEntity)
                                        // ✅ اول مود انتخاب رو ببند و اسنک‌بار راهنما رو kill کن
                                        onClickAnswerAnchorToPickingAnswerId(null)      // یعنی همون startPickingTargetForAnswer(null)


                                    } else {
                                        // ✅ قبل از باز کردن منو، همه مودها را ببند
                                        onClickAnswerAnchorToPickingAnswerId(null)
                                        // این دوتا را هم باید از بیرون پاس بدهی یا همانجا کنترل کنی:
                                        // pickingQuestionIdForFreeAnswer = null
                                        // pickingQuestionIdToChangeParent = null

                                        questionMenuExpanded = true
                                    }
                                },
                                onLongPress = { },
                                onPositioned = { childCords ->
                                    containerCords?.let { cont ->
                                        val centerInChild = Offset(
                                            x = childCords.size.width / 2f,
                                            y = childCords.size.height / 2f
                                        )
                                        val centerInContainer =
                                            cont.localPositionOf(childCords, centerInChild)
                                        registerQuestionAnchor(qId, centerInContainer)
                                    }
                                },
                                selectQuestionEvent =
                                pickedAnswerIdToTargetQuestion != null ||
                                        pickingQuestionIdToChangeParent == qId

                            )

                            DropdownMenu(
                                expanded = questionMenuExpanded,
                                onDismissRequest = { questionMenuExpanded = false },
                                offset = DpOffset(x = 0.dp, y = 0.dp)
                            ) {
                                DropdownMenuItem(
                                    enabled = isRootQuestion,
                                    text = {
                                        Text(
                                            "اتصال به پاسخ جدید",
                                            color = if (isRootQuestion)
                                                MaterialTheme.colorScheme.onSurface
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    },
                                    onClick = {
                                        if (!isRootQuestion) return@DropdownMenuItem
                                        questionMenuExpanded = false
                                        onClickQuestionAnchorToCreateNewQuestion(questionEntity)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("اتصال به پاسخ آزاد") },
                                    onClick = {
                                        questionMenuExpanded = false
                                        onClickQuestionAnchorToStartPickingFreeAnswer(qId)
                                    }
                                )

                                val enableInsertIntermediateBeforeQuestion =
                                    !filterLock && !isRootQuestion && canChangeSingleParent


                                DropdownMenuItem(
                                    enabled = enableInsertIntermediateBeforeQuestion,
                                    text = {
                                        Text(
                                            "اضافه کردن سوال واسط",
                                            color = if (enableInsertIntermediateBeforeQuestion)
                                                MaterialTheme.colorScheme.onSurface
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    },
                                    onClick = {
                                        if (!enableInsertIntermediateBeforeQuestion) return@DropdownMenuItem
                                        questionMenuExpanded = false
                                        onInsertIntermediateBeforeQuestion(qId)
                                    }
                                )


                                val enableChangeParent = (!isRootQuestion && canChangeSingleParent)

                                DropdownMenuItem(
                                    enabled = enableChangeParent,
                                    text = {
                                        Text(
                                            "تغییر یگانه پاسخ والد",
                                            color = if (enableChangeParent)
                                                MaterialTheme.colorScheme.onSurface
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    },
                                    onClick = {
                                        if (!enableChangeParent) return@DropdownMenuItem
                                        questionMenuExpanded = false
                                        onClickQuestionAnchorToChangeParentStart(qId)
                                    }
                                )


                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // عنوان سؤال
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 140.dp, maxWidth = 140.dp)
                                .clickable(enabled = editeMode) { /* بعداً ادیت inline */ }
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = dimFactor),
                                    RoundedCornerShape(6.dp)
                                )
                                .then(
                                    if (lowDetail) Modifier
                                    else Modifier.border(
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary
                                        ),
                                        RoundedCornerShape(6.dp)
                                    )
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 1.dp)
                                    .widthIn(min = 44.dp, max = 140.dp)
                                    .wrapContentHeight(),
                                text = questionEntity.title.ellipsize(17),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme
                                    .onPrimaryContainer
                                    .copy(alpha = dimFactor),
                            )
                        }

                        Spacer(Modifier.width(40.dp))
                    }

                    HorizontalDivider(
                        thickness = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(160.dp)
                    )


                    // --- ستون پاسخ‌ها و ریشه اصلی  ---
                    Row(
                        modifier = Modifier
                            .sizeIn(maxWidth = 220.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        //ایکون سوال ریشه اصلی
                        Column(
                            modifier = Modifier.sizeIn(minWidth = 40.dp, maxWidth = 40.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isRootQuestion) {
                                val enabledSetRoot = !filterLock && !isMainRoot

                                IconButton(
                                    onClick = {
                                        if (!enabledSetRoot) return@IconButton
                                        onClickSetAsMainRoot(qId)
                                    },
                                    enabled = enabledSetRoot
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "ریشه اصلی",
                                        tint = if (isMainRoot)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    )
                                }
                            }

                        }

                        //پاسخ ها و هندلشان
                        Column(
                            modifier = Modifier
                                .sizeIn(minWidth = 180.dp, maxWidth = 180.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = dimFactor),
                                    RoundedCornerShape(6.dp)
                                )
                                .then(
                                    if (lowDetail) Modifier
                                    else Modifier.border(
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = dimFactor)
                                        ),
                                        RoundedCornerShape(6.dp)
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            sortedAnswers.forEach { ans ->
                                val aId = ans.id ?: return@forEach

                                key(aId) {
                                    var answerMenuExpanded by rememberSaveable(aId) {
                                        mutableStateOf(
                                            false
                                        )
                                    }

                                    val answerInPath = !filterActive || (aId in answersInPath)

                                    val isPickFreeAnswerMode =
                                        pickingQuestionIdForFreeAnswer != null && aId in freeAnswerIds

                                    val isChangeParentMode =
                                        pickingQuestionIdToChangeParent != null


                                    val baseDim = if (answerInPath) 1f else 0.18f
                                    val answerDim = if (isPickFreeAnswerMode) 1f else baseDim



                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 2.dp)
                                            .sizeIn(minWidth = 160.dp, maxWidth = 160.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        // چیپ پاسخ
                                        Box(
                                            modifier = Modifier

                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(
                                                        alpha = answerDim
                                                    ),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .then(
                                                    if (lowDetail) Modifier
                                                    else Modifier.border(
                                                        BorderStroke(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.secondary.copy(
                                                                alpha = answerDim
                                                            )
                                                        ),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                )
                                                .padding(horizontal = 2.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // حداقل عرض برای چیپ
                                            Box(
                                                modifier = Modifier
                                                    .width(40.dp)
                                                    .height(0.dp)
                                            )

                                            // ✅ اگر در حالت فیلتر هستیم و این Answer نوت دارد، آیکون را نشان بده
                                            if (filterActive && aId in describedAnswerIds) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd) // ✅ End در RTL یعنی سمت چپ بصری (انتهای متن RTL)
                                                        .padding(start = 4.dp)      // فاصله از متن
                                                        .size(14.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.tertiary.copy(
                                                                alpha = 0.25f
                                                            ),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.EditNote, // یا EditNote
                                                        contentDescription = "یادداشت انتخاب",
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.tertiary
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.EditNote, // یا EditNote
                                                        contentDescription = "یادداشت انتخاب",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }


                                            Text(
                                                modifier = Modifier
                                                    .sizeIn(minWidth = 120.dp, maxWidth = 120.dp),
                                                text = ans.title.ellipsize(17),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme
                                                    .onSecondaryContainer
                                                    .copy(alpha = answerDim)
                                            )
                                        }

                                        Spacer(Modifier.width(15.dp))

                                        // هندل پاسخ (در RTL سمت چپِ ردیف می‌ایستد)
                                        Box {
                                            AnchorHandle(
                                                enabled = !filterLock,
                                                filled = aId in answersFilled,
                                                onClick = {
                                                    if (filterLock) return@AnchorHandle
                                                    when {
                                                        // 1️⃣ اتصال به پاسخ آزاد
                                                        isPickFreeAnswerMode -> {
                                                            onFreeAnswerPicked(ans)
                                                        }

                                                        // 2️⃣ تغییر پاسخ والد
                                                        isChangeParentMode -> {
                                                            onAnswerPickedAsNewParent(ans)
                                                        }

                                                        // 3️⃣ حالت عادی
                                                        else -> {
                                                            answerMenuExpanded = true
                                                        }
                                                    }
                                                },
                                                onLongPress = { },
                                                onPositioned = { childCords ->
                                                    containerCords?.let { cont ->
                                                        val centerInChild = Offset(
                                                            x = childCords.size.width / 2f,
                                                            y = childCords.size.height / 2f
                                                        )
                                                        val centerInContainer =
                                                            cont.localPositionOf(
                                                                childCords,
                                                                centerInChild
                                                            )
                                                        registerAnswerAnchor(aId, centerInContainer)
                                                    }
                                                },
                                                // کمی دورش را پررنگ‌تر کنیم وقتی کاندیداست
                                                selectQuestionEvent = isPickFreeAnswerMode || isChangeParentMode

                                            )

                                            val hasChild = (aId in answersFilled) // answersFilled همون answersWithOutgoing هست
                                            val canAttachChild = !filterLock && !hasChild

                                            DropdownMenu(
                                                expanded = answerMenuExpanded,
                                                onDismissRequest = { answerMenuExpanded = false },
                                                offset = DpOffset(x = 0.dp, y = 0.dp)
                                            ) {
                                                DropdownMenuItem(
                                                    enabled = canAttachChild,
                                                    text = { Text("اتصال به سؤال جدید") },
                                                    onClick = {
                                                        answerMenuExpanded = false
                                                        onClickAnswerAnchorToCreateNewQuestion(ans)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    enabled = canAttachChild,
                                                    text = { Text("اتصال به سؤال موجود") },
                                                    onClick = {
                                                        answerMenuExpanded = false
                                                        onClickAnswerAnchorToPickingAnswerId(aId)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    enabled = !filterLock && hasChild, // پاک کردن اتصال وقتی بچه هست معنی دارد
                                                    text = { Text("پاک کردن اتصال موجود") },
                                                    onClick = {
                                                        answerMenuExpanded = false
                                                        onClickAnswerAnchorToDeleteExistingRelation(
                                                            ans
                                                        )
                                                    }
                                                )

                                                val hasChild = (aId in answersFilled) // چون answersFilled = answersWithOutgoing
                                                val enableInsertIntermediateAfterAnswer = !filterLock && hasChild

                                                DropdownMenuItem(
                                                    enabled = enableInsertIntermediateAfterAnswer,
                                                    text = {
                                                        Text(
                                                            "اضافه کردن سوال واسط",
                                                            color = if (enableInsertIntermediateAfterAnswer)
                                                                MaterialTheme.colorScheme.onSurface
                                                            else
                                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                        )
                                                    },
                                                    onClick = {
                                                        if (!enableInsertIntermediateAfterAnswer) return@DropdownMenuItem
                                                        answerMenuExpanded = false
                                                        onInsertIntermediateAfterAnswer(aId)
                                                    }
                                                )

                                            }
                                        }

                                    }
                                }
                            }
                        }



                    }
                }
            }

            // منوی سه‌نقطه
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = { unitMenuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "گزینه‌ها")
                }
                DropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false },
                    offset = DpOffset(x = 0.dp, y = (-12).dp),
                ) {
                    DropdownMenuItem(
                        text = { Text(if (editeMode) "خروج از ویرایش" else "ویرایش") },
                        onClick = { onClickEditeQuestion(questionEntity.id) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "حذف",
                                color = if (filterLock)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        enabled = !filterLock,
                        onClick = {
                            unitMenuExpanded = false
                            onClickDeleteQuestion(questionEntity)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("کپی") },
                        enabled = !filterLock,
                        onClick = {
                            unitMenuExpanded = false
                            onClickCopyQuestion(qId)
                        }
                    )


                }
            }
        }
    }
}


@Composable
private fun AnchorHandle(
    filled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onPositioned: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    enabled: Boolean = true,
    selectQuestionEvent: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selectQuestionEvent) 3.dp else 1.5.dp,
                color = if (selectQuestionEvent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .onGloballyPositioned { onPositioned(it) }
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!filled) {
            Text(
                "+", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ConfirmResolveDialog(
    childIds: List<Int>,
    onDismiss: () -> Unit,
    onOpenGraph: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اتصالات چندگانه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("این سؤال به ${childIds.size} سؤال دیگر وصل است:")
                Text(
                    text = childIds.joinToString(prefix = "فرزندان: [", postfix = "]"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("لطفاً در گراف مشخص کنید کدام اتصالات باقی بماند/تغییر کند.")
            }
        },
        confirmButton = {
            Button(onClick = onOpenGraph) { Text("باز کردن گراف") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}





