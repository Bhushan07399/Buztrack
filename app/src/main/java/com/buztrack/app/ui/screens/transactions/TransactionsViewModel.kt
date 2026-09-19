package com.buztrack.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buztrack.app.data.models.Transaction
import com.buztrack.app.data.models.TransactionType
import com.buztrack.app.data.repository.BuztrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TransactionFilter(val displayName: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense"),
    CREDIT("Credit Given"),
    COLLECTION("Credit Collection"),
    SUPPLIER("Supplier Dues")
}

class TransactionsViewModel(private val repository: BuztrackRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        repository.transactions,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        list.filter { tx ->
            val matchesFilter = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.INCOME -> tx.type == TransactionType.INCOME
                TransactionFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                TransactionFilter.CREDIT -> tx.type == TransactionType.CUSTOMER_CREDIT
                TransactionFilter.COLLECTION -> tx.type == TransactionType.CUSTOMER_PAYMENT
                TransactionFilter.SUPPLIER -> tx.type == TransactionType.SUPPLIER_PURCHASE || tx.type == TransactionType.SUPPLIER_PAYMENT
            }

            val matchesQuery = query.isBlank() ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    (tx.partyName?.contains(query, ignoreCase = true) == true) ||
                    tx.paymentMethod.displayName.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    class Factory(private val repository: BuztrackRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(repository) as T
        }
    }
}
