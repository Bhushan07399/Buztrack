package com.buztrack.app.data.remote.api

import com.buztrack.app.data.remote.dto.ApiResponse
import com.buztrack.app.data.remote.dto.CustomerDto
import com.buztrack.app.data.remote.dto.TransactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CustomerApiService {
    @GET("customers")
    suspend fun getCustomers(): Response<ApiResponse<List<CustomerDto>>>

    @POST("customers")
    suspend fun createCustomer(@Body request: CustomerDto): Response<ApiResponse<CustomerDto>>

    @GET("customers/{id}")
    suspend fun getCustomerById(@Path("id") id: String): Response<ApiResponse<CustomerDto>>

    @GET("customers/{id}/transactions")
    suspend fun getCustomerTransactions(@Path("id") id: String): Response<ApiResponse<List<TransactionDto>>>

    @PUT("customers/{id}")
    suspend fun updateCustomer(@Path("id") id: String, @Body request: CustomerDto): Response<ApiResponse<Any>>

    @DELETE("customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: String): Response<ApiResponse<Any>>
}
