package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.CustomerDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface CustomerRepository {
    val customers: StateFlow<List<CustomerDto>>
    suspend fun fetchCustomers(): NetworkResult<List<CustomerDto>>
    suspend fun addCustomer(name: String, phone: String, address: String? = null): NetworkResult<CustomerDto>
    suspend fun recordCredit(customerId: String, amount: Double, note: String, date: String): NetworkResult<CustomerDto>
    suspend fun recordPayment(customerId: String, amount: Double, paymentMethod: String, note: String, date: String): NetworkResult<CustomerDto>
}
