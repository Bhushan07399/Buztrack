package com.buztrack.app.ui.screens.customers

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
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
import com.buztrack.app.ui.components.ExportDialogSheet
import com.buztrack.app.ui.components.SectionHeader
import com.buztrack.app.ui.components.TransactionRow
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PendingAmber
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: CustomersViewModel,
    customerId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    viewModel.selectCustomer(customerId)

    val customer by viewModel.selectedCustomer.collectAsState()
    val transactions by viewModel.customerTransactions.collectAsState()

    var showGiveCreditSheet by remember { mutableStateOf(false) }
    var showReceivePaymentSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val currentCust = customer ?: return

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = currentCust.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = currentCust.phone, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                        }
                    }
                }
                Button(
                    onClick = { showExportSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Export", fontWeight = FontWeight.Bold)
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
                        Text(text = "PENDING UDHAARI BALANCE", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatInr(currentCust.pendingAmount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = PendingAmber
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Total Credit Given", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(text = CurrencyUtils.formatInr(currentCust.totalCreditGiven), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Payments Received", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(text = CurrencyUtils.formatInr(currentCust.totalPaymentsReceived), style = MaterialTheme.typography.titleMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
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
                        onClick = { showGiveCreditSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PendingAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Give Credit", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showReceivePaymentSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Receive Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SectionHeader(title = "Transaction History")
            }

            items(transactions, key = { it.id }) { tx ->
                TransactionRow(transaction = tx)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showGiveCreditSheet) {
        GiveCreditSheet(
            customerName = currentCust.name,
            onDismiss = { showGiveCreditSheet = false },
            onSave = { amount, note ->
                viewModel.giveCredit(currentCust.id, amount, note)
                showGiveCreditSheet = false
            }
        )
    }

    if (showReceivePaymentSheet) {
        ReceivePaymentSheet(
            customerName = currentCust.name,
            onDismiss = { showReceivePaymentSheet = false },
            onSave = { amount, method, note ->
                viewModel.receivePayment(currentCust.id, amount, method, note)
                showReceivePaymentSheet = false
            }
        )
    }

    if (showExportSheet) {
        ExportDialogSheet(
            title = "Export ${currentCust.name} Statement",
            onDismiss = { showExportSheet = false },
            onExportFormat = { format ->
                showExportSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GiveCreditSheet(
    customerName: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Give Credit to $customerName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Credit Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Items Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onSave(amt, note.ifBlank { "Credit Given" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PendingAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Save Credit Record", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceivePaymentSheet(
    customerName: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, method: PaymentMethod, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Receive Payment from $customerName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount Received (₹)") },
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
                        onSave(amt, selectedMethod, note.ifBlank { "Credit Payment Collection" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Record Payment", fontWeight = FontWeight.Bold)
            }
        }
    }
}
