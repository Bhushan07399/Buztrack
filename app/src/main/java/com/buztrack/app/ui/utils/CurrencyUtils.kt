package com.buztrack.app.ui.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatInr(amount: Double, showSymbol: Boolean = true): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val formatted = formatter.format(amount)
        // Clean trailing .00 if whole number for sleek UI
        val result = if (amount % 1.0 == 0.0) {
            formatted.replace(".00", "")
        } else {
            formatted
        }
        return if (!showSymbol) {
            result.replace("₹", "").trim()
        } else {
            result
        }
    }
}
