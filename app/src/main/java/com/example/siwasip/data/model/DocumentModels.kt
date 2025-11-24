package com.example.siwasip.data.model

data class Pagination(
    val total: Int = 0,
    val current_page: Int = 1,
    val per_page: Int = 15
)

data class Document(
    val id: Int?,
    val title: String?,
    val filename: String?,
    val description: String?,
    val uploaded_at: String?,
    val file_path: String?
)

data class DocumentListResponse(
    val data: List<Document> = emptyList(),
    val pagination: Pagination? = null
)

data class BasicResponse(
    val message: String? = null,
    val success: Boolean? = null
)