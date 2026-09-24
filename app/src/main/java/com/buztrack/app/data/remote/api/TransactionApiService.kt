package com.buztrack.app.data.remote.api

import com.buztrack.app.data.remote.dto.ApiResponse
import com.buztrack.app.data.remote.dto.TransactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApiService {
    @GET("transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("paymentMethod") paymentMethod: String? = null,
        @Query("customerId") customerId: String? = null,
        @Query("supplierId") supplierId: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<TransactionDto>>>

    @POST("transactions")
    suspend fun createTransaction(@Body request: TransactionDto): Response<ApiResponse<TransactionDto>>

    @GET("transactions/{id}")
    suspend fun getTransactionById(@Path("id") id: String): Response<ApiResponse<TransactionDto>>

    @PUT("transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body request: TransactionDto): Response<ApiResponse<Any>>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<ApiResponse<Any>>
}
