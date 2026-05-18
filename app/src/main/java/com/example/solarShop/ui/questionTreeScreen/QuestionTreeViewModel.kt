package com.example.solarShop.ui.questionTreeScreen

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.dataStore.QuestionTreePrefsDataStore
import com.example.solarShop.data.dataStore.QuestionTreeViewPrefs
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.question_answers.AddChildResult
import com.example.solarShop.data.room.tables.question_answers.DeleteLinkResult
import com.example.solarShop.data.room.tables.question_answers.JoinChildResult
import com.example.solarShop.data.room.tables.question_answers.PromoteRootResult
import com.example.solarShop.data.room.tables.question_answers.QuestionAnswersRepository
import com.example.solarShop.data.room.tables.question_answers.SmartDeleteResult
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerImageEntity
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerRepository
import com.example.solarShop.data.room.tables.question_answers.question.AnswerNextQuestionCrossRef
import com.example.solarShop.data.room.tables.question_answers.question.AnswerWithNextQuestions
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionRepository
import com.example.solarShop.data.room.tables.question_answers.question.QuestionWithAnswers
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class QuestionTreeViewModel @Inject constructor(
    @ApplicationContext private val app: Context,
    private val userRepo: UserRepository,
    private val questionRepo: QuestionRepository,
    private val answerRepo: AnswerRepository,
    appInfoRepo: AppInfoRepository,
    private val questionAnswersRepo: QuestionAnswersRepository,
    private val questionTreePrefs: QuestionTreePrefsDataStore,
) : ViewModel() {

    // ورودی محلی UI (فقط داخل VM نگه می‌داریم)


    // جریان‌های دیتابیس
    private val appInfoFlow = appInfoRepo.observeAppInfo().distinctUntilChanged()
    private val allQuestionWithAnswers = questionRepo.observeAllQuestionWithAnswers().distinctUntilChanged()
    private val allNextQuestionsForAnswer = questionRepo.observeAllNextQuestionsForAnswer().distinctUntilChanged()

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

    private val allAnswerEdgesFlow =
        questionRepo.observeAllAnswerEdges().distinctUntilChanged()


    // UiState نهایی
    val uiState: StateFlow<QuestionUiState> =
        combine(
            appInfoFlow,
            allQuestionWithAnswers,
            allNextQuestionsForAnswer,
            currentUserFlow,
            allAnswerEdgesFlow, // ✅ جدید
        ) { appInfo, questionWithAnswers, nextQuestionsForAnswer, currentUser, allEdges ->
            QuestionUiState(
                appInfoEntity = appInfo,
                questionWithAnswersList = questionWithAnswers,
                answerWithNextQuestionsList = nextQuestionsForAnswer,
                allAnswerEdges = allEdges, // ✅ جدید
                currentUserEntity = currentUser,
                isDataLoaded = true
            )
        }.flowOn(Dispatchers.Default)   // ⬅️ combine روی بک‌گراند
            .conflate()                    // ⬅️ در صورت بارانِ آپدیت، وسطی‌ها رو بنداز دور
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly, // ⬅️ اختیاری: زودتر گرم‌شدن دیتا
                initialValue = QuestionUiState(),
            )


    // سفارش فعلی که فیلتر روی آن فعال است (یا null اگر فیلتر نداریم)
    private val _filterOrderId = MutableStateFlow<Int?>(null)

    // استیت نهایی فیلتر برای UI
    @OptIn(ExperimentalCoroutinesApi::class)
    val filterState: StateFlow<QuestionTreeFilterState?> =
        _filterOrderId
            .flatMapLatest { orderId ->
                if (orderId == null) {
                    flowOf(null)
                } else {
                    combine(
                        questionRepo.observeChoicesForOrder(orderId),
                        questionRepo.observeAllAnswerEdges()
                    ) { choices, allEdges ->

                        if (choices.isEmpty()) {
                            // هیچ انتخابی برای این سفارش ثبت نشده → عملاً فیلتر نداریم
                            return@combine null
                        }

                        val answerIdsInPath = choices.map { it.answerId }.toSet()
                        val questionIdsFromChoices = choices.map { it.questionId }.toSet()
                        val questionIdsFromEdges = allEdges
                            .filter { it.answerId in answerIdsInPath }
                            .map { it.nextQuestionId }
                            .toSet()
                        val describedAnswerIds = choices
                            .filter { !it.choiceDescription.isNullOrBlank() }
                            .mapNotNull  { it.answerId }
                            .toSet()

                        QuestionTreeFilterState(
                            orderId = orderId,
                            questionIdsInPath = questionIdsFromChoices + questionIdsFromEdges,
                            answerIdsInPath = answerIdsInPath,
                            answerIdsWithChoiceDescription = describedAnswerIds
                        )

                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    private val _mainRootEvent = MutableSharedFlow<MainRootEvent>(extraBufferCapacity = 1)
    val mainRootEvent: SharedFlow<MainRootEvent> = _mainRootEvent



    // 🔹 زوم و پن ذخیره‌شده در DataStore
    val viewPrefs = questionTreePrefs.prefsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = QuestionTreeViewPrefs()   // scale=0.5, offset=0,0
        )

    private val _copyCreated = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1)
    // newQuestionId to sourceQuestionId
    val copyCreated: SharedFlow<Pair<Int, Int>> = _copyCreated


    fun saveViewPrefs(scale: Float, offset: Offset) {
        viewModelScope.launch(Dispatchers.IO) {
            questionTreePrefs.updateView(
                scale = scale,
                offsetX = offset.x,
                offsetY = offset.y
            )
        }
    }



    //  رویداد یک‌بارمصرف برای سناریوی ایجاد یونیت سوال
    private val _addChildResult = MutableSharedFlow<AddChildResult>(extraBufferCapacity = 1)
    val addChildResult: SharedFlow<AddChildResult> = _addChildResult
    fun onClickAnswerAnchorToCreateNewQuestion(fromAnswerId: Int) {
        viewModelScope.launch {
            // 1) اول چک کن: این Answer قبلاً به چیزی وصل شده؟
            val existing = questionRepo.getNextQuestionIdsForAnswer(fromAnswerId)

            if (existing.isNotEmpty()) {
                // ⛔️ اجازه نده. به UI پیام بده.
                _addChildResult.emit(
                    AddChildResult.BlockedAlreadyHasChild(
                        answerId = fromAnswerId,
                        existingChildIds = existing
                    )
                )
                return@launch
            }

            // 2) اگر خالی بود، سؤال جدید بساز
            val childId = questionRepo.insertQuestion(
                QuestionEntity(
                    id = null,
                    title = "" // یا هر مقدار دلخواه اولیه
                )
            ).toInt()
            // 3) یک پاسخ برای "سؤال جدید" بساز
            answerRepo.insertAnswer(
                AnswerEntity(
                    id = null,
                    questionId = childId,
                    title = "" // مثلاً "ورود" یا هرچی دوست داری
                )
            ).toInt()

            // 3) لینک (Answer -> Question)
            questionRepo.upsertAnswerNextQuestion(
                AnswerNextQuestionCrossRef(
                    answerId = fromAnswerId,
                    nextQuestionId = childId
                )
            )

            // 4) گزارش موفقیت
            _addChildResult.emit(AddChildResult.Created(fromAnswerId, childId))

            // برای Undo: سوال جدید را دوباره حذف می‌کنیم
            lastUndoAction = UndoAction.NewChildQuestion(childId)

        }
    }



    //  رویداد یک‌بارمصرف برای اتصال به سوال موجود
    private val _joinChildResult = MutableSharedFlow<JoinChildResult>(extraBufferCapacity = 1)
    val joinChildResult: SharedFlow<JoinChildResult> = _joinChildResult
    fun onClickQuestionAnchorToLinkToExistingQuestion(aId: Int?, question: QuestionEntity) {
        viewModelScope.launch {
            val qId = question.id ?: return@launch
            val newAid = aId ?: return@launch

            val ownerQId = questionAnswersRepo.getOwnerQuestionIdOfAnswer(newAid) ?: return@launch
            if (ownerQId == qId) {
                _joinChildResult.emit(JoinChildResult.InvalidSelfParent(newAid, qId))
                return@launch
            }

            if (questionAnswersRepo.edgeExists(newAid, qId)) {
                _joinChildResult.emit(JoinChildResult.AlreadyConnected(newAid, qId))
                return@launch
            }

            val existingChildren = answerRepo.getChildIdsForAnswer(newAid)
            if (existingChildren.isNotEmpty()) {
                _joinChildResult.emit(
                    JoinChildResult.AnswerAlreadyHasChild(newAid, existingChildren)
                )
                return@launch
            }

            val cycle = questionAnswersRepo.hasPath(fromQuestionId = qId, toQuestionId = ownerQId)
            if (cycle) {
                _joinChildResult.emit(JoinChildResult.WouldCreateCycle(newAid, qId))
                return@launch
            }

            val oldIncoming = answerRepo.getIncomingAnswer(qId)

            if (oldIncoming == null) {
                // ✅ ساخت edge جدید
                questionRepo.upsertAnswerNextQuestion(
                    AnswerNextQuestionCrossRef(answerId = newAid, nextQuestionId = qId)
                )

                // ✅ Undo درست: حذف همین edge
                val undo = UndoAction.DeleteEdge(answerId = newAid, questionId = qId)

                _joinChildResult.emit(JoinChildResult.Joined(newAid, qId, undo))
                return@launch
            }

            // ✅ rewire
            questionAnswersRepo.rewireIncomingAnswerTo(questionId = qId, newAnswerId = newAid)

            // ✅ Undo درست: برگرداندن به oldIncoming واقعی (نه 0)
            val undo = UndoAction.RewireChild(
                questionId = qId,
                oldAnswerId = oldIncoming.id!!,
                newAnswerId = newAid
            )

            _joinChildResult.emit(JoinChildResult.Joined(newAid, qId, undo))
        }
    }


    fun undo(action: UndoAction) {
        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is UndoAction.RewireChild -> {
                    // برگشت به والد قبلی باید replace واقعی باشد
                    questionAnswersRepo.replaceIncomingParent(
                        questionId = action.questionId,
                        newAnswerId = action.oldAnswerId
                    )
                }

                is UndoAction.DeleteEdge -> {
                    questionAnswersRepo.deleteEdge(
                        answerId = action.answerId,
                        childQuestionId = action.questionId
                    )
                }

                // برای آینده اگر خواستی از این مسیر هم restore کنی:
                is UndoAction.RestoreEdge -> {
                    questionRepo.upsertAnswerNextQuestion(
                        AnswerNextQuestionCrossRef(
                            answerId = action.answerId,
                            nextQuestionId = action.questionId
                        )
                    )
                }

                else -> Unit
            }
        }
    }



    //  رویداد یک‌بارمصرف برای سناریوی  3 پاک کردن یونیت سوال
    private val _deleteNeedsResolution = MutableSharedFlow<SmartDeleteResult.BlockedMultipleChildren>()
    val deleteNeedsResolution = _deleteNeedsResolution
    // رویداد موفقیت (دلخواه)
    private val _deleteDone = MutableSharedFlow<SmartDeleteResult>()
    val deleteDone = _deleteDone
    fun deleteQuestionSmart(question: QuestionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val qId = question.id ?: return@launch

            // 🔹 ۱) اسنپ‌شات قبل از حذف (برای Undo)
            // از uiState فعلی، سوال و جواب‌هایش را پیدا می‌کنیم
            val snapshotQwa = uiState.value.questionWithAnswersList
                .firstOrNull { it.question.id == qId }

            val answersOfQuestion: List<AnswerEntity> =
                snapshotQwa?.answers.orEmpty()

            val answerIds = answersOfQuestion.mapNotNull { it.id }
            val imagesOfAnswers: List<AnswerImageEntity> =
                if (answerIds.isEmpty()) emptyList()
                else answerRepo.getImagesForAnswerIds(answerIds)


            // 🚩 کل جدول یال‌ها را قبل از حذف ذخیره می‌کنیم
            val allEdgesBefore = questionRepo.getAllAnswerNextQuestions()

            // 🔹 ۲) حذف هوشمند
            when (val result = questionAnswersRepo.smartDeleteQuestion(question)) {

                is SmartDeleteResult.BlockedMultipleChildren -> {
                    // اصلاً حذف انجام نشده → Undo معنی ندارد
                    lastUndoAction = null
                    _deleteNeedsResolution.emit(result)
                }

                SmartDeleteResult.DeletedDirectly -> {
                    // ✅ حذف ساده → می‌توانیم کل وضعیت را برگردانیم
                    lastUndoAction = UndoAction.DeleteQuestionSmart(
                        question = question,
                        answers = answersOfQuestion,
                        answerImages = imagesOfAnswers,
                        allEdgesBefore = allEdgesBefore
                    )
                    _deleteDone.emit(result)
                }

                is SmartDeleteResult.RewiredAndDeleted -> {
                    // ✅ همین‌جا هم Undo فعال است
                    lastUndoAction = UndoAction.DeleteQuestionSmart(
                        question = question,
                        answers = answersOfQuestion,
                        answerImages = imagesOfAnswers,
                        allEdgesBefore = allEdgesBefore
                    )

                    _deleteDone.emit(result)
                }

                SmartDeleteResult.NoOp -> {
                    lastUndoAction = null
                    _deleteDone.emit(result)
                }
            }
        }
    }





    // رویداد یک‌بارمصرف حذف لینک
    private val _deleteLinkResult = MutableSharedFlow<DeleteLinkResult>(extraBufferCapacity = 1)
    val deleteLinkResult: SharedFlow<DeleteLinkResult> = _deleteLinkResult

    fun onClickAnswerAnchorToDeleteExistingRelation(answer: AnswerEntity) {
        viewModelScope.launch {
            val aId = answer.id ?: return@launch

            val childIds = answerRepo.getChildIdsForAnswer(aId)

            when {
                childIds.isEmpty() -> {
                    _deleteLinkResult.emit(DeleteLinkResult.NotFound(aId))
                }

                childIds.size > 1 -> {
                    _deleteLinkResult.emit(DeleteLinkResult.NeedSelection(aId, childIds))
                }

                else -> {
                    val qId = childIds.first()

                    // ✅ از این به بعد حتی اگر تنها والد بود هم اجازه حذف داریم
                    val ok = questionAnswersRepo.deleteEdge(answerId = aId, childQuestionId = qId)

                    if (ok) {
                        // ✅ Undo: همین یال را برگردان (insert مجدد)
                        lastUndoAction = UndoAction.RestoreEdge(answerId = aId, questionId = qId)

                        _deleteLinkResult.emit(DeleteLinkResult.Deleted(aId, qId))
                    } else {
                        _deleteLinkResult.emit(DeleteLinkResult.NotFound(aId))
                    }
                }
            }
        }
    }




    // رویداد یک‌بارمصرف ساخت سوال برای ریشه
    private val _promoteRootResult = MutableSharedFlow<PromoteRootResult>(extraBufferCapacity = 1)
    val promoteRootResult: SharedFlow<PromoteRootResult> = _promoteRootResult
    fun onClickQuestionAnchorToCreateNewQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            val oldRootId = question.id ?: return@launch

            // 1) اول مطمئن شو این سؤال "بدون والد" است
            //   (اگر متدی با این نام نداری، یکی بساز: مثلا getIncomingAnswer(questionId))
            val incoming = answerRepo.getIncomingAnswer(oldRootId)
            if (incoming != null) {
                // یعنی این سؤال ریشه نیست (یا دست‌کم بدون والد نیست)
                _promoteRootResult.emit(
                    PromoteRootResult.AlreadyHasParent(
                        questionId = oldRootId,
                        parentAnswerId = incoming.id ?: -1
                    )
                )
                return@launch
            }

            // 2) یک سؤالِ جدید بساز (می‌شود ریشه‌ی جدید)
            val newRootId = questionRepo.insertQuestion(
                QuestionEntity(
                    id = null,
                    title = "" // اگر عنوان پیش‌فرض می‌خواهی، این‌جا بگذار
                )
            ).toInt()

            // 3) یک پاسخ برای "سؤال جدید" بساز (پلِ بین ریشهٔ جدید و ریشهٔ قدیم)
            val bridgeAnswerId = answerRepo.insertAnswer(
                AnswerEntity(
                    id = null,
                    questionId = newRootId,
                    title = "" // مثلاً "ورود" یا هرچی دوست داری
                )
            ).toInt()

            // 4) یال: bridgeAnswer -> oldRootQuestion
            questionRepo.upsertAnswerNextQuestion(
                AnswerNextQuestionCrossRef(
                    answerId = bridgeAnswerId,
                    nextQuestionId = oldRootId
                )
            )

            // (اختیاری) اگر می‌خواهی فوراً فوکوس/اسکرول روی ریشهٔ جدید شود:
            // newQuestionIdCreated.value = newRootId

            // برای Undo: ریشهٔ جدید را حذف می‌کنیم
            lastUndoAction = UndoAction.PromoteRoot(newRootQuestionId = newRootId)


            // 5) گزارش به UI
            _promoteRootResult.emit(
                PromoteRootResult.Success(
                    newRootQuestionId = newRootId,
                    bridgeAnswerId = bridgeAnswerId,
                    oldRootQuestionId = oldRootId
                )
            )
        }
    }


    // آخرین عملیات قابل Undo
    private var lastUndoAction: UndoAction? = null
    fun undoLastChange() {
        val action = lastUndoAction ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is UndoAction.NewChildQuestion -> {
                    // سوالی که تازه ساخته‌ایم را حذف می‌کنیم
                    val q = questionRepo.getQuestionById(action.questionId) ?: return@launch
                    questionAnswersRepo.smartDeleteQuestion(q)
                }

                is UndoAction.RestoreEdge -> {
                    // لینک Answer→Question را دوباره برمی‌گردانیم
                    questionRepo.upsertAnswerNextQuestion(
                        AnswerNextQuestionCrossRef(
                            answerId = action.answerId,
                            nextQuestionId = action.questionId
                        )
                    )
                }

                is UndoAction.RewireChild -> {
                    val ok = answerRepo.existsAnswer(action.oldAnswerId)
                    if (ok) {
                        questionAnswersRepo.rewireIncomingAnswerTo(
                            questionId = action.questionId,
                            newAnswerId = action.oldAnswerId
                        )
                    } else {
                        // اگر والد قبلی وجود ندارد، اتصال فعلیِ این سؤال را پاک کن تا ریشه شود
                        questionAnswersRepo.deleteIncomingEdgesOfQuestion(action.questionId)
                    }
                }

                is UndoAction.DeleteEdge -> {
                    questionAnswersRepo.deleteEdge(
                        answerId = action.answerId,
                        childQuestionId = action.questionId
                    )
                }

                is UndoAction.PromoteRoot -> {
                    // ریشهٔ جدید را حذف می‌کنیم (قدیمی سر جایش می‌ماند)
                    val q = questionRepo.getQuestionById(action.newRootQuestionId) ?: return@launch
                    questionAnswersRepo.smartDeleteQuestion(q)
                }

                is UndoAction.DeleteQuestionSmart -> {
                    // 1) خود سؤال را برمی‌گردانیم
                    questionRepo.insertQuestion(action.question)

                    // 2) جواب‌های متعلق به این سؤال را برمی‌گردانیم
                    action.answers.forEach { ans ->
                        answerRepo.insertAnswer(ans)
                    }

                    // 3) تصاویر جواب‌ها (✅ همین قسمت مشکل توست)
                    if (action.answerImages.isNotEmpty()) {
                        // اگر این متد را داری:
                        answerRepo.insertAnswerImages(action.answerImages)

                    }

                    // 4) تمام یال‌ها را به وضعیت قبل برمی‌گردانیم
                    questionRepo.clearRoutes()
                    questionRepo.insertRoutes(action.allEdgesBefore)
                }

                is UndoAction.InsertIntermediate -> {
                    val midQ = questionRepo.getQuestionById(action.middleQuestionId) ?: return@launch
                    // حذف هوشمند midQ معمولاً:
                    // - خودش و پاسخ داخلی‌اش را حذف می‌کند
                    // - اتصال ورودیِ midQ را به فرزندش rewire می‌کند
                    questionAnswersRepo.smartDeleteQuestion(midQ)
                }

                is UndoAction.CopyQuestionDetached -> {
                    val q = questionRepo.getQuestionById(action.newQuestionId) ?: return@launch
                    questionAnswersRepo.smartDeleteQuestion(q)
                }


            }

            // هر Undo فقط یک بار
            lastUndoAction = null
        }
    }

    fun setFilterOrder(orderId: Int?) {
        _filterOrderId.value = orderId
    }

    fun clearFilter() {
        _filterOrderId.value = null
    }

    fun onClickFreeAnswerToAttachQuestion(
        freeAnswerId: Int,
        childQuestionId: Int
    ) {
        viewModelScope.launch {
            val newAid = freeAnswerId
            val qId = childQuestionId

            // 1) پاسخ متعلق به کدام سؤال است؟
            val ownerQId = questionAnswersRepo.getOwnerQuestionIdOfAnswer(newAid) ?: return@launch

            // 2) جلوگیری از خود-والد شدن
            if (ownerQId == qId) {
                _joinChildResult.emit(JoinChildResult.InvalidSelfParent(newAid, qId))
                return@launch
            }

            // 3) اگر اتصال قبلاً وجود دارد
            if (questionAnswersRepo.edgeExists(newAid, qId)) {
                _joinChildResult.emit(JoinChildResult.AlreadyConnected(newAid, qId))
                return@launch
            }

            // 4) این جواب واقعاً باید "آزاد" باشد: نباید بچه‌ی دیگری داشته باشد
            val existingChildren = answerRepo.getChildIdsForAnswer(newAid)
            if (existingChildren.isNotEmpty()) {
                _joinChildResult.emit(
                    JoinChildResult.AnswerAlreadyHasChild(
                        answerId = newAid,
                        existingChildIds = existingChildren
                    )
                )
                return@launch
            }

            // 5) گاردِ چرخه
            val cycle = questionAnswersRepo.hasPath(fromQuestionId = qId, toQuestionId = ownerQId)
            if (cycle) {
                _joinChildResult.emit(JoinChildResult.WouldCreateCycle(newAid, qId))
                return@launch
            }

            // 6) Answer قبلی که این سؤال به آن وصل بوده (برای Undo لازم است)
            val oldIncoming = answerRepo.getIncomingAnswer(qId)
            if (oldIncoming == null) {
                questionRepo.upsertAnswerNextQuestion(
                    AnswerNextQuestionCrossRef(answerId = newAid, nextQuestionId = qId)
                )

                val undo = UndoAction.DeleteEdge(answerId = newAid, questionId = qId)

                _joinChildResult.emit(JoinChildResult.Joined(newAid, qId, undo))
                return@launch
            }

            // 7) امن است → فقط «ورودی سؤال مقصد» را به این Answer تغییر می‌دهیم
            questionAnswersRepo.rewireIncomingAnswerTo(
                questionId = qId,
                newAnswerId = newAid
            )

            // 8) برای Undo
            lastUndoAction = UndoAction.RewireChild(
                questionId = qId,
                oldAnswerId = oldIncoming.id!!,
                newAnswerId = newAid
            )

            // 9) گزارش موفقیت (همان اسنک‌بار قبلی "اتصال جدید برقرار شد.")
            val undo = UndoAction.RewireChild(
                questionId = qId,
                oldAnswerId = oldIncoming.id,
                newAnswerId = newAid
            )

            _joinChildResult.emit(JoinChildResult.Joined(newAid, qId, undo))
        }
    }

    fun changeQuestionParentToExistingAnswer(questionId: Int, newAnswerId: Int) {
        viewModelScope.launch {
            val qId = questionId
            val newAid = newAnswerId

            val ownerQId = questionAnswersRepo.getOwnerQuestionIdOfAnswer(newAid) ?: return@launch
            if (ownerQId == qId) {
                _joinChildResult.emit(JoinChildResult.InvalidSelfParent(newAid, qId))
                return@launch
            }

            val oldIncoming = answerRepo.getIncomingAnswer(qId) ?: return@launch // این منو فقط برای غیر ریشه گذاشتی
            if (oldIncoming.id == newAid) {
                _joinChildResult.emit(JoinChildResult.AlreadyConnected(newAid, qId))
                return@launch
            }

            val cycle = questionAnswersRepo.hasPath(fromQuestionId = qId, toQuestionId = ownerQId)
            if (cycle) {
                _joinChildResult.emit(JoinChildResult.WouldCreateCycle(newAid, qId))
                return@launch
            }

            // ✅ فقط همین یک تغییر
            questionAnswersRepo.replaceIncomingParent(questionId = qId, newAnswerId = newAid)

            val undo = UndoAction.RewireChild(
                questionId = qId,
                oldAnswerId = oldIncoming.id!!,
                newAnswerId = newAid
            )

            _joinChildResult.emit(JoinChildResult.Joined(newAid, qId, undo))
        }
    }

    fun setAsMainRoot(questionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            questionRepo.setRootQuestion(questionId)
            _mainRootEvent.tryEmit(MainRootEvent.ChangedByUser(questionId))
        }
    }


    fun ensureMainRootExists() {
        viewModelScope.launch(Dispatchers.IO) {
            val main = questionRepo.getMainRootIdOrNull()
            if (main != null) return@launch

            val anyRootId = questionRepo.findAnyGraphRootQuestionIdOrNull() ?: return@launch
            questionRepo.setRootQuestion(anyRootId)

            // ✅ فقط initialize است، نه تغییر توسط کاربر
            _mainRootEvent.tryEmit(MainRootEvent.Initialized(anyRootId))
        }
    }



    // رویداد یک‌بارمصرف ساخت سؤال واسط
    private val _insertIntermediateResult = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val insertIntermediateResult: SharedFlow<Unit> = _insertIntermediateResult
    fun insertIntermediateAfterAnswer(answerId: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            // 1) child فعلی این Answer
            val childIds = answerRepo.getChildIdsForAnswer(answerId)
            if (childIds.size != 1) {
                // اگر 0 یا چندتا بود، انجام نده (UI هم معمولاً اجازه نمی‌دهد)
                return@launch
            }
            val oldChildQId = childIds.first()

            // 2) ساخت سؤال واسط + یک پاسخ برایش
            val midQId = questionRepo.insertQuestion(QuestionEntity(id = null, title = "")).toInt()
            val midAId = answerRepo.insertAnswer(AnswerEntity(id = null, questionId = midQId, title = "")).toInt()

            // 3) تبدیل edge:
            //   قبل:  answerId -> oldChildQId
            //   بعد:  answerId -> midQId  و  midAId -> oldChildQId

            // حذف اتصال قبلی
            questionAnswersRepo.deleteEdge(answerId = answerId, childQuestionId = oldChildQId)

            // اتصال جدید
            questionRepo.upsertAnswerNextQuestion(
                AnswerNextQuestionCrossRef(answerId = answerId, nextQuestionId = midQId)
            )
            questionRepo.upsertAnswerNextQuestion(
                AnswerNextQuestionCrossRef(answerId = midAId, nextQuestionId = oldChildQId)
            )

            // 4) Undo: با حذف هوشمند midQ، معمولاً parent را مستقیم به oldChild وصل می‌کند
            lastUndoAction = UndoAction.InsertIntermediate(middleQuestionId = midQId)

            _insertIntermediateResult.tryEmit(Unit)
        }
    }


    fun insertIntermediateBeforeQuestion(questionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            // 1) والد (incoming answer) این سؤال
            val incoming = answerRepo.getIncomingAnswer(questionId) ?: return@launch
            val parentAnswerId = incoming.id ?: return@launch

            // اگر چند incoming داشته باشد، getIncomingAnswer معمولاً یکی را می‌دهد.
            // برای اطمینانِ کامل، بهتر است count را هم چک کنیم (اگر متد داری).
            // فعلاً همین را می‌پذیریم، یا اگر خواستی بعداً دقیق‌ترش می‌کنیم.

            // 2) ساخت سؤال واسط + پاسخ داخلش
            val midQId = questionRepo.insertQuestion(QuestionEntity(id = null, title = "")).toInt()
            val midAId = answerRepo.insertAnswer(AnswerEntity(id = null, questionId = midQId, title = "")).toInt()

            // 3) تبدیل edge:
            //   قبل: parentAnswerId -> questionId
            //   بعد: parentAnswerId -> midQId  و  midAId -> questionId

            questionAnswersRepo.deleteEdge(answerId = parentAnswerId, childQuestionId = questionId)

            questionRepo.upsertAnswerNextQuestion(
                AnswerNextQuestionCrossRef(answerId = parentAnswerId, nextQuestionId = midQId)
            )
            questionRepo.upsertAnswerNextQuestion(
                AnswerNextQuestionCrossRef(answerId = midAId, nextQuestionId = questionId)
            )

            lastUndoAction = UndoAction.InsertIntermediate(middleQuestionId = midQId)

            _insertIntermediateResult.tryEmit(Unit)
        }
    }

    private val _copyResult = MutableSharedFlow<CopyUiEvent>(extraBufferCapacity = 1)
    val copyResult: SharedFlow<CopyUiEvent> = _copyResult
    fun copyQuestionDetached(questionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            // 1) سوال اصلی + جواب‌ها را از snapshot ui بگیر (سریع و همگام با UI)
            val src = uiState.value.questionWithAnswersList.firstOrNull { it.question.id == questionId }
                ?: return@launch

            val srcQ = src.question
            val srcAnswers = src.answers.sortedBy { it.sortOrder }

            // 2) ساخت سوال جدید (بدون parent/edge)
            val newTitle = if (srcQ.title.isBlank()) "" else "${srcQ.title} (کپی)"
            val newQId = questionRepo.insertQuestion(
                QuestionEntity(id = null, title = newTitle, isRoot = false)
            ).toInt()

            // 3) ساخت پاسخ‌های جدید برای سوال جدید (بدون edge)
            val answerIdMap = mutableMapOf<Int, Int>() // oldAid -> newAid

            srcAnswers.forEach { a ->
                val oldAid = a.id ?: return@forEach
                val newAid = answerRepo.insertAnswer(
                    AnswerEntity(
                        id = null,
                        questionId = newQId,
                        title = a.title,
                        sortOrder = a.sortOrder,
                        note = a.note
                    )
                ).toInt()
                answerIdMap[oldAid] = newAid
            }

            val imagesDir = File(app.filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            for ((oldAid, newAid) in answerIdMap) {
                val oldImages = answerRepo.getImagesByAnswer(oldAid)

                val sorted = oldImages.sortedBy { it.sortOrder }
                for (oldImg in sorted) {
                    val srcFile = File(imagesDir, oldImg.fileName)
                    if (!srcFile.exists()) continue   // 👈 به جای return@forEach

                    val ext = oldImg.fileName.substringAfterLast('.', missingDelimiterValue = "")
                    val safeExt = if (ext.isBlank()) "jpg" else ext

                    val newFileName = "img_${System.currentTimeMillis()}_${System.nanoTime()}.$safeExt"
                    val dstFile = File(imagesDir, newFileName)

                    srcFile.copyTo(dstFile, overwrite = false)

                    val newEntity = oldImg.copy(
                        id = null,
                        answerId = newAid,
                        fileName = newFileName,
                        createdAt = System.currentTimeMillis()
                    )

                    answerRepo.insertImage(newEntity)
                }
            }



            // 4) Undo: حذف سوال جدید (smart delete)
            lastUndoAction = UndoAction.CopyQuestionDetached(newQuestionId = newQId)

            _copyCreated.tryEmit(newQId to questionId)
            _copyResult.tryEmit(CopyUiEvent(srcQuestionId = questionId, newQuestionId = newQId))


        }
    }



}





data class QuestionUiState(
    val currentUserEntity: UserEntity? = null,
    val questionWithAnswersList:  List<QuestionWithAnswers> = listOf(),
    val answerWithNextQuestionsList:  List<AnswerWithNextQuestions> = listOf(),
    val allAnswerEdges: List<AnswerNextQuestionCrossRef> = emptyList(),
    val appInfoEntity: AppInfoEntity? = null,
    val isDataLoaded: Boolean = false
)

data class Edge(val fromAnswerId: Int, val toQuestionId: Int)

// برای اسلات‌های هر لایه: یا یک سؤال است، یا یک گپ خالی برای عبور یال
data class Slot(
    val questionId: Int?,   // اگر null → گپ
    val isGap: Boolean
)

// پلان کلی چینش لایه‌ها
data class LayoutPlan(
    val orderedLayers: List<List<Int>>,      // فقط آیدی سؤال‌ها در هر لایه
    val slotsPerLayer: List<List<Slot>>,     // همان لایه‌ها، ولی با گپ‌ها
    val questionLayer: Map<Int, Int>,        // questionId → layerIndex
    val longEdges: Set<Edge>                 // یال‌هایی که بیش از یک لایه را رد می‌کنند
)

data class QuestionTreeFilterState(
    val orderId: Int,
    val questionIdsInPath: Set<Int?>,
    val answerIdsInPath: Set<Int?>,
    val answerIdsWithChoiceDescription: Set<Int>
)

data class CopyUiEvent(
    val srcQuestionId: Int,
    val newQuestionId: Int
)


// ✅ انواع عملیات قابل برگشت
sealed class UndoAction {
    data class NewChildQuestion(val questionId: Int) : UndoAction()
    data class RestoreEdge(val answerId: Int, val questionId: Int) : UndoAction()
    data class DeleteEdge(val answerId: Int, val questionId: Int) : UndoAction()
    data class RewireChild(val questionId: Int, val oldAnswerId: Int, val newAnswerId: Int) : UndoAction()
    data class PromoteRoot(val newRootQuestionId: Int) : UndoAction()
    data class DeleteQuestionSmart(
        val question: QuestionEntity,
        val answers: List<AnswerEntity>,
        val answerImages: List<AnswerImageEntity>,
        val allEdgesBefore: List<AnswerNextQuestionCrossRef>
    ) : UndoAction()
    data class InsertIntermediate(val middleQuestionId: Int) : UndoAction()
    data class CopyQuestionDetached(val newQuestionId: Int) : UndoAction()

}

sealed class MainRootEvent {
    data class Initialized(val questionId: Int) : MainRootEvent()
    data class ChangedByUser(val questionId: Int) : MainRootEvent()
}
