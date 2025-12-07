package com.example.siwasip.data.model

import com.google.gson.annotations.SerializedName

data class ProfileData(
    val id: Int?,
    val name: String?,
    val email: String?,
    @SerializedName("photo_url")
    val photoUrl: String?
)

data class ProfileResponse(
    val message: String?,
    val data: ProfileData?
)