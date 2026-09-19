package com.buztrack.app.ui.screens.dailyclosing

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.repository.BuztrackRepository
import com.buztrack.app.ui.components.SectionHeader
import com.buztrack.app.ui.theme.CardBorder
import com.buztrack.app.ui.theme.DarkSlate
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PendingAmber
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.SecondaryGreen
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.utils.CurrencyUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyClosingScreen(
    repository: BuztrackRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val todaySummary = repository.getTodaySummary()
    val openingCash by repository.openingCash.collectAsState()
    val closings by repository.dailyClosings.collectAsState()

    val todayDate = repository.getTodayDateString()
    val isTodayClosed = closings.any { it.date == todayDate && it.isClosed }

    val expectedCash = openingCash + todaySummary.cashIncome - todaySummary.cashExpense

    var actualCashText by remember { mutableStateOf(expectedCash.toInt().toString()) }
    var notesText by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(isTodayClosed) }

    val actualCash = actualCashText.toDoubleOrNull() ?: 0.0
    val difference = actualCash - expectedCash

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
                    Text(text = "Daily Business Closing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Match your cash before closing the shop", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "TODAY'S SUMMARY ($todayDate)", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
                            if (showSuccessMessage) {
                                Surface(shape = RoundedCornerShape(12.dp), color = SecondaryGreen) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Day Closed", style = MaterialTheme.typography.labelSmall, color = IncomeGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Today's Income", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(text = CurrencyUtils.formatInr(todaySummary.totalIncome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Today's Expenses", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(text = CurrencyUtils.formatInr(todaySummary.totalExpenses), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Expected Cash in Register", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = CurrencyUtils.formatInr(expectedCash), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // INPUT ACTUAL CASH
                        Text(text = "Actual Cash Counted (₹)", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = actualCashText,
                            onValueChange = { actualCashText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !showSuccessMessage,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // DIFFERENCE BADGE
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Difference / Shortage", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            val diffColor = when {
                                difference == 0.0 -> IncomeGreen
                                difference < 0 -> ExpenseRed
                                else -> PendingAmber
                            }
                            val prefix = if (difference > 0) "+" else ""
                            Text(
                                text = "$prefix${CurrencyUtils.formatInr(difference)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("Closing notes (optional)...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !showSuccessMessage,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.closeDay(
                                        date = todayDate,
                                        expectedCash = expectedCash,
                                        actualCash = actualCash,
                                        notes = notesText
                                    )
                                    showSuccessMessage = true
                                }
                            },
                            enabled = !showSuccessMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showSuccessMessage) "Day Closed Successfully" else "Close Business Day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Previous Closed Days")
            }

            items(closings, key = { it.id }) { closing ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = closing.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Closed at ${closing.closedAt}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Actual Cash: ${CurrencyUtils.formatInr(closing.actualCash)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            Text(text = "Diff: ${CurrencyUtils.formatInr(closing.difference)}", style = MaterialTheme.typography.labelSmall, color = if (closing.difference >= 0) IncomeGreen else ExpenseRed)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
