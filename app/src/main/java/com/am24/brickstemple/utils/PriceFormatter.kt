package com.am24.brickstemple.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale


object PriceFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        decimalSeparator = '.'
    }

    private val formatter = DecimalFormat("0.00", symbols)

    fun format(price: Double): String {
        return formatter.format(price)
    }
}
