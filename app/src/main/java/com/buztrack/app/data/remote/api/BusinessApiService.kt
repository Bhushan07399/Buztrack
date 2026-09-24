package com.buztrack.app.data.remote.api

import com.buztrack.app.data.remote.dto.ApiResponse
import com.buztrack.app.data.remote.dto.BusinessDto
import com.buztrack.app.data.remote.dto.SwitchBusinessRequestDto
import com.buztrack.app.data.remote.dto.UpdateBusinessProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface BusinessApiService {
    @GET("business/profile")
    suspend fun getProfile(): Response<ApiResponse<BusinessDto>>

    @PUT("business/profile")
    suspend fun updateProfile(@Body request: UpdateBusinessProfileDto): Response<ApiResponse<BusinessDto>>

    @GET("business/my-businesses")
    suspend fun getMyBusinesses(): Response<ApiResponse<List<BusinessDto>>>

    @POST("business/switch")
    suspend fun switchBusiness(@Body request: SwitchBusinessRequestDto): Response<ApiResponse<Any>>
}
