package com.buztrack.app.data.repository.impl

import com.buztrack.app.data.remote.api.SupplierApiService
import com.buztrack.app.data.remote.dto.SupplierDto
import com.buztrack.app.data.remote.network.NetworkResult
import com.buztrack.app.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkSupplierRepository(
    private val supplierApiService: SupplierApiService
) : SupplierRepository {

    private val _suppliers = MutableStateFlow<List<SupplierDto>>(emptyList())
    override val suppliers: StateFlow<List<SupplierDto>> = _suppliers

    override suspend fun fetchSuppliers(): NetworkResult<List<SupplierDto>> {
        return try {
            val response = supplierApiService.getSuppliers()
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()?.data ?: emptyList()
                _suppliers.value = list
                NetworkResult.Success(list)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to fetch suppliers")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun addSupplier(name: String, phone: String, address: String?): NetworkResult<SupplierDto> {
        return try {
            val response = supplierApiService.createSupplier(
                SupplierDto(
                    id = "",
                    name = name,
                    phone = phone,
                    address = address
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val created = response.body()?.data!!
                _suppliers.value = listOf(created) + _suppliers.value
                NetworkResult.Success(created)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to add supplier")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun recordPurchase(
        supplierId: String,
        amount: Double,
        note: String,
        date: String
    ): NetworkResult<SupplierDto> {
        return fetchSuppliers().let {
            val updated = _suppliers.value.find { it.id == supplierId }
            if (updated != null) NetworkResult.Success(updated)
            else NetworkResult.Error(null, "Supplier not found")
        }
    }

    override suspend fun recordPayment(
        supplierId: String,
        amount: Double,
        paymentMethod: String,
        note: String,
        date: String
    ): NetworkResult<SupplierDto> {
        return fetchSuppliers().let {
            val updated = _suppliers.value.find { it.id == supplierId }
            if (updated != null) NetworkResult.Success(updated)
            else NetworkResult.Error(null, "Supplier not found")
        }
    }
}
