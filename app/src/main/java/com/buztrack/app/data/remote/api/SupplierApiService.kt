package com.buztrack.app.data.remote.api

import com.buztrack.app.data.remote.dto.ApiResponse
import com.buztrack.app.data.remote.dto.SupplierDto
import com.buztrack.app.data.remote.dto.TransactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SupplierApiService {
    @GET("suppliers")
    suspend fun getSuppliers(): Response<ApiResponse<List<SupplierDto>>>

    @POST("suppliers")
    suspend fun createSupplier(@Body request: SupplierDto): Response<ApiResponse<SupplierDto>>

    @GET("suppliers/{id}")
    suspend fun getSupplierById(@Path("id") id: String): Response<ApiResponse<SupplierDto>>

    @GET("suppliers/{id}/transactions")
    suspend fun getSupplierTransactions(@Path("id") id: String): Response<ApiResponse<List<TransactionDto>>>

    @PUT("suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: String, @Body request: SupplierDto): Response<ApiResponse<Any>>

    @DELETE("suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: String): Response<ApiResponse<Any>>
}
