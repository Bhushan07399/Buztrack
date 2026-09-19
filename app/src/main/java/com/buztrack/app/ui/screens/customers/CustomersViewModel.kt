package com.buztrack.app.ui.screens.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buztrack.app.data.models.Customer
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.data.models.Transaction
import com.buztrack.app.data.repository.BuztrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomersViewModel(private val repository: BuztrackRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId = _selectedCustomerId.asStateFlow()

    val filteredCustomers: StateFlow<List<Customer>> = combine(
        repository.customers,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedCustomer: StateFlow<Customer?> = combine(
        repository.customers,
        _selectedCustomerId
    ) { list, id ->
        list.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val customerTransactions: StateFlow<List<Transaction>> = combine(
        repository.transactions,
        _selectedCustomerId
    ) { list, id ->
        if (id == null) emptyList() else list.filter { it.partyId == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectCustomer(customerId: String?) {
        _selectedCustomerId.value = customerId
    }

    fun addCustomer(name: String, phone: String) {
        viewModelScope.launch {
            repository.addCustomer(name, phone)
        }
    }

    fun giveCredit(customerId: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.giveCustomerCredit(
                customerId = customerId,
                amount = amount,
                date = repository.getTodayDateString(),
                note = note
            )
        }
    }

    fun receivePayment(customerId: String, amount: Double, method: PaymentMethod, note: String) {
        viewModelScope.launch {
            repository.receiveCustomerPayment(
                customerId = customerId,
                amount = amount,
                paymentMethod = method,
                date = repository.getTodayDateString(),
                note = note
            )
        }
    }

    class Factory(private val repository: BuztrackRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomersViewModel(repository) as T
        }
    }
}
