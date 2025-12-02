// ProfileRepository.kt (BARU)

package com.example.siwasip.data.repository

import com.example.siwasip.data.model.BasicResponse
import com.example.siwasip.data.model.ProfileData
import com.example.siwasip.data.remote.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ProfileRepository(
    private val tokenProvider: () -> String?
) {
    private fun bearerToken(): String {
        val token = tokenProvider() ?: ""
        return if (token.isNotBlank()) "Bearer $token" else ""
    }

    private fun String.toRequestBodyText(): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), this)
    }

    suspend fun getProfile(): ProfileData {
        return ApiClient.api.getProfile(auth = bearerToken())
    }
    suspend fun updateProfile(
        username: String,
        email: String,
        password: String? = null,
        imageFile: File? = null
    ): BasicResponse {
        val textPlain = "text/plain".toMediaTypeOrNull()

        val methodBody = "PUT".toRequestBodyText()
        val usernameBody = username.toRequestBodyText()
        val emailBody = email.toRequestBodyText()

        val passwordBody: RequestBody?
        val passwordConfBody: RequestBody?

        if (!password.isNullOrBlank()) {
            val pw = password.toRequestBody(textPlain)
            passwordBody = pw
            passwordConfBody = pw
        } else {
            passwordBody = null
            passwordConfBody = null
        }

        val imagePart: MultipartBody.Part? = imageFile?.let {
            val mediaType = "image/*".toMediaTypeOrNull() // Sesuaikan dengan tipe file yang diharapkan server
            val requestFile = RequestBody.create(mediaType, it)
            MultipartBody.Part.createFormData(
                name = "photo_url", // Nama field di API untuk file avatar
                filename = it.name,
                body = requestFile
            )
        }

        return ApiClient.api.updateProfile(
            auth = bearerToken(),
            method = methodBody,
            username = usernameBody,
            email = emailBody,
            password = passwordBody,
            passwordConfirmation = passwordConfBody,
            photo_url = imagePart
        )
    }

    suspend fun logout(): BasicResponse {
        return ApiClient.api.logout(auth = bearerToken())
    }
}