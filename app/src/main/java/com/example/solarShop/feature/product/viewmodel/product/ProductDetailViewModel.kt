package com.example.solarShop.feature.product.viewmodel.product

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.dataStore.DollarRatePreferencesDataSource
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.pricing.CurrencyRateEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import com.example.solarShop.domain.product.ProductPriceCalculator
import com.example.solarShop.utils.ProductPdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val attributeRepository: AttributeRepository,
    private val pricingRepository: PricingRepository,
    private val inventoryRepository: InventoryRepository,
    @ApplicationContext private val app: Context,
    private val productImageRepository: ProductImageRepository,
    private val dollarRatePrefs: DollarRatePreferencesDataSource,
    private val productPdfExporter: ProductPdfExporter,
) : ViewModel() {

    private val productId =
        checkNotNull(
            savedStateHandle.get<Int>("productId")
        )

    private val productFlow =
        productRepository.observeProductFullInfo(productId)

    private val activePurchasePriceFlow =
        flow {
            emit(
                pricingRepository.getActivePurchasePrice(productId)
            )
        }



    private val salePricesFlow =
        pricingRepository.observeSalePrices(productId)

    private val currencyRatesFlow =
        pricingRepository.observeCurrencyRateHistory("USD")

    private val manualDollarRateFlow =
        dollarRatePrefs.manualDollarRateFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    private val attributesFlow =
        productFlow.flatMapLatest { product ->

            val categoryId =
                product?.product?.categoryId

            if (categoryId == null) {
                flowOf(emptyList())
            } else {
                attributeRepository.observeProductAttributeDisplayInfo(
                    productId = productId,
                    categoryId = categoryId
                )
            }
        }

    private val currentStockFlow =
        inventoryRepository.observeCurrentStock(productId)

    private val transactionsFlow =
        inventoryRepository.observeTransactions(productId)

    private val imagesFlow =
        productImageRepository.observeImagesForProduct(productId)

    @Suppress("UNCHECKED_CAST")
    val uiState =
        combine(
            productFlow,
            attributesFlow,
            activePurchasePriceFlow,
            currentStockFlow,
            transactionsFlow,
            imagesFlow,
            salePricesFlow,
            currencyRatesFlow,
            manualDollarRateFlow
        ) { arr: Array<Any?> ->

            val product = arr[0] as? com.example.solarShop.data.local.relation.product.ProductFullInfo
            val attributes = arr[1] as List<ProductAttributeDisplayInfo>
            val activePurchasePrice =
                arr[2] as? com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity

            val currentStock = arr[3] as Double
            val transactions = arr[4] as List<InventoryTransactionEntity>
            val images = arr[5] as List<ProductImageEntity>
            val salePrices = arr[6] as List<ProductSalePriceEntity>
            val currencyRates = arr[7] as List<CurrencyRateEntity>
            val manualDollarRate = arr[8] as Long?

            val apiDollarRate =
                currencyRates.firstOrNull()?.rateToman

            val dailyDollarRate =
                manualDollarRate ?: apiDollarRate

            val latestConsumerSale =
                salePrices.firstOrNull {
                    it.priceType == "consumer"
                }

            val latestColleagueSale =
                salePrices.firstOrNull {
                    it.priceType == "colleague"
                }

            val consumerSalePriceResult =
                ProductPriceCalculator.calculate(
                    buyPriceDollar = latestConsumerSale?.baseDollarPrice
                        ?: activePurchasePrice?.buyPriceDollar,
                    buyPriceToman = activePurchasePrice?.buyPriceToman,
                    purchaseDollarRateToman = activePurchasePrice?.dollarRateToman,
                    todayDollarRateToman = dailyDollarRate,
                    profitPercent = latestConsumerSale?.profitPercent ?: 0.0,
                    fixedProfitToman = 0L
                )

            val colleagueSalePriceResult =
                ProductPriceCalculator.calculate(
                    buyPriceDollar = latestColleagueSale?.baseDollarPrice
                        ?: activePurchasePrice?.buyPriceDollar,
                    buyPriceToman = activePurchasePrice?.buyPriceToman,
                    purchaseDollarRateToman = activePurchasePrice?.dollarRateToman,
                    todayDollarRateToman = dailyDollarRate,
                    profitPercent = latestColleagueSale?.profitPercent ?: 0.0,
                    fixedProfitToman = 0L
                )

            val salePriceResult =
                pricingRepository.calculateSalePrice(
                    productId = productId,
                    todayDollarRateToman = dailyDollarRate
                )

            ProductDetailUiState(
                isLoading = false,
                product = product,
                attributes = attributes,
                activePurchasePrice = activePurchasePrice,
                salePriceResult = salePriceResult,
                consumerSalePriceResult = consumerSalePriceResult,
                colleagueSalePriceResult = colleagueSalePriceResult,
                salePrices = salePrices,
                dailyDollarRateToman = dailyDollarRate,
                currentStock = currentStock,
                inventoryTransactions = transactions,
                images = images.map { image ->
                    val file =
                        File(
                            File(app.filesDir, "images"),
                            image.fileName
                        )

                    val uri =
                        FileProvider.getUriForFile(
                            app,
                            "${app.packageName}.fileprovider",
                            file
                        )

                    ProductImageUi(
                        id = image.id,
                        uri = uri
                    )
                }
            )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ProductDetailUiState()
            )

    fun importProductImage(src: Uri) {
        viewModelScope.launch {
            productImageRepository.addImageFromUri(
                productId = productId,
                src = src
            )
        }
    }

    fun createProductCameraTempUri(): Pair<File, Uri> {
        return productImageRepository.createCameraTempUri()
    }

    fun importProductImageFromCameraTemp(tempFile: File) {
        viewModelScope.launch {
            productImageRepository.addImageFromCameraTemp(
                productId = productId,
                tempFile = tempFile
            )
        }
    }

    fun setImageAsCover(imageId: Int) {
        viewModelScope.launch {
            productImageRepository.setAsCover(
                productId = productId,
                imageId = imageId
            )
        }
    }

    fun deleteProductImage(imageId: Int) {
        viewModelScope.launch {
            productImageRepository.deleteImage(imageId)
        }
    }

    fun exportProductPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value
            val productFullInfo = state.product ?: return@launch

            val visibleAttributes = state.attributes.filter {
                !it.valueText.isNullOrBlank()
            }

            val file = productPdfExporter.exportProductDetail(
                product = productFullInfo,
                attributes = visibleAttributes,
                images = state.images.map { it.uri }
            )

            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            app.startActivity(intent)
        }
    }
}