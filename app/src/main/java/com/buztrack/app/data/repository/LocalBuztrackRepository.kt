package com.buztrack.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.buztrack.app.data.models.Bill
import com.buztrack.app.data.models.BillItem
import com.buztrack.app.data.models.BusinessProfile
import com.buztrack.app.data.models.Categories
import com.buztrack.app.data.models.Customer
import com.buztrack.app.data.models.DailyClosing
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.data.models.Supplier
import com.buztrack.app.data.models.TodaySummary
import com.buztrack.app.data.models.Transaction
import com.buztrack.app.data.models.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalBuztrackRepository private constructor(context: Context) : BuztrackRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("buztrack_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    override val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    override val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    override val bills: StateFlow<List<Bill>> = _bills.asStateFlow()

    private val _dailyClosings = MutableStateFlow<List<DailyClosing>>(emptyList())
    override val dailyClosings: StateFlow<List<DailyClosing>> = _dailyClosings.asStateFlow()

    private val _businessProfile = MutableStateFlow(BusinessProfile())
    override val businessProfile: StateFlow<BusinessProfile> = _businessProfile.asStateFlow()

    private val _openingCash = MutableStateFlow(10000.0)
    override val openingCash: StateFlow<Double> = _openingCash.asStateFlow()

    init {
        loadDataFromStorage()
    }

    override fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadDataFromStorage() {
        val isInitialized = prefs.getBoolean("is_initialized", false)
        if (!isInitialized) {
            seedDefaultDemoData()
            return
        }

        try {
            val txJson = prefs.getString("transactions", null)
            if (txJson != null) {
                val type = object : TypeToken<List<Transaction>>() {}.type
                _transactions.value = gson.fromJson(txJson, type) ?: emptyList()
            }

            val custJson = prefs.getString("customers", null)
            if (custJson != null) {
                val type = object : TypeToken<List<Customer>>() {}.type
                _customers.value = gson.fromJson(custJson, type) ?: emptyList()
            }

            val suppJson = prefs.getString("suppliers", null)
            if (suppJson != null) {
                val type = object : TypeToken<List<Supplier>>() {}.type
                _suppliers.value = gson.fromJson(suppJson, type) ?: emptyList()
            }

            val billsJson = prefs.getString("bills", null)
            if (billsJson != null) {
                val type = object : TypeToken<List<Bill>>() {}.type
                _bills.value = gson.fromJson(billsJson, type) ?: emptyList()
            }

            val closingsJson = prefs.getString("closings", null)
            if (closingsJson != null) {
                val type = object : TypeToken<List<DailyClosing>>() {}.type
                _dailyClosings.value = gson.fromJson(closingsJson, type) ?: emptyList()
            }

            val profileJson = prefs.getString("business_profile", null)
            if (profileJson != null) {
                _businessProfile.value = gson.fromJson(profileJson, BusinessProfile::class.java)
            }

            _openingCash.value = prefs.getFloat("opening_cash", 0.0f).toDouble()
        } catch (e: Exception) {
            clearAllBusinessData()
        }
    }

    private fun persistData() {
        prefs.edit().apply {
            putString("transactions", gson.toJson(_transactions.value))
            putString("customers", gson.toJson(_customers.value))
            putString("suppliers", gson.toJson(_suppliers.value))
            putString("bills", gson.toJson(_bills.value))
            putString("closings", gson.toJson(_dailyClosings.value))
            putString("business_profile", gson.toJson(_businessProfile.value))
            putFloat("opening_cash", _openingCash.value.toFloat())
            putBoolean("is_initialized", true)
            apply()
        }
    }

    override suspend fun resetToDefaultDemoData() {
        clearAllBusinessData()
    }

    private fun clearAllBusinessData() {
        _transactions.value = emptyList()
        _customers.value = emptyList()
        _suppliers.value = emptyList()
        _bills.value = emptyList()
        _dailyClosings.value = emptyList()
        _businessProfile.value = BusinessProfile(scannedBillsCount = 0)
        _openingCash.value = 0.0

        persistData()
    }

    private fun seedDefaultDemoData() {
        val today = getTodayDateString()
        
        // Initial Customers
        val cust1 = Customer(id = "c1", name = "Rahul Patil", phone = "+91 98234 11223", totalCreditGiven = 3500.0, totalPaymentsReceived = 2000.0, pendingAmount = 1500.0, lastTransactionDate = today)
        val cust2 = Customer(id = "c2", name = "Amit Traders", phone = "+91 99887 76655", totalCreditGiven = 12000.0, totalPaymentsReceived = 8000.0, pendingAmount = 4000.0, lastTransactionDate = today)
        val cust3 = Customer(id = "c3", name = "Sneha Verma", phone = "+91 94112 33445", totalCreditGiven = 1200.0, totalPaymentsReceived = 500.0, pendingAmount = 700.0, lastTransactionDate = today)
        val defaultCustomers = listOf(cust1, cust2, cust3)

        // Initial Suppliers
        val supp1 = Supplier(id = "s1", name = "ABC Traders", phone = "+91 98111 22334", totalPurchases = 25800.0, totalPaid = 10000.0, pendingAmount = 15800.0, lastPurchaseDate = today)
        val supp2 = Supplier(id = "s2", name = "XYZ Wholesale", phone = "+91 97222 33445", totalPurchases = 18500.0, totalPaid = 18500.0, pendingAmount = 0.0, lastPurchaseDate = "2026-09-15")
        val supp3 = Supplier(id = "s3", name = "PQR Distributors", phone = "+91 96333 44556", totalPurchases = 9200.0, totalPaid = 4200.0, pendingAmount = 5000.0, lastPurchaseDate = "2026-09-17")
        val defaultSuppliers = listOf(supp1, supp2, supp3)

        // Initial Bills
        val bill1 = Bill(
            id = "b1",
            supplierId = "s1",
            supplierName = "ABC Traders",
            invoiceNumber = "INV-2458",
            date = today,
            subtotal = 4000.0,
            gstAmount = 720.0,
            totalAmount = 4720.0,
            items = listOf(
                BillItem(name = "Premium Tea Powder", quantity = 10, unitPrice = 200.0, totalPrice = 2000.0),
                BillItem(name = "Refined Cooking Oil 5L", quantity = 5, unitPrice = 400.0, totalPrice = 2000.0)
            ),
            verificationStatus = "Verified"
        )
        val defaultBills = listOf(bill1)

        // Initial Today Transactions
        val defaultTxList = mutableListOf(
            Transaction(type = TransactionType.INCOME, amount = 6300.0, category = "Sales", paymentMethod = PaymentMethod.CASH, date = today, time = "09:15 AM", note = "Morning counter cash sales"),
            Transaction(type = TransactionType.INCOME, amount = 5200.0, category = "Sales", paymentMethod = PaymentMethod.UPI, date = today, time = "11:40 AM", note = "UPI QR payments"),
            Transaction(type = TransactionType.INCOME, amount = 1000.0, category = "Sales", paymentMethod = PaymentMethod.CARD, date = today, time = "01:10 PM", note = "Card swipe sale"),
            Transaction(type = TransactionType.EXPENSE, amount = 2000.0, category = "Purchase", paymentMethod = PaymentMethod.CASH, date = today, time = "10:00 AM", note = "Dairy products cash stock purchase"),
            Transaction(type = TransactionType.EXPENSE, amount = 1500.0, category = "Transport", paymentMethod = PaymentMethod.UPI, date = today, time = "02:30 PM", note = "Goods auto freight delivery charge"),
            Transaction(type = TransactionType.EXPENSE, amount = 700.0, category = "Electricity", paymentMethod = PaymentMethod.UPI, date = today, time = "03:15 PM", note = "Shop bill partial payment"),
            Transaction(type = TransactionType.CUSTOMER_CREDIT, amount = 1500.0, category = "Customer Credit", paymentMethod = PaymentMethod.CASH, date = today, time = "04:00 PM", note = "Monthly groceries on udhari", partyId = cust1.id, partyName = cust1.name),
            Transaction(type = TransactionType.CUSTOMER_PAYMENT, amount = 1000.0, category = "Customer Payment", paymentMethod = PaymentMethod.UPI, date = today, time = "05:20 PM", note = "Old credit cleared", partyId = cust2.id, partyName = cust2.name),
            Transaction(type = TransactionType.SUPPLIER_PAYMENT, amount = 3000.0, category = "Supplier Payment", paymentMethod = PaymentMethod.CASH, date = today, time = "06:00 PM", note = "Partial payment to ABC Traders", partyId = supp1.id, partyName = supp1.name)
        )

        _customers.value = defaultCustomers
        _suppliers.value = defaultSuppliers
        _bills.value = defaultBills
        _transactions.value = defaultTxList
        _dailyClosings.value = emptyList()
        _businessProfile.value = BusinessProfile()
        _openingCash.value = 10000.0

        persistData()
    }

    override fun getTodaySummary(): TodaySummary {
        val today = getTodayDateString()
        val todayTxs = _transactions.value.filter { it.date == today }

        var incomeTotal = 0.0
        var expenseTotal = 0.0

        var cashInc = 0.0
        var upiInc = 0.0
        var cardInc = 0.0
        var bankInc = 0.0

        var cashExp = 0.0
        var creditGivenToday = 0.0

        for (tx in todayTxs) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    incomeTotal += tx.amount
                    when (tx.paymentMethod) {
                        PaymentMethod.CASH -> cashInc += tx.amount
                        PaymentMethod.UPI -> upiInc += tx.amount
                        PaymentMethod.CARD -> cardInc += tx.amount
                        PaymentMethod.BANK -> bankInc += tx.amount
                    }
                }
                TransactionType.EXPENSE -> {
                    expenseTotal += tx.amount
                    if (tx.paymentMethod == PaymentMethod.CASH) {
                        cashExp += tx.amount
                    }
                }
                TransactionType.CUSTOMER_CREDIT -> {
                    creditGivenToday += tx.amount
                }
                TransactionType.CUSTOMER_PAYMENT -> {
                    // Payment received from customer adds to income/cash flow
                    incomeTotal += tx.amount
                    when (tx.paymentMethod) {
                        PaymentMethod.CASH -> cashInc += tx.amount
                        PaymentMethod.UPI -> upiInc += tx.amount
                        PaymentMethod.CARD -> cardInc += tx.amount
                        PaymentMethod.BANK -> bankInc += tx.amount
                    }
                }
                TransactionType.SUPPLIER_PAYMENT -> {
                    expenseTotal += tx.amount
                    if (tx.paymentMethod == PaymentMethod.CASH) {
                        cashExp += tx.amount
                    }
                }
                TransactionType.SUPPLIER_PURCHASE -> {
                    // Credit purchase recorded as expense if accounted
                }
            }
        }

        val pendingCustTotal = _customers.value.sumOf { it.pendingAmount }
        val pendingSuppTotal = _suppliers.value.sumOf { it.pendingAmount }

        return TodaySummary(
            date = today,
            totalIncome = incomeTotal,
            totalExpenses = expenseTotal,
            netResult = incomeTotal - expenseTotal,
            cashIncome = cashInc,
            upiIncome = upiInc,
            cardIncome = cardInc,
            bankIncome = bankInc,
            cashExpense = cashExp,
            creditGivenToday = creditGivenToday,
            pendingCustomerCollectionTotal = pendingCustTotal,
            pendingSupplierTotal = pendingSuppTotal
        )
    }

    override suspend fun addIncome(
        amount: Double,
        category: String,
        paymentMethod: PaymentMethod,
        date: String,
        note: String
    ) {
        val tx = Transaction(
            type = TransactionType.INCOME,
            amount = amount,
            category = category,
            paymentMethod = paymentMethod,
            date = date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            note = note
        )
        _transactions.value = listOf(tx) + _transactions.value
        persistData()
    }

    override suspend fun addExpense(
        amount: Double,
        category: String,
        paymentMethod: PaymentMethod,
        date: String,
        note: String,
        partyName: String?,
        billId: String?
    ) {
        val tx = Transaction(
            type = TransactionType.EXPENSE,
            amount = amount,
            category = category,
            paymentMethod = paymentMethod,
            date = date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            note = note,
            partyName = partyName,
            billId = billId
        )
        _transactions.value = listOf(tx) + _transactions.value
        persistData()
    }

    override suspend fun giveCustomerCredit(
        customerId: String,
        amount: Double,
        date: String,
        note: String
    ) {
        val cust = _customers.value.find { it.id == customerId } ?: return
        val updatedCust = cust.copy(
            totalCreditGiven = cust.totalCreditGiven + amount,
            pendingAmount = cust.pendingAmount + amount,
            lastTransactionDate = date
        )
        _customers.value = _customers.value.map { if (it.id == customerId) updatedCust else it }

        val tx = Transaction(
            type = TransactionType.CUSTOMER_CREDIT,
            amount = amount,
            category = "Customer Credit",
            paymentMethod = PaymentMethod.CASH,
            date = date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            note = note,
            partyId = customerId,
            partyName = cust.name
        )
        _transactions.value = listOf(tx) + _transactions.value
        persistData()
    }

    override suspend fun receiveCustomerPayment(
        customerId: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        date: String,
        note: String
    ) {
        val cust = _customers.value.find { it.id == customerId } ?: return
        val updatedCust = cust.copy(
            totalPaymentsReceived = cust.totalPaymentsReceived + amount,
            pendingAmount = (cust.pendingAmount - amount).coerceAtLeast(0.0),
            lastTransactionDate = date
        )
        _customers.value = _customers.value.map { if (it.id == customerId) updatedCust else it }

        val tx = Transaction(
            type = TransactionType.CUSTOMER_PAYMENT,
            amount = amount,
            category = "Credit Collection",
            paymentMethod = paymentMethod,
            date = date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            note = note,
            partyId = customerId,
            partyName = cust.name
        )
        _transactions.value = listOf(tx) + _transactions.value
        persistData()
    }

    override suspend fun addCustomer(name: String, phone: String) {
        val newCust = Customer(
            id = UUID.randomUUID().toString(),
            name = name,
            phone = phone,
            lastTransactionDate = getTodayDateString()
        )
        _customers.value = _customers.value + newCust
        persistData()
    }

    override suspend fun addSupplierPurchase(
        supplierId: String,
        amount: Double,
        date: String,
        note: String,
        billId: String?
    ) {
        val supp = _suppliers.value.find { it.id == supplierId } ?: return
        val updatedSupp = supp.copy(
            totalPurchases = supp.totalPurchases + amount,
            pendingAmount = supp.pendingAmount + amount,
            lastPurchaseDate = date
        )
        _suppliers.value = _suppliers.value.map { if (it.id == supplierId) updatedSupp else it }

        val tx = Transaction(
            type = TransactionType.SUPPLIER_PURCHASE,
            amount = amount,
            category = "Purchase",
            paymentMethod = PaymentMethod.CASH,
            date = date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            note = note,
            partyId = supplierId,
            partyName = supp.name,
            billId = billId
        )
        _transactions.value = listOf(tx) + _transactions.value
        persistData()
    }

    override suspend fun recordSupplierPayment(
        supplierId: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        date: String,
        note: String
    ) {
        val supp = _suppliers.value.find { it.id == supplierId } ?: return
        val updatedSupp = supp.copy(
            totalPaid = supp.totalPaid + amount,
            pendingAmount = (supp.pendingAmount - amount).coerceAtLeast(0.0),
            lastPurchaseDate = date
        )
        _suppliers.value = _suppliers.value.map { if (it.id == supplierId) updatedSupp else it }

        val tx = Transaction(
            type = TransactionType.SUPPLIER_PAYMENT,
            amount = amount,
            category = "Supplier Payment",
            paymentMethod = paymentMethod,
            date = date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            note = note,
            partyId = supplierId,
            partyName = supp.name
        )
        _transactions.value = listOf(tx) + _transactions.value
        persistData()
    }

    override suspend fun addSupplier(name: String, phone: String) {
        val newSupp = Supplier(
            id = UUID.randomUUID().toString(),
            name = name,
            phone = phone,
            lastPurchaseDate = getTodayDateString()
        )
        _suppliers.value = _suppliers.value + newSupp
        persistData()
    }

    override suspend fun saveBill(bill: Bill, createTransaction: Boolean) {
        // Check duplicate invoice number
        val existingBill = _bills.value.find { it.id == bill.id }
        val duplicateFound = _bills.value.any { it.invoiceNumber.equals(bill.invoiceNumber, ignoreCase = true) && it.id != bill.id }
        val finalBill = bill.copy(isDuplicate = duplicateFound)

        if (existingBill != null) {
            _bills.value = _bills.value.map { if (it.id == bill.id) finalBill else it }
        } else {
            _bills.value = listOf(finalBill) + _bills.value
            // Update scanned bills count
            val currentProf = _businessProfile.value
            _businessProfile.value = currentProf.copy(scannedBillsCount = currentProf.scannedBillsCount + 1)
        }

        // Link with supplier if found or create supplier
        var supp = _suppliers.value.find { it.name.equals(bill.supplierName, ignoreCase = true) }
        if (supp == null && bill.supplierName.isNotBlank()) {
            val newSupp = Supplier(
                name = bill.supplierName,
                phone = "+91 98000 00000",
                totalPurchases = bill.totalAmount,
                pendingAmount = bill.totalAmount,
                lastPurchaseDate = bill.date
            )
            _suppliers.value = _suppliers.value + newSupp
            supp = newSupp
        } else if (supp != null) {
            val updatedSupp = supp.copy(
                totalPurchases = supp.totalPurchases + bill.totalAmount,
                pendingAmount = supp.pendingAmount + bill.totalAmount,
                lastPurchaseDate = bill.date
            )
            _suppliers.value = _suppliers.value.map { if (it.id == supp?.id) updatedSupp else it }
        }

        if (createTransaction) {
            addExpense(
                amount = bill.totalAmount,
                category = "Purchase",
                paymentMethod = PaymentMethod.CASH,
                date = bill.date,
                note = "Bill Scanned: ${bill.invoiceNumber}",
                partyName = bill.supplierName,
                billId = bill.id
            )
        }
        persistData()
    }

    override suspend fun deleteBill(billId: String) {
        _bills.value = _bills.value.filter { it.id != billId }
        _transactions.value = _transactions.value.filter { it.billId != billId }
        persistData()
    }

    override suspend fun deleteTransaction(transactionId: String) {
        _transactions.value = _transactions.value.filter { it.id != transactionId }
        persistData()
    }

    override suspend fun closeDay(
        date: String,
        expectedCash: Double,
        actualCash: Double,
        notes: String
    ): DailyClosing {
        val summary = getTodaySummary()
        val diff = actualCash - expectedCash
        val closing = DailyClosing(
            date = date,
            expectedIncome = summary.totalIncome,
            expectedExpense = summary.totalExpenses,
            expectedCash = expectedCash,
            actualCash = actualCash,
            difference = diff,
            notes = notes,
            closedAt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date()),
            isClosed = true
        )
        _dailyClosings.value = listOf(closing) + _dailyClosings.value.filter { it.date != date }
        persistData()
        return closing
    }

    companion object {
        @Volatile
        private var INSTANCE: LocalBuztrackRepository? = null

        fun getInstance(context: Context): LocalBuztrackRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = LocalBuztrackRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
