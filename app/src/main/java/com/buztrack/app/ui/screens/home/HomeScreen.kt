package com.buztrack.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buztrack.app.ui.components.BuztrackTopBar
import com.buztrack.app.ui.components.SectionHeader
import com.buztrack.app.ui.components.StatCard
import com.buztrack.app.ui.components.TransactionRow
import com.buztrack.app.ui.screens.expense.AddExpenseBottomSheet
import com.buztrack.app.ui.screens.income.AddIncomeBottomSheet
import com.buztrack.app.ui.theme.CardBorder
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.ExpenseRedBg
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PendingAmber
import com.buztrack.app.ui.theme.PendingAmberBg
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.SecondaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.theme.UpiBlue
import com.buztrack.app.ui.theme.UpiBlueBg
import com.buztrack.app.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val businessProfile by viewModel.businessProfile.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val showAddIncome by viewModel.showAddIncome.collectAsState()
    val showAddExpense by viewModel.showAddExpense.collectAsState()

    val todayTransactions = transactions.take(5)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        BuztrackTopBar(
            businessName = businessProfile.businessName,
            ownerName = businessProfile.ownerName,
            dateString = todaySummary.date,
            onProfileClick = onNavigateToProfile
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. TODAY'S MONEY OVERVIEW CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryGreen.copy(alpha = 0.2f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TODAY'S BUSINESS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Net Result Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (todaySummary.netResult >= 0) PrimaryGreen.copy(alpha = 0.2f) else ExpenseRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Net: ${CurrencyUtils.formatInr(todaySummary.netResult)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (todaySummary.netResult >= 0) PrimaryGreen else ExpenseRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Income",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatInr(todaySummary.totalIncome),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Expenses",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatInr(todaySummary.totalExpenses),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Payment Methods Divider & Breakdown
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF334155))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PaymentMethodItem(label = "Cash", amount = todaySummary.cashIncome, color = SecondaryGreen, textColor = IncomeGreen)
                            PaymentMethodItem(label = "UPI", amount = todaySummary.upiIncome, color = UpiBlueBg, textColor = UpiBlue)
                            PaymentMethodItem(label = "Card", amount = todaySummary.cardIncome, color = PendingAmberBg, textColor = PendingAmber)
                            PaymentMethodItem(label = "Bank", amount = todaySummary.bankIncome, color = Color(0xFFE2E8F0), textColor = TextSecondary)
                        }
                    }
                }
            }

            // 2. QUICK ACTIONS BAR
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "+ Income",
                        icon = Icons.Default.ArrowUpward,
                        bgColor = SecondaryGreen,
                        contentColor = IncomeGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openAddIncome() }
                    )
                    QuickActionButton(
                        title = "+ Expense",
                        icon = Icons.Default.ArrowDownward,
                        bgColor = ExpenseRedBg,
                        contentColor = ExpenseRed,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openAddExpense() }
                    )
                    QuickActionButton(
                        title = "+ Credit",
                        icon = Icons.Default.PersonAdd,
                        bgColor = PendingAmberBg,
                        contentColor = PendingAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToCustomers() }
                    )
                    QuickActionButton(
                        title = "+ Payment",
                        icon = Icons.Default.CreditCard,
                        bgColor = UpiBlueBg,
                        contentColor = UpiBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToSuppliers() }
                    )
                }
            }

            // 3. SCAN PURCHASE BILL BANNER
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToScan() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF059669), Color(0xFF10B981))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Bill",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE047),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SMART BILL SCANNER",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFDE047),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Scan Purchase Bill",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Extract supplier, GST & total instantly",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Scan Now",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 4. PENDING MONEY SUMMARY CARDS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Pending Collection",
                        amount = todaySummary.pendingCustomerCollectionTotal,
                        icon = Icons.Default.Group,
                        iconBgColor = PendingAmberBg,
                        iconColor = PendingAmber,
                        amountColor = PendingAmber,
                        subtitle = "From Customers",
                        onClick = onNavigateToCustomers,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Supplier Pending",
                        amount = todaySummary.pendingSupplierTotal,
                        icon = Icons.Default.ShoppingBag,
                        iconBgColor = ExpenseRedBg,
                        iconColor = ExpenseRed,
                        amountColor = ExpenseRed,
                        subtitle = "To Suppliers",
                        onClick = onNavigateToSuppliers,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. RECENT TRANSACTIONS HEADER & LIST
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    actionText = "View All",
                    onActionClick = onNavigateToTransactions
                )
            }

            items(todayTransactions, key = { it.id }) { tx ->
                TransactionRow(
                    transaction = tx,
                    onClick = onNavigateToTransactions
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // BOTTOM SHEETS
    if (showAddIncome) {
        AddIncomeBottomSheet(
            onDismiss = { viewModel.closeAddIncome() },
            onSave = { amount, category, method, note ->
                viewModel.addIncome(amount, category, method, note)
            }
        )
    }

    if (showAddExpense) {
        AddExpenseBottomSheet(
            onDismiss = { viewModel.closeAddExpense() },
            onSave = { amount, category, method, note ->
                viewModel.addExpense(amount, category, method, note)
            }
        )
    }
}

@Composable
private fun PaymentMethodItem(
    label: String,
    amount: Double,
    color: Color,
    textColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color
        ) {
            Text(
                text = CurrencyUtils.formatInr(amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}
