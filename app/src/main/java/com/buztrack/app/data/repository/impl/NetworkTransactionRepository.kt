package com.buztrack.app.data.repository.impl

import com.buztrack.app.data.remote.api.TransactionApiService
import com.buztrack.app.data.remote.dto.TransactionDto
import com.buztrack.app.data.remote.network.NetworkResult
import com.buztrack.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkTransactionRepository(
    private val transactionApiService: TransactionApiService
) : TransactionRepository {

    private val _transactions = MutableStateFlow<List<TransactionDto>>(emptyList())
    override val transactions: StateFlow<List<TransactionDto>> = _transactions

    override suspend fun fetchTransactions(): NetworkResult<List<TransactionDto>> {
        return try {
            val response = transactionApiService.getTransactions()
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()?.data ?: emptyList()
                _transactions.value = list
                NetworkResult.Success(list)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to fetch transactions")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun createTransaction(transaction: TransactionDto): NetworkResult<TransactionDto> {
        return try {
            val response = transactionApiService.createTransaction(transaction)
            if (response.isSuccessful && response.body()?.success == true) {
                val created = response.body()?.data!!
                _transactions.value = listOf(created) + _transactions.value
                NetworkResult.Success(created)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to create transaction")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun deleteTransaction(id: String): NetworkResult<Boolean> {
        return try {
            val response = transactionApiService.deleteTransaction(id)
            if (response.isSuccessful && response.body()?.success == true) {
                _transactions.value = _transactions.value.filter { it.id != id }
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(response.code(), response.body()?.error ?: "Failed to delete transaction")
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, "Network error: ${e.localizedMessage}")
        }
    }
}
