package com.buztrack.app.ui.screens.more

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.repository.BuztrackRepository
import com.buztrack.app.ui.theme.CardBorder
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.SecondaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    repository: BuztrackRepository,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToCashBook: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToDailyClosing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val businessProfile by repository.businessProfile.collectAsState()
    var showSubscriptionSheet by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showResetSuccessSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP BAR
        Surface(color = DarkSlate, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = businessProfile.businessName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "Owner: ${businessProfile.ownerName} • Demo Business • Sample Data", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SUBSCRIPTION BANNER
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSubscriptionSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "BUZTRACK PRO PLAN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "${businessProfile.scannedBillsCount}/${businessProfile.scannedBillsLimit} Bills Scanned • Upgrade to Unlimited", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // GROUP 1: BUSINESS MONEY
            item {
                Text(text = "BUSINESS MONEY", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
            }

            item {
                MenuItem(title = "Customer Credit / Udhari", subtitle = "Track customer dues and collections", icon = Icons.Default.Group, onClick = onNavigateToCustomers)
            }

            item {
                MenuItem(title = "Suppliers", subtitle = "Track purchases and pending payments", icon = Icons.Default.ShoppingBag, onClick = onNavigateToSuppliers)
            }

            item {
                MenuItem(title = "Cash Book", subtitle = "See cash received, spent and balance", icon = Icons.Default.AccountBalanceWallet, onClick = onNavigateToCashBook)
            }

            // GROUP 2: BILLS & CLOSING
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "BILLS & CLOSING", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
            }

            item {
                MenuItem(title = "Purchase Bills", subtitle = "Store and review purchase bills", icon = Icons.Default.DocumentScanner, onClick = onNavigateToBills)
            }

            item {
                MenuItem(title = "Daily Closing", subtitle = "Match expected and actual cash", icon = Icons.Default.LockClock, onClick = onNavigateToDailyClosing)
            }

            // GROUP 3: ACCOUNT & SETTINGS
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "ACCOUNT & SETTINGS", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
            }

            item {
                MenuItem(title = "Business Profile", subtitle = "GSTIN: DEMO-SAMPLE • Jaipur Shop", icon = Icons.Default.Business, onClick = { showSubscriptionSheet = true })
            }

            // GROUP 4: DEMO CONTROL
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "DEMO CONTROL", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
            }

            item {
                MenuItem(
                    title = "Reset Demo Data",
                    subtitle = "Clear all transactions & reset to fresh ledger",
                    icon = Icons.Default.Refresh,
                    onClick = { showResetConfirmDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showSubscriptionSheet) {
        SubscriptionPreviewSheet(onDismiss = { showSubscriptionSheet = false })
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset all business data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all transactions, customers, suppliers, bills and ledger records to start fresh.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmDialog = false
                        coroutineScope.launch {
                            repository.resetToDefaultDemoData()
                            showResetSuccessSheet = true
                        }
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showResetSuccessSheet) {
        ModalBottomSheet(onDismissRequest = { showResetSuccessSheet = false }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Ledger Cleared Successfully", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "All transactions, customers, suppliers, bills and daily closings have been cleared to a fresh empty state.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showResetSuccessSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Got it", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = SecondaryGreen, modifier = Modifier.size(40.dp)) {
                    Icon(imageVector = icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.padding(10.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionPreviewSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "BUZTRACK Subscription Plans", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // FREE PLAN CARD
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CardBorder), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "FREE PLAN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "₹0 / Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                    Text(text = "Includes up to 10 scanned bills per month & complete daily ledger.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PRO PLAN CARD
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(2.dp, PrimaryGreen), colors = CardDefaults.cardColors(containerColor = SecondaryGreen.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "PRO PLAN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkSlate)
                        Text(text = "₹299 / Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                    Text(text = "Includes up to 100 AI bill scans/month, multi-device backup & advanced WhatsApp reminders.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen), modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text(text = "Close Plan Preview", fontWeight = FontWeight.Bold)
            }
        }
    }
}
