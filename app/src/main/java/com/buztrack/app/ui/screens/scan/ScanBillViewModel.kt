package com.buztrack.app.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buztrack.app.data.models.Bill
import com.buztrack.app.data.models.BillItem
import com.buztrack.app.data.repository.BuztrackRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ScanState {
    object Idle : ScanState()
    data class Processing(val stepText: String, val progress: Float) : ScanState()
    data class Extracted(val bill: Bill) : ScanState()
    data class Success(val bill: Bill) : ScanState()
}

class ScanBillViewModel(private val repository: BuztrackRepository) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun startSimulatedScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Processing("Reading bill image...", 0.2f)
            delay(600)
            _scanState.value = ScanState.Processing("Detecting supplier name...", 0.4f)
            delay(600)
            _scanState.value = ScanState.Processing("Extracting invoice number & date...", 0.6f)
            delay(600)
            _scanState.value = ScanState.Processing("Calculating line items & GST...", 0.8f)
            delay(600)
            _scanState.value = ScanState.Processing("Finalizing extraction...", 1.0f)
            delay(400)

            // Extracted Bill Sample Data
            val today = repository.getTodayDateString()
            val extractedBill = Bill(
                id = UUID.randomUUID().toString(),
                supplierName = "ABC Traders",
                invoiceNumber = "INV-${(1000..9999).random()}",
                date = today,
                subtotal = 4000.0,
                gstAmount = 720.0,
                totalAmount = 4720.0,
                items = listOf(
                    BillItem(name = "Product A - Tea Powder 1kg", quantity = 10, unitPrice = 200.0, totalPrice = 2000.0),
                    BillItem(name = "Product B - Refined Oil 5L", quantity = 5, unitPrice = 400.0, totalPrice = 2000.0)
                ),
                verificationStatus = "Verified"
            )

            _scanState.value = ScanState.Extracted(extractedBill)
        }
    }

    fun confirmAndSaveBill(bill: Bill) {
        viewModelScope.launch {
            repository.saveBill(bill, createTransaction = true)
            _scanState.value = ScanState.Success(bill)
        }
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    class Factory(private val repository: BuztrackRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanBillViewModel(repository) as T
        }
    }
}
