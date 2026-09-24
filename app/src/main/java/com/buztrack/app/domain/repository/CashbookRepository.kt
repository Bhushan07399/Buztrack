package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.DailyClosingDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface CashbookRepository {
    val dailyClosings: StateFlow<List<DailyClosingDto>>
    suspend fun fetchDailyClosings(): NetworkResult<List<DailyClosingDto>>
    suspend fun recordDailyClosing(closing: DailyClosingDto): NetworkResult<DailyClosingDto>
}
