package com.buztrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.models.Transaction
import com.buztrack.app.data.models.TransactionType
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.ExpenseRedBg
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PendingAmber
import com.buztrack.app.ui.theme.PendingAmberBg
import com.buztrack.app.ui.theme.SecondaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.theme.UpiBlueBg
import com.buztrack.app.ui.utils.CurrencyUtils

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (icon, iconBg, iconTint) = when (transaction.type) {
        TransactionType.INCOME -> Triple(Icons.Default.ArrowUpward, SecondaryGreen, IncomeGreen)
        TransactionType.EXPENSE -> Triple(Icons.Default.ArrowDownward, ExpenseRedBg, ExpenseRed)
        TransactionType.CUSTOMER_CREDIT -> Triple(Icons.Default.Person, PendingAmberBg, PendingAmber)
        TransactionType.CUSTOMER_PAYMENT -> Triple(Icons.Default.ArrowUpward, SecondaryGreen, IncomeGreen)
        TransactionType.SUPPLIER_PURCHASE -> Triple(Icons.AutoMirrored.Filled.ReceiptLong, UpiBlueBg, ExpenseRed)
        TransactionType.SUPPLIER_PAYMENT -> Triple(Icons.Default.ShoppingBag, ExpenseRedBg, ExpenseRed)
    }

    val amountPrefix = if (transaction.type.isPositive) "+" else "-"
    val amountColor = if (transaction.type.isPositive) IncomeGreen else ExpenseRed

    val title = when {
        !transaction.partyName.isNullOrBlank() -> transaction.partyName
        else -> transaction.category
    }

    val subtitle = buildString {
        append(transaction.category)
        if (!transaction.note.isBlank()) {
            append(" • ")
            append(transaction.note)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.type.displayName,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyUtils.formatInr(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PaymentMethodBadge(method = transaction.paymentMethod)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transaction.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
