package com.am24.brickstemple.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PriceFormatter {

    private val formatter = DecimalFormat("0.00").apply {
        decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
    }

    fun format(price: String): String {
        return try {
            val num = price.toDouble()
            formatter.format(num)
        } catch (e: Exception) {
            price
        }
    }
}
