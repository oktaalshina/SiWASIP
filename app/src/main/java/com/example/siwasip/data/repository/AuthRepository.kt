package com.example.siwasip.data.repository

import com.example.siwasip.data.local.Prefs
import com.example.siwasip.data.model.LoginRequest
import com.example.siwasip.data.model.User
import com.example.siwasip.data.remote.ApiClient


class AuthRepository {

    suspend fun login(email: String, password: String): User? {
        val body = LoginRequest(email = email, password = password)
        val response = ApiClient.api.login(body)

        // Ambil token
        val tokenFromRoot = response.token
        val tokenFromData = response.data?.token

        val token = tokenFromRoot ?: tokenFromData
        if (!token.isNullOrBlank()) {
            Prefs.authToken = token
        }

        val userFromAdmin = response.admin
        val userFromData = response.data?.user

        return userFromAdmin ?: userFromData
    }

    fun logout() {
        Prefs.clear()
    }

    fun getSavedToken(): String? = Prefs.authToken
}