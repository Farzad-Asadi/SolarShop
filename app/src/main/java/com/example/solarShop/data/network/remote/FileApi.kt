package com.example.solarShop.data.network.remote

import com.example.solarShop.data.network.dto.file.FileExistsResponseDto
import com.example.solarShop.data.network.dto.file.FileUploadResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.io.File

class FileApi(
    private val client: HttpClient
) {

    suspend fun uploadImage(file: File): String? {
        if (!file.exists()) return null

        val response = client.post("files/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = file.readBytes(),
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"${file.name}\""
                                )
                            }
                        )
                    }
                )
            )
        }

        if (!response.status.isSuccess()) return null

        return response.body<FileUploadResponseDto>().fileName
    }

    suspend fun fileExists(fileName: String): Boolean {
        return client.get("files/exists/$fileName")
            .body<FileExistsResponseDto>()
            .exists
    }

    suspend fun downloadFile(fileName: String): ByteArray? {
        val response = client.get("files/$fileName")

        if (!response.status.isSuccess()) return null

        return response.body()
    }
}