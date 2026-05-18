package com.example.solarShop.data.room.tables.orderAll.priceEstimate



import com.example.solarShop.data.room.tables.market_prices.ClosetMarketDefaultsDao
import com.example.solarShop.data.room.tables.market_prices.ClosetMarketDefaultsEntity
import com.example.solarShop.data.room.tables.market_prices.MarketPricesDao
import com.example.solarShop.data.room.tables.market_prices.MarketPricesEntity
import com.example.solarShop.ui.orderScreen.orderPriceEstimate.CabinetEstimateInputs
import com.example.solarShop.ui.orderScreen.orderPriceEstimate.ClosetEstimateInputs
import com.example.solarShop.ui.orderScreen.orderPriceEstimate.ClosetMode
import com.example.solarShop.utils.EstimateApproach
import com.example.solarShop.utils.EstimateCategory
import com.example.solarShop.utils.JsonExt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


@Singleton
class PriceEstimateRepository @Inject constructor(
    private val marketDao: MarketPricesDao,
    private val closetMarketDao: ClosetMarketDefaultsDao,
    private val estimateDao: PriceEstimateDao,
    @Named("networkJson")private val json: kotlinx.serialization.json.Json
) {
    /** تضمین می‌کند که رکورد پیش‌فرض MarketPrices وجود داشته باشد */
    private suspend fun ensureMarketDefaults(): MarketPricesEntity {
        val cur = marketDao.getOnce()
        return if (cur == null) {
            val def = MarketPricesEntity()
            marketDao.upsert(def)
            def
        } else cur
    }


    suspend fun getMarketPrices(): MarketPricesEntity = ensureMarketDefaults()
    fun observeMarketPrices(): Flow<MarketPricesEntity?> = marketDao.observe()


    suspend fun getOrCreateCabinetEstimate(orderId: Int, cat: EstimateCategory, title: String): PriceEstimateEntity {
        val found = estimateDao.getOnce(orderId, cat)
        if (found != null) return found
        val created = PriceEstimateEntity(orderId = orderId , category = cat , title =title )
        val id = estimateDao.insert(created).toInt()
        return created.copy(id = id)
    }


    fun observeCabinetEstimate(orderId: Int): Flow<PriceEstimateEntity?> =
        estimateDao.observe(orderId, EstimateCategory.CABINET) ?: flowOf(null)


    suspend fun saveCabinetInputs(estimateId: Int, inputs: CabinetEstimateInputs) {
        val json = JsonExt.toJson(inputs)
        estimateDao.updateInputsJson(estimateId, json)
    }

    suspend fun saveClosetInputs(estimateId: Int, inputs: ClosetEstimateInputs) {
        val json = JsonExt.toJson(inputs)
        estimateDao.updateInputsJson(estimateId, json)
    }


    suspend fun loadCabinetInputs(estimate: PriceEstimateEntity): CabinetEstimateInputs =
        runCatching { JsonExt.fromJson<CabinetEstimateInputs>(estimate.inputsJson) }
            .getOrElse { CabinetEstimateInputs() }


    suspend fun updateApproach(estimateId: Int, approach: EstimateApproach) {
        estimateDao.updateApproach(estimateId, approach)
    }


    suspend fun updateHeaderOverrides(
        estimateId: Int,
        pricePerMeter: Long?,
        platePrice: Long?,
        installationFeePerMeter: Long?,
        transportation: Long?,
    ) {
        estimateDao.updateHeaderOverrides(estimateId, pricePerMeter, platePrice, installationFeePerMeter, transportation)
    }



    /* ---------- مشترک ---------- */
    fun observe(orderId: Int, cat: EstimateCategory) =
        estimateDao.observeByOrderAndCategory(orderId, cat)

    suspend fun getOrCreate(orderId: Int, cat: EstimateCategory, title: String): PriceEstimateEntity {
        estimateDao.getByOrderAndCategory(orderId, cat)?.let { return it }
        val id = estimateDao.upsert(
            PriceEstimateEntity(orderId = orderId, category = cat, title = title)
        ).toInt()
        return PriceEstimateEntity(id = id, orderId = orderId, category = cat, title = title)
    }

    /* ---------- کمد: inputsJson ---------- */
    suspend fun loadClosetInputs(entity: PriceEstimateEntity): ClosetEstimateInputs {
        val raw = entity.inputsJson
        return try {
            val j = json.decodeFromString<ClosetInputsJson>(raw)
            ClosetEstimateInputs(
                mode = ClosetMode.valueOf(j.mode),
                width = j.width, height = j.height,
                depth = j.depth, stdDepth = j.stdDepth
            )
        } catch (_: Throwable) {
            ClosetEstimateInputs() // دیفالت
        }
    }

//    suspend fun saveClosetInputs(estimateId: Int, inp: ClosetEstimateInputs) {
//        val j = ClosetInputsJson(
//            mode = inp.mode.name,
//            width = inp.width, height = inp.height,
//            depth = inp.depth, stdDepth = inp.stdDepth
//        )
//        estimateDao.updateInputsJson(estimateId, json.encodeToString(ClosetInputsJson.serializer(), j))
//    }

    /* ---------- کمد: header overrides ---------- */
    suspend fun updateClosetHeaderOverrides(
        estimateId: Int,
        pricePerM2: Long?, installPerM2: Long?, transportation: Long?
    ) {
        estimateDao.updateHeaderOverrides(
            id = estimateId,
            ppm = pricePerM2,
            plate = null,                         // کمد: استفاده نمی‌شود
            install = installPerM2,
            transport = transportation
        )
    }


    suspend fun updateEstimateResult(estimateId: Int, result: Long?) {
        estimateDao.updateEstimateResult(
            estimateId = estimateId,
            result = result
        )
    }

    fun observeEstimateForOrder(orderId: Int): Flow<PriceEstimateEntity?> {
        return estimateDao.observeLatestByOrderId(orderId)
    }



    suspend fun getClosetMarketDefaults(): ClosetMarketDefaultsEntity {
        val existing = closetMarketDao.get()
        if (existing != null) return existing

        // اگر نبود، بساز (صفرها یعنی "هنوز تنظیم نشده")
        val created = ClosetMarketDefaultsEntity(
            id = 1,
            updatedAt = System.currentTimeMillis()
        )
        closetMarketDao.upsert(created)
        return created
    }

    suspend fun updateClosetMarketDefaults(
        pricePerM2: Long,
        installFeePerM2: Long,
        transportation: Long
    ) {
        val cur = getClosetMarketDefaults()
        closetMarketDao.upsert(
            cur.copy(
                pricePerM2 = pricePerM2,
                installFeePerM2 = installFeePerM2,
                transportation = transportation,
                updatedAt = System.currentTimeMillis()
            )
        )
    }


    suspend fun updateCabinetMarketDefaults(
        pricePerMeter: Long,
        platePrice: Long,
        installationFeePerMeter: Long,
        transportation: Long
    ) {
        val cur = getMarketPrices() // همون ensureMarketDefaults رو صدا می‌زنه
        marketDao.upsert(
            cur.copy(
                pricePerMeter = pricePerMeter,
                platePrice = platePrice,
                installationFeePerMeter = installationFeePerMeter,
                transportationSuggestion = transportation,
                updatedAt = System.currentTimeMillis() // اگر این ستون را داری
            )
        )
    }





}