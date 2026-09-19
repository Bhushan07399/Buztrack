package com.buztrack.app.ui.screens.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.ui.components.SectionHeader
import com.buztrack.app.ui.components.TransactionRow
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    viewModel: SuppliersViewModel,
    supplierId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    viewModel.selectSupplier(supplierId)

    val supplier by viewModel.selectedSupplier.collectAsState()
    val transactions by viewModel.supplierTransactions.collectAsState()

    var showAddPurchaseSheet by remember { mutableStateOf(false) }
    var showRecordPaymentSheet by remember { mutableStateOf(false) }

    val currentSupp = supplier ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP BAR
        Surface(color = DarkSlate, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = currentSupp.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = currentSupp.phone, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SUMMARY CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "PENDING SUPPLIER PAYABLE", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatInr(currentSupp.pendingAmount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Total Purchases", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(text = CurrencyUtils.formatInr(currentSupp.totalPurchases), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Total Paid", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(text = CurrencyUtils.formatInr(currentSupp.totalPaid), style = MaterialTheme.typography.titleMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ACTIONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showAddPurchaseSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Purchase", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showRecordPaymentSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Record Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SectionHeader(title = "Purchase & Payment History")
            }

            items(transactions, key = { it.id }) { tx ->
                TransactionRow(transaction = tx)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddPurchaseSheet) {
        AddSupplierPurchaseSheet(
            supplierName = currentSupp.name,
            onDismiss = { showAddPurchaseSheet = false },
            onSave = { amount, note ->
                viewModel.addPurchase(currentSupp.id, amount, note)
                showAddPurchaseSheet = false
            }
        )
    }

    if (showRecordPaymentSheet) {
        RecordSupplierPaymentSheet(
            supplierName = currentSupp.name,
            onDismiss = { showRecordPaymentSheet = false },
            onSave = { amount, method, note ->
                viewModel.recordPayment(currentSupp.id, amount, method, note)
                showRecordPaymentSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSupplierPurchaseSheet(
    supplierName: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Add Purchase Bill from $supplierName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Purchase Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Bill / Goods Note") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onSave(amt, note.ifBlank { "Wholesale Stock Purchase" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Save Purchase Record", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordSupplierPaymentSheet(
    supplierName: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, method: PaymentMethod, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Record Payment to $supplierName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Payment Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Payment Mode", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.values().forEach { method ->
                    val isSel = selectedMethod == method
                    Button(
                        onClick = { selectedMethod = method },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSel) PrimaryGreen else Color(0xFFF1F5F9)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = method.displayName, color = if (isSel) Color.White else TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onSave(amt, selectedMethod, note.ifBlank { "Supplier Payment" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Confirm Payment", fontWeight = FontWeight.Bold)
            }
        }
    }
}
