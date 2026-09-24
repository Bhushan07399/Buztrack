package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.BillDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface BillRepository {
    val bills: StateFlow<List<BillDto>>
    suspend fun fetchBills(): NetworkResult<List<BillDto>>
    suspend fun scanBillOcr(imageBytes: ByteArray): NetworkResult<BillDto>
    suspend fun saveBill(bill: BillDto): NetworkResult<BillDto>
    suspend fun deleteBill(billId: String): NetworkResult<Boolean>
}
