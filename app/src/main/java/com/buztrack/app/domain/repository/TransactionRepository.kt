package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.TransactionDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface TransactionRepository {
    val transactions: StateFlow<List<TransactionDto>>
    suspend fun fetchTransactions(): NetworkResult<List<TransactionDto>>
    suspend fun createTransaction(transaction: TransactionDto): NetworkResult<TransactionDto>
    suspend fun deleteTransaction(id: String): NetworkResult<Boolean>
}
