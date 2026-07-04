package com.example.solarShop.ui.orderScreen

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.dataStore.DisplayPreferencesDataSource
import com.example.solarShop.data.dataStore.OrderCheckpointStore
import com.example.solarShop.data.dataStore.OrderSummaryCheckpoint
import com.example.solarShop.data.dataStore.TimelineSuggestion
import com.example.solarShop.data.dataStore.defaultMessageFor
import com.example.solarShop.data.dataStore.detectSuggestion
import com.example.solarShop.data.dataStore.suggestKeyFromSystemKey
import com.example.solarShop.data.dataStore.toCheckpoint
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.orderAll.OrderAllRepository
import com.example.solarShop.data.room.tables.orderAll.OrderSummary
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.orderAll.order.OrderWithTimelineItem
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.OrderTimelineSuggestionEntity
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemEntity
import com.example.solarShop.data.room.tables.orderAll.orderTimelineItem.TimelineItemRepository
import com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep.OrderWorkflowStepEntity
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateEntity
import com.example.solarShop.data.room.tables.question_answers.CatalogRepository
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerRepository
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionRepository
import com.example.solarShop.data.room.tables.question_answers.question.QuestionWithAnswers
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceEntity
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceRepository
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.UserWorkflowStepEntity
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.WorkflowRepository
import com.example.solarShop.ui.orderScreen.orderCatalog.AnswerItemUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

//region xxxxxx
//endregion xxxxxx


@HiltViewModel
class OrderViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val clientRepo: ClientRepository,
    private val orderRepo: OrderRepository,
    @ApplicationContext context: Context,
    private val catalogRepo: CatalogRepository,
    private val selectedChoiceRepo: SelectedChoiceRepository,
    private val questionRepo: QuestionRepository,
    private val workflowRepository: WorkflowRepository,
    private val timelineItemRepo: TimelineItemRepository,
    private val orderAllRepository: OrderAllRepository,
    displayPrefs: DisplayPreferencesDataSource,
    private val checkpointStore: OrderCheckpointStore,
    answerRepo: AnswerRepository,
    appInfoRepo: AppInfoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val filesDir: File = context.filesDir

    private var resumeJob: Job? = null

    private var visibleJob: kotlinx.coroutines.Job? = null

    private val pendingSystemKeyState = MutableStateFlow<String?>(null)
    private val checkpointState = MutableStateFlow<OrderSummaryCheckpoint?>(null)

    // همون که داشتی:
    private val lastCheckpointByOrderId = mutableMapOf<Int, OrderSummaryCheckpoint>()



    //region UiState

    private val _pendingSuggestion = MutableStateFlow<TimelineSuggestion?>(null)
    private val pendingSuggestionStateFlow: StateFlow<TimelineSuggestion?> =
        _pendingSuggestion.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )


    val displayPrefsState: StateFlow<DisplayPreferences> =
        displayPrefs.prefsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DisplayPreferences()
        )



    private val orderIdStateFlow: StateFlow<Int> =
        savedStateHandle.getStateFlow("orderId", -1)

    private val currentOrderIdFlow: Flow<Int?> =
        orderIdStateFlow
            .map { if (it == -1) null else it }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pendingSuggestionFlow: Flow<OrderTimelineSuggestionEntity?> =
        currentOrderIdFlow
            .filterNotNull()
            .flatMapLatest { orderId -> timelineItemRepo.observePending(orderId) }
            .flowOn(Dispatchers.IO)


    // جریان‌های دیتابیس
    private val appInfoFlow = appInfoRepo.observeAppInfo()
    private val _currentQuestion = MutableStateFlow<QuestionEntity?>(null)

    //اولین سؤال بدون والد
    private val rootQuestionFlow: Flow<QuestionEntity?> = answerRepo.observeRootQuestion()
    private val currentQuestionFlow: Flow<QuestionEntity?> =
        combine(rootQuestionFlow, _currentQuestion) { root, current ->
            // در ابتدا اگر current تهی است، همون firstQuestion (root) رو برگردون
            current ?: root
        }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentOrderEntity: Flow<OrderEntity?> =
        currentOrderIdFlow
            .flatMapLatest { id ->
                if (id == null) flowOf(null)
                else orderRepo.observeOrderById(id)
            }
            .flowOn(Dispatchers.IO)





    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentChoiceFlow: Flow<SelectedChoiceEntity?> =
        combine(currentOrderIdFlow, currentQuestionFlow) { orderId, q ->
            orderId to q?.id
        }
            .filter { (orderId, qId) -> orderId != null && qId != null }
            .flatMapLatest { (orderId, qId) ->
                selectedChoiceRepo.observeChoice(orderId!!, qId!!)
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val baseAnswerItemsFlow: Flow<List<AnswerItemUnit>> =
        combine(currentQuestionFlow, currentOrderIdFlow) { q, orderId -> q?.id to orderId }
            .filter { (qId, oId) -> qId != null && oId != null }
            .flatMapLatest { (qId, oId) ->
                catalogRepo.currentAnswerItems(
                    questionId = qId!!,
                    orderId = oId!!,
                    filesDir = filesDir
                )
            }

    private val currentAnswerItemUnitFlow: Flow<List<AnswerItemUnit>> =
        combine(baseAnswerItemsFlow, currentChoiceFlow) { items, choice ->
            if (choice == null) {
                // هنوز چیزی انتخاب نشده → همه بدون نوت
                items.map { it.copy(note = "") }
            } else {
                val selectedAnswerId = choice.answerId
                val note = choice.choiceDescription.orEmpty()
                items.map { aiu ->
                    if (aiu.answerId == selectedAnswerId) aiu.copy(note = note)
                    else aiu.copy(note = "")
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserFlow: Flow<UserEntity?> =
        appInfoFlow
            .map { it?.currentUserId }        // فقط شناسه کاربر جاری
            .distinctUntilChanged()          // فقط وقتی تغییر کنه
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)              // اگر یوزری انتخاب نشده
                } else {
                    userRepo.observeUserById(id)
                }
            }
            .flowOn(Dispatchers.IO)           // اگر ریپو روی IO نیست

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentOrderWithTimelineItem: Flow<OrderWithTimelineItem?> =
        currentOrderEntity
            .map { it?.id }
            .distinctUntilChanged()          // فقط وقتی تغییر کنه
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)              // اگر یوزری انتخاب نشده
                } else {
                    orderRepo.observeOrderWithTimelineItem(id)
                }
            }
            .flowOn(Dispatchers.IO)                // اگر ریپو روی IO نیست

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentClientEntity: Flow<ClientEntity?> =
        currentOrderEntity
            .map { it?.clientId }
            .distinctUntilChanged()          // فقط وقتی تغییر کنه
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)              // اگر یوزری انتخاب نشده
                } else {
                    clientRepo.observeClientById(id)
                }
            }
            .flowOn(Dispatchers.IO)           // اگر ریپو روی IO نیست

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalAnsweredFlow: Flow<Int?> =
        currentOrderIdFlow
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)              // اگر یوزری انتخاب نشده
                } else {
                    selectedChoiceRepo.observeTotalAnswered(id)
                }
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val answeredOrderFlow: Flow<Int?> =
        combine(currentOrderIdFlow, currentQuestionFlow) { orderId, q ->
            orderId to q?.id
        }
            .distinctUntilChanged()
            .flatMapLatest { (orderId, questionId) ->
                if (orderId == null || questionId == null) {
                    flowOf(null)
                } else {
                    selectedChoiceRepo.observeAnsweredOrder(orderId, questionId)
                }
            }
            .flowOn(Dispatchers.IO)

    // ۲-۱) انتخاب‌های سفارش جاری (Map<questionId, answerId>)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val choicesMapFlow: Flow<Map<Int, Int>> =
        currentOrderIdFlow
            .filterNotNull()
            .flatMapLatest { orderId ->
                questionRepo.observeChoicesForOrder(orderId)
            }
            .map { rows ->
                rows.mapNotNull { r ->
                    val q = r.questionId
                    val a = r.answerId
                    if (q != null && a != null) q to a else null
                }.toMap()
            }
            .flowOn(Dispatchers.IO)

    // ۲-۲) یال‌های پاسخ→سؤال بعدی (Map<answerId, nextQuestionId>)
    private val answerEdgesFlow: Flow<Map<Int, Int>> =
        questionRepo.observeAllAnswerEdges()
            .map { edges -> edges.associate { it.answerId to it.nextQuestionId } }
            .flowOn(Dispatchers.IO)

    // ۲-۳) totalInLine: طولِ مسیرِ قابل‌پیگیری از ریشه
    private val totalInLineFlow: Flow<Int> =
        combine(rootQuestionFlow, choicesMapFlow, answerEdgesFlow) { rootQ, choicesMap, ansEdges ->
            Triple(rootQ?.id, choicesMap, ansEdges)
        }.map { (rootId, choices, ansEdges) ->
            if (rootId == null) return@map 0

            var count = 0
            var qId: Int? = rootId
            val visited = mutableSetOf<Int>()

            while (qId != null && visited.add(qId)) {
                count += 1
                val selectedAnswerId = choices[qId]
                count += 1
                // آیا برای این سؤال پاسخی انتخاب شده؟
                if (selectedAnswerId == null) break           // نه → همین‌جا مسیر فعلاً متوقف است
                qId =
                    ansEdges[selectedAnswerId]               // سؤال بعدی از روی یال answer→question
            }
            count
        }.flowOn(Dispatchers.Default)




    @OptIn(ExperimentalCoroutinesApi::class)
    val workflowTemplateFlow: Flow<List<UserWorkflowStepEntity>> =
        workflowRepository.observeTemplateSteps()

    @OptIn(ExperimentalCoroutinesApi::class)
    val orderWorkflowStepsFlow: Flow<List<OrderWorkflowStepEntity>> =
        currentOrderIdFlow
            .filterNotNull()
            .flatMapLatest { orderId ->
                workflowRepository.observeOrderSteps(orderId)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val orderSummaryFlow: Flow<OrderSummary?> =
        currentOrderIdFlow
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    orderAllRepository
                        .observeOrderSummary(id)
                        .map { it as OrderSummary? }
                }
            }
            .flowOn(Dispatchers.IO)



    // UiState نهایی
    val uiState: StateFlow<OrderUiState> =
        combine(
            appInfoFlow,              // 0
            currentUserFlow,          // 1
            currentOrderEntity,       // 2
            orderSummaryFlow,         // 3
            currentClientEntity,      // 4
            currentOrderWithTimelineItem, // 5
            rootQuestionFlow,         // 5
            currentAnswerItemUnitFlow,// 6
            currentQuestionFlow,      // 7
            totalAnsweredFlow,        // 8
            answeredOrderFlow,        // 9
            totalInLineFlow,           // 10
            pendingSuggestionStateFlow,
        ) { arr ->
            val appInfo        = arr[0] as AppInfoEntity?
            val currentUser    = arr[1] as UserEntity?
            val currentOrder   = arr[2] as OrderEntity?
            val summary        = arr[3] as OrderSummary?
            val currentClient  = arr[4] as ClientEntity?
            val orderWithTimelineItem = arr[5] as OrderWithTimelineItem?
            val rootQuestion   = arr[6] as QuestionEntity?
            val currentAnswerItemUnit =
                (arr[7] as? List<*>)?.filterIsInstance<AnswerItemUnit>().orEmpty()
            val currentQuestion= arr[8] as QuestionEntity?
            val answeredOrder  = arr[9] as Int?
            val totalAnswered  = arr[10] as Int?
            val totalInLine    = arr[11] as Int
            val pendingSug     = arr[12] as TimelineSuggestion?
            val progressPercent= summary?.progressPercent ?: 0
            // اگر بعداً priceEstimateTotal هم خواستی می‌تونی از summary استفاده کنی

            OrderUiState(
                appInfoEntity = appInfo,
                currentUserEntity = currentUser,
                currentOrderEntity = currentOrder,
                currentClientEntity = currentClient,
                currentOrderWithTimelineItem = orderWithTimelineItem,
                firstQuestion = rootQuestion,
                currentAnswerItemUnit = currentAnswerItemUnit,
                currentQuestion = currentQuestion,
                answeredOrder = answeredOrder,
                totalAnswered = totalAnswered,
                totalInLine = totalInLine,
                progressPercent = progressPercent,
                currentOrderSummary=summary,
                pendingSuggestion = pendingSug,
                isDataLoaded = true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrderUiState(),
        )


    //endregion UiState


    //region Funs

    fun onRenameOrder(newName: String) {
        val order =
            uiState.value.currentOrderEntity ?: return

        val cleanName =
            newName.trim()

        if (cleanName.isBlank()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val affectedRows =
                orderRepo.updateOrder(
                    order.copy(
                        name = cleanName,
                        updatedAt = System.currentTimeMillis()
                    )
                )

            android.util.Log.d(
                "OrderRename",
                "rename orderId=${order.id}, newName=$cleanName, affectedRows=$affectedRows"
            )

            val after =
                order.id?.let { orderRepo.getOrderById(it) }

            android.util.Log.d(
                "OrderRename",
                "after rename from db = ${after?.name}"
            )
        }
    }

    fun onApplyWorkflowStatus(selectedStepId: Int) {
        val order = uiState.value.currentOrderEntity ?: return
        val orderId = order.id ?: return

        viewModelScope.launch(Dispatchers.IO) {

            // 1) فقط این step برای این سفارش Completed باشد
            workflowRepository.setSingleStepCompleted(
                orderId = orderId,
                stepId = selectedStepId
            )

            // 2) Step را از Template بخوانیم تا عنوان و درصدش را داشته باشیم
            val step = workflowRepository.getStepById(selectedStepId) ?: return@launch

            // 3) آیتم جدید تایم‌لاین
            val item = TimelineItemEntity(
                orderId = orderId,
                title = step.title,
                description = " ${step.title} (${step.weightPercent}٪)",
                date = System.currentTimeMillis(),
                completed = true
            )

            timelineItemRepo.insertTimelineItem(item)
        }
    }


    fun onSubmitWorkflowEdits(drafts: List<DraftWorkflowStep>, deletedIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            workflowRepository.saveTemplateEdits(
                drafts = drafts,
                deletedIds = deletedIds,
                userKey = null
            )
        }
    }

    fun consumeSuggestion(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        timelineItemRepo.consume(id)
    }

    fun applySuggestionToTimeline(s: OrderTimelineSuggestionEntity) = viewModelScope.launch(Dispatchers.IO) {
        // 1) از روی systemKey، مرحله را پیدا کن
        val stepId = workflowRepository.getStepBySystemKey(s.systemKey) ?: return@launch

        // 2) مثل انتخاب مرحله در وضعیت سفارش
        onApplyWorkflowStatus(stepId.id ?:0)

        // 3) consume تا دیگه نشان داده نشه
        timelineItemRepo.consume(s.id!!)
    }



    fun onSuggestionAccepted(orderId: Int, systemKey: String) {
        viewModelScope.launch(Dispatchers.IO) {

            applyWorkflowBySystemKey(orderId, systemKey)

            // pending پاک (RAM + DataStore)
            pendingSystemKeyState.value = null
            checkpointStore.setPendingSuggestion(orderId, null)

            // checkpoint را جلو ببر (RAM + DataStore)
            val latest = lastCheckpointByOrderId[orderId]
            if (latest != null) {
                checkpointState.value = latest
                checkpointStore.setCheckpoint(orderId, latest)
            }

            _pendingSuggestion.value = null
        }
    }

    fun currentOrderIdForNavigation(): Int? {
        return orderIdStateFlow.value.takeIf { it != -1 }
            ?: uiState.value.currentOrderEntity?.id
    }


    fun onOrderScreenHidden() {
        visibleJob?.cancel()
        visibleJob = null
    }



    fun onSuggestionDismissed(orderId: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            checkpointStore.setPendingSuggestion(orderId, null)
            pendingSystemKeyState.value = null

            lastCheckpointByOrderId[orderId]?.let { latest ->
                checkpointState.value = latest
                checkpointStore.setCheckpoint(orderId, latest)
            }

            _pendingSuggestion.value = null
        }
    }



    fun onSaveOrderNote(note: String) {
        val orderId = uiState.value.currentOrderEntity?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            orderRepo.updateOrderNote(orderId, note)
        }
    }



    private suspend fun applyWorkflowBySystemKey(orderId: Int, systemKey: String) {
        // 1) پیدا کردن step از template بر اساس systemKey
        val step = workflowRepository.getStepBySystemKey(systemKey) ?: return
        val stepId = step.id ?: return

        // 2) فقط همین مرحله completed شود
        workflowRepository.setSingleStepCompleted(
            orderId = orderId,
            stepId = stepId
        )

        // 3) تایم‌لاین ثبت شود
        val item = TimelineItemEntity(
            orderId = orderId,
            title = step.title,
            description = "${step.title} (${step.weightPercent}٪)",
            date = System.currentTimeMillis(),
            completed = true
        )
        timelineItemRepo.insertTimelineItem(item)
    }

    fun onOrderScreenPaused() {
        resumeJob?.cancel()
        resumeJob = null
    }

    fun onOrderScreenResumed(orderId: Int) {
        resumeJob?.cancel()

        resumeJob = viewModelScope.launch {
            // 1) اگر pending از قبل داریم، همونو نمایش بده
            val pendingKey = checkpointStore.pendingSuggestionFlow(orderId).first()
            if (!pendingKey.isNullOrBlank()) {
                val key = suggestKeyFromSystemKey(pendingKey)
                if (key != null) {
                    _pendingSuggestion.value = TimelineSuggestion(key, defaultMessageFor(key))
                    return@launch
                } else {
                    // کلید نامعتبر => پاکسازی
                    checkpointStore.setPendingSuggestion(orderId, null)
                }
            }

            // 2) آخرین summary فعلی این سفارش
            val summary = orderSummaryFlow.first() ?: return@launch
            val current = summary.toCheckpoint()
            lastCheckpointByOrderId[orderId] = current

            // 3) اگر checkpoint نداریم => baseline بساز
            val prev = checkpointStore.checkpointFlow(orderId).first()
            if (prev == null) {
                checkpointStore.setCheckpoint(orderId, current)
                _pendingSuggestion.value = null
                return@launch
            }

            // 4) detect
            val sug = detectSuggestion(current = current, prev = prev)
            if (sug != null) {
                _pendingSuggestion.value = sug
                checkpointStore.setPendingSuggestion(orderId, sug.key.systemKey)
            } else {
                _pendingSuggestion.value = null
            }
        }
    }


    private fun setUiFromPendingSystemKey(systemKey: String?) {
        if (systemKey.isNullOrBlank()) {
            _pendingSuggestion.value = null
            return
        }
        val key = suggestKeyFromSystemKey(systemKey)
        if (key == null) {
            _pendingSuggestion.value = null
            return
        }
        _pendingSuggestion.value = TimelineSuggestion(
            key = key,
            message = defaultMessageFor(key)
        )
    }


    fun onOrderScreenVisible(orderId: Int) {
        visibleJob?.cancel()

        visibleJob = viewModelScope.launch {

            // 1) فقط یک بار از DataStore مقدار اولیه را بگیر
            val initialCheckpoint = checkpointStore.checkpointFlow(orderId).first()
            val initialPending = checkpointStore.pendingSuggestionFlow(orderId).first()

            checkpointState.value = initialCheckpoint
            pendingSystemKeyState.value = initialPending
            setUiFromPendingSystemKey(initialPending)

            // 2) از این به بعد فقط summary را گوش کن و با state داخل VM تصمیم بگیر
            orderSummaryFlow
                .filterNotNull()
                .map { it.toCheckpoint() }
                .distinctUntilChanged()
                .collect { current ->

                    lastCheckpointByOrderId[orderId] = current

                    // اگر pending داریم، پیشنهاد جدید نساز (همان را نگه دار)
                    val pendingKey = pendingSystemKeyState.value
                    if (!pendingKey.isNullOrBlank()) {
                        // UI باید مطابق pending باشد
                        setUiFromPendingSystemKey(pendingKey)
                        return@collect
                    }

                    val prev = checkpointState.value
                    if (prev == null) {
                        // اولین بار: baseline بساز
                        checkpointState.value = current
                        checkpointStore.setCheckpoint(orderId, current)
                        _pendingSuggestion.value = null
                        return@collect
                    }

                    val sug = detectSuggestion(current = current, prev = prev)
                    if (sug != null) {
                        _pendingSuggestion.value = sug

                        // pending را همزمان هم در RAM هم در DataStore ست کن
                        pendingSystemKeyState.value = sug.key.systemKey
                        checkpointStore.setPendingSuggestion(orderId, sug.key.systemKey)
                    } else {
                        _pendingSuggestion.value = null
                    }
                }
        }
    }

    private var debugJob: kotlinx.coroutines.Job? = null

    fun debugWatchDataStore(orderId: Int) {
        debugJob?.cancel()
        debugJob = viewModelScope.launch {
            launch {
                checkpointStore.checkpointFlow(orderId).collect { cp ->
                    android.util.Log.d("DS", "checkpoint($orderId) = $cp")
                }
            }
            launch {
                checkpointStore.pendingSuggestionFlow(orderId).collect { p ->
                    android.util.Log.d("DS", "pending($orderId) = $p")
                }
            }
        }
    }


    //endregion Funs

}

//region dataClasses

data class OrderUiState(
    val appInfoEntity: AppInfoEntity? = null,
    val firstQuestion: QuestionEntity? = null,
    val currentQuestion: QuestionEntity? = null,
    val currentAnswerItemUnit: List<AnswerItemUnit> = listOf(),


    val currentUserEntity: UserEntity? = null,
    val currentOrderEntity: OrderEntity? = null,
    val currentOrderSummary: OrderSummary? = null,
    val currentClientEntity: ClientEntity? = null,
    val currentOrderWithTimelineItem: OrderWithTimelineItem? = null,
    val currentClientOrderEntities: List<OrderEntity>? = null,
    val currentOrderSelectedChoiceEntityList: List<SelectedChoiceEntity>? = null,
    val priceEstimateEntity: PriceEstimateEntity? = null,
    val questions: List<QuestionWithAnswers> = listOf(),

    val answeredOrder: Int? = null,
    val totalAnswered: Int? = null,
    val totalInLine: Int? = null,

    val progressPercent: Int = 0,

    val isDataLoaded: Boolean = false,

    val pendingSuggestion: TimelineSuggestion? = null,
)

data class QuickTileModel(
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val accent: Color
)

data class DraftWorkflowStep(
    val id: Int?,                 // اگر null یا منفی بود یعنی جدید/موقت
    val title: String,
    val weightPercent: Int,
    val sortOrder: Int = 0,
    val isLocked: Boolean = false
)

//endregion dataClasses

//region sealedClasses
//endregion sealedClasses
