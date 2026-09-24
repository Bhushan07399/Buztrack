package com.buztrack.app

import com.buztrack.app.data.remote.dto.AuthResponseDto
import com.buztrack.app.data.remote.dto.BusinessDto
import com.buztrack.app.data.remote.dto.UserDto
import com.buztrack.app.data.remote.network.NetworkResult
import com.buztrack.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAuthRepository : AuthRepository {
    private val _authToken = MutableStateFlow<String?>(null)
    override val authToken: StateFlow<String?> = _authToken

    private val _activeBusinessId = MutableStateFlow<String?>(null)
    override val activeBusinessId: StateFlow<String?> = _activeBusinessId

    private val _isAuthenticated = MutableStateFlow<Boolean>(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    override suspend fun register(
        phone: String,
        fullName: String,
        businessName: String,
        password: String
    ): NetworkResult<AuthResponseDto> {
        _authToken.value = "mock_jwt_token_123"
        _activeBusinessId.value = "biz_mock_123"
        _isAuthenticated.value = true
        return NetworkResult.Success(
            AuthResponseDto(
                token = "mock_jwt_token_123",
                user = UserDto(id = "usr_1", fullName = fullName, phone = phone),
                business = BusinessDto(id = "biz_mock_123", name = businessName, businessName = businessName)
            )
        )
    }

    override suspend fun login(phone: String, password: String): NetworkResult<AuthResponseDto> {
        return if (password == "password123") {
            _authToken.value = "mock_jwt_token_123"
            _activeBusinessId.value = "biz_mock_123"
            _isAuthenticated.value = true
            NetworkResult.Success(
                AuthResponseDto(
                    token = "mock_jwt_token_123",
                    user = UserDto(id = "usr_1", fullName = "Rahul Sharma", phone = phone),
                    business = BusinessDto(id = "biz_mock_123", name = "Sharma Store", businessName = "Sharma Store")
                )
            )
        } else {
            NetworkResult.Error(401, "Invalid phone number or password")
        }
    }

    override suspend fun logout() {
        _authToken.value = null
        _activeBusinessId.value = null
        _isAuthenticated.value = false
    }
}

class AuthUnitTest {

    @Test
    fun testSuccessfulLogin() {
        val mockRepo = MockAuthRepository()
        assertFalse(mockRepo.isAuthenticated.value)

        // Simulate login
        kotlinx.coroutines.runBlocking {
            val result = mockRepo.login("9876543210", "password123")
            assertTrue(result is NetworkResult.Success)
            assertTrue(mockRepo.isAuthenticated.value)
            assertEquals("mock_jwt_token_123", mockRepo.authToken.value)
        }
    }

    @Test
    fun testFailedLoginWithWrongPassword() {
        val mockRepo = MockAuthRepository()

        kotlinx.coroutines.runBlocking {
            val result = mockRepo.login("9876543210", "WRONG_PASS")
            assertTrue(result is NetworkResult.Error)
            assertFalse(mockRepo.isAuthenticated.value)
        }
    }

    @Test
    fun testLogoutClearsSession() {
        val mockRepo = MockAuthRepository()

        kotlinx.coroutines.runBlocking {
            mockRepo.login("9876543210", "password123")
            assertTrue(mockRepo.isAuthenticated.value)

            mockRepo.logout()
            assertFalse(mockRepo.isAuthenticated.value)
            assertEquals(null, mockRepo.authToken.value)
        }
    }
}
