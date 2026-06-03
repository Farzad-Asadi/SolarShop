package com.example.solarShop.data.repository.productImage

import android.net.Uri
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ProductImageRepository {

    fun observeImagesForProduct(
        productId: Int
    ): Flow<List<ProductImageEntity>>

    suspend fun addImageFromUri(
        productId: Int,
        src: Uri
    )

    fun createCameraTempUri(): Pair<File, Uri>

    suspend fun addImageFromCameraTemp(
        productId: Int,
        tempFile: File
    )

    suspend fun deleteImage(
        imageId: Int
    )

    suspend fun setAsCover(
        productId: Int,
        imageId: Int
    )

    fun observeImagesForProducts(
        productIds: List<Int>
    ): Flow<List<ProductImageEntity>>

    suspend fun addExistingImageFile(
        productId: Int,
        fileName: String,
        sortOrder: Int
    )


    suspend fun saveImageOrder(imageIds: List<Int>)

    suspend fun getMaxSortOrder(productId: Int): Int?
}