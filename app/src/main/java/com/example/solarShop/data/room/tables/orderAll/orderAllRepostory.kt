package com.example.solarShop.data.room.tables.orderAll


import com.example.solarShop.data.room.appDatabase.InvoiceType
import com.example.solarShop.data.room.tables.contract.ContractInstanceDao
import com.example.solarShop.data.room.tables.orderAll.order.OrderDao
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.data.room.tables.orderAll.orderCost.OrderCostRepository
import com.example.solarShop.data.room.tables.orderAll.orderInvoice.InvoiceDocumentDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefDao
import com.example.solarShop.data.room.tables.orderAll.orderPhoto.OrderPhotoRefEntity
import com.example.solarShop.data.room.tables.orderAll.priceEstimate.PriceEstimateRepository
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceRepository
import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.WorkflowRepository
import com.example.solarShop.ui.orderScreen.orderCosts.CostScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OrderAllRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val workflowRepo: WorkflowRepository,
    private val priceEstimateRepo: PriceEstimateRepository,
    private val selectedChoiceRepo: SelectedChoiceRepository,
    private val contractInstanceDao: ContractInstanceDao,
    private val orderCostRepo: OrderCostRepository,
    private val orderPhotoRefDao: OrderPhotoRefDao,
    private val invoiceDao: InvoiceDocumentDao,
) {

    fun observeOrderSummary(orderId: Int): Flow<OrderSummary> {

        val orderFlow: Flow<OrderEntity> =
            orderDao.observeOrderById(orderId)
                .filterNotNull()

        val priceEstimateFlow: Flow<Long?> =
            priceEstimateRepo
                .observeEstimateForOrder(orderId)
                .map { it?.priceEstimateResult ?: 0L}

        val catalogSelectedCountFlow: Flow<Int?> =
            selectedChoiceRepo
                .observeTotalAnswered(orderId)
                .map {   it ?: 0 }

        val hasContractFlow: Flow<Boolean> =
            contractInstanceDao.observeSingleByOrder(orderId)
                .map { it != null }
                .distinctUntilChanged()



        val costResultFlow: Flow<Long> =
            combine(
                orderCostRepo.receiptsSum(CostScope.Order, orderId, clientId = -1),
                orderCostRepo.expensesSum(CostScope.Order, orderId, clientId = -1),
            ) { r, e -> r - e }



        // لیست عکس‌های این سفارش
        val photosFlow: Flow<List<OrderPhotoRefEntity>> =
            orderPhotoRefDao.observeByOrder(orderId)   // همون که توی ریپوی عکس استفاده می‌کنی
                .flowOn(Dispatchers.IO)

        // تعداد کل عکس‌ها
        val galleryPhotoCountFlow: Flow<Int> =
            photosFlow
                .map { list -> list.size }            // ۰ اگر خالی باشد
                .flowOn(Dispatchers.Default)

        // تعداد عکس‌های pinned
        val pinnedPhotoCountFlow: Flow<Int> =
            photosFlow
                .map { list -> list.count { it.isPinned } }   // اینجا اسم فیلد pinned رو با فیلد واقعی عوض کن
                .flowOn(Dispatchers.Default)



        // آیا برای این سفارش پیش‌فاکتور دارد؟
        val hasPreFactureFlow: Flow<Boolean> =
            invoiceDao
                .observeInvoicesForOrder(orderId, InvoiceType.PROFORMA)
                .map { invoices -> invoices.isNotEmpty() }
                .distinctUntilChanged()

        // آیا برای این سفارش فاکتور دارد؟
        val hasFactureFlow: Flow<Boolean> =
            invoiceDao
                .observeInvoicesForOrder(orderId, InvoiceType.INVOICE)
                .map { invoices -> invoices.isNotEmpty() }
                .distinctUntilChanged()


        val progressFlow: Flow<Int> =
            workflowRepo.observeOrderProgress(orderId)
                .map { it.coerceIn(0, 100) }
                .distinctUntilChanged()

        return combine(
            orderFlow,
            priceEstimateFlow,
            catalogSelectedCountFlow,
            hasContractFlow,
            costResultFlow,
            galleryPhotoCountFlow,
            pinnedPhotoCountFlow,
            hasPreFactureFlow,
            hasFactureFlow,
            progressFlow,
        ) { arr: Array<Any?> ->

            val order           = arr[0] as OrderEntity
            val priceEstimate   = arr[1] as Long
            val catalogCount    = arr[2] as Int
            val hasContract     = arr[3] as Boolean
            val costResult      = arr[4] as Long
            val galleryCount    = arr[5] as Int
            val pinnedCount     = arr[6] as Int
            val hasPreFacture   = arr[7] as Boolean
            val hasFacture      = arr[8] as Boolean
            val progressPercent = arr[9] as Int

            OrderSummary(
                order = order,
                priceEstimateTotal = priceEstimate,
                catalogSelectedCount = catalogCount,
                hasContract = hasContract,
                costResult = costResult,
                galleryPhotoCount = galleryCount,
                pinnedPhotoCount = pinnedCount,
                hasPreFacture = hasPreFacture,
                hasFacture = hasFacture,
                progressPercent = progressPercent,
            )
        }


    }

}



data class OrderSummary(
    val order: OrderEntity,
    val priceEstimateTotal: Long = 0L,
    val catalogSelectedCount: Int = 0,
    val hasContract: Boolean = false,
    val costResult: Long = 0L,
    val galleryPhotoCount: Int = 0,
    val pinnedPhotoCount: Int = 0,
    val hasPreFacture: Boolean = false,
    val hasFacture: Boolean = false,
    val progressPercent: Int = 0,
)
