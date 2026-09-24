package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.BusinessDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface BusinessRepository {
    val currentBusiness: StateFlow<BusinessDto?>
    suspend fun fetchBusinessProfile(): NetworkResult<BusinessDto>
    suspend fun updateBusinessProfile(name: String, phone: String, address: String, gstin: String): NetworkResult<BusinessDto>
}
