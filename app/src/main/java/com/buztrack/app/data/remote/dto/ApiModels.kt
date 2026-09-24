package com.buztrack.app.data.remote.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?,
    val details: Any? = null,
    val message: String? = null
)

data class RegisterRequestDto(
    val fullName: String,
    val phone: String,
    val password: String,
    val businessName: String,
    val businessType: String = "RETAIL",
    val email: String? = null
)

data class LoginRequestDto(
    val phone: String,
    val password: String
)

data class UpdateBusinessProfileDto(
    val businessName: String,
    val owner: String? = null,
    val mobile: String? = null,
    val address: String? = null,
    val GSTIN: String? = null, // Optional GSTIN
    val logo: String? = null,
    val businessType: String? = null
)

data class SwitchBusinessRequestDto(
    val businessId: String
)

data class UserDto(
    val id: String,
    val fullName: String? = null,
    val phone: String,
    val email: String? = null
)

data class BusinessDto(
    val id: String,
    val name: String,
    val businessName: String? = null,
    val ownerName: String? = null,
    val owner: String? = null,
    val phone: String? = null,
    val mobile: String? = null,
    val address: String? = null,
    val gstin: String? = null,
    val GSTIN: String? = null,
    val role: String? = "OWNER",
    val businessType: String? = "RETAIL",
    val logo: String? = null,
    val currency: String? = "INR"
)

data class AuthResponseDto(
    val token: String,
    val user: UserDto,
    val business: BusinessDto
)

data class TransactionDto(
    val id: String,
    val type: String,
    val amount: Double,
    val categoryName: String,
    val paymentMethod: String,
    val transactionDate: String,
    val transactionTime: String? = null,
    val note: String? = null,
    val customerId: String? = null,
    val supplierId: String? = null,
    val billId: String? = null
)

data class CustomerDto(
    val id: String,
    val name: String,
    val phone: String,
    val address: String? = null,
    val gstin: String? = null,
    val totalCreditGiven: Double = 0.0,
    val totalPaymentsReceived: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val lastTransactionAt: String? = null
)

data class SupplierDto(
    val id: String,
    val name: String,
    val phone: String,
    val address: String? = null,
    val gstin: String? = null,
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val lastPurchaseAt: String? = null
)

data class BillItemDto(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double
)

data class BillDto(
    val id: String,
    val supplierName: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val subtotal: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val verificationStatus: String,
    val isDuplicate: Boolean,
    val items: List<BillItemDto> = emptyList()
)

data class DailyClosingDto(
    val id: String,
    val date: String? = null,
    val closingDate: String? = null,
    val expectedIncome: Double = 0.0,
    val expectedExpense: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val difference: Double = 0.0,
    val notes: String? = null,
    val isClosed: Boolean = true
)

data class CashbookSummaryDto(
    val date: String,
    val openingCash: Double,
    val cashIn: Double,
    val cashOut: Double,
    val expectedCash: Double
)

data class SetOpeningCashRequestDto(
    val date: String,
    val openingCash: Double
)

data class CloseDayRequestDto(
    val date: String,
    val actualCash: Double,
    val notes: String? = null
)

data class SubscriptionLimitsDto(
    val maxStaffMembers: Int,
    val maxScannedBillsPerMonth: Int,
    val scannedBillsUsedThisMonth: Int
)

data class SubscriptionDto(
    val businessId: String,
    val planCode: String,
    val planName: String,
    val status: String,
    val limits: SubscriptionLimitsDto,
    val startDate: String,
    val endDate: String? = null
)
