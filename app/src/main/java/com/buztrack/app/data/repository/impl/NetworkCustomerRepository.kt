package com.buztrack.app.data.repository.impl

import com.buztrack.app.data.remote.api.CustomerApiService
import com.buztrack.app.data.remote.dto.CustomerDto
import com.buztrack.app.data.remote.network.NetworkResult
import com.buztrack.app.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkCustomerRepository(
    private val customerApiService: CustomerApiService
) : CustomerRepository {

    private val _customers = MutableStateFlow<List<CustomerDto>>(emptyList())
    override val customers: StateFlow<List<CustomerDto>> = _customers

    override suspend fun fetchCustomers(): NetworkResult<List<CustomerDto>> {
        return try {
            val response = customerApiService.getCustomers()
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()?.data ?: emptyList()
                _customers.value = list
                NetworkResult.Success(list)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to fetch customers")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun addCustomer(name: String, phone: String, address: String?): NetworkResult<CustomerDto> {
        return try {
            val response = customerApiService.createCustomer(
                CustomerDto(
                    id = "",
                    name = name,
                    phone = phone,
                    address = address
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val created = response.body()?.data!!
                _customers.value = listOf(created) + _customers.value
                NetworkResult.Success(created)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to add customer")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun recordCredit(
        customerId: String,
        amount: Double,
        note: String,
        date: String
    ): NetworkResult<CustomerDto> {
        // Delegates to customer record update or re-fetches customer state
        return fetchCustomers().let {
            val updated = _customers.value.find { it.id == customerId }
            if (updated != null) NetworkResult.Success(updated)
            else NetworkResult.Error(null, "Customer not found")
        }
    }

    override suspend fun recordPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        note: String,
        date: String
    ): NetworkResult<CustomerDto> {
        return fetchCustomers().let {
            val updated = _customers.value.find { it.id == customerId }
            if (updated != null) NetworkResult.Success(updated)
            else NetworkResult.Error(null, "Customer not found")
        }
    }
}
