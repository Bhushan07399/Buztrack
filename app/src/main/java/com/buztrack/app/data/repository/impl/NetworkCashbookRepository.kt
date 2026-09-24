package com.buztrack.app.data.repository.impl

import com.buztrack.app.data.remote.api.CashbookApiService
import com.buztrack.app.data.remote.dto.CloseDayRequestDto
import com.buztrack.app.data.remote.dto.DailyClosingDto
import com.buztrack.app.data.remote.network.NetworkResult
import com.buztrack.app.domain.repository.CashbookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkCashbookRepository(
    private val cashbookApiService: CashbookApiService
) : CashbookRepository {

    private val _dailyClosings = MutableStateFlow<List<DailyClosingDto>>(emptyList())
    override val dailyClosings: StateFlow<List<DailyClosingDto>> = _dailyClosings

    override suspend fun fetchDailyClosings(): NetworkResult<List<DailyClosingDto>> {
        return try {
            val response = cashbookApiService.getDailyClosings()
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()?.data ?: emptyList()
                _dailyClosings.value = list
                NetworkResult.Success(list)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to fetch daily closings")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun recordDailyClosing(closing: DailyClosingDto): NetworkResult<DailyClosingDto> {
        return try {
            val dateStr = closing.date ?: closing.closingDate ?: ""
            val response = cashbookApiService.closeDay(
                CloseDayRequestDto(
                    date = dateStr,
                    actualCash = closing.actualCash,
                    notes = closing.notes
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val created = response.body()?.data!!
                _dailyClosings.value = listOf(created) + _dailyClosings.value
                NetworkResult.Success(created)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Daily closing failed")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }
}
