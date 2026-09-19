package com.buztrack.app.data.repository

import com.buztrack.app.data.models.Bill
import com.buztrack.app.data.models.BusinessProfile
import com.buztrack.app.data.models.Customer
import com.buztrack.app.data.models.DailyClosing
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.data.models.Supplier
import com.buztrack.app.data.models.TodaySummary
import com.buztrack.app.data.models.Transaction
import kotlinx.coroutines.flow.StateFlow

interface BuztrackRepository {
    val transactions: StateFlow<List<Transaction>>
    val customers: StateFlow<List<Customer>>
    val suppliers: StateFlow<List<Supplier>>
    val bills: StateFlow<List<Bill>>
    val dailyClosings: StateFlow<List<DailyClosing>>
    val businessProfile: StateFlow<BusinessProfile>
    val openingCash: StateFlow<Double>

    fun getTodaySummary(): TodaySummary
    fun getTodayDateString(): String

    suspend fun addIncome(
        amount: Double,
        category: String,
        paymentMethod: PaymentMethod,
        date: String,
        note: String
    )

    suspend fun addExpense(
        amount: Double,
        category: String,
        paymentMethod: PaymentMethod,
        date: String,
        note: String,
        partyName: String? = null,
        billId: String? = null
    )

    suspend fun giveCustomerCredit(
        customerId: String,
        amount: Double,
        date: String,
        note: String
    )

    suspend fun receiveCustomerPayment(
        customerId: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        date: String,
        note: String
    )

    suspend fun addCustomer(name: String, phone: String)

    suspend fun addSupplierPurchase(
        supplierId: String,
        amount: Double,
        date: String,
        note: String,
        billId: String? = null
    )

    suspend fun recordSupplierPayment(
        supplierId: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        date: String,
        note: String
    )

    suspend fun addSupplier(name: String, phone: String)

    suspend fun saveBill(bill: Bill, createTransaction: Boolean = true)
    suspend fun deleteBill(billId: String)

    suspend fun deleteTransaction(transactionId: String)

    suspend fun closeDay(
        date: String,
        expectedCash: Double,
        actualCash: Double,
        notes: String
    ): DailyClosing

    suspend fun resetToDefaultDemoData()
}
