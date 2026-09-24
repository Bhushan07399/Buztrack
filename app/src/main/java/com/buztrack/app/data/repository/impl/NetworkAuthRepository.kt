package com.buztrack.app.data.repository.impl

import com.buztrack.app.data.remote.api.AuthApiService
import com.buztrack.app.data.remote.dto.AuthResponseDto
import com.buztrack.app.data.remote.dto.LoginRequestDto
import com.buztrack.app.data.remote.dto.RegisterRequestDto
import com.buztrack.app.data.remote.network.NetworkResult
import com.buztrack.app.data.session.SessionManager
import com.buztrack.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkAuthRepository(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val _authToken = MutableStateFlow<String?>(sessionManager.getAuthToken())
    override val authToken: StateFlow<String?> = _authToken

    private val _activeBusinessId = MutableStateFlow<String?>(sessionManager.getActiveBusinessId())
    override val activeBusinessId: StateFlow<String?> = _activeBusinessId

    private val _isAuthenticated = MutableStateFlow<Boolean>(sessionManager.isLoggedIn())
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    override suspend fun register(
        phone: String,
        fullName: String,
        businessName: String,
        password: String
    ): NetworkResult<AuthResponseDto> {
        return try {
            val response = authApiService.register(
                RegisterRequestDto(
                    fullName = fullName,
                    phone = phone,
                    password = password,
                    businessName = businessName
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data!!
                saveSessionData(data)
                NetworkResult.Success(data)
            } else {
                val errorMsg = response.body()?.error ?: "Registration failed. Please check your details."
                NetworkResult.Error(response.code(), errorMsg)
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Unable to connect to server. Please check your internet connection.")
        }
    }

    override suspend fun login(phone: String, password: String): NetworkResult<AuthResponseDto> {
        return try {
            val response = authApiService.login(LoginRequestDto(phone = phone, password = password))

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data!!
                saveSessionData(data)
                NetworkResult.Success(data)
            } else {
                val errorMsg = response.body()?.error ?: "Invalid phone number or password."
                NetworkResult.Error(response.code(), errorMsg)
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Unable to connect to server. Please check your internet connection.")
        }
    }

    override suspend fun logout() {
        try {
            authApiService.logout()
        } catch (_: Exception) {}
        sessionManager.clearSession()
        _authToken.value = null
        _activeBusinessId.value = null
        _isAuthenticated.value = false
    }

    private fun saveSessionData(data: AuthResponseDto) {
        sessionManager.saveSession(
            token = data.token,
            userId = data.user.id,
            phone = data.user.phone,
            activeBusinessId = data.business.id,
            businessName = data.business.name ?: "My Business",
            fullName = data.user.fullName
        )
        _authToken.value = data.token
        _activeBusinessId.value = data.business.id
        _isAuthenticated.value = true
    }
}
