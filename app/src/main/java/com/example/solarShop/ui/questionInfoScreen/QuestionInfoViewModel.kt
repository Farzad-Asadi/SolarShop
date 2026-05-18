package com.example.solarShop.ui.questionInfoScreen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.question_answers.QuestionAnswersRepository
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
import com.example.solarShop.repo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class QuestionInfoViewModel @Inject constructor(
    @ApplicationContext private val app: Context,
    private val userRepo: UserRepository,
    private val questionRepo: QuestionRepository,
    private val answerRepo: AnswerRepository,
    appInfoRepo: AppInfoRepository,
    private val imageRepo: ImageRepository,
    private val questionAnswersRepo: QuestionAnswersRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

//region State



    // questionId که از Nav (مثلاً از کاتالوگ) آمده؛ اگر -1 باشد یعنی چیزی نفرستاده‌ایم
    private val navQuestionIdFlow: StateFlow<Int> =
        savedStateHandle.getStateFlow("questionId", -1)



    // ورودی محلی UI (فقط داخل VM نگه می‌داریم)
    private val newQuestionIdCreated = MutableStateFlow<Int?>(null)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentQuestionFlow: Flow<QuestionEntity?> =
        navQuestionIdFlow
            .map { rawId ->
                if (rawId == -1) null else rawId   // -1 یعنی چیزی نفرستاده‌ایم
            }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    questionRepo.observeQuestionById(id)
                }
            }
            .flowOn(Dispatchers.IO)


    // childQuestionId -> parentAnswerId
    private val _entryParentByQuestionId = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val entryParentByQuestionId: StateFlow<Map<Int, Int>> = _entryParentByQuestionId


    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAnswerParentFlow: Flow<AnswerEntity?> =
        combine(
            currentQuestionFlow.map { it?.id }.distinctUntilChanged(),
            entryParentByQuestionId
        ) { qId, entryMap ->
            qId to entryMap
        }.flatMapLatest { (qId, entryMap) ->
            if (qId == null) {
                flowOf(null)
            } else {
                val parentAnswerId = entryMap[qId]

                if (parentAnswerId != null) {
                    // 👈 اگر از طریق یک جواب مشخص وارد این سوال شده‌ایم
                    answerRepo.observeAnswerById(parentAnswerId)
                } else {
                    // 👈 در غیر این صورت، همان والد ساختاری
                    questionRepo.observeSingleIncomingAnswer(qId)
                }
            }
        }.flowOn(Dispatchers.IO)


    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAnswersFlow: Flow<List<AnswerEntity>?> =
        currentQuestionFlow
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    answerRepo.observeAnswersForQuestion(id)
                }
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAnswerImagesFlow: Flow<List<AnswerImageEntity>> =
        currentAnswersFlow
            .flatMapLatest { answers ->
                val ids = answers?.mapNotNull { it.id }?.distinct().orEmpty()
                if (ids.isEmpty()) flowOf(emptyList())
                else answerRepo.observeImagesForAnswerIds(ids)
            }
            .flowOn(Dispatchers.IO)



    @OptIn(ExperimentalCoroutinesApi::class)
    val answersAndAnswerChildrenFlow: Flow<List<Pair<AnswerEntity, QuestionEntity?>>> =
        currentQuestionFlow
            .flatMapLatest { q ->
                val qId = q?.id ?: return@flatMapLatest flowOf(emptyList())
                questionAnswersRepo.observeAnswersWithChildrenForQuestion(qId)
            }
            .map { list ->
                list.map { awn ->
                    awn.answer to awn.nextQuestions.firstOrNull() // ممکنه null باشه
                }
            }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()



    private val _highlightAnswerId = MutableStateFlow<Int?>(null)
    val highlightAnswerId: StateFlow<Int?> = _highlightAnswerId


    private val _visualNavDir = MutableStateFlow<VisualNavDir?>(null)
    val visualNavDir = _visualNavDir.asStateFlow()

    private fun consumeNavDir() {
        _visualNavDir.value = null
    }



    private val _uiState = MutableStateFlow(QuestionInfoUiState())
    // UiState نهایی
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<QuestionInfoUiState> =
        combine(
            appInfoFlow,
            allQuestionWithAnswers,
            allNextQuestionsForAnswer,
            currentUserFlow,
            newQuestionIdCreated,
            currentQuestionFlow,
            currentAnswerParentFlow,
            currentAnswersFlow,
            currentAnswerImagesFlow,
            answersAndAnswerChildrenFlow
        ) { arr ->

            val appInfo                 = arr[0] as AppInfoEntity?
            val questionWithAnswers     = arr[1] as List<QuestionWithAnswers>
            val nextQuestionsForAnswer  = arr[2] as List<AnswerWithNextQuestions>
            val currentUser             = arr[3] as UserEntity?
            val newQuestionId           = arr[4] as Int?
            val currentQuestion         = arr[5] as QuestionEntity?
            val currentAnswerParent     = arr[6] as AnswerEntity?
            val currentAnswers          = arr[7] as List<AnswerEntity>?
            val currentAnswerImages     = arr[8] as List<AnswerImageEntity>
            // گروه‌بندی بر اساس answerId و تبدیل به آیتم‌های UI
            val grouped: Map<Int, List<AnswerImageUi>> =
                currentAnswerImages
                    .groupBy { it.answerId ?: -1 }
                    .filterKeys { it != -1 }
                    .mapValues { (_, list) ->
                        list.map { ai ->
                            val f = File(File(app.filesDir, "images"), ai.fileName)
                            val u = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", f)
                            Log.d("IMG14", "uri=$u type=$f exists=${f.exists()} size=${f.length()} name=${f.name}")
                            AnswerImageUi(id = ai.id, uri = u)
                        }
                    }
            val answersAndAnswerChildren = arr[9] as List<Pair<AnswerEntity, QuestionEntity?>>

            QuestionInfoUiState(
                appInfoEntity = appInfo,
                questionWithAnswersList = questionWithAnswers,
                answerWithNextQuestionsList = nextQuestionsForAnswer,
                answersAndAnswerChildren=answersAndAnswerChildren,
                currentUserEntity = currentUser,
                newQuestionIdCreated = newQuestionId,
                currentQuestionEntity = currentQuestion,
                currentAnswerParentEntity = currentAnswerParent, // 👈 اینجا درست شد
                currentAnswers =currentAnswers ,
                currentAnswersImages=currentAnswerImages,
                imagesByAnswerUi = grouped,
                isDataLoaded = true
            )
        }
            .flowOn(Dispatchers.Default)
            .conflate()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = QuestionInfoUiState()
            )




//endregion State

//region funs


    fun addAnswerToQuestion(parentQuestionId: Int?, answerTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("STATE_KEYS", savedStateHandle.keys().joinToString())

            val qId = parentQuestionId ?: return@launch

            val maxOrder = answerRepo.getMaxSortOrderForQuestion(qId) ?: -1
            val newOrder = maxOrder + 1

            answerRepo.insertAnswer(
                AnswerEntity(
                    id = null,
                    questionId = qId,
                    title = answerTitle,
                    sortOrder = newOrder
                )
            )
        }
    }


    fun updateQuestionTitle(questionEntity: QuestionEntity?, title :String) {
        viewModelScope.launch {
            questionEntity?.id?.let { questionRepo.updateQuestionTitleById(it, title) }
        }
    }

    fun updateAnswerTitle(answerEntity: AnswerEntity?, title :String) {
        viewModelScope.launch {
            answerEntity?.id?.let { answerRepo.updateAnswerTitleById(it, title) }
        }
    }

    fun deleteAnswerImageEntity(answerImageEntityId: Int?) {
        if (answerImageEntityId == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val img = answerRepo.getAnswerImageById(answerImageEntityId) ?: run {
                // اگر رکورد نبود، چیزی برای حذف نیست
                return@launch
            }

            // 1) اول فایل
            val deleted = imageRepo.deleteInternalImage(img.fileName)
            Log.d("IMG_DEL", "file=${img.fileName} deleted=$deleted")

            // 2) بعد رکورد DB
            answerRepo.deleteAnswerImageEntityById(answerImageEntityId)

            // اگر می‌خوای UI سریع‌تر هم sync شه (اختیاری):
            // refreshAnswerImages(img.answerId ?: return@launch)
        }
    }


    fun onClickAnswerParent(answer: AnswerEntity?) {
        viewModelScope.launch(Dispatchers.IO) {
            val aId = answer?.id ?: return@launch
            val parentQId = answerRepo.getQuestionIdForAnswer(aId) ?: return@launch

            withContext(Dispatchers.Main) {
                _highlightAnswerId.value = aId
                _visualNavDir.value = VisualNavDir.ToParentRight
                savedStateHandle["questionId"] = parentQId
                consumeNavDir()
            }
        }
    }

    fun onClickAnswerChild(parentAnswerId: Int, answerChildId: Int?) {
        val childId = answerChildId ?: return

        // ✅ مسیر ورود را ثبت کن تا parent handle درست شود
        _entryParentByQuestionId.update { it + (childId to parentAnswerId) }

        _visualNavDir.value = VisualNavDir.ToChildLeft
        savedStateHandle["questionId"] = childId
        consumeNavDir()
    }

    private fun mapImagesUi(dbImages: List<AnswerImageEntity>): List<AnswerImageUi> =
        dbImages.sortedBy { it.createdAt }.map { img ->
            val f = File(File(app.filesDir, "images"), img.fileName)
            val u = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", f)

            AnswerImageUi(id = img.id, uri = u)
        }

    private fun refreshAnswerImages(answerId: Int) = viewModelScope.launch {
        val dbImages = withContext(Dispatchers.IO) { answerRepo.getImagesByAnswer(answerId) }
        val uiItems = mapImagesUi(dbImages)
        _uiState.update { s ->
            s.copy(imagesByAnswerUi = s.imagesByAnswerUi.toMutableMap().apply {
                put(answerId, uiItems)
            })
        }
    }

    fun importAnswerImage(answerId: Int, src: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            addAnswerImageFromUri(answerId, src)
            refreshAnswerImages(answerId)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }


    private suspend fun addAnswerImageFromUri(answerId: Int, src: Uri) {
        val (file, _) = imageRepo.saveCompressedToInternal(src = src)

        val maxOrder = answerRepo.getMaxSortOrderForAnswer(answerId) ?: -1
        val newOrder = maxOrder + 1

        val entity = AnswerImageEntity(
            id = null,
            answerId = answerId,
            fileName = file.name,
            createdAt = System.currentTimeMillis(),
            sortOrder = newOrder
        )
        answerRepo.insertImage(entity)
    }


    fun onClickAddChildQuestion(fromAnswerId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = questionRepo.getNextQuestionIdsForAnswer(fromAnswerId)
            val childId = if (existing.isNotEmpty()) {
                existing.first()
            } else {
                val newChildId = questionRepo.insertQuestion(QuestionEntity(id = null, title = "")).toInt()
                answerRepo.insertAnswer(AnswerEntity(id = null, questionId = newChildId, title = ""))
                questionRepo.upsertAnswerNextQuestion(
                    AnswerNextQuestionCrossRef(answerId = fromAnswerId, nextQuestionId = newChildId)
                )
                newChildId
            }

            withContext(Dispatchers.Main) {
                _entryParentByQuestionId.update { it + (childId to fromAnswerId) }
                _visualNavDir.value = VisualNavDir.ToChildLeft
                savedStateHandle["questionId"] = childId
                consumeNavDir()
            }
        }
    }

    fun onClickDeleteAnswer(answer: AnswerEntity?) {
        viewModelScope.launch(Dispatchers.IO) {
            val aId = answer?.id ?: return@launch

            // 1) همه تصاویر این answer را بگیر
            val imgs = answerRepo.getImagesByAnswer(aId)
            imgs.forEach { imageRepo.deleteInternalImage(it.fileName) }

            // 2) بعد خود answer (و رکوردهای مرتبط)
            answerRepo.deleteAnswer(answer)
        }
    }


    fun updateAnswerNote(answerEntity: AnswerEntity?, note: String) {
        viewModelScope.launch {
            answerEntity?.id?.let { answerRepo.updateAnswerNoteById(it, note) }
        }
    }

    fun onReorderImages( newOrder: List<AnswerImageUi>) {
        viewModelScope.launch(Dispatchers.IO) {

            newOrder.forEachIndexed { index, ui ->
                val id = ui.id ?: return@forEachIndexed
                answerRepo.updateImageOrder(id, index)
            }
        }
    }

    fun onReorderAnswers(questionId: Int, newOrder: List<AnswerEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = newOrder.mapNotNull { it.id }
            if (ids.isEmpty()) return@launch
            answerRepo.updateAnswersOrder(ids)
        }
    }

    fun createParentForRoot(rootQuestionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            // ✅ اگر واقعاً ریشه نیست (از قبل والد دارد)، فقط برو والدش
            val incoming = answerRepo.getIncomingAnswer(rootQuestionId) // (در پروژه‌ات قبلاً داشتی)
            if (incoming != null) {
                withContext(Dispatchers.Main) {
                    _highlightAnswerId.value = incoming.id
                    _visualNavDir.value = VisualNavDir.ToParentRight
                    savedStateHandle["questionId"] = incoming.questionId   // سوالِ صاحبِ این Answer = والد
                    consumeNavDir()
                }
                return@launch
            }

            // 1) ساخت سوال والد
            val parentQId = questionRepo.insertQuestion(
                QuestionEntity(id = null, title = "")
            ).toInt()

            // 2) ساخت پاسخ پل داخل والد (حداقل یک پاسخ)
            val bridgeAnswerId = answerRepo.insertAnswer(
                AnswerEntity(id = null, questionId = parentQId, title = "")
            ).toInt()

            // 3) اتصال پاسخ پل به سوال ریشه (والد جدید -> ریشه)
            questionRepo.upsertAnswerNextQuestion(
                AnswerNextQuestionCrossRef(
                    answerId = bridgeAnswerId,
                    nextQuestionId = rootQuestionId
                )
            )

            // 4) ناوبری به والد جدید + هایلایت پاسخ پل
            withContext(Dispatchers.Main) {
                // مسیر ورود برای اینکه وقتی از والد میای به بچه، handle درست باشه
                _entryParentByQuestionId.update { it + (rootQuestionId to bridgeAnswerId) }

                _highlightAnswerId.value = bridgeAnswerId
                _visualNavDir.value = VisualNavDir.ToParentRight
                savedStateHandle["questionId"] = parentQId
                consumeNavDir()
            }
        }
    }

    fun createAnswerCameraTempUri(): Pair<File, Uri> {
        return imageRepo.createCameraTempUri()
    }

    fun importAnswerImageFromCameraTemp(answerId: Int, tempFile: File) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (file, _) = imageRepo.compressCameraTempToInternal(tempFile)

                val maxOrder = answerRepo.getMaxSortOrderForAnswer(answerId) ?: -1
                val entity = AnswerImageEntity(
                    id = null,
                    answerId = answerId,
                    fileName = file.name,
                    createdAt = System.currentTimeMillis(),
                    sortOrder = maxOrder + 1
                )
                answerRepo.insertImage(entity)

                refreshAnswerImages(answerId)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }






//endregion funs


}

//region dataClasses

data class QuestionInfoUiState(
    val currentUserEntity: UserEntity? = null,
    val questionWithAnswersList:  List<QuestionWithAnswers> = listOf(),
    val answerWithNextQuestionsList:  List<AnswerWithNextQuestions> = listOf(),
    val answersAndAnswerChildren: List<Pair<AnswerEntity,QuestionEntity?>> = listOf(),
    val appInfoEntity: AppInfoEntity? = null,
    val newQuestionIdCreated :Int? =null,
    val currentQuestionEntity: QuestionEntity? = null,
    val currentAnswerParentEntity: AnswerEntity? = null,
    val currentAnswers: List<AnswerEntity>? = listOf(),
    val currentAnswersImages: List<AnswerImageEntity> = listOf(),
    val imagesByAnswerUi: Map<Int, List<AnswerImageUi>> = emptyMap(),


    val isDataLoaded: Boolean = false
)

data class AnswerImageUi(
    val id: Int?,            // id رکورد در DB (برای حذف)
    val uri: Uri             // content:// برای نمایش
)

data class AnswerFullscreenPayload(
    val title: String,
    val uris: List<Uri>,
    val startIndex: Int
)

data class QuestionBundle(
    val question: QuestionEntity?,
    val answerParent: AnswerEntity?,
    val answersAndAnswerChildren: List<Pair<AnswerEntity, QuestionEntity?>>,
    val imagesByAnswerUi: Map<Int, List<AnswerImageUi>>,
)

data class NavRequest(
    val targetQuestionId: Int,
    val dir: VisualNavDir,
    val viaParentAnswerId: Int? = null,   // 👈 مهم
    val highlightAnswerId: Int? = null    // 👈 برای برگشت به والد (هایلایت جواب)
)

data class AdjacentIds(
    val currentId: Int? = null,
    val prevId: Int? = null,
    val nextId: Int? = null
)


//endregion dataClasses

//region sealedClasses

enum class QuestionNavDirection { Up, Down, None }

enum class VisualNavDir { ToParentRight, ToChildLeft ,None }


//endregion sealedClasses