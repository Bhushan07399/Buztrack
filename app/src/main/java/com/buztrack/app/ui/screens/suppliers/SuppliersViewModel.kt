package com.buztrack.app.ui.screens.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.data.models.Supplier
import com.buztrack.app.data.models.Transaction
import com.buztrack.app.data.repository.BuztrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SuppliersViewModel(private val repository: BuztrackRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<String?>(null)
    val selectedSupplierId = _selectedSupplierId.asStateFlow()

    val filteredSuppliers: StateFlow<List<Supplier>> = combine(
        repository.suppliers,
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

    val selectedSupplier: StateFlow<Supplier?> = combine(
        repository.suppliers,
        _selectedSupplierId
    ) { list, id ->
        list.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val supplierTransactions: StateFlow<List<Transaction>> = combine(
        repository.transactions,
        _selectedSupplierId
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

    fun selectSupplier(supplierId: String?) {
        _selectedSupplierId.value = supplierId
    }

    fun addSupplier(name: String, phone: String) {
        viewModelScope.launch {
            repository.addSupplier(name, phone)
        }
    }

    fun addPurchase(supplierId: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.addSupplierPurchase(
                supplierId = supplierId,
                amount = amount,
                date = repository.getTodayDateString(),
                note = note
            )
        }
    }

    fun recordPayment(supplierId: String, amount: Double, method: PaymentMethod, note: String) {
        viewModelScope.launch {
            repository.recordSupplierPayment(
                supplierId = supplierId,
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
            return SuppliersViewModel(repository) as T
        }
    }
}
