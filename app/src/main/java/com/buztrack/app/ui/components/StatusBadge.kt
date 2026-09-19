package com.buztrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.buztrack.app.data.models.PaymentMethod
import com.buztrack.app.ui.theme.CardGrayBg
import com.buztrack.app.ui.theme.ExpenseRed
import com.buztrack.app.ui.theme.ExpenseRedBg
import com.buztrack.app.ui.theme.IncomeGreen
import com.buztrack.app.ui.theme.PendingAmber
import com.buztrack.app.ui.theme.PendingAmberBg
import com.buztrack.app.ui.theme.SecondaryGreen
import com.buztrack.app.ui.theme.TextPrimary
import com.buztrack.app.ui.theme.TextSecondary
import com.buztrack.app.ui.theme.UpiBlue
import com.buztrack.app.ui.theme.UpiBlueBg

@Composable
fun StatusBadge(
    text: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
fun PaymentMethodBadge(method: PaymentMethod) {
    val (bgColor, textColor) = when (method) {
        PaymentMethod.CASH -> SecondaryGreen to IncomeGreen
        PaymentMethod.UPI -> UpiBlueBg to UpiBlue
        PaymentMethod.CARD -> PendingAmberBg to PendingAmber
        PaymentMethod.BANK -> CardGrayBg to TextSecondary
    }
    StatusBadge(text = method.displayName, bgColor = bgColor, textColor = textColor)
}

@Composable
fun VerificationBadge(status: String) {
    val isVerified = status.equals("Verified", ignoreCase = true)
    val bgColor = if (isVerified) SecondaryGreen else PendingAmberBg
    val textColor = if (isVerified) IncomeGreen else PendingAmber
    StatusBadge(text = status, bgColor = bgColor, textColor = textColor)
}
