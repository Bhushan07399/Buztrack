package com.buztrack.app.ui.screens.cashbook

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.data.repository.BuztrackRepository
import com.buztrack.app.ui.components.SectionHeader
import com.buztrack.app.ui.components.TransactionRow
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBookScreen(
    repository: BuztrackRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openingCash by repository.openingCash.collectAsState()
    val transactions by repository.transactions.collectAsState()
    val todaySummary = repository.getTodaySummary()

    // Dynamic Cash Calculations
    val cashIn = todaySummary.cashIncome
    val cashOut = todaySummary.cashExpense
    val expectedClosingCash = openingCash + cashIn - cashOut

    val cashTransactions = transactions.filter { it.paymentMethod == PaymentMethod.CASH }

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
                    Text(
                        text = "Daily Cash Book",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Check your expected cash balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CASH SUMMARY CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "EXPECTED CLOSING CASH", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = CurrencyUtils.formatInr(expectedClosingCash),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(16.dp))

                        // CALCULATION STEPS
                        CashCalcRow(label = "Opening Cash Balance", amount = openingCash, isAddition = null)
                        CashCalcRow(label = "Cash Income / Collection (+)", amount = cashIn, isAddition = true)
                        CashCalcRow(label = "Cash Expense / Supplier Payment (-)", amount = cashOut, isAddition = false)
                    }
                }
            }

            item {
                SectionHeader(title = "Today's Cash Ledger")
            }

            items(cashTransactions, key = { it.id }) { tx ->
                TransactionRow(transaction = tx)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CashCalcRow(label: String, amount: Double, isAddition: Boolean?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
        val color = when (isAddition) {
            true -> IncomeGreen
            false -> ExpenseRed
            null -> Color.White
        }
        val prefix = when (isAddition) {
            true -> "+ "
            false -> "- "
            null -> ""
        }
        Text(
            text = "$prefix${CurrencyUtils.formatInr(amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
