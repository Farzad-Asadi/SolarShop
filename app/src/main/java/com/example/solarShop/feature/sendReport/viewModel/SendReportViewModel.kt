package com.example.solarShop.feature.sendReport.viewModel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.dataStore.DollarRatePreferencesDataSource
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import com.example.solarShop.domain.product.ProductPriceCalculator
import com.example.solarShop.utils.pdf.ProductPriceListPdfExporter
import com.example.solarShop.utils.pdf.ProductPriceReportRow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject



@HiltViewModel
class SendReportViewModel @Inject constructor(
    @ApplicationContext private val app: Context,
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val pricingRepository: PricingRepository,
    private val dollarRatePrefs: DollarRatePreferencesDataSource,
    private val productPriceListPdfExporter: ProductPriceListPdfExporter
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(SendReportUiState())

    val uiState: StateFlow<SendReportUiState> =
        _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.observeActiveCategories().collect { categories ->

                val currentSelectedIds =
                    _uiState.value.categories
                        .filter { it.isSelected }
                        .map { it.id }
                        .toSet()

                val isFirstLoad =
                    _uiState.value.categories.isEmpty()

                _uiState.update { oldState ->
                    oldState.copy(
                        categories = categories.mapNotNull { category ->
                            val id = category.id ?: return@mapNotNull null

                            SendReportCategoryUi(
                                id = id,
                                name = category.name,
                                isSelected = if (isFirstLoad) {
                                    true
                                } else {
                                    id in currentSelectedIds
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    fun setIncludeConsumerPrice(value: Boolean) {
        _uiState.update {
            it.copy(includeConsumerPrice = value)
        }
    }

    fun setIncludeColleaguePrice(value: Boolean) {
        _uiState.update {
            it.copy(includeColleaguePrice = value)
        }
    }

    fun toggleCategory(categoryId: Int) {
        _uiState.update { state ->
            state.copy(
                categories = state.categories.map { category ->
                    if (category.id == categoryId) {
                        category.copy(isSelected = !category.isSelected)
                    } else {
                        category
                    }
                }
            )
        }
    }

    fun setAllCategoriesSelected(selected: Boolean) {
        _uiState.update { state ->
            state.copy(
                categories = state.categories.map {
                    it.copy(isSelected = selected)
                }
            )
        }
    }

    fun exportProductsWithPricesPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            val options =
                _uiState.value

            val selectedCategoryIds =
                options.categories
                    .filter { it.isSelected }
                    .map { it.id }
                    .toSet()

            if (selectedCategoryIds.isEmpty()) {
                return@launch
            }

            _uiState.update {
                it.copy(isLoading = true)
            }

            val products =
                productRepository.observeActiveProductsFullInfo()
                    .first()
                    .filter {
                        it.product.categoryId in selectedCategoryIds
                    }

            val productIds =
                products.mapNotNull { it.product.id }

            val images =
                productImageRepository.observeImagesForProducts(productIds).first()

            val coverByProductId =
                images
                    .groupBy { it.productId }
                    .mapValues { (_, list) ->
                        list.minByOrNull { it.sortOrder }
                    }

            val currencyRates =
                pricingRepository.observeCurrencyRateHistory("USD").first()

            val apiDollarRate =
                currencyRates.firstOrNull()?.rateToman

            val manualDollarRate =
                dollarRatePrefs.manualDollarRateFlow.first()

            val dailyDollarRate =
                manualDollarRate ?: apiDollarRate

            val rows =
                products.map { productFullInfo ->

                    val productId =
                        productFullInfo.product.id

                    val activePurchasePrice =
                        productId?.let {
                            pricingRepository.getActivePurchasePrice(it)
                        }

                    val consumerSale =
                        productId?.let {
                            pricingRepository.getActiveSalePrice(
                                productId = it,
                                priceType = "consumer"
                            )
                        }

                    val colleagueSale =
                        productId?.let {
                            pricingRepository.getActiveSalePrice(
                                productId = it,
                                priceType = "colleague"
                            )
                        }

                    val consumerPrice =
                        ProductPriceCalculator.calculate(
                            buyPriceDollar = consumerSale?.baseDollarPrice
                                ?: activePurchasePrice?.buyPriceDollar,
                            buyPriceToman = activePurchasePrice?.buyPriceToman,
                            purchaseDollarRateToman = activePurchasePrice?.dollarRateToman,
                            todayDollarRateToman = dailyDollarRate,
                            profitPercent = consumerSale?.profitPercent ?: 0.0,
                            fixedProfitToman = 0L
                        )?.finalSalePriceToman

                    val colleaguePrice =
                        ProductPriceCalculator.calculate(
                            buyPriceDollar = colleagueSale?.baseDollarPrice
                                ?: activePurchasePrice?.buyPriceDollar,
                            buyPriceToman = activePurchasePrice?.buyPriceToman,
                            purchaseDollarRateToman = activePurchasePrice?.dollarRateToman,
                            todayDollarRateToman = dailyDollarRate,
                            profitPercent = colleagueSale?.profitPercent ?: 0.0,
                            fixedProfitToman = 0L
                        )?.finalSalePriceToman

                    val coverImage =
                        productId?.let {
                            coverByProductId[it]
                        }

                    val coverUri =
                        coverImage?.let { image ->
                            val file = File(
                                File(app.filesDir, "images"),
                                image.fileName
                            )

                            FileProvider.getUriForFile(
                                app,
                                "${app.packageName}.fileprovider",
                                file
                            )
                        }

                    ProductPriceReportRow(
                        categoryName = productFullInfo.category?.name ?: "بدون دسته‌بندی",
                        coverUri = coverUri,
                        name = productFullInfo.product.name,
                        model = productFullInfo.product.model ?: "-",
                        brand = productFullInfo.brand?.name ?: "-",
                        consumerPriceToman = consumerPrice,
                        colleaguePriceToman = colleaguePrice
                    )
                }

            val file =
                productPriceListPdfExporter.exportProductsWithPrices(
                    rows = rows,
                    includeConsumerPrice = options.includeConsumerPrice,
                    includeColleaguePrice = options.includeColleaguePrice
                )

            openPdf(file)

            _uiState.update {
                it.copy(isLoading = false)
            }
        }
    }

    private fun openPdf(file: File) {
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


data class SendReportCategoryUi(
    val id: Int,
    val name: String,
    val isSelected: Boolean = true
)

data class SendReportUiState(
    val isLoading: Boolean = false,
    val includeConsumerPrice: Boolean = true,
    val includeColleaguePrice: Boolean = true,
    val categories: List<SendReportCategoryUi> = emptyList()
)