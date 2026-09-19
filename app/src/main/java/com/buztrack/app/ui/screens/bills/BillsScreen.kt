package com.buztrack.app.ui.screens.bills

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.models.Bill
import com.buztrack.app.data.repository.BuztrackRepository
import com.buztrack.app.ui.components.EmptyState
import com.buztrack.app.ui.components.VerificationBadge
import com.buztrack.app.ui.theme.CardBorder
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PendingAmber
import com.buztrack.app.ui.theme.PendingAmberBg
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.SecondaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.utils.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    repository: BuztrackRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bills by repository.bills.collectAsState()
    var selectedBill by remember { mutableStateOf<Bill?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP BAR
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
                    Text(text = "Scanned Purchase Bills", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Digital archive of supplier invoices", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                }
            }
        }

        if (bills.isEmpty()) {
            EmptyState(
                title = "No Scanned Bills",
                subtitle = "Scan purchase invoices using the camera scanner to keep digital bill records.",
                icon = Icons.Default.DocumentScanner
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bills, key = { it.id }) { bill ->
                    BillCard(bill = bill, onClick = { selectedBill = bill })
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (selectedBill != null) {
        BillDetailSheet(
            bill = selectedBill!!,
            onDismiss = { selectedBill = null },
            onDelete = {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.deleteBill(selectedBill!!.id)
                }
                selectedBill = null
            }
        )
    }
}

@Composable
private fun BillCard(bill: Bill, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = SecondaryGreen, modifier = Modifier.size(38.dp)) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = bill.supplierName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "${bill.invoiceNumber} • ${bill.date}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
                VerificationBadge(status = bill.verificationStatus)
            }

            if (bill.isDuplicate) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = PendingAmberBg) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = PendingAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Possible Duplicate Invoice Number", style = MaterialTheme.typography.labelSmall, color = PendingAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${bill.items.size} Items Detected", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(text = CurrencyUtils.formatInr(bill.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillDetailSheet(
    bill: Bill,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = bill.supplierName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Invoice ${bill.invoiceNumber} • ${bill.date}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "ITEMIZED BREAKDOWN", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            bill.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "${item.quantity}x ${item.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = CurrencyUtils.formatInr(item.totalPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(text = CurrencyUtils.formatInr(bill.subtotal), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "GST Amount", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(text = CurrencyUtils.formatInr(bill.gstAmount), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Total Bill Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = CurrencyUtils.formatInr(bill.totalAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
