package com.matrixmarket.app.util

// Client-side validation so bad input is caught before it ever reaches the API,
// using the same rules the API enforces.
object InputValidator {

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= 6

    fun isValidName(name: String): Boolean = name.trim().length in 2..80

    // Parses a rand amount like "450", "450.5" or "450,50". Null if not a valid amount.
    fun parsePrice(input: String): Double? {
        val cleaned = input.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return if (value < 0 || value > 1_000_000) null else value
    }

    // Keeps only characters that can form a price while the user types.
    fun filterPriceInput(input: String): String =
        input.filter { it.isDigit() || it == '.' || it == ',' }.take(10)

    fun isValidTitle(title: String): Boolean = title.trim().length in 3..120

    fun isValidDescription(description: String): Boolean = description.trim().length in 5..2000
}
