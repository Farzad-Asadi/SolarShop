package com.example.solarShop.feature.product.viewmodel.product

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productRepository: ProductRepository,
    attributeRepository: AttributeRepository,
    private val pricingRepository: PricingRepository,
    private val inventoryRepository: InventoryRepository,
    @ApplicationContext private val app: Context,
    private val productImageRepository: ProductImageRepository,
) : ViewModel() {

    private val productId =
        checkNotNull(
            savedStateHandle.get<Int>("productId")
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState =
        productRepository
            .observeProductFullInfo(productId)
            .flatMapLatest { product ->

                if (product == null) {
                    flowOf(
                        ProductDetailUiState(
                            isLoading = false
                        )
                    )
                } else {

                    combine(
                        attributeRepository.observeProductAttributeDisplayInfo(
                            productId = productId,
                            categoryId = product.product.categoryId
                        ),
                        inventoryRepository.observeCurrentStock(productId),
                        inventoryRepository.observeTransactions(productId),
                        productImageRepository.observeImagesForProduct(productId)
                    ) { attributes, currentStock, transactions, images ->

                        val activePrice =
                            pricingRepository.getActivePurchasePrice(productId)

                        val salePriceResult =
                            pricingRepository.calculateSalePrice(productId)

                        ProductDetailUiState(
                            isLoading = false,
                            product = product,
                            attributes = attributes,
                            activePurchasePrice = activePrice,
                            salePriceResult = salePriceResult,
                            currentStock = currentStock,
                            inventoryTransactions = transactions,
                            images = images.map { image ->
                                val file = File(File(app.filesDir, "images"), image.fileName)
                                val uri = FileProvider.getUriForFile(
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
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
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
}