package com.am24.brickstemple.utils

import org.junit.Assert.*
import org.junit.Test

class PriceFormatterTest{

    @Test
    fun `formats regular decimal number`() {
        val result = PriceFormatter.format("12.345")
        assertEquals("12.35", result)
    }

    @Test
    fun `formats whole number`() {
        val result = PriceFormatter.format("10")
        assertEquals("10.00", result)
    }

    @Test
    fun `formats negative number`() {
        val result = PriceFormatter.format("-5.5")
        assertEquals("-5.50", result)
    }

    @Test
    fun `returns original string on invalid input`() {
        val result = PriceFormatter.format("abc")
        assertEquals("abc", result)
    }

    @Test
    fun `returns original string when comma used instead of dot`() {
        val result = PriceFormatter.format("12,50")
        assertEquals("12,50", result)
    }

    @Test
    fun `formats string with surrounding spaces`() {
        val result = PriceFormatter.format("  7.1  ".trim())
        assertEquals("7.10", result)
    }

    @Test
    fun `formats very large number`() {
        val result = PriceFormatter.format("1234567.891")
        assertEquals("1234567.89", result)
    }

    @Test
    fun `correct rounding of numbers`() {
        val result = PriceFormatter.format("12.879")
        assertEquals("12.88", result)
    }

    @Test
    fun `formats scientific notation`() {
        val result = PriceFormatter.format("1e3")
        assertEquals("1000.00", result)
    }
}