package com.buztrack.app.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buztrack.app.data.models.Bill
import com.buztrack.app.data.models.BillItem
import com.buztrack.app.data.remote.network.NetworkResult
import com.buztrack.app.data.repository.BuztrackRepository
import com.buztrack.app.domain.repository.BillRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ScanState {
    data object Idle : ScanState()
    data class Processing(val stepText: String, val progress: Float) : ScanState()
    data class Extracted(val bill: Bill) : ScanState()
    data class Error(val message: String) : ScanState()
    data class DuplicateWarning(val bill: Bill, val existingBillId: String) : ScanState()
    data class Success(val bill: Bill) : ScanState()
}

class ScanBillViewModel(
    private val repository: BuztrackRepository,
    private val billRepository: BillRepository? = null
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun processRealBillOcr(imageBytes: ByteArray) {
        viewModelScope.launch {
            _scanState.value = ScanState.Processing("Uploading bill image...", 0.2f)
            delay(300)
            _scanState.value = ScanState.Processing("Extracting text with AI OCR...", 0.5f)

            if (billRepository != null) {
                when (val result = billRepository.scanBillOcr(imageBytes)) {
                    is NetworkResult.Success -> {
                        val dto = result.data
                        val today = repository.getTodayDateString()
                        val extractedBill = Bill(
                            id = UUID.randomUUID().toString(),
                            supplierName = dto.supplierName,
                            invoiceNumber = dto.invoiceNumber,
                            date = dto.invoiceDate.ifBlank { today },
                            subtotal = dto.subtotal,
                            gstAmount = dto.taxAmount,
                            totalAmount = dto.totalAmount,
                            items = dto.items.map {
                                BillItem(name = it.name, quantity = it.quantity.toInt(), unitPrice = it.unitPrice, totalPrice = it.totalPrice)
                            },
                            verificationStatus = "Pending Verification"
                        )
                        _scanState.value = ScanState.Extracted(extractedBill)
                    }
                    is NetworkResult.Error -> {
                        _scanState.value = ScanState.Error(result.message)
                    }
                    is NetworkResult.Loading -> {
                        _scanState.value = ScanState.Processing("Processing OCR...", 0.8f)
                    }
                }
            } else {
                startSimulatedScan()
            }
        }
    }

    fun startSimulatedScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Processing("Reading bill image...", 0.2f)
            delay(400)
            _scanState.value = ScanState.Processing("Detecting supplier name...", 0.4f)
            delay(400)
            _scanState.value = ScanState.Processing("Extracting invoice number & date...", 0.6f)
            delay(400)
            _scanState.value = ScanState.Processing("Calculating line items & GST...", 0.8f)
            delay(400)
            _scanState.value = ScanState.Processing("Finalizing extraction...", 1.0f)
            delay(300)

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

    class Factory(
        private val repository: BuztrackRepository,
        private val billRepository: BillRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanBillViewModel(repository, billRepository) as T
        }
    }
}
