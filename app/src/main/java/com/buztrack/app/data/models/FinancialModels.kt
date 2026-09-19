package com.buztrack.app.data.models

import java.util.UUID

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card"),
    BANK("Bank")
}

enum class TransactionType(val displayName: String, val isPositive: Boolean) {
    INCOME("Income", true),
    EXPENSE("Expense", false),
    CUSTOMER_CREDIT("Credit Given", false),
    CUSTOMER_PAYMENT("Credit Collection", true),
    SUPPLIER_PURCHASE("Supplier Purchase", false),
    SUPPLIER_PAYMENT("Supplier Payment", false)
}

object Categories {
    val INCOME_CATEGORIES = listOf(
        "Sales",
        "Service",
        "Other Income"
    )

    val EXPENSE_CATEGORIES = listOf(
        "Purchase",
        "Transport",
        "Electricity",
        "Rent",
        "Salary",
        "Marketing",
        "Packaging",
        "Food",
        "Maintenance",
        "Other"
    )
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val paymentMethod: PaymentMethod,
    val date: String, // YYYY-MM-DD
    val time: String = "10:30 AM",
    val note: String = "",
    val partyId: String? = null,
    val partyName: String? = null,
    val billId: String? = null
)

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val totalCreditGiven: Double = 0.0,
    val totalPaymentsReceived: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val lastTransactionDate: String = ""
)

data class Supplier(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val lastPurchaseDate: String = ""
)

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double = quantity * unitPrice
)

data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val supplierId: String? = null,
    val supplierName: String,
    val invoiceNumber: String,
    val date: String,
    val subtotal: Double,
    val gstAmount: Double,
    val totalAmount: Double,
    val items: List<BillItem> = emptyList(),
    val verificationStatus: String = "Verified", // "Verified", "Pending Verification"
    val isDuplicate: Boolean = false
)

data class DailyClosing(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val expectedIncome: Double,
    val expectedExpense: Double,
    val expectedCash: Double,
    val actualCash: Double,
    val difference: Double,
    val notes: String = "",
    val closedAt: String = "",
    val isClosed: Boolean = true
)

data class BusinessProfile(
    val businessName: String = "Sharma General Store",
    val ownerName: String = "Rahul Sharma",
    val phone: String = "+91 98765 43210",
    val address: String = "Shop No. 12, Main Market, Jaipur",
    val gstin: String = "DEMO-SAMPLE",
    val planName: String = "FREE",
    val scannedBillsCount: Int = 4,
    val scannedBillsLimit: Int = 10
)

data class TodaySummary(
    val date: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netResult: Double,
    val cashIncome: Double,
    val upiIncome: Double,
    val cardIncome: Double,
    val bankIncome: Double,
    val cashExpense: Double,
    val creditGivenToday: Double,
    val pendingCustomerCollectionTotal: Double,
    val pendingSupplierTotal: Double
)
