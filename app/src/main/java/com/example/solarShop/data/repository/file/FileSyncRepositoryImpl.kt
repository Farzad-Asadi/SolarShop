package com.example.solarShop.data.repository.file

import com.example.solarShop.data.network.remote.FileApi
import com.example.solarShop.repo.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FileSyncRepositoryImpl @Inject constructor(
    private val fileApi: FileApi,
    private val imageRepository: ImageRepository
) : FileSyncRepository {

    override suspend fun uploadIfNeeded(
        fileName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        if (fileName.isNullOrBlank()) return@withContext true

        val localFile = imageRepository.getInternalImageFile(fileName)

        if (!localFile.exists()) return@withContext false

        val existsOnServer = fileApi.fileExists(fileName)

        if (existsOnServer) return@withContext true

        val uploadedName = fileApi.uploadImage(localFile)

        uploadedName == fileName || uploadedName != null
    }

    override suspend fun downloadIfMissing(
        fileName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        if (fileName.isNullOrBlank()) return@withContext true

        if (imageRepository.internalImageExists(fileName)) {
            return@withContext true
        }

        val bytes = fileApi.downloadFile(fileName)
            ?: return@withContext false

        imageRepository.saveBytesToInternalImage(
            fileName = fileName,
            bytes = bytes
        )

        true
    }
}