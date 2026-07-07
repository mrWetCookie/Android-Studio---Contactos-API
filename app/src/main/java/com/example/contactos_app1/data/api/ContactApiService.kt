package com.example.contactos_app1.data.api

import com.example.contactos_app1.data.Contact
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ContactApiService {

    @GET("api/users")
    suspend fun getUsers(): Response<List<UserResponse>>

    @GET("api/users/{id}")
    suspend fun getUser(
        @Path("id") id: Int
    ): Response<UserResponse>

    @POST("api/users")
    suspend fun createUser(
        @Body contact: Contact
    ): Response<UserResponse>

    @Multipart  // ✅ NUEVO: Para crear con imagen
    @POST("api/users")
    suspend fun createUserWithImage(
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<UserResponse>

    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body contact: Contact
    ): Response<UserResponse>

    @Multipart  // ✅ NUEVO: Para actualizar con imagen
    @PUT("api/users/{id}")
    suspend fun updateUserWithImage(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<UserResponse>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(
        @Path("id") id: Int
    ): Response<Unit>
}