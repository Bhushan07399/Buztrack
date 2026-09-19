package com.buztrack.app.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.data.models.TransactionType
import com.buztrack.app.data.repository.BuztrackRepository
import com.buztrack.app.ui.theme.CardBorder
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PendingAmber
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.theme.UpiBlue
import com.buztrack.app.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    repository: BuztrackRepository,
    modifier: Modifier = Modifier
) {
    val transactions by repository.transactions.collectAsState()

    // Monthly Aggregations
    var totalInc = 0.0
    var totalExp = 0.0

    val categoryMap = mutableMapOf<String, Double>()
    val paymentMap = mutableMapOf<PaymentMethod, Double>()

    transactions.forEach { tx ->
        when (tx.type) {
            TransactionType.INCOME, TransactionType.CUSTOMER_PAYMENT -> {
                totalInc += tx.amount
                paymentMap[tx.paymentMethod] = (paymentMap[tx.paymentMethod] ?: 0.0) + tx.amount
            }
            TransactionType.EXPENSE, TransactionType.SUPPLIER_PAYMENT -> {
                totalExp += tx.amount
                categoryMap[tx.category] = (categoryMap[tx.category] ?: 0.0) + tx.amount
            }
            else -> {}
        }
    }

    val netResult = totalInc - totalExp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP BAR
        Surface(color = DarkSlate, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Business Analytics & Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "See income, expenses and trends", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MONTHLY NET RESULT CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "RECORDED BUSINESS SUMMARY", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(12.dp), color = PrimaryGreen.copy(alpha = 0.2f)) {
                                Text(text = "September 2026", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = CurrencyUtils.formatInr(netResult),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (netResult >= 0) IncomeGreen else ExpenseRed
                        )
                        Text(text = "Net Cash Result (Income − recorded expenses)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Recorded Income", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                                Text(text = CurrencyUtils.formatInr(totalInc), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Recorded Expenses", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                                Text(text = CurrencyUtils.formatInr(totalExp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }
                }
            }

            // INCOME VS EXPENSES VISUAL COMPARISON BAR (EXACT 100% PERCENTAGE CALCULATION)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Income vs Expenses Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val totalVal = totalInc + totalExp
                        val (incPct, expPct) = if (totalVal > 0) {
                            val incRatio = (totalInc / totalVal) * 100.0
                            val roundedInc = Math.round(incRatio * 10.0) / 10.0
                            val roundedExp = Math.round((100.0 - roundedInc) * 10.0) / 10.0
                            Pair(roundedInc, roundedExp)
                        } else {
                            Pair(50.0, 50.0)
                        }

                        val incWeight = (incPct / 100.0).toFloat().coerceIn(0.01f, 0.99f)
                        val expWeight = (1f - incWeight).coerceIn(0.01f, 0.99f)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Income ${incPct}%", style = MaterialTheme.typography.labelMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            Text(text = "Expense ${expPct}%", style = MaterialTheme.typography.labelMedium, color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                            Box(modifier = Modifier.weight(incWeight).fillMaxHeight().background(IncomeGreen))
                            Box(modifier = Modifier.weight(expWeight).fillMaxHeight().background(ExpenseRed))
                        }
                    }
                }
            }

            // EXPENSE CATEGORY BREAKDOWN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Top Expense Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (categoryMap.isEmpty()) {
                            Text(text = "No expenses recorded yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        } else {
                            categoryMap.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
                                val pct = if (totalExp > 0) (amt / totalExp).toFloat() else 0f
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(text = CurrencyUtils.formatInr(amt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        color = ExpenseRed,
                                        trackColor = Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // PAYMENT METHOD DISTRIBUTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Payment Mode Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        PaymentMethod.values().forEach { method ->
                            val amt = paymentMap[method] ?: 0.0
                            val color = when (method) {
                                PaymentMethod.CASH -> IncomeGreen
                                PaymentMethod.UPI -> UpiBlue
                                PaymentMethod.CARD -> PendingAmber
                                PaymentMethod.BANK -> DarkSlate
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = method.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = CurrencyUtils.formatInr(amt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
