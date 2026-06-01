package com.example.solarShop.data.repository.productImage

import android.net.Uri
import com.example.solarShop.data.local.dao.product.ProductImageDao
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.repo.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ProductImageRepositoryImpl @Inject constructor(
    private val productImageDao: ProductImageDao,
    private val imageRepository: ImageRepository
) : ProductImageRepository {

    override fun observeImagesForProduct(productId: Int) =
        productImageDao.observeImagesForProduct(productId)

    override fun createCameraTempUri(): Pair<File, Uri> {
        return imageRepository.createCameraTempUri()
    }

    override suspend fun addImageFromUri(
        productId: Int,
        src: Uri
    ) = withContext(Dispatchers.IO) {

        val (file, _) = imageRepository.saveCompressedToInternal(src)

        val maxOrder = productImageDao.getMaxSortOrder(productId) ?: -1

        productImageDao.insert(
            ProductImageEntity(
                productId = productId,
                fileName = file.name,
                createdAt = System.currentTimeMillis(),
                sortOrder = maxOrder + 1
            )
        )
    }

    override suspend fun addImageFromCameraTemp(
        productId: Int,
        tempFile: File
    ) = withContext(Dispatchers.IO) {

        val (file, _) = imageRepository.compressCameraTempToInternal(tempFile)

        val maxOrder = productImageDao.getMaxSortOrder(productId) ?: -1

        productImageDao.insert(
            ProductImageEntity(
                productId = productId,
                fileName = file.name,
                createdAt = System.currentTimeMillis(),
                sortOrder = maxOrder + 1
            )
        )
    }

    override suspend fun deleteImage(
        imageId: Int
    ) = withContext(Dispatchers.IO) {

        val image = productImageDao.getById(imageId) ?: return@withContext

        imageRepository.deleteInternalImage(image.fileName)

        productImageDao.deleteById(imageId)
    }

    override suspend fun setAsCover(
        productId: Int,
        imageId: Int
    ) = withContext(Dispatchers.IO) {

        val images =
            productImageDao.getImagesForProduct(productId)

        val selected =
            images.firstOrNull { it.id == imageId }
                ?: return@withContext

        val reordered =
            buildList {

                add(selected)

                images
                    .filter { it.id != imageId }
                    .forEach { add(it) }
            }

        reordered.forEachIndexed { index, image ->

            val id = image.id ?: return@forEachIndexed

            productImageDao.updateOrder(
                imageId = id,
                sortOrder = index
            )
        }
    }

    override fun observeImagesForProducts(
        productIds: List<Int>
    ): Flow<List<ProductImageEntity>> {
        return if (productIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            productImageDao.observeImagesForProducts(productIds)
        }
    }

}