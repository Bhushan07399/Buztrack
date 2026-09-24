package com.buztrack.app.data.remote.api

import com.buztrack.app.data.remote.dto.ApiResponse
import com.buztrack.app.data.remote.dto.CashbookSummaryDto
import com.buztrack.app.data.remote.dto.CloseDayRequestDto
import com.buztrack.app.data.remote.dto.DailyClosingDto
import com.buztrack.app.data.remote.dto.SetOpeningCashRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CashbookApiService {
    @GET("cashbook/summary")
    suspend fun getCashbookSummary(@Query("date") date: String? = null): Response<ApiResponse<CashbookSummaryDto>>

    @POST("cashbook/opening")
    suspend fun setOpeningCash(@Body request: SetOpeningCashRequestDto): Response<ApiResponse<Any>>

    @POST("cashbook/close")
    suspend fun closeDay(@Body request: CloseDayRequestDto): Response<ApiResponse<DailyClosingDto>>

    @GET("cashbook/closings")
    suspend fun getDailyClosings(): Response<ApiResponse<List<DailyClosingDto>>>
}
