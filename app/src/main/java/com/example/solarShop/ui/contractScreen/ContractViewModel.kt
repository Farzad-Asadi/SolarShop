package com.example.solarShop.ui.contractScreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.dataStore.SessionDataStore.Keys.currentUserId
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.client.ClientRepository
import com.example.solarShop.data.room.tables.client.ClientWithOrders
import com.example.solarShop.data.room.tables.contract.ContractInstanceDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceEntity
import com.example.solarShop.data.room.tables.contract.ContractInstanceFull
import com.example.solarShop.data.room.tables.contract.ContractInstanceFullDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceNoteDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractInstancePartyDao
import com.example.solarShop.data.room.tables.contract.ContractInstancePartyEntity
import com.example.solarShop.data.room.tables.contract.ContractInstanceSectionDao
import com.example.solarShop.data.room.tables.contract.ContractInstanceSectionEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateFull
import com.example.solarShop.data.room.tables.contract.ContractTemplateFullDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateNoteEntity
import com.example.solarShop.data.room.tables.contract.ContractTemplateRepository
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionDao
import com.example.solarShop.data.room.tables.contract.ContractTemplateSectionEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.user.UserEntity
import com.example.solarShop.data.room.tables.user.UserRepository
import com.example.solarShop.utils.PdfContractExporter
import com.example.solarShop.utils.buildFinalTitle
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ContractViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val clientRepo: ClientRepository,
    private val orderRepo: OrderRepository,
    private val appInfoRepo: AppInfoRepository,
    templateFullDao: ContractTemplateFullDao, // فعلاً نیاز مستقیم نداریم؛ می‌تونی حذفش کنی اگر جای دیگه مصرف نمی‌شود
    private val templateSectionDao: ContractTemplateSectionDao,
    private val contractTemplateNoteDao: ContractTemplateNoteDao,
    private val contractRepo: ContractTemplateRepository,
    private val pdfExporter: PdfContractExporter,
    private val contractInstanceFullDao: ContractInstanceFullDao,
    private val contractInstanceNoteDao: ContractInstanceNoteDao,
    private val contractInstanceSectionDao: ContractInstanceSectionDao,
    private val contractInstanceDao: ContractInstanceDao,
    private val instancePartyDao: ContractInstancePartyDao,
    @ApplicationContext private val app: Context,
    private val savedStateHandle: SavedStateHandle,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private fun requireOrderIdArgOrNull(): Int? =
        savedStateHandle.get<Int>("orderId")?.takeIf { it != -1 }



    // ---------- Flows پایگاه‌داده ----------
    private val appInfoFlow = appInfoRepo.observeAppInfo()
    private val orderEntityListFlow = orderRepo.observeAllOrders()


    // orderId جاری
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


    private val currentUserIdFlow: Flow<Int?> =
        dataStore.data.map { prefs ->
            prefs[currentUserId].takeIf { it != -1 }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserFlow: Flow<UserEntity?> =
        currentUserIdFlow
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(null)
                else userRepo.observeUserById(id)
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentClientWithOrdersFlow: Flow<List<ClientWithOrders>?> =
        currentUserFlow
            .map { it?.userKey }
            .distinctUntilChanged()
            .flatMapLatest { userKey ->
                if (userKey == null) flowOf(null) else clientRepo.observeClientsWithOrdersByUserId(userKey)
            }
            .flowOn(Dispatchers.IO)

    // ---------- لیست تمپلیت‌ها + لودینگ ----------
    private val _isLoadingTemplates = MutableStateFlow(false)
    private val templatesFlow: StateFlow<List<ContractTemplateEntity>> =
        contractRepo.observeAll() // Flow<List<ContractTemplateEntity>>
            .onStart { _isLoadingTemplates.value = true }
            .onEach { _isLoadingTemplates.value = false }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- انتخاب تمپلیت توسط کاربر ----------
    private val _selectedTemplateId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val templateFullFlow: StateFlow<ContractTemplateFull?> =
        _selectedTemplateId
            .flatMapLatest { id ->
                if (id == null) flowOf(null) else contractRepo.observeFullById(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---------- State های UI-only که قبلاً هم داشتی ----------
    private val _editingNote = MutableStateFlow<ContractTemplateNoteEntity?>(null)
    val editingNote: StateFlow<ContractTemplateNoteEntity?> = _editingNote.asStateFlow()

    private val _pendingDeleteNoteId = MutableStateFlow<Int?>(null)
    val pendingDeleteNoteId: StateFlow<Int?> = _pendingDeleteNoteId.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _editingSection = MutableStateFlow<ContractTemplateSectionEntity?>(null)
    val editingSection: StateFlow<ContractTemplateSectionEntity?> = _editingSection.asStateFlow()

    private val _pendingDeleteSectionId = MutableStateFlow<Int?>(null)
    val pendingDeleteSectionId: StateFlow<Int?> = _pendingDeleteSectionId.asStateFlow()



    //SavedStateHandle از
    // مقدار پیش‌فرض: PROFILE
    private val entrySourceFlow: StateFlow<ContractEntrySource> =
        savedStateHandle.getStateFlow("entrySource", "profile")
            .map { s ->
                when (s.lowercase()) {
                    "order" -> ContractEntrySource.ORDER
                    else -> ContractEntrySource.PROFILE
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContractEntrySource.PROFILE)

    // ContractViewModel

    private val orderIdFlow: StateFlow<Int?> =
        savedStateHandle.getStateFlow("orderId", -1)
            .map { if (it == -1) null else it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)




    private val _currentInstanceId = MutableStateFlow<Int?>(null)
    private val currentInstanceId: StateFlow<Int?> = _currentInstanceId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val instanceFullFlow: StateFlow<ContractInstanceFull?> =
        currentInstanceId.flatMapLatest { id ->
            if (id == null) flowOf(null) else contractInstanceFullDao.observeFullById(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)



    private val _editingParties = MutableStateFlow<List<ContractInstancePartyEntity>>(emptyList())
    val editingParties: StateFlow<List<ContractInstancePartyEntity>> = _editingParties






    private val _partiesInitTried = MutableStateFlow(false)

    private val _editingInstanceSection = MutableStateFlow<ContractInstanceSectionEntity?>(null)
    val editingInstanceSection = _editingInstanceSection.asStateFlow()

    private val _editingInstanceNote = MutableStateFlow<ContractInstanceNoteEntity?>(null)
    val editingInstanceNote = _editingInstanceNote.asStateFlow()

    private val _pendingDeleteInstanceSectionId = MutableStateFlow<Int?>(null)
    val pendingDeleteInstanceSectionId = _pendingDeleteInstanceSectionId.asStateFlow()

    private val _pendingDeleteInstanceNoteId = MutableStateFlow<Int?>(null)
    val pendingDeleteInstanceNoteId = _pendingDeleteInstanceNoteId.asStateFlow()


    @OptIn(ExperimentalCoroutinesApi::class)
    private val orderInstanceFlow: StateFlow<ContractInstanceEntity?> =
        orderIdFlow.flatMapLatest { oid ->
            if (oid == null) flowOf(null) else contractInstanceDao.observeSingleByOrder(oid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)


    // ---------- UiState واحد و نهایی ----------
    val uiState: StateFlow<ContractUiState> =
        combine(
            appInfoFlow,
            currentUserFlow,
            currentClientWithOrdersFlow,
            orderEntityListFlow,
            templatesFlow,
            templateFullFlow,
            _isLoadingTemplates,
            entrySourceFlow,
            currentInstanceId,      // ⬅️ جدید
            instanceFullFlow,
            orderInstanceFlow,
            currentOrderEntity
        ) { arr ->


            val appInfo =                  arr[0] as AppInfoEntity?
            val currentUser =              arr[1] as UserEntity?
            val currentClientWithOrders =  (arr[2] as? List<*>)?.filterIsInstance<ClientWithOrders>()
            val orderList =                (arr[3] as? List<*>)?.filterIsInstance<OrderEntity>()
            val templates =                (arr[4] as? List<*>)?.filterIsInstance<ContractTemplateEntity>() ?: emptyList()
            val templateFull =             arr[5] as ContractTemplateFull?
            val isLoadingTemplates =       arr[6] as Boolean
            val entrySource =              arr[7] as ContractEntrySource
            val currentInstanceId =        arr[8]  as Int?
            val instanceFull =             arr[9]  as ContractInstanceFull?
            val orderInstance =            arr[10] as ContractInstanceEntity?
            val currentOrder =             arr[11] as OrderEntity?

            ContractUiState(
                appInfoEntity = appInfo,
                currentUserEntity = currentUser,
                currentClientWithOrders = currentClientWithOrders,
                orderEntityList = orderList,
                templates = templates,
                template = templateFull,
                isLoadingTemplates = isLoadingTemplates,
                isDataLoaded = true,
                entrySource = entrySource,
                currentInstanceId = currentInstanceId,
                instanceFull = instanceFull,
                orderExistingInstance = orderInstance,
                orderTitle = currentOrder?.name,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ContractUiState()
        )

    init {
        viewModelScope.launch {
            instanceFullFlow.collect { full ->
                val incoming = full?.parties.orEmpty()
                if (incoming.isNotEmpty()) {
                    _editingParties.value = incoming
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            combine(orderInstanceFlow, currentOrderEntity) { inst, order ->
                inst to (order?.name?.trim().orEmpty())
            }.collect { (inst, title) ->
                if (inst == null) return@collect
                if (title.isBlank()) return@collect
                if (inst.title != title) {
                    contractInstanceDao.updateTitle(inst.id, title)
                }
            }
        }

    }


    // ---------- رویدادهای واحد ----------
    fun onEvent(event: ContractEvent) {
        when (event) {
            // Template / PDF
            is ContractEvent.SelectTemplate -> {
                val templateId = event.templateId
                val src = uiState.value.entrySource

                if (src == ContractEntrySource.ORDER) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val orderId = requireOrderIdArgOrNull()
                            ?: // پیشنهاد: دیگه نرو حالت ادیت Template؛ فقط برگرد
                            return@launch

                        // ⬅️ اگر قبلاً ساخته شده، همان را باز کن
                        val existing = contractInstanceDao.getSingleByOrder(orderId)
                        if (existing != null) {
                            _currentInstanceId.value = existing.id
                            return@launch
                        }

                        val title = uiState.value.orderTitle?.trim().takeUnless { it.isNullOrEmpty() }
                            ?: "سفارش #$orderId"

                        val newInstanceId = contractRepo.createInstanceFromTemplate(
                            orderId = orderId,
                            templateId = templateId,
                            instanceTitle = title,
                            instanceDescription = ""
                        )


                        _currentInstanceId.value = newInstanceId


                        // Parties را غیرهمزمان مقداردهی اولیه کن (منتظرش نمی‌مانیم)
                        ensureInstancePartiesInitialized(
                            instanceId = newInstanceId,
                            templateId = templateId,                 // ⬅️ حالا با templateId کار می‌کنیم
                            currentUser = uiState.value.currentUserEntity,
                            orderId = orderId
                        )

                        // اگر لازم داری TemplateFull هم برای نمایش لود شود:
//                        _selectedTemplateId.value = templateId
                    }
                } else {

                    // PROFILE: مستقیم TemplateFull را لود کن
                    _selectedTemplateId.value = templateId
                }
            }

            ContractEvent.RefreshTemplates -> Unit // در حال حاضر observeAll() reactive هست


            is ContractEvent.PreviewTemplatePdf -> onPreviewTemplatePdfClicked(event.templateId)

            is ContractEvent.DeleteExistingInstance -> {
                viewModelScope.launch(Dispatchers.IO) {
                    deleteInstanceDeep(event.instanceId)
                }
            }

            ContractEvent.BackToTemplatePicker -> {
                // ✅ برای هر دو مسیر PROFILE و ORDER
                _selectedTemplateId.value = null
                _currentInstanceId.value = null

                // ادیت‌ها/دیالوگ‌ها هم جمع شوند
                _editingSection.value = null
                _editingNote.value = null
                _pendingDeleteSectionId.value = null
                _pendingDeleteNoteId.value = null

                _editingInstanceSection.value = null
                _editingInstanceNote.value = null
                _pendingDeleteInstanceSectionId.value = null
                _pendingDeleteInstanceNoteId.value = null
            }



            // Section
            is ContractEvent.EditSection -> onEditSection(event.section)
            ContractEvent.CancelEditSection -> onCancelEditSection()
            is ContractEvent.ChangeEditingSection -> onChangeEditingSection(event.transform)
            ContractEvent.SaveEditingSection -> onSaveEditingSection()
            is ContractEvent.RequestDeleteSection -> onRequestDeleteSection(event.section)
            ContractEvent.DismissDeleteSection -> onDismissDeleteSection()
            ContractEvent.ConfirmDeleteSection -> onConfirmDeleteSection()
            is ContractEvent.AddNewSection -> onAddNewSection(event.templateId)

            // Note
            is ContractEvent.EditNote -> onEditNote(event.note)
            ContractEvent.CancelEditNote -> onCancelEditNote()
            is ContractEvent.ChangeEditingNote -> onChangeEditingNote(event.transform)
            ContractEvent.SaveEditingNote -> onSaveEditingNote()
            is ContractEvent.RequestDeleteNote -> onRequestDeleteNote(event.note)
            ContractEvent.DismissDeleteNote -> onDismissDeleteNote()
            ContractEvent.ConfirmDeleteNote -> onConfirmDeleteNote()


            is ContractEvent.AddNewNote -> onAddNewNote(event.sectionId)

            // ContractEvent
            is ContractEvent.CopyTemplateWithTitle -> onCopyTemplate(event.templateId, event.newTitle)
            is ContractEvent.DeleteTemplate -> onDeleteTemplate(event.templateId)

            // Instance Parties
            is ContractEvent.ChangeInstanceParty -> onChangeInstanceParty(event.partyId, event.transform)
            ContractEvent.SaveInstanceParties -> onSaveInstanceParties()

            is ContractEvent.OpenExistingInstance -> {
                _currentInstanceId.value = event.instanceId
            }

            is ContractEvent.SaveSingleInstanceParty -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val party = _editingParties.value.firstOrNull { it.id == event.partyId } ?: return@launch
                    // فقط همین یکی
                    instancePartyDao.upsertAll(listOf(party))

                    // 2) اگر می‌خواهی همان رفتار SaveAll را داشته باشد:
                    syncProfilesFromParties()
                }
            }


        }
    }

    private suspend fun deleteInstanceDeep(instanceId: Int) {
        // اگر الان همان instance باز است، اول ببندش
        if (_currentInstanceId.value == instanceId) {
            _currentInstanceId.value = null
        }

        // اگر FKها CASCADE باشند، همین یک حذف کافی است.
        // ولی برای اطمینان (و جلوگیری از orphan) عمیق پاک می‌کنیم.
        try {
            instancePartyDao.deleteByInstance(instanceId)
        } catch (_: Throwable) {}

        try {
            // اگر برای sections/noteها DAO های bulk delete داری:
            contractInstanceNoteDao.deleteByInstance(instanceId)     // اگر نداری، پایین توضیح دادم
        } catch (_: Throwable) {}

        try {
            contractInstanceSectionDao.deleteByInstance(instanceId)  // اگر نداری، پایین توضیح دادم
        } catch (_: Throwable) {}

        // در آخر خود instance
        contractInstanceDao.deleteById(instanceId)

        // لیست UI هم خالی شود (اگر لازم شد)
        _editingParties.value = emptyList()
    }

    // ---------- منطق تمپلتها ----------
    private fun onCopyTemplate(sourceTemplateId: Int, newTitle: String) = viewModelScope.launch(Dispatchers.IO) {
        val full = contractRepo.observeFullById(sourceTemplateId).first() ?: return@launch
        val source = full.template

        val newTemplateId = contractRepo.insert(
            ContractTemplateEntity(
                id = 0,
                userKey =source.userKey ,
                title = newTitle.take(120),
                description = source.description
            )
        ).toInt()

        val sectionIdMap = mutableMapOf<Int, Int>()
        for (swn in full.sectionsWithNotes) {
            val s = swn.section
            val newSectionId = templateSectionDao.insert(
                s.copy(id = 0, templateId = newTemplateId)
            ).toInt()
            sectionIdMap[s.id] = newSectionId
            for (n in swn.notes) {
                contractTemplateNoteDao.insert(n.copy(id = 0, sectionId = newSectionId))
            }
        }
    }
    private fun onDeleteTemplate(templateId: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val rows = contractRepo.deleteById(templateId) // حالا Int برمی‌گرداند
            if (rows == 0) {
                // TODO: _uiEvents.emit(UiEvent.Snackbar("قراردادی با این شناسه پیدا نشد"))
            }
        } catch (t: Throwable) {
            // TODO: _uiEvents.emit(UiEvent.Snackbar("خطا در حذف قرارداد"))
        }
    }



    // ---------- منطق بخش‌ها ----------
    private fun onEditSection(section: ContractTemplateSectionEntity) {
        _editingSection.value = section
    }
    private fun onCancelEditSection() { _editingSection.value = null }
    private fun onChangeEditingSection(transform: (ContractTemplateSectionEntity) -> ContractTemplateSectionEntity) {
        _editingSection.update { it?.let(transform) }
    }
    private fun onSaveEditingSection() = viewModelScope.launch(Dispatchers.IO) {
        val s = _editingSection.value ?: return@launch
        if (s.id == 0) {
            templateSectionDao.insert(s.copy())
        } else {
            templateSectionDao.update(s.copy())
        }
        _editingSection.value = null
    }
    private fun onRequestDeleteSection(section: ContractTemplateSectionEntity) {
        if (section.isRequired) return
        _pendingDeleteSectionId.value = section.id
    }
    private fun onDismissDeleteSection() { _pendingDeleteSectionId.value = null }
    private fun onConfirmDeleteSection() = viewModelScope.launch(Dispatchers.IO) {
        val id = _pendingDeleteSectionId.value ?: return@launch
        val t = uiState.value.template ?: return@launch
        val section = t.sectionsWithNotes.firstOrNull { it.section.id == id }?.section ?: return@launch

        templateSectionDao.delete(section)

        // رینامبر اختیاری
        try {
            val remaining = templateSectionDao.allByTemplate(section.templateId).sortedBy { it.orderNo }
            remaining.forEachIndexed { index, s ->
                val desired = index + 1
                if (s.orderNo != desired) templateSectionDao.updateOrder(s.id, desired)
            }
        } catch (_: Throwable) {}

        _pendingDeleteSectionId.value = null
    }
    private fun onAddNewSection(templateId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val all = templateSectionDao.allByTemplate(templateId)
        val nextOrder = (all.maxOfOrNull { it.orderNo } ?: 0) + 1
        _editingSection.value = ContractTemplateSectionEntity(
            id = 0,
            templateId = templateId,
            orderNo = nextOrder,
            title = buildFinalTitle(nextOrder, "عنوان ماده جدید"),
            body = "",
            isDefaultVisible = true,
            isRequired = false
        )
    }

    // ---------- منطق تبصره‌ها ----------
    private fun onEditNote(note: ContractTemplateNoteEntity) { _editingNote.value = note }
    private fun onCancelEditNote() { _editingNote.value = null }
    private fun onChangeEditingNote(transform: (ContractTemplateNoteEntity) -> ContractTemplateNoteEntity) {
        _editingNote.update { it?.let(transform) }
    }
    private fun onSaveEditingNote() = viewModelScope.launch(Dispatchers.IO) {
        val n = _editingNote.value ?: return@launch
        if (n.id == 0) contractTemplateNoteDao.insert(n.copy()) else contractTemplateNoteDao.update(n.copy())
        _editingNote.value = null
    }
    private fun onRequestDeleteNote(note: ContractTemplateNoteEntity) {
        _pendingDeleteNoteId.value = note.id
    }
    private fun onDismissDeleteNote() { _pendingDeleteNoteId.value = null }
    private fun onConfirmDeleteNote() = viewModelScope.launch(Dispatchers.IO) {
        val noteId = _pendingDeleteNoteId.value ?: return@launch
        val t = uiState.value.template ?: return@launch

        val sectionId = t.sectionsWithNotes
            .asSequence()
            .flatMap { swn -> swn.notes.asSequence().map { n -> n to swn.section.id } }
            .firstOrNull { (n, _) -> n.id == noteId }
            ?.second ?: return@launch

        contractRepo.deleteNoteAndRenumber(sectionId, noteId)
        _pendingDeleteNoteId.value = null
    }
    private fun onAddNewNote(sectionId: Int) = viewModelScope.launch(Dispatchers.IO) {
        // ترتیب فعلی تبصره‌های این ماده
        val current = contractTemplateNoteDao.bySection(sectionId)
        val nextOrder = (current.maxOfOrNull { it.orderNo } ?: 0) + 1

        // پیش‌نویس تبصره‌ی جدید (id=0 یعنی هنوز ذخیره نشده)
        val draft = ContractTemplateNoteEntity(
            id = 0,
            sectionId = sectionId,
            orderNo = nextOrder,
            title = null,
            body = ""
        )

        _editingNote.value = draft
    }

    // ---------- PDF ----------
    private fun onPreviewTemplatePdfClicked(templateId: Int) {
        if (_isExporting.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val file = pdfExporter.exportTemplate(templateId)
                openPdf(file)
            } catch (_: Throwable) {
                // TODO: Snackbar از طریق UiEvent
            } finally {
                _isExporting.value = false
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(intent)
    }


    //instance
    fun onInstanceEvent(event: ContractInstanceEvent) {
        when (event) {
            // Parties
            is ContractInstanceEvent.ChangeParty -> onChangeInstanceParty(event.partyId, event.transform)
            ContractInstanceEvent.SaveParties -> onSaveInstanceParties()

            // Sections
            is ContractInstanceEvent.EditSection -> _editingInstanceSection.value = event.section
            ContractInstanceEvent.CancelEditSection -> _editingInstanceSection.value = null
            is ContractInstanceEvent.ChangeEditingSection ->
                _editingInstanceSection.update { it?.let(event.transform) }
            is ContractInstanceEvent.SaveEditingSection -> onSaveEditingInstanceSection(event.instanceId)
            is ContractInstanceEvent.AddNewSection -> onAddNewInstanceSection(event.instanceId)
            is ContractInstanceEvent.RequestDeleteSection -> _pendingDeleteInstanceSectionId.value = event.sectionId
            ContractInstanceEvent.DismissDeleteSection -> _pendingDeleteInstanceSectionId.value = null
            ContractInstanceEvent.ConfirmDeleteSection -> onConfirmDeleteInstanceSection()

            // Notes
            is ContractInstanceEvent.EditNote -> _editingInstanceNote.value = event.note
            ContractInstanceEvent.CancelEditNote -> _editingInstanceNote.value = null
            is ContractInstanceEvent.ChangeEditingNote ->
                _editingInstanceNote.update { it?.let(event.transform) }
            is ContractInstanceEvent.SaveEditingNote -> onSaveEditingInstanceNote(event.sectionId)
            is ContractInstanceEvent.AddNewNote -> onAddNewInstanceNote(event.sectionId)
            is ContractInstanceEvent.RequestDeleteNote -> _pendingDeleteInstanceNoteId.value = event.noteId
            ContractInstanceEvent.DismissDeleteNote -> _pendingDeleteInstanceNoteId.value = null
            ContractInstanceEvent.ConfirmDeleteNote -> onConfirmDeleteInstanceNote()

            // Preview
            is ContractInstanceEvent.PreviewPdf -> onPreviewInstancePdf(event.instanceId)
        }
    }

    private fun String?.nz(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    private fun ensureInstancePartiesInitialized(
        instanceId: Int,
        templateId: Int,
        currentUser: UserEntity?,
        orderId: Int?,
    ) = viewModelScope.launch(Dispatchers.IO) {

        // ✅ اگر قبلاً هست، همون رو نمایش بده
        val existing = instancePartyDao.getByInstance(instanceId)
        if (existing.isNotEmpty()) {
            _editingParties.value = existing
            return@launch
        }

        val tpl = contractRepo.getTemplateFullById(templateId) ?: return@launch
        val clientOfThisOrder: ClientEntity? = uiState.value.currentClientWithOrders
            ?.firstOrNull { cwo -> cwo.orders.any { it.id == orderId } }
            ?.clientEntity

        fun String?.nz(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
        fun String.norm() = trim().lowercase()
        fun isContractor(role: String) = role.norm().let {
            it.contains("پیمانکار") || it.contains("مجری") || it.contains("contractor") || it.contains("seller") || it.contains("vendor")
        }
        fun isClient(role: String) = role.norm().let {
            it.contains("خریدار") || it.contains("کارفرما") || it.contains("مشتری") || it.contains("employer") || it.contains("buyer") || it.contains("client")
        }

        val hasContractorRole = tpl.parties.any { isContractor(it.role) }
        val hasClientRole     = tpl.parties.any { isClient(it.role) }

        val items = tpl.parties.mapIndexed { index, tp ->
            val role = tp.role

            val useClient = when {
                isClient(role) -> true
                !hasClientRole && hasContractorRole -> index != tpl.parties.indexOfFirst { isContractor(it.role) }
                !hasClientRole && !hasContractorRole && tpl.parties.size == 2 -> index == 0
                else -> false
            }
            val useUser = when {
                isContractor(role) -> true
                !hasContractorRole && hasClientRole -> index != tpl.parties.indexOfFirst { isClient(it.role) }
                !hasClientRole && !hasContractorRole && tpl.parties.size == 2 -> index == 1
                else -> false
            }

            val c = if (useClient) clientOfThisOrder else null
            val u = if (useUser) currentUser else null

            ContractInstancePartyEntity(
                id = 0,
                instanceId = instanceId,
                role = role,
                fullName = (c?.name ?: u?.name).nz(),
                fatherFullName = null,
                nationalId = (c?.nationalCode ?: u?.nationalCode).nz(),
                companyName = (c?.workshop ?: u?.workshop).nz(),
                address = (c?.address ?: u?.address).nz(),
                phone = (c?.mobilePhone ?: u?.mobilePhone).nz()
            )
        }

        // ✅ با IGNORE کرش نمی‌کنه
        instancePartyDao.insertAll(items)

        // ✅ همیشه بعدش از DB بخون و state رو پر کن (برای نمایش)
        _editingParties.value = instancePartyDao.getByInstance(instanceId)
    }


    private fun onChangeInstanceParty(
        partyId: Int,
        transform: (ContractInstancePartyEntity) -> ContractInstancePartyEntity
    ) {
        _editingParties.update { list ->
            list.map { if (it.id == partyId) transform(it) else it }
        }
    }



    private fun onSaveInstanceParties() = viewModelScope.launch(Dispatchers.IO) {
        try {
            // 1) خود Party ها ذخیره شوند
            instancePartyDao.upsertAll(_editingParties.value)

            // 2) همگام‌سازی User / Client بر اساس نقش‌ها
            syncProfilesFromParties()

        } catch (_: Throwable) {
            // TODO: خطا
        }
    }


    private suspend fun syncProfilesFromParties() {
        // فقط وقتی از مسیر سفارش آمده‌ایم معنی دارد
        if (uiState.value.entrySource != ContractEntrySource.ORDER) return

        val orderId = requireOrderIdArgOrNull() ?: return
        val currentUser = uiState.value.currentUserEntity ?: return

        // مشتریِ همین سفارش را پیدا کن (مثل init خودت)
        val clientOfThisOrder: ClientEntity? = uiState.value.currentClientWithOrders
            ?.firstOrNull { cwo -> cwo.orders.any { it.id == orderId } }
            ?.clientEntity

        val parties = _editingParties.value
        if (parties.isEmpty()) return

        val contractorParty = parties.firstOrNull { isContractorRole(it.role) }
        val buyerParty      = parties.firstOrNull { isClientRole(it.role) }

        // --- 1) پیمانکار -> UserEntity (به جز موبایل) ---
        if (contractorParty != null) {
            val updatedUser = currentUser.copy(
                name = contractorParty.fullName.nz() ?: currentUser.name,
                nationalCode = contractorParty.nationalId.nz() ?: currentUser.nationalCode,
                workshop = contractorParty.companyName.nz() ?: currentUser.workshop,
                address = contractorParty.address.nz() ?: currentUser.address,
                // ✅ موبایل را دست نمی‌زنیم
                // mobilePhone = currentUser.mobilePhone
            )
            userRepo.updateUser (updatedUser) // اگر متد اسمش فرق دارد، همینجا اصلاح کن
        }

        // --- 2) خریدار -> ClientEntity ---
        if (buyerParty != null && clientOfThisOrder != null) {
            val updatedClient = clientOfThisOrder.copy(
                name = buyerParty.fullName.nz() ?: clientOfThisOrder.name,
                nationalCode = buyerParty.nationalId.nz() ?: clientOfThisOrder.nationalCode,
                workshop = buyerParty.companyName.nz() ?: clientOfThisOrder.workshop,
                address = buyerParty.address.nz() ?: clientOfThisOrder.address,
                // درباره موبایل/تلفن چیزی نگفتی؛
                // اگر می‌خواهی تلفنِ مشتری هم sync شود این خط را فعال کن:
                // mobilePhone = buyerParty.phone.nz() ?: clientOfThisOrder.mobilePhone
            )
            clientRepo.updateClient (updatedClient) // اگر متد اسمش فرق دارد، همینجا اصلاح کن
        }
    }



    // ===== Instance Sections =====
    private fun onEditInstanceSection(section: ContractInstanceSectionEntity) {
        _editingInstanceSection.value = section
    }
    private fun onCancelEditInstanceSection() { _editingInstanceSection.value = null }
    private fun onChangeEditingInstanceSection(
        transform: (ContractInstanceSectionEntity) -> ContractInstanceSectionEntity
    ) { _editingInstanceSection.update { it?.let(transform) } }

    private fun onSaveEditingInstanceSection(instanceId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val s = _editingInstanceSection.value ?: return@launch
        if (s.id == 0) {
            val nextOrder = contractInstanceSectionDao.maxOrder(instanceId) + 1
            contractInstanceSectionDao.insert(
                s.copy(instanceId = instanceId, orderNo = if (s.orderNo <= 0) nextOrder else s.orderNo)
            )
        } else {
            contractInstanceSectionDao.update(s.copy())
        }
        _editingInstanceSection.value = null
    }

    private fun onAddNewInstanceSection(instanceId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val nextOrder = contractInstanceSectionDao.maxOrder(instanceId) + 1
        _editingInstanceSection.value = ContractInstanceSectionEntity(
            id = 0,
            instanceId = instanceId,
            orderNo = nextOrder,
            title = "",
            body = ""
        )
    }

    private fun onConfirmDeleteInstanceSection() = viewModelScope.launch(Dispatchers.IO) {
        val id = _pendingDeleteInstanceSectionId.value ?: return@launch
        val full = uiState.value.instanceFull ?: return@launch
        val section = full.sectionsWithNotes.firstOrNull { it.section.id == id }?.section ?: return@launch

        contractInstanceSectionDao.delete(section)

        // رینامبر 1..N
        val remaining = contractInstanceSectionDao.allByInstance(section.instanceId).sortedBy { it.orderNo }
        remaining.forEachIndexed { index, s ->
            val desired = index + 1
            if (s.orderNo != desired) contractInstanceSectionDao.updateOrder(s.id, desired)
        }

        _pendingDeleteInstanceSectionId.value = null
    }

    // ===== Instance Notes =====
    private fun onSaveEditingInstanceNote(sectionId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val n = _editingInstanceNote.value ?: return@launch
        if (n.id == 0) {
            val nextOrder = contractInstanceNoteDao.maxOrder(sectionId) + 1
            contractInstanceNoteDao.insert(
                n.copy(instanceSectionId = sectionId, orderNo = if (n.orderNo <= 0) nextOrder else n.orderNo)
            )
        } else {
            contractInstanceNoteDao.update(n.copy())
        }
        _editingInstanceNote.value = null
    }

    private fun onAddNewInstanceNote(sectionId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val nextOrder = contractInstanceNoteDao.maxOrder(sectionId) + 1
        _editingInstanceNote.value = ContractInstanceNoteEntity(
            id = 0,
            instanceSectionId = sectionId,
            orderNo = nextOrder,
            title = null,
            body = ""
        )
    }

    private fun onConfirmDeleteInstanceNote() = viewModelScope.launch(Dispatchers.IO) {
        val noteId = _pendingDeleteInstanceNoteId.value ?: return@launch
        val full = uiState.value.instanceFull ?: return@launch

        val secId: Int = full.sectionsWithNotes
            .firstNotNullOfOrNull { swn -> swn.notes.firstOrNull { it.id == noteId }?.let { swn.section.id } }
            ?: return@launch

        val note = contractInstanceNoteDao.getById(noteId) ?: return@launch
        contractInstanceNoteDao.delete(note)

        // رینامبر نوت‌های باقی‌مانده
        val remaining = contractInstanceNoteDao.allByInstanceSection(secId).sortedBy { it.orderNo }
        remaining.forEachIndexed { index, e ->
            val desired = index + 1
            if (e.orderNo != desired) contractInstanceNoteDao.updateOrder(e.id, desired)
        }

        _pendingDeleteInstanceNoteId.value = null
    }

    // ===== Instance Preview =====
    private fun onPreviewInstancePdf(instanceId: Int) {
        if (_isExporting.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val full = contractInstanceFullDao.getFullById(instanceId) ?: return@launch
                val file = pdfExporter.exportInstance(full)   // 👈 حالا اینستنس
                openPdf(file)
            } catch (_: Throwable) {
                // TODO: Snackbar
            } finally {
                _isExporting.value = false
            }
        }
    }


    private fun String.norm() = trim().lowercase()

    private fun isContractorRole(role: String): Boolean {
        val r = role.norm()
        return r.contains("پیمانکار") || r.contains("مجری") ||
                r.contains("contractor") || r.contains("seller") || r.contains("vendor")
    }

    private fun isClientRole(role: String): Boolean {
        val r = role.norm()
        return r.contains("خریدار") || r.contains("کارفرما") || r.contains("مشتری") ||
                r.contains("employer") || r.contains("buyer") || r.contains("client")
    }




}

// -------------------- UI State & Events --------------------








data class ContractUiState(
    val appInfoEntity: AppInfoEntity? = null,
    val currentUserEntity: UserEntity? = null,
    val currentClientWithOrders: List<ClientWithOrders>? = null,
    val currentUserClientEntityList: List<ClientEntity>? = listOf(),
    val clientWithOrdersList: List<ClientWithOrders>? = listOf(),
    val orderEntityList: List<OrderEntity>? = listOf(),
    val template: ContractTemplateFull? = null,              // تمپلیت انتخاب‌شده
    val templates: List<ContractTemplateEntity> = emptyList(),// لیست برای پنجرهٔ انتخاب
    val isLoadingTemplates: Boolean = false,
    val isDataLoaded: Boolean = false,
    val entrySource: ContractEntrySource = ContractEntrySource.PROFILE,
    val currentInstanceId: Int? = null,
    val instanceFull: ContractInstanceFull? = null,
    val orderExistingInstance: ContractInstanceEntity? = null,
    val orderTitle: String? = null,
)

data class PartyFormState(
    val fullName: String = "",
    val mobileNational: String = "",   // برای MyPhoneField (10 رقم بعد از 9)
    val landline: String = "",
    val nationalCode: String = "",
    val companyName: String = "",
    val address: String = "",
    val isMobileValid: Boolean = true,
    val isLandlineValid: Boolean = true,
    val isNationalValid: Boolean = true,
)




sealed interface UiEvent {
    data class Snackbar(val message: String) : UiEvent
}

sealed interface ContractEvent {
    // Template / Picker / PDF
    data class SelectTemplate(val templateId: Int) : ContractEvent
    data object RefreshTemplates : ContractEvent
    data class PreviewTemplatePdf(val templateId: Int) : ContractEvent

    data class DeleteExistingInstance(val instanceId: Int) : ContractEvent


    // Section
    data class EditSection(val section: ContractTemplateSectionEntity) : ContractEvent
    data object CancelEditSection : ContractEvent
    data class ChangeEditingSection(
        val transform: (ContractTemplateSectionEntity) -> ContractTemplateSectionEntity
    ) : ContractEvent
    data object SaveEditingSection : ContractEvent
    data class RequestDeleteSection(val section: ContractTemplateSectionEntity) : ContractEvent
    data object DismissDeleteSection : ContractEvent
    data object ConfirmDeleteSection : ContractEvent
    data class AddNewSection(val templateId: Int) : ContractEvent

    // Note
    data class EditNote(val note: ContractTemplateNoteEntity) : ContractEvent
    data object CancelEditNote : ContractEvent
    data class ChangeEditingNote(
        val transform: (ContractTemplateNoteEntity) -> ContractTemplateNoteEntity
    ) : ContractEvent
    data object SaveEditingNote : ContractEvent
    data class RequestDeleteNote(val note: ContractTemplateNoteEntity) : ContractEvent
    data object DismissDeleteNote : ContractEvent
    data object ConfirmDeleteNote : ContractEvent
    data class AddNewNote(val sectionId: Int) : ContractEvent

    // ContractEvent
    data class CopyTemplateWithTitle(val templateId: Int, val newTitle: String) : ContractEvent
    data class DeleteTemplate(val templateId: Int) : ContractEvent

    // Instance Parties
    data class ChangeInstanceParty(
        val partyId: Int, // یا اگر id نداریم، با index هم میشه
        val transform: (ContractInstancePartyEntity) -> ContractInstancePartyEntity
    ) : ContractEvent

    data object SaveInstanceParties : ContractEvent

    data class OpenExistingInstance(val instanceId: Int) : ContractEvent

    data class SaveSingleInstanceParty(val partyId: Int) : ContractEvent


    data object BackToTemplatePicker : ContractEvent

}

sealed interface ContractInstanceEvent {
    // Parties
    data class ChangeParty(
        val partyId: Int,
        val transform: (ContractInstancePartyEntity) -> ContractInstancePartyEntity
    ) : ContractInstanceEvent
    data object SaveParties : ContractInstanceEvent

    // Sections
    data class EditSection(val section: ContractInstanceSectionEntity) : ContractInstanceEvent
    data object CancelEditSection : ContractInstanceEvent
    data class ChangeEditingSection(
        val transform: (ContractInstanceSectionEntity) -> ContractInstanceSectionEntity
    ) : ContractInstanceEvent
    data class SaveEditingSection(val instanceId: Int) : ContractInstanceEvent
    data class AddNewSection(val instanceId: Int) : ContractInstanceEvent
    data class RequestDeleteSection(val sectionId: Int) : ContractInstanceEvent
    data object DismissDeleteSection : ContractInstanceEvent
    data object ConfirmDeleteSection : ContractInstanceEvent

    // Notes
    data class EditNote(val note: ContractInstanceNoteEntity) : ContractInstanceEvent
    data object CancelEditNote : ContractInstanceEvent
    data class ChangeEditingNote(
        val transform: (ContractInstanceNoteEntity) -> ContractInstanceNoteEntity
    ) : ContractInstanceEvent
    data class SaveEditingNote(val sectionId: Int) : ContractInstanceEvent
    data class AddNewNote(val sectionId: Int) : ContractInstanceEvent
    data class RequestDeleteNote(val noteId: Int) : ContractInstanceEvent
    data object DismissDeleteNote : ContractInstanceEvent
    data object ConfirmDeleteNote : ContractInstanceEvent

    // Preview (اختیاری)
    data class PreviewPdf(val instanceId: Int) : ContractInstanceEvent
}



enum class ContractEntrySource { PROFILE, ORDER }