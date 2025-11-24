package com.example.siwasip.data.remote

import com.example.siwasip.data.model.BasicResponse
import com.example.siwasip.data.model.DocumentListResponse
import com.example.siwasip.data.model.LoginRequest
import com.example.siwasip.data.model.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header

interface SiwasisApi {

    // Login
    @POST("login")
    suspend fun login(
        @Body body: LoginRequest
    ): LoginResponse

    // List dokumen
    @GET("documents")
    suspend fun getDocuments(
        @Header("Authorization") auth: String,
        @Query("page") page: Int? = null,
        @Query("q") query: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("per_page") perPage: Int? = 15
    ): DocumentListResponse

    // Upload dokumen
    @Multipart
    @POST("documents")
    suspend fun uploadDocument(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("uploaded_at") uploadedAt: RequestBody
    ): BasicResponse

    // Update dokumen
    @Multipart
    @POST("documents/{id}")
    suspend fun updateDocument(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Part("_method") method: RequestBody,
        @Part file: MultipartBody.Part?,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("uploaded_at") uploadedAt: RequestBody
    ): BasicResponse

    // Delete dokumen
    @DELETE("documents/{id}")
    suspend fun deleteDocument(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): BasicResponse
}