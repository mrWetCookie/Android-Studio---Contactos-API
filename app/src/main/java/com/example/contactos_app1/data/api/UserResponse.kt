package com.example.contactos_app1.data.api

data class UserResponse(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val images: List<ImageResponse> = emptyList(),
    val email_verified_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class ImageResponse(
    val id: Int,
    val url: String,
    val imageable_type: String? = null,
    val imageable_id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)