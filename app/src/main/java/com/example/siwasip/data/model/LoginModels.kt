package com.example.siwasip.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class User(
    val id: Int?,
    val name: String?,
    val email: String?,
    val username: String?
)

data class LoginResponse(
    val token: String?,
    val data: LoginDataWrapper? = null,
    val admin: User? = null
)

data class LoginDataWrapper(
    val token: String?,
    val user: User?
)