package com.example.solarShop.data.repository.productImage

import android.net.Uri
import com.example.solarShop.data.local.dao.product.ProductImageDao
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.repo.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
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
        val now = System.currentTimeMillis()
        productImageDao.insert(


        ProductImageEntity(
            uid = UUID.randomUUID().toString(),
            productId = productId,
            fileName = file.name,
            createdAt = now,
            sortOrder = maxOrder + 1,
            updatedAt = now,
            deletedAt = null,
            isSynced = false
        )
        )
    }

    override suspend fun addImageFromCameraTemp(
        productId: Int,
        tempFile: File
    ) = withContext(Dispatchers.IO) {

        val (file, _) = imageRepository.compressCameraTempToInternal(tempFile)

        val maxOrder = productImageDao.getMaxSortOrder(productId) ?: -1
        val now = System.currentTimeMillis()
        productImageDao.insert(
            ProductImageEntity(
                uid = UUID.randomUUID().toString(),
                productId = productId,
                fileName = file.name,
                createdAt = now,
                sortOrder = maxOrder + 1,
                updatedAt = now,
                deletedAt = null,
                isSynced = false
            )
        )
    }

    override suspend fun deleteImage(
        imageId: Int
    ) = withContext(Dispatchers.IO) {

        val image = productImageDao.getById(imageId) ?: return@withContext

        productImageDao.softDeleteById(imageId)
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

    override suspend fun addExistingImageFile(
        productId: Int,
        fileName: String,
        sortOrder: Int
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        productImageDao.insert(
            ProductImageEntity(
                uid = UUID.randomUUID().toString(),
                productId = productId,
                fileName = fileName,
                createdAt = now,
                sortOrder = sortOrder,
                updatedAt = now,
                deletedAt = null,
                isSynced = false
            )
        )
    }

    override suspend fun saveImageOrder(
        imageIds: List<Int>
    ) = withContext(Dispatchers.IO) {
        imageIds.forEachIndexed { index, imageId ->
            productImageDao.updateOrder(
                imageId = imageId,
                sortOrder = index
            )
        }
    }

    override suspend fun getMaxSortOrder(
        productId: Int
    ): Int? = withContext(Dispatchers.IO) {
        productImageDao.getMaxSortOrder(productId)
    }


    override suspend fun getUnsyncedProductImages(): List<ProductImageEntity> =
        withContext(Dispatchers.IO) {
            productImageDao.getUnsyncedProductImages()
        }

    override suspend fun markProductImagesSynced(
        uids: List<String>
    ) = withContext(Dispatchers.IO) {
        productImageDao.markProductImagesSynced(uids)
    }

    override suspend fun upsertProductImageByUid(
        image: ProductImageEntity
    ): Long = withContext(Dispatchers.IO) {
        val existing = image.uid
            ?.takeIf { it.isNotBlank() }
            ?.let { productImageDao.getByUid(it) }

        if (existing == null) {
            return@withContext productImageDao.upsertProductImage(image)
        }

        if (existing.deletedAt != null && image.deletedAt == null) {
            return@withContext existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > image.updatedAt) {
            return@withContext existing.id?.toLong() ?: 0L
        }

        productImageDao.upsertProductImage(
            image.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getAllImagesForBackup(): List<ProductImageEntity> =
        productImageDao.getAllImagesForBackup()

}