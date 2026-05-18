package com.example.solarShop.ui.orderScreen.orderPriceEstimate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.EPS
import com.example.solarShop.LengthUnit
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.data.dataStore.DisplayPreferencesDataSource
import com.example.solarShop.data.room.tables.appInfo.AppInfoEntity
import com.example.solarShop.data.room.tables.appInfo.AppInfoRepository
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.order.OrderRepository
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateEntity
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateRepository
import com.example.solarShop.utils.EstimateCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToLong


@HiltViewModel
class PriceEstimateViewModel @Inject constructor(
    private val orderRepo: OrderRepository,
    displayPrefs: DisplayPreferencesDataSource,
    private val priceEstimateRepo: PriceEstimateRepository,
    appInfoRepo: AppInfoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var isDirty = false


    //region  uiState




    private val appInfoFlow = appInfoRepo.observeAppInfo()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentEstimateEntityFlow: Flow<PriceEstimateEntity?> =
        currentOrderIdFlow
            .filterNotNull()
            .flatMapLatest { orderId ->
                priceEstimateRepo.observeCabinetEstimate(orderId) ?: flowOf(null)
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val closetEntityFlow: Flow<PriceEstimateEntity?> =
        currentOrderIdFlow
            .filterNotNull()
            .flatMapLatest { orderId ->
                priceEstimateRepo.observe(orderId, EstimateCategory.CLOSET)
            }
            .flowOn(Dispatchers.IO)




    //cabinet
    private val _caInputs = MutableStateFlow(CabinetEstimateInputs())
    private val _caMarket = MutableStateFlow(CabinetMarketParams())

    // تنظیمات طول/پول از DataStore (سراسری)
    private val settingsFlow: StateFlow<DisplayPreferences> =
        displayPrefs.prefsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DisplayPreferences()
        )

    //جهت نشان دادن قیمت تخمینی در صفحه سفارش
    @OptIn(ExperimentalCoroutinesApi::class)
    val savedEstimateTotalFlow: StateFlow<Long> =
        currentEstimateEntityFlow
            .filterNotNull()   // ← اضافه کن
            .mapLatest { entity ->
                val inputs = priceEstimateRepo.loadCabinetInputs(entity)
                val market = buildMarketFrom(entity)
                computeCabinetEstimate(inputs, market).totalPrice
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val cabinetEstimateState: StateFlow<CabinetEstimateState> =
        combine(_caInputs, _caMarket, settingsFlow) { inp, mk, settings ->
            val cabinetPriceBreakdown = computeCabinetEstimate(inp, mk)
            CabinetEstimateState(
                isLoading = false,
                inputs = inp,
                market = mk,
                breakdown = cabinetPriceBreakdown,
                settings = settings,
                error = null
            )
        }
            .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CabinetEstimateState()
        )



    //Closet
    private val _clInputs = MutableStateFlow(ClosetEstimateInputs())
    private val _clMarket = MutableStateFlow(ClosetMarketParams())

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedClosetTotalFlow: StateFlow<Long> =
        closetEntityFlow
            .filterNotNull()
            .mapLatest { entity ->
//                val inputs = priceEstimateRepo.loadClosetInputs(entity)
                val inputs = priceEstimateRepo.loadClosetInputs(entity)
                val market = buildClosetMarketFrom(entity)
                computeClosetSimple(inputs, market).totalPrice
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val closetEstimateState: StateFlow<ClosetEstimateState> =
        combine(_clInputs, _clMarket, settingsFlow) { inp, mk , settings ->
            val closetPriceBreakdown = computeClosetSimple(inp, mk)
            ClosetEstimateState(
                isLoading = false,
                inputs = inp,
                market = mk,
                breakdown = closetPriceBreakdown,
                settings = settings,
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClosetEstimateState())

    // نگه داشتن تب انتخابی در ویومدل (از UI ست می‌شود)
    private val _selectedEstimateTab = MutableStateFlow(0) // 0 = کابینت, 1 = کمد
    val selectedEstimateTab: StateFlow<Int> = _selectedEstimateTab
    fun setSelectedEstimateTab(i: Int) { _selectedEstimateTab.value = i }

    // قیمت‌های لایو هر تب
    private val cabinetLiveTotalFlow: Flow<Long> =
        cabinetEstimateState.map { it.breakdown?.totalPrice ?: 0L }

    private val closetLiveTotalFlow: Flow<Long> =
        closetEstimateState.map { it.breakdown?.totalPrice ?: 0L }

    // آیا کاربر داخل پنجرهٔ تخمین است؟ (از UI ست کن)
    private val _isPriceWindowOpen = MutableStateFlow(false)
    fun setPriceWindowOpen(open: Boolean) { _isPriceWindowOpen.value = open }

    // اولویت نهایی برای نمایش
    val displayableTotalFlow: StateFlow<Long> =
        combine(_isPriceWindowOpen, _selectedEstimateTab, cabinetLiveTotalFlow, closetLiveTotalFlow, savedEstimateTotalFlow) { open, tab, cab, clo, saved ->
            when {
                open && tab == 1 -> clo      // تب کمد باز است
                open && tab == 0 -> cab      // تب کابینت باز است
                else             -> saved    // خارج از پنجره، آخرین ذخیره
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)



    // UiState نهایی
    val uiState: StateFlow<PriceEstimateUiState> =
        combine(
            appInfoFlow,
            currentOrderEntity,
            displayableTotalFlow
//            currentUserFlow,
//            currentClientEntity,
//            currentOrderWithTimelineItem,
//            rootQuestionFlow,
//            currentAnswerItemUnitFlow,
//            currentQuestionFlow,
//            totalAnsweredFlow,
//            answeredOrderFlow,
//            totalInLineFlow
        ) { arr ->
            val appInfo = arr[0] as AppInfoEntity?
            val currentOrder = arr[1] as OrderEntity?
            val displayableTotal = arr[2] as Long?
//            val currentUser = arr[1] as UserEntity?
//            val currentClient = arr[3] as ClientEntity?
//            val orderWithTimelineItem = arr[4] as OrderWithTimelineItem?
//            val rootQuestionFlow = arr[5] as QuestionEntity?
//            val currentAnswerItemUnit =
//                (arr[6] as? List<*>)?.filterIsInstance<com.example.bambo.ui.orderScreen.AnswerItemUnit>().orEmpty()
//            val currentQuestion = arr[7] as QuestionEntity?
//            val answeredOrder = arr[8] as Int?
//            val totalAnswered = arr[9] as Int?
//            val totalInLine = arr[10] as Int




            PriceEstimateUiState(
                appInfoEntity = appInfo,
                currentOrderEntity = currentOrder,
                displayableTotal = displayableTotal,
//                currentUserEntity = currentUser,
//                currentClientEntity = currentClient,
//                currentOrderWithTimelineItem = orderWithTimelineItem,
//                firstQuestion = rootQuestionFlow,
//                currentAnswerItemUnit = currentAnswerItemUnit,
//                currentQuestion = currentQuestion,
//                answeredOrder = answeredOrder,
//                totalAnswered = totalAnswered,
//                totalInLine = totalInLine,
                isDataLoaded = true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PriceEstimateUiState(),
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            currentOrderIdFlow
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { orderId ->
                    // ← سفارش عوض شد: از صفر شروع کن
                    isDirty = false

                    // کابینت:
                    val caEntity = priceEstimateRepo.getOrCreateCabinetEstimate(orderId, EstimateCategory.CABINET, title = "کابینت")
                    val caInputs = priceEstimateRepo.loadCabinetInputs(caEntity)
                    val caMarket = buildMarketFrom(caEntity)

                    // ← DB باید برنده باشه تا اولین UI رفرش صحیح باشه
                    _caInputs.value = caInputs
                    _caMarket.value = caMarket



                    // کمد:
                    val entity = priceEstimateRepo.getOrCreateCabinetEstimate(orderId, EstimateCategory.CLOSET, title = "کمد")
                    val inputs = priceEstimateRepo.loadClosetInputs(entity)
                    val market = buildClosetMarketFrom(entity) // پایین
                    _clInputs.value = inputs
                    _clMarket.value = market


                    val cabHas = caEntity.hasMeaningfulData()
                    val cloHas = entity.hasMeaningfulData()

                    val tab = when {
                        !cabHas && !cloHas -> 0 // اولین بار: کابینت
                        cabHas && !cloHas -> 0
                        !cabHas && cloHas -> 1
                        else -> if (entity.updatedAt > caEntity.updatedAt) 1 else 0 // هر دو داده دارند
                    }
                    _selectedEstimateTab.value = tab
                    if (!cabHas && !cloHas) {
                        _caInputs.update { it.copy(mode = CabinetMode.PRO) }
                    }


                }
        }
    }

    //endregion uiState


    //region CabinetPriceEstimateFuns
    private fun clampMeters(v: Double) = v.coerceIn(0.0, 30.0)

    private fun roundToHalf(v: Double) = kotlin.math.round(v * 2.0) / 2.0

    private fun applyStep(currentMeters: Double, deltaMeters: Double): Double {
        return clampMeters(roundToHalf(currentMeters + deltaMeters))
    }

    private fun dimensionDeltaPrice(
        pricePerMeter: Long,
        unitMeter: Double,              // m
        shareMultiplier: Double,        // 1.0 = 100%, 2.0 = 200% (نه درصد! ضریب)
        current: Double,                // همان واحدِ baseline (اینجا متر)
        baseline: Double,               // مقدار مبنا (متر)
        elasticity: Double = 1.0,       // 1=خطی، >1 حساس‌تر، <1 ملایم‌تر
        minCurrent: Double = 0.10,      // حداقل منطقی (مثلاً 10cm = 0.10m)
        maxCurrent: Double = 3.00,      // حداکثر منطقی (مثلاً 3m)
        maxUpPct: Int = 200,            // سقف افزایش نسبت به basePrice (مثلاً 200% = 2x)
        maxDownPct: Int = 50            // سقف کاهش (مثلاً 50% = نصف)
    ): Long {
        // هم‌واحدی: current و baseline هر دو «متر»
        val c = current.coerceIn(minCurrent, maxCurrent).coerceAtLeast(1e-6)

        // همه چیز روی Double تا از پرش/گرد شدن Long جلوگیری شود
        val basePrice = pricePerMeter.toDouble() * unitMeter * shareMultiplier

        // نسبتِ انحراف از مبنا
        val factor = (c / baseline).pow(elasticity)    // baseline → 1.0

        // دلتا: اگر current = baseline → factor=1 → delta=0
        val rawDelta = basePrice * (factor - 1.0)

        // کلمپ احتیاطی: نگذاریم از محدوده‌های معقول خارج شود
        val maxUp   = basePrice * (maxUpPct / 100.0)    // حداکثر افزایش قابل‌قبول
        val maxDown = -basePrice * (maxDownPct / 100.0) // حداکثر کاهش قابل‌قبول

        val clamped = rawDelta.coerceIn(maxDown, maxUp)

        return clamped.roundToLong()
    }

    private fun computeCabinetEstimate(
        inputs: CabinetEstimateInputs,
        market: CabinetMarketParams,
    ): CabinetPriceBreakdown {

        val totalMeter = inputs.effectiveTotalMeter()


        // 1) ورودی مؤثر + ماسک ReadOnly (ماسک را برای UI در state می‌فرستیم – پایین‌تر نشان می‌دهم)
//        val (eff, _) = deriveInputsForCompute(inputs)

        // 2) از این به بعد فقط eff
        val pricePerMeter: Long = market.pricePerMeter
        val platePrice: Long = market.platePrice
        val installFeePerMeter: Long = market.installFeePerMeter
        val transportation: Long = market.transportation

        // اندازه‌ها (نمونه: قبلی‌ها را دست نمی‌زنیم)
        val totalMeterPrice = (pricePerMeter * totalMeter).toLong()
        val installTotal = (installFeePerMeter * totalMeter).toLong()
        val meterWithoutBaseUnitPrice =
            (-((pricePerMeter * inputs.ratioBasePct) * inputs.meterWithoutBaseUnit)).toLong()
        val meterWithoutWallUnitPrice =
            (-((pricePerMeter * inputs.ratioWallPct) * inputs.meterWithoutWallUnit)).toLong()

        // مثال: اگر بعداً خواستی از eff.baseUnitMeter استفاده کنی:
        val baseUnitMeterPrice =
            (pricePerMeter * inputs.baseUnitMeter * inputs.ratioBasePct).toLong()

        val baseUnitDepthPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.baseUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioBasePct,      // سهم یونیت زمینی (0..1)
            current = inputs.baseUnitDepth,     // عمق فعلی (cm)
            baseline = 0.55,                  // مبنا: 55cm
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )
        val baseUnitHeightPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.baseUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioBasePct,      // سهم یونیت زمینی (0..1)
            current = inputs.baseUnitHeight,     // عمق فعلی (cm)
            baseline = 0.77,                  // مبنا: 55cm
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )
        val baseUnitBaseHeightPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.baseUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioBasePct,      // سهم یونیت زمینی (0..1)
            current = inputs.baseUnitBaseHeight,     // عمق فعلی (cm)
            baseline = 0.1,                  // مبنا: 55cm
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )

        val wallUnitMeterPrice =
            (pricePerMeter * inputs.wallUnitMeter * inputs.ratioWallPct).toLong()
        val wallUnitDepthPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.wallUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioWallPct,      // سهم یونیت زمینی (0..1)
            current = inputs.wallUnitDepth,     // عمق فعلی (cm)
            baseline = 0.33,
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )
        val wallUnitHeightPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.wallUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioWallPct,      // سهم یونیت زمینی (0..1)
            current = inputs.wallUnitHeight,     // عمق فعلی (cm)
            baseline = 0.7,
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )

        val tallUnitMeterPrice = (pricePerMeter * inputs.tallUnitMeter * inputs.ratioTallPct).toLong()
        val tallUnitDepthPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.tallUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioTallPct,      // سهم یونیت زمینی (0..1)
            current = inputs.tallUnitDepth,     // عمق فعلی (cm)
            baseline = 0.55,
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )
        val tallUnitHeightPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.tallUnitMeter,
            shareMultiplier = inputs.ratioTallPct,
            current = inputs.tallUnitHeight,
            baseline = 2.2,
            elasticity = 1.0,
            minCurrent = 0.0,
            maxCurrent = 4.0
        )

        val decorateUnitMeterPrice = (pricePerMeter * inputs.decorateUnitMeter * inputs.ratioDecoratePct).toLong()
        val decorateUnitDepthPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.decorateUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioDecoratePct,      // سهم یونیت زمینی (0..1)
            current = inputs.decorateUnitDepth,     // عمق فعلی (cm)
            baseline = 0.55,
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )
        val decorateUnitHeightPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.decorateUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioDecoratePct,      // سهم یونیت زمینی (0..1)
            current = inputs.decorateUnitHeight,     // عمق فعلی (cm)
            baseline = 2.2,
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 4.0
        )

        val islandUnitMeterPrice = (pricePerMeter * inputs.islandUnitMeter * inputs.ratioIslandPct).toLong()
        val islandUnitDepthPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.islandUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioIslandPct,      // سهم یونیت زمینی (0..1)
            current = inputs.islandUnitDepth,     // عمق فعلی (cm)
            baseline = 0.55,
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )
        val islandUnitHeightPrice = dimensionDeltaPrice(
            pricePerMeter = market.pricePerMeter,
            unitMeter = inputs.islandUnitMeter,     // متر یونیت زمینی
            shareMultiplier = inputs.ratioIslandPct,      // سهم یونیت زمینی (0..1)
            current = inputs.islandUnitHeight,     // عمق فعلی (cm)
            baseline = 0.77,
            elasticity = 1.0,                   // خطی
            minCurrent = 0.0,
            maxCurrent = 1.0
        )

        val refrigeratorUnitMeterPrice =(pricePerMeter * inputs.refrigeratorUnitMeter * inputs.ratioRefrigeratorPct).toLong()

        val laundryUnitMeterPrice = (inputs.laundryUnitMeter * platePrice).toLong()
        val dishwasherUnitMeterPrice = (inputs.dishwasherUnitMeter * platePrice).toLong()




        val totalPrice =
            transportation +
                    totalMeterPrice +
                    installTotal +
                    baseUnitDepthPrice +
                    baseUnitHeightPrice +
                    baseUnitBaseHeightPrice +
                    wallUnitDepthPrice +
                    wallUnitHeightPrice +
                    tallUnitDepthPrice +
                    tallUnitHeightPrice +
                    decorateUnitDepthPrice +
                    decorateUnitHeightPrice +
                    islandUnitDepthPrice +
                    islandUnitHeightPrice +
                    refrigeratorUnitMeterPrice +
                    laundryUnitMeterPrice +
                    dishwasherUnitMeterPrice



        return CabinetPriceBreakdown(
            totalPrice = totalPrice,
            totalMeterPrice = totalMeterPrice,
            meterWithoutBaseUnitPrice = meterWithoutBaseUnitPrice,
            meterWithoutWallUnitPrice = meterWithoutWallUnitPrice,
            baseUnitMeterPrice = baseUnitMeterPrice,
            baseUnitDepthPrice = baseUnitDepthPrice,
            baseUnitHeightPrice = baseUnitHeightPrice,
            baseUnitBaseHeightPrice = baseUnitBaseHeightPrice,
            wallUnitMeterPrice = wallUnitMeterPrice,
            wallUnitDepthPrice = wallUnitDepthPrice,
            wallUnitHeightPrice = wallUnitHeightPrice,
            tallUnitMeterPrice = tallUnitMeterPrice,
            tallUnitDepthPrice = tallUnitDepthPrice,
            tallUnitHeightPrice = tallUnitHeightPrice,
            decorateUnitMeterPrice = decorateUnitMeterPrice,
            decorateUnitDepthPrice = decorateUnitDepthPrice,
            decorateUnitHeightPrice = decorateUnitHeightPrice,
            islandUnitMeterPrice = islandUnitMeterPrice,
            islandUnitDepthPrice = islandUnitDepthPrice,
            islandUnitHeightPrice = islandUnitHeightPrice,
            refrigeratorUnitMeterPrice = refrigeratorUnitMeterPrice,
            laundryUnitMeterPrice = laundryUnitMeterPrice,
            dishwasherUnitMeterPrice = dishwasherUnitMeterPrice,
        )
    }

    private fun parseMoney(raw: String): Long {
        val digits = raw.filter { it.isDigit() } // ارقام فارسی/لاتین اوکی می‌شود
        return digits.toLongOrNull() ?: 0L
    }

    fun onCabinetEvent(e: CabinetEstimateEvent) {
        isDirty = true
        when (e) {
            is CabinetEstimateEvent.ChangeMode -> {
                _caInputs.update { prev ->
                    when (e.mode) {
                        CabinetMode.SIMPLE -> {
                            // فقط اگر واقعاً در پرو/نیمه‌پیشرفته ورودی داشتیم، ساده را با جمع پرو sync کن
                            if (prev.hasAnyProInput()) {
                                prev.copy(
                                    mode = CabinetMode.SIMPLE,
                                    totalMeterSimple = prev.totalMeterPro()
                                )
                            } else {
                                // هیچ ورودی پرو داده نشده → مقدار قبلی ساده را دست نزن
                                prev.copy(mode = CabinetMode.SIMPLE)
                            }
                        }
                        CabinetMode.SEMI_PRO, CabinetMode.PRO -> {
                            // از ساده به پرو/نیمه‌پیشرفته می‌رویم: مقدار simple را نگه دار، دست نزن
                            // (بعداً اگر کاربر یونیت‌ها را پر کرد، hasAnyProInput() ترو می‌شود)
                            prev.copy(mode = e.mode)
                        }
                    }
                }
            }
            is CabinetEstimateEvent.ChangeTotalMeterSimple -> {
                _caInputs.update { it.copy(totalMeterSimple = (e.m ?: 0.0).coerceAtLeast(0.0)) }
            }

            is CabinetEstimateEvent.ChangeSimpleUnit -> {
                val m = clampMeters(roundToHalf(e.meters ?: 0.0))
                _caInputs.update {
                    when (e.field) {
                        SimpleUnitField.BASE -> it.copy(baseUnitMeter = m)
                        SimpleUnitField.WALL -> it.copy(wallUnitMeter = m)
                        SimpleUnitField.TALL -> it.copy(tallUnitMeter = m)
                        SimpleUnitField.DECOR -> it.copy(decorateUnitMeter = m)
                        SimpleUnitField.ISLAND -> it.copy(islandUnitMeter = m)
                    }
                }
            }

            is CabinetEstimateEvent.StepSimpleUnit -> {
                _caInputs.update {
                    when (e.field) {
                        SimpleUnitField.BASE -> it.copy(
                            baseUnitMeter = applyStep(it.baseUnitMeter, e.deltaMeters)
                        )

                        SimpleUnitField.WALL -> it.copy(
                            wallUnitMeter = applyStep(it.wallUnitMeter, e.deltaMeters)
                        )

                        SimpleUnitField.TALL -> it.copy(
                            tallUnitMeter = applyStep(it.tallUnitMeter, e.deltaMeters)
                        )

                        SimpleUnitField.DECOR -> it.copy(
                            decorateUnitMeter = applyStep(it.decorateUnitMeter, e.deltaMeters)
                        )

                        SimpleUnitField.ISLAND -> it.copy(
                            islandUnitMeter = applyStep(it.islandUnitMeter, e.deltaMeters)
                        )
                    }
                }
            }


            //مارکت
            is CabinetEstimateEvent.ChangeCabinetPerMeter -> _caMarket.update {
                it.copy(pricePerMeter = parseMoney(e.v))
            }

            is CabinetEstimateEvent.ChangePlateCabinet -> _caMarket.update {
                it.copy(
                    platePrice = e.v.toLongOrNull() ?: 0
                )
            }

            is CabinetEstimateEvent.ChangeInstallFeePerMeter -> _caMarket.update {
                it.copy(
                    installFeePerMeter = e.v.toLongOrNull() ?: 0
                )
            }

            is CabinetEstimateEvent.ChangeTransportation -> _caMarket.update {
                it.copy(
                    transportation = e.v.toLongOrNull() ?: 0
                )
            }

            // اندازه ها
            is CabinetEstimateEvent.ChangeTotalMeter -> _caInputs.update {
                when (it.mode) {
                    CabinetMode.SIMPLE -> it.copy(
                        totalMeterSimple = e.v ?: it.totalMeterSimple
                    )
                    CabinetMode.SEMI_PRO, CabinetMode.PRO -> it.copy(

                    )
                }
            }


            is CabinetEstimateEvent.ChangeMeterWithoutBaseUnit -> _caInputs.update {
                it.copy(
                    meterWithoutBaseUnit = e.v ?: it.meterWithoutBaseUnit
                )
            }

            is CabinetEstimateEvent.ChangeMeterWithoutWallUnit -> _caInputs.update {
                it.copy(
                    meterWithoutWallUnit = e.v ?: it.meterWithoutWallUnit
                )
            }

            //یونیت زمینی
            is CabinetEstimateEvent.ChangeRatioBasePct -> _caInputs.update {
                it.copy(
                    ratioBasePct = e.v ?: it.ratioBasePct
                )
            }

            is CabinetEstimateEvent.ChangeBaseUnitMeter -> _caInputs.update {
                it.copy(
                    baseUnitMeter = e.v ?: it.baseUnitMeter
                )
            }

            is CabinetEstimateEvent.ChangeBaseUnitDepth -> _caInputs.update {
                it.copy(
                    baseUnitDepth = e.v ?: it.baseUnitDepth
                )
            }

            is CabinetEstimateEvent.ChangeBaseUnitHeight -> _caInputs.update {
                it.copy(
                    baseUnitHeight = e.v ?: it.baseUnitHeight
                )
            }

            is CabinetEstimateEvent.ChangeBaseUnitBaseHeight -> _caInputs.update {
                it.copy(
                    baseUnitBaseHeight = e.v ?: it.baseUnitBaseHeight // ✅ درست
                )
            }

            //یونیت هوایی
            is CabinetEstimateEvent.ChangeRatioWallPct -> _caInputs.update {
                it.copy(
                    ratioWallPct = e.v ?: it.ratioWallPct
                )
            }

            is CabinetEstimateEvent.ChangeWallUnitMeter -> _caInputs.update {
                it.copy(
                    wallUnitMeter = e.v ?: it.wallUnitMeter
                )
            }

            is CabinetEstimateEvent.ChangeWallUnitDepth -> _caInputs.update {
                it.copy(
                    wallUnitDepth = e.v ?: it.wallUnitDepth
                )
            }

            is CabinetEstimateEvent.ChangeWallUnitHeight -> _caInputs.update {
                it.copy(
                    wallUnitHeight = e.v ?: it.wallUnitHeight
                )
            }

            //یونیت ایستاده
            is CabinetEstimateEvent.ChangeRatioTallPct -> _caInputs.update {
                it.copy(
                    ratioTallPct = e.v ?: it.ratioTallPct
                )
            }

            is CabinetEstimateEvent.ChangeTallUnitMeter -> _caInputs.update {
                it.copy(
                    tallUnitMeter = e.v ?: it.tallUnitMeter
                )
            }

            is CabinetEstimateEvent.ChangeTallUnitDepth -> _caInputs.update {
                it.copy(
                    tallUnitDepth = e.v ?: it.tallUnitDepth
                )
            }

            is CabinetEstimateEvent.ChangeTallUnitHeight -> _caInputs.update {
                it.copy(
                    tallUnitHeight = e.v ?: it.tallUnitHeight
                )
            }

            //یونیت دکوری
            is CabinetEstimateEvent.ChangeRatioDecoratePct -> _caInputs.update {
                it.copy(
                    ratioDecoratePct = e.v ?: it.ratioDecoratePct
                )
            }

            is CabinetEstimateEvent.ChangeDecorateUnitMeter -> _caInputs.update {
                it.copy(
                    decorateUnitMeter = e.v ?: it.decorateUnitMeter
                )
            }

            is CabinetEstimateEvent.ChangeDecorateUnitDepth -> _caInputs.update {
                it.copy(
                    decorateUnitDepth = e.v ?: it.decorateUnitDepth
                )
            }

            is CabinetEstimateEvent.ChangeDecorateUnitHeight -> _caInputs.update {
                it.copy(
                    decorateUnitHeight = e.v ?: it.decorateUnitHeight
                )
            }

            //یونیت جزیره
            is CabinetEstimateEvent.ChangeRatioIslandPct -> _caInputs.update {
                it.copy(
                    ratioIslandPct = e.v ?: it.ratioIslandPct
                )
            }

            is CabinetEstimateEvent.ChangeIslandUnitMeter -> _caInputs.update {
                it.copy(
                    islandUnitMeter = e.v ?: it.islandUnitMeter
                )
            }

            is CabinetEstimateEvent.ChangeIslandUnitDepth -> _caInputs.update {
                it.copy(
                    islandUnitDepth = e.v ?: it.islandUnitDepth
                )
            }

            is CabinetEstimateEvent.ChangeIslandUnitHeight -> _caInputs.update {
                it.copy(
                    islandUnitHeight = e.v ?: it.islandUnitHeight
                )
            }

            //یونیت یخچال
            is CabinetEstimateEvent.ChangeRatioRefrigeratorPct -> _caInputs.update {
                it.copy(
                    ratioRefrigeratorPct = e.v ?: it.ratioRefrigeratorPct
                )
            }

            is CabinetEstimateEvent.ChangeRefrigeratorUnitMeter -> _caInputs.update {
                it.copy(
                    refrigeratorUnitMeter = e.v ?: it.refrigeratorUnitMeter
                )
            }

            //متغیرات دیگر
            is CabinetEstimateEvent.ChangeLaundryUnitMeter -> _caInputs.update {
                it.copy(
                    laundryUnitMeter = e.v ?: it.laundryUnitMeter
                )
            }

            is CabinetEstimateEvent.ChangeDishwasherUnitMeter -> _caInputs.update {
                it.copy(
                    dishwasherUnitMeter = e.v ?: it.dishwasherUnitMeter
                )
            }

            /* Bottom bar actions */
            CabinetEstimateEvent.SaveEstimateToDb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val orderId = uiState.value.currentOrderEntity?.id ?: return@launch

//                    orderRepo.updateOrderPriceEstimate(orderId ,
//                        uiState.value.displayableTotal.toString()
//                    )

                    val total = cabinetEstimateState.value.breakdown?.totalPrice ?: 0L


                    // رکورد را داشته باش (اگر نبود بساز)
                    val entity = priceEstimateRepo.getOrCreateCabinetEstimate(orderId,EstimateCategory.CABINET,"کابینت")

                    // 1) ذخیرهٔ inputs جاری
                    priceEstimateRepo.saveCabinetInputs(entity.id, _caInputs.value)

                    // 2) ذخیرهٔ overrideهای هدر (همان مقادیری که کاربر در «قیمت‌های بازار» زده)
                    priceEstimateRepo.updateHeaderOverrides(
                        estimateId = entity.id,
                        pricePerMeter = _caMarket.value.pricePerMeter,
                        platePrice = _caMarket.value.platePrice,
                        installationFeePerMeter = _caMarket.value.installFeePerMeter,
                        transportation = _caMarket.value.transportation
                    )

                    // 4) ذخیره به عنوان پیش‌فرض‌های کاربر برای ورودی‌های جدید (کابینت)
                    priceEstimateRepo.updateCabinetMarketDefaults(
                        pricePerMeter = _caMarket.value.pricePerMeter,
                        platePrice = _caMarket.value.platePrice,
                        installationFeePerMeter = _caMarket.value.installFeePerMeter,
                        transportation = _caMarket.value.transportation
                    )


                    // 3) ذخیرهٔ نتیجهٔ نهایی تخمین در خود PriceEstimateEntity
                    priceEstimateRepo.updateEstimateResult(
                        estimateId = entity.id,
                        result = total
                    )

                }
            }

            CabinetEstimateEvent.CancelEstimate -> {
                // سیاستت: ریست به آخرین ذخیره؟ یا صرفاً نادیده بگیر؟
                // اینجا می‌تونی undo/restore بزنی اگر snapshot داری.
            }
        }
    }

    // جمع متراژ «حرفه‌ای»
    private fun CabinetEstimateInputs.totalMeterPro(): Double =
        (baseUnitMeter     * ratioBasePct)     +
                (wallUnitMeter     * ratioWallPct)     +
                (tallUnitMeter     * ratioTallPct)     +
                (decorateUnitMeter * ratioDecoratePct) +
                (islandUnitMeter   * ratioIslandPct)

    private fun CabinetEstimateInputs.hasAnyProInput(): Boolean =
        totalMeterPro() > EPS

    // انتخاب «متراژ مؤثر» بر اساس مود
    private fun CabinetEstimateInputs.effectiveTotalMeter(): Double =
        when (mode) {
            CabinetMode.SIMPLE -> totalMeterSimple
            CabinetMode.SEMI_PRO, CabinetMode.PRO -> totalMeterPro()
        }

//    // مارکت دیفالت؛ با overrides ادغام می‌کنیم
//    private fun Long?.orDefault(nonZeroFallback: Long): Long =
//        if (this == null || this == 0L) nonZeroFallback else this

    private suspend fun buildMarketFrom(entity: PriceEstimateEntity?): CabinetMarketParams {
        // ۱) پیش‌فرض‌های درون‌کدی (همینی که می‌خوای)
        val codeDefaults = CabinetMarketParams()

        // ۲) اگر توی جدول مارکت هم چیزی داری، فقط وقتی non-zero بود استفاده کن
        val dbDefaults = priceEstimateRepo.getMarketPrices()  // ممکنه صفر/خالی باشه

        val pricePerMeter =
            entity?.pricePerMeterOverride.orOverride(
                dbDefaults.pricePerMeter.orIfNonZero(codeDefaults.pricePerMeter)
            )


        val platePrice =
            entity?.platePriceOverride.orOverride(
                dbDefaults.platePrice.orIfNonZero(codeDefaults.platePrice)
            )

        val installFeePerMeter =
            entity?.installationFeePerMeterOverride.orOverride(
                dbDefaults.installationFeePerMeter.orIfNonZero(codeDefaults.installFeePerMeter)
            )

        val transportation =
            entity?.transportationPrice.orOverride(
                dbDefaults.transportationSuggestion.orIfNonZero(codeDefaults.transportation)
            )


        return CabinetMarketParams(
            pricePerMeter = pricePerMeter,
            platePrice = platePrice,
            installFeePerMeter = installFeePerMeter,
            transportation = transportation
        )
    }

    private fun PriceEstimateEntity?.hasMeaningfulData(): Boolean {
        if (this == null) return false
        val hasInputs = inputsJson.isNotBlank() && inputsJson != "{}"
        val hasOverrides = (pricePerMeterOverride != null && pricePerMeterOverride != 0L) ||
                (platePriceOverride != null && platePriceOverride != 0L) ||
                (installationFeePerMeterOverride != null && installationFeePerMeterOverride != 0L) ||
                (transportationPrice != null && transportationPrice != 0L)
        val hasResult = (priceEstimateResult != null && priceEstimateResult != 0L)
        return hasInputs || hasOverrides || hasResult
    }


    //endregion CabinetPriceEstimateFuns


    //region ClosetPriceEstimateFuns

    private fun Double.clamp(min: Double, max: Double) = coerceIn(min, max)



    private fun ClosetEstimateInputs.frontArea(): Double =
        (width.coerceAtLeast(0.0) * height.coerceAtLeast(0.0))

    private fun computeClosetSimple(
        inp: ClosetEstimateInputs,
        mk: ClosetMarketParams,
        elasticity: Double = 1.0,    // بعداً اگر خواستی حساس‌تر/ملایم‌ترش کن
        maxUpPct: Int = 200,         // سقف افزایش (٪ نسبت به baseAreaPrice)
        maxDownPct: Int = 50         // سقف کاهش
    ): ClosetPriceBreakdown {

        val w = inp.width.clamp(0.0, 20.0)
        val h = inp.height.clamp(0.0, 4.0)
        val d = inp.depth.clamp(0.3, 1.2)
        val s = inp.stdDepth.clamp(0.3, 1.2)

        val area = (w * h).coerceAtLeast(0.0)

        val baseAreaPrice = kotlin.math.round(mk.pricePerM2 * area).toLong()
        val installPrice  = kotlin.math.round(mk.installFeePerM2 * area).toLong()

        val factor = (d / s).pow(elasticity)
        val rawAdj = baseAreaPrice.toDouble() * (factor - 1.0)

        val maxUp   = baseAreaPrice * (maxUpPct / 100.0)
        val maxDown = -baseAreaPrice * (maxDownPct / 100.0)
        val depthAdj = rawAdj.coerceIn(maxDown, maxUp).roundToLong()

        val total = mk.transportation + baseAreaPrice + installPrice + depthAdj

        return ClosetPriceBreakdown(
            totalPrice = total,
            areaM2 = area,
            baseAreaPrice = baseAreaPrice,
            installPrice = installPrice,
            depthAdjustment = depthAdj
        )
    }

    fun onClosetEvent(e: ClosetEstimateEvent) {
        isDirty = true
        when (e) {
            is ClosetEstimateEvent.ChangeWidth -> _clInputs.update { it.copy(width  = (e.v ?: 0.0).coerceAtLeast(0.0)) }
            is ClosetEstimateEvent.ChangeHeight -> _clInputs.update { it.copy(height = (e.v ?: 0.0).coerceAtLeast(0.0)) }
            is ClosetEstimateEvent.ChangeDepth -> _clInputs.update { it.copy(depth  = (e.v ?: it.depth)) }
            is ClosetEstimateEvent.ChangeStdDepth -> _clInputs.update { it.copy(stdDepth = (e.v ?: it.stdDepth)) }

            is ClosetEstimateEvent.ChangeMode -> {
                _clInputs.update { prev ->
                    prev.copy(mode = e.mode)
                }
            }

            is ClosetEstimateEvent.ChangePricePerM2 -> _clMarket.update { it.copy(pricePerM2 = parseMoney(e.v)) }
            is ClosetEstimateEvent.ChangeInstallFeePerM2 -> _clMarket.update {
                it.copy(installFeePerM2 = parseMoney(e.v))
            }
            is ClosetEstimateEvent.ChangeTransportation -> _clMarket.update {
                it.copy(transportation = parseMoney(e.v))
            }
            ClosetEstimateEvent.CancelEstimate -> TODO()
            ClosetEstimateEvent.SaveEstimateToDb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val orderId = uiState.value.currentOrderEntity?.id ?: return@launch
//                    orderRepo.updateOrderPriceEstimate(orderId ,
//                        uiState.value.displayableTotal.toString()
//                    )

                    val total = closetEstimateState.value.breakdown?.totalPrice ?: 0L


                    val entity = priceEstimateRepo.getOrCreateCabinetEstimate(orderId, EstimateCategory.CLOSET, "کمد")
                    // inputsJson
                    priceEstimateRepo.saveClosetInputs(entity.id, _clInputs.value)

//                    priceEstimateRepo.saveClosetInputs(entity.id, _clInputs.value)
                    // header overrides
                    priceEstimateRepo.updateClosetHeaderOverrides(
                        estimateId = entity.id,
                        pricePerM2 = _clMarket.value.pricePerM2,
                        installPerM2 = _clMarket.value.installFeePerM2,
                        transportation = _clMarket.value.transportation
                    )

                    // 4) ذخیره به عنوان پیش‌فرض‌های کاربر برای ورودی‌های جدید
                    priceEstimateRepo.updateClosetMarketDefaults(
                        pricePerM2 = _clMarket.value.pricePerM2,
                        installFeePerM2 = _clMarket.value.installFeePerM2,
                        transportation = _clMarket.value.transportation
                    )


                    // 3) ذخیرهٔ نتیجهٔ نهایی تخمین در خود PriceEstimateEntity
                    priceEstimateRepo.updateEstimateResult(
                        estimateId = entity.id,
                        result = total
                    )


                }
            }
        }
    }



    private fun Long?.orOverride(fallback: Long): Long = this ?: fallback
    private fun Long.orIfNonZero(fallback: Long): Long = if (this == 0L) fallback else this

    private suspend fun buildClosetMarketFrom(entity: PriceEstimateEntity?): ClosetMarketParams {
        val codeDefaults = ClosetMarketParams() // 7_500_000, 600_000, 1_200_000
        val dbDefaults = priceEstimateRepo.getClosetMarketDefaults()

        val pricePerM2 =
            entity?.pricePerMeterOverride.orOverride(
                dbDefaults.pricePerM2.orIfNonZero(codeDefaults.pricePerM2)
            )

        val installFeePerM2 =
            entity?.installationFeePerMeterOverride.orOverride(
                dbDefaults.installFeePerM2.orIfNonZero(codeDefaults.installFeePerM2)
            )

        val transportation =
            entity?.transportationPrice.orOverride(
                dbDefaults.transportation.orIfNonZero(codeDefaults.transportation)
            )

        return ClosetMarketParams(
            pricePerM2 = pricePerM2,
            installFeePerM2 = installFeePerM2,
            transportation = transportation
        )
    }






    //endregion ClosetPriceEstimateFuns


}

//region DataClass

data class PriceEstimateUiState(
    val appInfoEntity: AppInfoEntity? = null,
    val currentOrderEntity: OrderEntity? = null,
    val isDataLoaded: Boolean = false,
    val displayableTotal: Long? = null
)

data class AnswerItemUnit(
    val answerId: Int,
    val title: String,
    val imageUris: List<String>, // می‌توانید با Glide/Coil هر منبعی بدهید: file://, content://, http(s)
    val selected: Boolean,
    val liked: Boolean = false,
    val note: String = "",
)


/* ------- PriceEstimateDataClass ------ */
//Cabinet
data class CabinetEstimateState(
    val isLoading: Boolean = true,
    val inputs: CabinetEstimateInputs = CabinetEstimateInputs(),
    val market: CabinetMarketParams = CabinetMarketParams(),
    val breakdown: CabinetPriceBreakdown? = null,
    val settings: DisplayPreferences = DisplayPreferences(),
    val error: String? = null
)

data class CabinetEstimateInputs(

    val totalMeterSimple: Double = 0.0,

    val mode: CabinetMode = CabinetMode.SIMPLE,

    // اندازه ها

    val meterWithoutBaseUnit: Double = 0.0,
    val meterWithoutWallUnit: Double = 0.0,

    //یونیت زمینی
    val ratioBasePct: Double = 0.6,
    val baseUnitMeter: Double = 0.0,
    val baseUnitDepth: Double = 0.55,
    val baseUnitHeight: Double = 0.77,
    val baseUnitBaseHeight: Double = 0.1,

    //یونیت هوایی
    val ratioWallPct: Double = 0.40,
    val wallUnitMeter: Double = 0.0,
    val wallUnitDepth: Double = 0.33,
    val wallUnitHeight: Double = 0.70,

    //یونیت ایستاده
    val ratioTallPct: Double = 1.8,
    val tallUnitMeter: Double = 0.0,
    val tallUnitDepth: Double = 0.55,
    val tallUnitHeight: Double = 2.2,

    //یونیت دکوری
    val ratioDecoratePct: Double = 2.0,
    val decorateUnitMeter: Double = 0.0,
    val decorateUnitDepth: Double = 0.55,
    val decorateUnitHeight: Double = 2.2,

    //یونیت جزیره
    val ratioIslandPct: Double = 1.0,
    val islandUnitMeter: Double = 0.0,
    val islandUnitDepth: Double = 0.55,
    val islandUnitHeight: Double = 0.77,

    //یونیت یخچال
    val ratioRefrigeratorPct: Double = 1.0,
    val refrigeratorUnitMeter: Double = 0.0,

    //متغیرات دیگر
    val laundryUnitMeter: Double = 0.0,
    val dishwasherUnitMeter: Double = 0.0,

    )

data class CabinetMarketParams(
    val pricePerMeter: Long = 8000000L,
    val platePrice: Long = 4500000L,
    val installFeePerMeter: Long = 800000L,
    val transportation: Long = 1500000L
)

data class CabinetPriceBreakdown(
    val totalPrice: Long,

    // اندازه ها
    val totalMeterPrice: Long,
    val meterWithoutBaseUnitPrice: Long,
    val meterWithoutWallUnitPrice: Long,
    //یونیت زمینی
    val baseUnitMeterPrice: Long,
    val baseUnitDepthPrice: Long,
    val baseUnitHeightPrice: Long,
    val baseUnitBaseHeightPrice: Long,

    //یونیت هوایی
    val wallUnitMeterPrice: Long,
    val wallUnitDepthPrice: Long,
    val wallUnitHeightPrice: Long,

    //یونیت ایستاده
    val tallUnitMeterPrice: Long,
    val tallUnitDepthPrice: Long,
    val tallUnitHeightPrice: Long,

    //یونیت دکوری
    val decorateUnitMeterPrice: Long,
    val decorateUnitDepthPrice: Long,
    val decorateUnitHeightPrice: Long,

    //یونیت جزیره
    val islandUnitMeterPrice: Long,
    val islandUnitDepthPrice: Long,
    val islandUnitHeightPrice: Long,

    //یونیت یخچال
    val refrigeratorUnitMeterPrice: Long,

    //متغیرات دیگر
    val laundryUnitMeterPrice: Long,
    val dishwasherUnitMeterPrice: Long,

    )

//Closet
data class ClosetEstimateState(
    val isLoading: Boolean = true,
    val inputs: ClosetEstimateInputs = ClosetEstimateInputs(),
    val market: ClosetMarketParams = ClosetMarketParams(),
    val breakdown: ClosetPriceBreakdown? = null,
    val settings: DisplayPreferences = DisplayPreferences(),
    val error: String? = null
)

data class ClosetEstimateInputs(

    val mode: ClosetMode = ClosetMode.SIMPLE,

    val width: Double = 0.0,          // متر
    val height: Double = 0.0,         // متر
    val depth: Double = 0.60,         // متر (عمق واقعی)
    val stdDepth: Double = 0.60       // متر (عمق استاندارد ذهن کاربر)
)

data class ClosetMarketParams(
    val pricePerM2: Long = 7_500_000L,        // قیمت هر متر مربع نما
    val installFeePerM2: Long = 600_000L,     // نصب هر متر مربع
    val transportation: Long = 1_200_000L     // حمل‌ونقل ثابت
)

data class ClosetPriceBreakdown(
    val totalPrice: Long,
    val areaM2: Double,
    val baseAreaPrice: Long,          // pricePerM2 * area
    val installPrice: Long,           // installFeePerM2 * area
    val depthAdjustment: Long,        // اثر عمق نسبت به stdDepth
)


data class EstimateSettings(
    val lengthUnit: LengthUnit = LengthUnit.METER,
    val currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN
)

//endregion DataClass

//region Events

sealed interface CabinetEstimateEvent {

    /* حالت صفحه */
    data class ChangeMode(val mode: CabinetMode) : CabinetEstimateEvent


    /* ورودی ساده (Simple) */
    data class ChangeTotalMeterSimple(val m: Double?): CabinetEstimateEvent

    data class StepSimpleUnit(val field: SimpleUnitField, val deltaMeters: Double) :
        CabinetEstimateEvent

    data class ChangeSimpleUnit(val field: SimpleUnitField, val meters: Double?) :
        CabinetEstimateEvent

    //مارکت
    data class ChangeCabinetPerMeter(val v: String) : CabinetEstimateEvent
    data class ChangePlateCabinet(val v: String) : CabinetEstimateEvent
    data class ChangeInstallFeePerMeter(val v: String) : CabinetEstimateEvent
    data class ChangeTransportation(val v: String) : CabinetEstimateEvent

    // اندازه ها
    data class ChangeTotalMeter(val v: Double?) : CabinetEstimateEvent
    data class ChangeMeterWithoutBaseUnit(val v: Double?) : CabinetEstimateEvent
    data class ChangeMeterWithoutWallUnit(val v: Double?) : CabinetEstimateEvent

    //یونیت زمینی
    data class ChangeRatioBasePct(val v: Double?) : CabinetEstimateEvent
    data class ChangeBaseUnitMeter(val v: Double?) : CabinetEstimateEvent
    data class ChangeBaseUnitDepth(val v: Double?) : CabinetEstimateEvent
    data class ChangeBaseUnitHeight(val v: Double?) : CabinetEstimateEvent
    data class ChangeBaseUnitBaseHeight(val v: Double?) : CabinetEstimateEvent

    //یونیت هوایی
    data class ChangeRatioWallPct(val v: Double?) : CabinetEstimateEvent
    data class ChangeWallUnitMeter(val v: Double?) : CabinetEstimateEvent
    data class ChangeWallUnitDepth(val v: Double?) : CabinetEstimateEvent
    data class ChangeWallUnitHeight(val v: Double?) : CabinetEstimateEvent

    //یونیت ایستاده
    data class ChangeRatioTallPct(val v: Double?) : CabinetEstimateEvent
    data class ChangeTallUnitMeter(val v: Double?) : CabinetEstimateEvent
    data class ChangeTallUnitDepth(val v: Double?) : CabinetEstimateEvent
    data class ChangeTallUnitHeight(val v: Double?) : CabinetEstimateEvent

    //یونیت دکوری
    data class ChangeRatioDecoratePct(val v: Double?) : CabinetEstimateEvent
    data class ChangeDecorateUnitMeter(val v: Double?) : CabinetEstimateEvent
    data class ChangeDecorateUnitDepth(val v: Double?) : CabinetEstimateEvent
    data class ChangeDecorateUnitHeight(val v: Double?) : CabinetEstimateEvent

    //یونیت جزیره
    data class ChangeRatioIslandPct(val v: Double?) : CabinetEstimateEvent
    data class ChangeIslandUnitMeter(val v: Double?) : CabinetEstimateEvent
    data class ChangeIslandUnitDepth(val v: Double?) : CabinetEstimateEvent
    data class ChangeIslandUnitHeight(val v: Double?) : CabinetEstimateEvent

    //یونیت یخچال
    data class ChangeRatioRefrigeratorPct(val v: Double?) : CabinetEstimateEvent
    data class ChangeRefrigeratorUnitMeter(val v: Double?) : CabinetEstimateEvent

    //متغیرات دیگر
    data class ChangeLaundryUnitMeter(val v: Double?) : CabinetEstimateEvent
    data class ChangeDishwasherUnitMeter(val v: Double?) : CabinetEstimateEvent


    /* اکشن‌های پایین صفحه */
    data object SaveEstimateToDb : CabinetEstimateEvent
    data object CancelEstimate : CabinetEstimateEvent

}

sealed interface ClosetEstimateEvent {

    /* حالت صفحه */
    data class ChangeMode(val mode: ClosetMode) : ClosetEstimateEvent

    // ورودی‌های کاربر
    data class ChangeWidth(val v: Double?): ClosetEstimateEvent
    data class ChangeHeight(val v: Double?): ClosetEstimateEvent
    data class ChangeDepth(val v: Double?): ClosetEstimateEvent
    data class ChangeStdDepth(val v: Double?): ClosetEstimateEvent



    // بازار
    data class ChangePricePerM2(val v: String): ClosetEstimateEvent
    data class ChangeInstallFeePerM2(val v: String): ClosetEstimateEvent
    data class ChangeTransportation(val v: String): ClosetEstimateEvent

    /* اکشن‌های پایین صفحه */
    data object SaveEstimateToDb : ClosetEstimateEvent
    data object CancelEstimate : ClosetEstimateEvent
}

//endregion Events

//region Enums

enum class CabinetMode {
    SIMPLE,         // ورودی‌های پایه، سریع‌ترین حالت تخمین
    SEMI_PRO,   // با جزئیات یونیت‌ها
    PRO        // با ضرایب و تنظیمات حرفه‌ای
}
enum class ClosetMode { SIMPLE, PRO } // فعلاً SIMPLE
enum class SimpleUnitField { BASE, WALL, TALL, DECOR, ISLAND }

//endregion Enums