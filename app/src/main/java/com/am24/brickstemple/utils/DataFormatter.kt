package com.am24.brickstemple.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    fun formatDate(dateStr: String): String {
        val parsed = LocalDateTime.parse(dateStr)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
        return parsed.format(formatter)
    }
}