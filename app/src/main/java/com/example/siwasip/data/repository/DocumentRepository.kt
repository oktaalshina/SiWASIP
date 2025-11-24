package com.example.siwasip.data.repository

import com.example.siwasip.data.model.BasicResponse
import com.example.siwasip.data.model.DocumentListResponse
import com.example.siwasip.data.remote.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class DocumentRepository(
    private val tokenProvider: () -> String?
) {

    private fun bearerToken(): String {
        val token = tokenProvider() ?: ""
        return if (token.isNotBlank()) "Bearer $token" else ""
    }

    suspend fun getDocuments(
        page: Int? = 1,
        query: String? = null,
        from: String? = null,
        to: String? = null,
        perPage: Int? = 15
    ): DocumentListResponse {
        return ApiClient.api.getDocuments(
            auth = bearerToken(),
            page = page,
            query = query,
            from = from,
            to = to,
            perPage = perPage
        )
    }

    suspend fun uploadDocument(
        file: File,
        title: String,
        description: String?,
        uploadedAt: String
    ): BasicResponse {
        val mediaType = "application/pdf".toMediaTypeOrNull()
        val fileRequestBody = RequestBody.create(mediaType, file)
        val filePart = MultipartBody.Part.createFormData(
            name = "file_path",
            filename = file.name,
            body = fileRequestBody
        )

        val titleBody = title.toRequestBodyText()
        val descBody = description?.toRequestBodyText()
        val uploadedAtBody = uploadedAt.toRequestBodyText()

        return ApiClient.api.uploadDocument(
            auth = bearerToken(),
            file = filePart,
            title = titleBody,
            description = descBody,
            uploadedAt = uploadedAtBody
        )
    }

    suspend fun updateDocument(
        id: Int,
        file: File?,
        title: String,
        description: String?,
        uploadedAt: String
    ): BasicResponse {
        val methodBody = "PUT".toRequestBodyText()

        val filePart: MultipartBody.Part? = file?.let {
            val mediaType = "application/pdf".toMediaTypeOrNull()
            val body = RequestBody.create(mediaType, it)
            MultipartBody.Part.createFormData(
                name = "file_path",
                filename = it.name,
                body = body
            )
        }

        val titleBody = title.toRequestBodyText()
        val descBody = description?.toRequestBodyText()
        val uploadedAtBody = uploadedAt.toRequestBodyText()

        return ApiClient.api.updateDocument(
            auth = bearerToken(),
            id = id,
            method = methodBody,
            file = filePart,
            title = titleBody,
            description = descBody,
            uploadedAt = uploadedAtBody
        )
    }

    suspend fun deleteDocument(id: Int): BasicResponse {
        return ApiClient.api.deleteDocument(
            auth = bearerToken(),
            id = id
        )
    }

    private fun String.toRequestBodyText(): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), this)
    }
}