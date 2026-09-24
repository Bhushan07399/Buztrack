package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.SupplierDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface SupplierRepository {
    val suppliers: StateFlow<List<SupplierDto>>
    suspend fun fetchSuppliers(): NetworkResult<List<SupplierDto>>
    suspend fun addSupplier(name: String, phone: String, address: String? = null): NetworkResult<SupplierDto>
    suspend fun recordPurchase(supplierId: String, amount: Double, note: String, date: String): NetworkResult<SupplierDto>
    suspend fun recordPayment(supplierId: String, amount: Double, paymentMethod: String, note: String, date: String): NetworkResult<SupplierDto>
}
