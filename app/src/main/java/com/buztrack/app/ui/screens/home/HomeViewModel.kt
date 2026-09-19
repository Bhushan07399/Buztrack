package com.buztrack.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buztrack.app.data.models.BusinessProfile
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.data.models.TodaySummary
import com.buztrack.app.data.models.Transaction
import com.buztrack.app.data.repository.BuztrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: BuztrackRepository) : ViewModel() {

    val businessProfile: StateFlow<BusinessProfile> = repository.businessProfile
    val transactions: StateFlow<List<Transaction>> = repository.transactions

    private val _showAddIncome = MutableStateFlow(false)
    val showAddIncome = _showAddIncome.asStateFlow()

    private val _showAddExpense = MutableStateFlow(false)
    val showAddExpense = _showAddExpense.asStateFlow()

    private val _showGiveCredit = MutableStateFlow(false)
    val showGiveCredit = _showGiveCredit.asStateFlow()

    private val _showReceivePayment = MutableStateFlow(false)
    val showReceivePayment = _showReceivePayment.asStateFlow()

    val todaySummary: StateFlow<TodaySummary> = repository.transactions.combine(
        repository.customers
    ) { _, _ ->
        repository.getTodaySummary()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.getTodaySummary()
    )

    fun openAddIncome() { _showAddIncome.value = true }
    fun closeAddIncome() { _showAddIncome.value = false }

    fun openAddExpense() { _showAddExpense.value = true }
    fun closeAddExpense() { _showAddExpense.value = false }

    fun openGiveCredit() { _showGiveCredit.value = true }
    fun closeGiveCredit() { _showGiveCredit.value = false }

    fun openReceivePayment() { _showReceivePayment.value = true }
    fun closeReceivePayment() { _showReceivePayment.value = false }

    fun addIncome(amount: Double, category: String, paymentMethod: PaymentMethod, note: String) {
        viewModelScope.launch {
            repository.addIncome(
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                date = repository.getTodayDateString(),
                note = note
            )
            closeAddIncome()
        }
    }

    fun addExpense(amount: Double, category: String, paymentMethod: PaymentMethod, note: String) {
        viewModelScope.launch {
            repository.addExpense(
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                date = repository.getTodayDateString(),
                note = note
            )
            closeAddExpense()
        }
    }

    class Factory(private val repository: BuztrackRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
