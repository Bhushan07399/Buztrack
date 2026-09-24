package com.buztrack.app.data.remote.api

import com.buztrack.app.data.remote.dto.ApiResponse
import com.buztrack.app.data.remote.dto.AuthResponseDto
import com.buztrack.app.data.remote.dto.LoginRequestDto
import com.buztrack.app.data.remote.dto.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<ApiResponse<AuthResponseDto>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponse<AuthResponseDto>>

    @GET("auth/me")
    suspend fun me(): Response<ApiResponse<AuthResponseDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>
}
