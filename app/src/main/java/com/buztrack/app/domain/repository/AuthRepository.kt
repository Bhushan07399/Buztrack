package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.AuthResponseDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authToken: StateFlow<String?>
    val activeBusinessId: StateFlow<String?>
    val isAuthenticated: StateFlow<Boolean>

    suspend fun register(phone: String, fullName: String, businessName: String, password: String): NetworkResult<AuthResponseDto>
    suspend fun login(phone: String, password: String): NetworkResult<AuthResponseDto>
    suspend fun logout()
}
