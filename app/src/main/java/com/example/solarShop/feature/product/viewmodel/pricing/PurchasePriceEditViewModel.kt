package com.example.solarShop.feature.product.viewmodel.pricing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.repository.pricing.PricingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchasePriceEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pricingRepository: PricingRepository
) : ViewModel() {

    private val productId =
        checkNotNull(
            savedStateHandle.get<Int>("productId")
        )

    private val _uiState = MutableStateFlow(
        PurchasePriceEditUiState(
            productId = productId
        )
    )

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {

            val activePrice =
                pricingRepository.getActivePurchasePrice(productId)

            if (activePrice != null) {

                _uiState.update {
                    it.copy(
                        buyPriceDollar =
                        activePrice.buyPriceDollar?.toString().orEmpty(),

                        buyPriceToman =
                        activePrice.buyPriceToman,

                        dollarRateToman =
                        activePrice.dollarRateToman,

                        note =
                        activePrice.note
                    )
                }

            } else {

                val latestRate =
                    pricingRepository.getLatestCurrencyRate("USD")

                _uiState.update {
                    it.copy(
                        dollarRateToman =
                        latestRate?.rateToman
                    )
                }
            }
        }
    }

    fun onDollarPriceChange(value: String) {
        val cleanValue = value.filter { it.isDigit() || it == '.' }
        val dollar = cleanValue.toDoubleOrNull()
        val rate = _uiState.value.dollarRateToman

        _uiState.update {
            it.copy(
                buyPriceDollar = cleanValue,
                buyPriceToman = if (cleanValue.isBlank()) null
                else if (dollar != null && rate != null) (dollar * rate).toLong()
                else it.buyPriceToman,
                lastInputSource = PurchasePriceInputSource.DOLLAR
            )
        }
    }

    fun onTomanPriceChange(value: Long?) {
        val rate = _uiState.value.dollarRateToman

        _uiState.update {
            it.copy(
                buyPriceToman = value,
                buyPriceDollar = if (value == null) ""
                else if (rate != null && rate > 0)
                    java.lang.String.format(
                        java.util.Locale.US,
                        "%.2f",
                        value.toDouble() / rate.toDouble()
                    )
                else it.buyPriceDollar,
                lastInputSource = PurchasePriceInputSource.TOMAN
            )
        }
    }

    fun onDollarRateChange(value: Long?) {
        _uiState.update { state ->

            val newState = when (state.lastInputSource) {
                PurchasePriceInputSource.DOLLAR -> {
                    val dollar = state.buyPriceDollar.toDoubleOrNull()

                    state.copy(
                        dollarRateToman = value,
                        buyPriceToman =
                        if (state.buyPriceDollar.isBlank()) null
                        else if (dollar != null && value != null) (dollar * value).toLong()
                        else state.buyPriceToman
                    )
                }

                PurchasePriceInputSource.TOMAN -> {
                    state.copy(
                        dollarRateToman = value,
                        buyPriceDollar =
                        if (state.buyPriceToman == null) ""
                        else if (value != null && value > 0)
                            java.lang.String.format(
                                java.util.Locale.US,
                                "%.2f",
                                state.buyPriceToman.toDouble() / value.toDouble()
                            )
                        else state.buyPriceDollar
                    )
                }
            }

            newState
        }
    }

    fun onNoteChange(value: String) {
        _uiState.update {
            it.copy(note = value)
        }
    }

    fun save(
        onSuccess: () -> Unit
    ) {

        val state = _uiState.value
        val productId = state.productId ?: return

        viewModelScope.launch {

            _uiState.update {
                it.copy(isSaving = true)
            }

            pricingRepository.setNewPurchasePrice(
                ProductPurchasePriceEntity(
                    productId = productId,

                    buyPriceDollar =
                    state.buyPriceDollar.toDoubleOrNull(),

                    buyPriceToman =
                    state.buyPriceToman,

                    dollarRateToman =
                    state.dollarRateToman,

                    note = state.note
                )
            )

            _uiState.update {
                it.copy(isSaving = false)
            }

            onSuccess()
        }
    }
}