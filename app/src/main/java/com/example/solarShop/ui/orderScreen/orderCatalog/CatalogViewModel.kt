package com.example.solarShop.ui.orderScreen.orderCatalog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.backupRestore.AttachmentController
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.orderAll.order.OrderWithTimelineItem
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostRepository
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateEntity
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateRepository
import com.example.solarShop.data.room.tables.question_answers.CatalogRepository
import com.example.solarShop.data.room.tables.question_answers.QuestionAnswersRepository
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerRepository
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionRepository
import com.example.solarShop.data.room.tables.question_answers.question.QuestionWithAnswers
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceEntity
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceRepository
import com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto.OrderAnswerSelectedPhotoEntity
import com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto.OrderAnswerSelectedPhotoRepository
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import com.example.solarShop.utils.buildPdfFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val clientRepo: ClientRepository,
    private val orderRepo: OrderRepository,
    @ApplicationContext context: Context,
    private val catalogRepo: CatalogRepository,
    private val selectedChoiceRepo: SelectedChoiceRepository,
    private val questionRepo: QuestionRepository,
    private val answerRepo: AnswerRepository,
    private val questionAnswersRepo: QuestionAnswersRepository,
    private val answerSelectedPhotoRepo: OrderAnswerSelectedPhotoRepository,
    private val priceEstimateRepo: PriceEstimateRepository,
    private val appInfoRepo: AppInfoRepository,
    private val repo: OrderCostRepository,
    private val attachmentController: AttachmentController,
    @ApplicationContext private val app: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {


    private val filesDir: File = context.filesDir






    private val orderIdStateFlow: StateFlow<Int> =
        savedStateHandle.getStateFlow("orderId", -1)

    private val currentOrderIdFlow: Flow<Int?> =
        orderIdStateFlow
            .map { if (it == -1) null else it }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentOrderEntity: Flow<OrderEntity?> =
        currentOrderIdFlow
            .flatMapLatest { id ->
                if (id == null) flowOf(null)
                else orderRepo.observeOrderById(id)
            }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedPhotoMapFlow: Flow<Map<Int, Int>> =
        currentOrderIdFlow
            .filterNotNull()
            .flatMapLatest { orderId ->
                answerSelectedPhotoRepo.observeSelectedPhotoMap(orderId)
                // خروجی: Flow<List<OrderAnswerSelectedPhotoEntity>>
            }
            .map { rows ->
                rows.associate { it.answerId to it.selectedPhotoId }
            }
            .flowOn(Dispatchers.IO)

    private val currentAnswerItemUnitFlow: Flow<List<AnswerItemUnit>> =
        combine(baseAnswerItemsFlow, currentChoiceFlow, selectedPhotoMapFlow) { items, choice, photoMap ->

            val withNotes = if (choice == null) {
                items.map { it.copy(note = "") }
            } else {
                val selectedAnswerId = choice.answerId
                val note = choice.choiceDescription.orEmpty()
                items.map { aiu ->
                    if (aiu.answerId == selectedAnswerId) aiu.copy(note = note)
                    else aiu.copy(note = "")
                }
            }

            // ✅ این قسمت جدید: اگر برای answer عکس انتخاب شده، بیاد اول
            withNotes.map { aiu ->
                val selectedPhotoId = photoMap[aiu.answerId] ?: return@map aiu

                val idx = aiu.imageIds.indexOf(selectedPhotoId)
                if (idx <= 0) return@map aiu // یا پیدا نشد یا همون اولی بود

                val newIds = buildList {
                    add(aiu.imageIds[idx])
                    addAll(aiu.imageIds.filterIndexed { i, _ -> i != idx })
                }

                val newUris = buildList {
                    add(aiu.imageUris[idx])
                    addAll(aiu.imageUris.filterIndexed { i, _ -> i != idx })
                }

                aiu.copy(
                    imageIds = newIds,
                    imageUris = newUris
                )
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


//    private val _pdfExportData = MutableStateFlow<PdfExportData?>(null)
//    val pdfExportData: StateFlow<PdfExportData?> = _pdfExportData

    private val _openPdfEvent = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val openPdfEvent: SharedFlow<Uri> = _openPdfEvent



    // UiState نهایی
    val uiState: StateFlow<CatalogUiState> =
        combine(
            appInfoFlow,
            currentUserFlow,
            currentOrderEntity,
            currentClientEntity,
            currentOrderWithTimelineItem,
            rootQuestionFlow,
            currentAnswerItemUnitFlow,
            currentQuestionFlow,
            totalAnsweredFlow,
            answeredOrderFlow,
            totalInLineFlow
        ) { arr ->
            val appInfo = arr[0] as AppInfoEntity?
            val currentUser = arr[1] as UserEntity?
            val currentOrder = arr[2] as OrderEntity?
            val currentClient = arr[3] as ClientEntity?
            val orderWithTimelineItem = arr[4] as OrderWithTimelineItem?
            val rootQuestionFlow = arr[5] as QuestionEntity?
            val currentAnswerItemUnit =
                (arr[6] as? List<*>)?.filterIsInstance<AnswerItemUnit>().orEmpty()
            val currentQuestion = arr[7] as QuestionEntity?
            val answeredOrder = arr[8] as Int?
            val totalAnswered = arr[9] as Int?
            val totalInLine = arr[10] as Int




            CatalogUiState(
                appInfoEntity = appInfo,
                currentUserEntity = currentUser,
                currentOrderEntity = currentOrder,
                currentClientEntity = currentClient,
                currentOrderWithTimelineItem = orderWithTimelineItem,
                firstQuestion = rootQuestionFlow,
                currentAnswerItemUnit = currentAnswerItemUnit,
                currentQuestion = currentQuestion,
                answeredOrder = answeredOrder,
                totalAnswered = totalAnswered,
                totalInLine = totalInLine,
                isDataLoaded = true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CatalogUiState(),
        )


    //این نسخه فقط بعد از مکث کوتاه ذخیره می‌کند و فشار I/O را کم می‌کند
    private val noteChanges = MutableSharedFlow<Triple<Int, Int, Int>>(extraBufferCapacity = 64)



    private val lastNoteCache = mutableMapOf<Int, String>()





    init {
        viewModelScope.launch {
            noteChanges
                .debounce(100) // ۳۰۰ms بعد از آخرین تغییر
                .collect { (answerId, orderId, questionId) ->
                    selectedChoiceRepo.upsertNote(
                        orderId,
                        questionId,
                        answerId,
                        lastNoteCache[answerId].orEmpty()
                    )
                }
        }
    }


    fun onClickNext() {
        viewModelScope.launch(Dispatchers.IO) {
            navMutex.withLock {
                val orderId = uiState.value.currentOrderEntity?.id ?: return@withLock
                val q =
                    uiState.value.currentQuestion ?: uiState.value.firstQuestion ?: return@withLock
                val qId = q.id ?: return@withLock

                // باید انتخابی برای سؤال جاری وجود داشته باشد
                val selectedAnswerId = selectedChoiceRepo.getSelectedAnswerId(orderId, qId)
                    ?: // اینجا می‌تونی یک ایونت اسنک‌بار بفرستی؛ فعلاً برمی‌گردیم
                    return@withLock

                val next = answerRepo.getNextQuestionForAnswer(selectedAnswerId)
                // اگر next = null یعنی به برگِ انتهایی رسیدیم؛ می‌تونی اینجا فینالایز کنی
                _currentQuestion.value = next ?: q   // یا همون q بمونه؛ انتخاب با تو
            }
        }
    }

    fun onClickPrevious() {
        viewModelScope.launch(Dispatchers.IO) {
            navMutex.withLock {
                val orderId = uiState.value.currentOrderEntity?.id ?: return@withLock
                val q =
                    uiState.value.currentQuestion ?: uiState.value.firstQuestion ?: return@withLock
                val qId = q.id ?: return@withLock

                // سؤالِ قبلی‌ای که انتخابش به qId رسیده
                val prevQId =
                    selectedChoiceRepo.getPreviousQuestionId(orderId, qId) ?: return@withLock
                val prevQ = questionRepo.getById(prevQId) ?: return@withLock

                _currentQuestion.value = prevQ
            }
        }
    }

    fun onToggleSelect(answerId: Int, selected: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            selectMutex.withLock {
                val orderId = uiState.value.currentOrderEntity?.id ?: return@withLock
                val q =
                    uiState.value.currentQuestion ?: uiState.value.firstQuestion ?: return@withLock
                val qId = q.id ?: return@withLock

                if (selected) {
                    // 1) انتخاب قبلی همین سوال؟
                    val prevAnswerId = selectedChoiceRepo.getSelectedAnswerId(orderId, qId)

                    // اگر همان گزینه دوباره انتخاب شده؛ هیچ‌کاری نکن
                    if (prevAnswerId != null && prevAnswerId == answerId) return@withLock

                    if (prevAnswerId == null) {
                        // اولین انتخاب برای این سوال → فقط ثبت کن، نیازی به Prune نیست
                        questionAnswersRepo.replaceSelectionAtQuestionWithoutPruning(
                            orderId = orderId,
                            questionId = qId,
                            answerId = answerId
                        )
                        return@withLock
                    }

                    // 2) مقایسه‌ی مقصد یال‌ها
                    val prevNextQ = questionRepo.getNextQuestionIdForAnswer(prevAnswerId)
                    val newNextQ = questionRepo.getNextQuestionIdForAnswer(answerId)

                    val sameBranch = prevNextQ == newNextQ
                    if (sameBranch) {
                        // مقصد یکسان است → فقط جایگزینی انتخاب همین سوال، بدون حذف دُم
                        questionAnswersRepo.replaceSelectionAtQuestionWithoutPruning(
                            orderId = orderId,
                            questionId = qId,
                            answerId = answerId
                        )
                    } else {
                        // شاخه عوض می‌شود → جایگزینی + حذف دُم از این سوال به بعد
                        questionAnswersRepo.replaceSelectionAndPruneTailAtQuestion(
                            orderId = orderId,
                            questionId = qId,
                            answerId = answerId
                        )
                    }
                } else {
                    // دی‌سلکت: طبق منطق قبلی دُم را هم حذف کن
                    selectedChoiceRepo.deleteSelection(
                        orderId = orderId,
                        questionId = qId,
                        answerId = answerId
                    )
                }
                // UI از روی Flowها آپدیت می‌شود
            }
        }
    }

    fun onNoteChange(answerId: Int, note: String) {
        viewModelScope.launch {
            val orderId = currentOrderIdFlow.firstOrNull() ?: return@launch
            val questionId = currentQuestionFlow.firstOrNull()?.id ?: return@launch
            lastNoteCache[answerId] = note
            noteChanges.tryEmit(Triple(answerId, orderId, questionId))
        }
    }

    private val navMutex = Mutex()


    private val selectMutex = Mutex()

    fun onClickFirst() {
        viewModelScope.launch(Dispatchers.IO) {
            navMutex.withLock {
                val root = uiState.value.firstQuestion ?: return@withLock
                _currentQuestion.value = root
            }
        }
    }

    fun onClickLast() {
        viewModelScope.launch(Dispatchers.IO) {
            navMutex.withLock {
                val orderId = uiState.value.currentOrderEntity?.id ?: return@withLock
                val root = uiState.value.firstQuestion ?: return@withLock

                var q = root
                var lastAnswered: QuestionEntity? = null   // ✅ آخرین سؤالِ پاسخ‌داده‌شده
                val visited = mutableSetOf<Int>()

                while (true) {
                    val qId = q.id ?: break
                    if (!visited.add(qId)) break  // حلقهٔ محافظتی

                    // پاسخی که برای این سؤال در این سفارش انتخاب شده
                    val selectedAnswerId =
                        selectedChoiceRepo.getSelectedAnswerId(orderId, qId)
                            ?: break  // ✅ اگر پاسخی نیست، یعنی q پاسخ ندارد → باید روی lastAnswered بایستیم

                    // ✅ این سؤال پاسخ دارد، پس آخرینِ پاسخ‌داده‌شده را آپدیت کن
                    lastAnswered = q

                    // سؤال بعدی بر اساس آن جواب
                    val nextQId = questionRepo.getNextQuestionIdForAnswer(selectedAnswerId)
                        ?: break // ✅ اگر سؤال بعدی نداریم، همین q آخرین سؤال پاسخ‌دار است

                    val nextQ = questionRepo.getById(nextQId) ?: break

                    q = nextQ
                }

                // ✅ اگر حداقل یک سؤال پاسخ داده شده، همان را نشان بده؛ وگرنه root
                _currentQuestion.value = lastAnswered ?: root
            }
        }
    }

    fun onSelectCatalogPhoto(answerId: Int, selectedPhotoId: Int) {
        val orderId = uiState.value.currentOrderEntity?.id ?: return
        val qId = uiState.value.currentQuestion?.id ?: return

        viewModelScope.launch(Dispatchers.IO) {
            answerSelectedPhotoRepo.upsert(
                OrderAnswerSelectedPhotoEntity(
                    orderId = orderId,
                    questionId = qId,
                    answerId = answerId,
                    selectedPhotoId = selectedPhotoId
                )
            )
        }
    }

    fun selectedPhotoIdFlow(answerId: Int): Flow<Int?> {
        val orderId = uiState.value.currentOrderEntity?.id ?: return flowOf(null)
        return answerSelectedPhotoRepo.selectedPhotoIdFlow(orderId, answerId)
    }

    private suspend fun buildOrderedRowsForPdf(orderId: Int): List<PdfExportRow> {
        val root = answerRepo.observeRootQuestion().first() ?: return emptyList()
        val choices = questionRepo.observeChoicesForOrder(orderId).first()

        // Map: questionId -> choiceRow (همان ردیفِ repo)
        val choiceByQuestionId = choices
            .mapNotNull { c -> c.questionId?.let { it to c } }
            .toMap()


        val selectedPhotoMap: Map<Int, Int> =
            answerSelectedPhotoRepo.observeSelectedPhotoMap(orderId).first()
                .associate { it.answerId to it.selectedPhotoId } // answerId -> selectedPhotoId


        val rows = mutableListOf<PdfExportRow>()
        val visited = mutableSetOf<Int>()

        var qId: Int? = root.id
        var index = 1

        while (qId != null && visited.add(qId)) {
            val c = choiceByQuestionId[qId] ?: break

            val aId = c.answerId ?: break

            val q = questionRepo.getById(qId) ?: break
            val a = answerRepo.getAnswerById(aId) ?: break

            val selectedPhotoId = selectedPhotoMap[aId] // answerId -> selectedPhotoId

            val photoIdForPdf: Int? =
                selectedPhotoId
                    ?: answerRepo.getFirstImageIdForAnswer(aId) // ✅ fallback: اولین عکس جواب


            val absPath = photoIdForPdf?.let { photoId ->
                val fileName = answerRepo.getImageFileNameById(photoId)
                if (fileName.isNullOrBlank()) null
                else File(File(app.filesDir, "images"), fileName).absolutePath
            }


            rows += PdfExportRow(
                index = index++,
                questionTitle = q.title,
                answerTitle = a.title,
                selectedPhotoAbsPath = absPath,
                note = c.choiceDescription.orEmpty()
            )

            // حرکت به سؤال بعدی طبق گراف
            qId = questionRepo.getNextQuestionIdForAnswer(aId)
        }

        return rows
    }


    private suspend fun preparePdfData(): PdfExportData? {
        val order = uiState.value.currentOrderEntity ?: return null
        val orderId = order.id ?: return null

        val client = uiState.value.currentClientEntity
        val clientName = client?.name ?: "بدون نام"
        val orderTitle = order.name ?: "سفارش"

        val choices = questionRepo.observeChoicesForOrder(orderId).first()

        val selectedPhotoMap: Map<Int, Int> =
            answerSelectedPhotoRepo.observeSelectedPhotoMap(orderId).first()
                .associate { it.answerId to it.selectedPhotoId }

        val rows = buildOrderedRowsForPdf(orderId)


        return PdfExportData(
            header = PdfExportHeader(
                clientName = clientName,
                orderTitle = orderTitle,
                orderId = orderId
            ),
            rows = rows
        )
    }


    @SuppressLint("QueryPermissionsNeeded")
    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // ✅ بعضی گوشی‌ها بدون NEW_TASK باز نمی‌کنن (وقتی context اپلیکیشن باشه)
        app.startActivity(intent)
    }



    private val _isExporting = MutableStateFlow(false)

    fun exportPdf() {
        if (_isExporting.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val data = preparePdfData() ?: return@launch
                val file = File(app.cacheDir, "catalog_${data.header.orderId}_${System.currentTimeMillis()}.pdf")
                buildPdfFile(app, answerRepo, file, data)
                openPdf(file)
            } finally {
                _isExporting.value = false
            }
        }
    }








}





data class CatalogUiState(
    val appInfoEntity: AppInfoEntity? = null,
    val firstQuestion: QuestionEntity? = null,
    val currentQuestion: QuestionEntity? = null,
    val currentAnswerItemUnit: List<AnswerItemUnit> = listOf(),


    val currentUserEntity: UserEntity? = null,
    val currentOrderEntity: OrderEntity? = null,
    val currentClientEntity: ClientEntity? = null,
    val currentOrderWithTimelineItem: OrderWithTimelineItem? = null,
    val currentClientOrderEntities: List<OrderEntity>? = null,
    val currentOrderSelectedChoiceEntityList: List<SelectedChoiceEntity>? = null,
    val priceEstimateEntity: PriceEstimateEntity? = null,
    val questions: List<QuestionWithAnswers> = listOf(),

    val answeredOrder: Int? = null,
    val totalAnswered: Int? = null,
    val totalInLine: Int? = null,


    val isDataLoaded: Boolean = false
)

data class AnswerItemUnit(
    val answerId: Int,
    val title: String,
    val imageIds: List<Int>,
    val imageUris: List<String>, // ...
    val selected: Boolean,
    val liked: Boolean = false,

    // 👇 این همون note سفارشی روی انتخابِ این سفارش است (choiceDescription)
    val note: String = "",

    // 👇 این Note ثابت خودِ Answer است (از جدول answer ها)
    val answerNote: String = "",
)

data class FullscreenPayload(
    val title: String,
    val answerId: Int,           // ✅ جدید
    val questionId: Int,         // ✅ جدید
    val uris: List<Uri>,
    val imageIds: List<Int>,     // ✅ جدید (هم‌اندازه uris)
    val note: String,
    val startIndex: Int = 0
)

data class PdfExportHeader(
    val clientName: String,
    val orderTitle: String,
    val orderId: Int,
)

data class PdfExportRow(
    val index: Int,
    val questionTitle: String,
    val answerTitle: String,
    val selectedPhotoAbsPath: String?,
    val note: String,            // همون choiceDescription
)

data class PdfExportData(
    val header: PdfExportHeader,
    val rows: List<PdfExportRow>
)
