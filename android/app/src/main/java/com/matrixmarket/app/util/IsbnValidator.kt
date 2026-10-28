package com.matrixmarket.app.util

// Validates that a scanned barcode really is a BOOK (lecturer feedback: "any EAN-13
// barcode is accepted as an ISBN, including non-book products").
// A book's EAN-13 starts with the "Bookland" prefix 978 or 979 and has a valid
// check digit. ISBN-10 (printed inside older books) is also accepted for typing in.
object IsbnValidator {

    // Removes spaces and hyphens, keeps a trailing X (ISBN-10 check digit).
    fun normalize(input: String?): String =
        input.orEmpty().filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase()

    fun isValidIsbn13(isbn: String): Boolean {
        if (isbn.length != 13 || !isbn.all { it.isDigit() }) return false
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) return false
        var sum = 0
        for (i in 0 until 12) {
            val digit = isbn[i] - '0'
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val check = (10 - sum % 10) % 10
        return check == isbn[12] - '0'
    }

    fun isValidIsbn10(isbn: String): Boolean {
        if (isbn.length != 10) return false
        var sum = 0
        for (i in 0 until 10) {
            val c = isbn[i]
            val value = when {
                c.isDigit() -> c - '0'
                i == 9 && c == 'X' -> 10
                else -> return false
            }
            sum += (10 - i) * value
        }
        return sum % 11 == 0
    }

    fun isValid(input: String?): Boolean {
        val isbn = normalize(input)
        return when (isbn.length) {
            13 -> isValidIsbn13(isbn)
            10 -> isValidIsbn10(isbn)
            else -> false
        }
    }
}
