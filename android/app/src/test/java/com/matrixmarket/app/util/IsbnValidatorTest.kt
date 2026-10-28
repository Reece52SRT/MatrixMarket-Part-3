package com.matrixmarket.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Lecturer feedback: only real BOOK barcodes may be accepted as ISBNs.
class IsbnValidatorTest {

    @Test
    fun acceptsRealBookIsbn13() {
        assertTrue(IsbnValidator.isValid("9781285741550"))   // Stewart, Calculus
        assertTrue(IsbnValidator.isValid("978-0-262-04630-5")) // hyphens allowed
        assertTrue(IsbnValidator.isValid("9798886450035".let { fixCheckDigit(it) }))
    }

    @Test
    fun acceptsIsbn10IncludingXCheckDigit() {
        assertTrue(IsbnValidator.isValid("0306406152"))
        assertTrue(IsbnValidator.isValid("080442957X"))
    }

    @Test
    fun rejectsGroceryBarcodeEvenWithValidChecksum() {
        // 5000112637922 is a valid EAN-13 (a soft drink) but not a book.
        assertFalse(IsbnValidator.isValid("5000112637922"))
    }

    @Test
    fun rejectsWrongCheckDigitAndJunk() {
        assertFalse(IsbnValidator.isValid("9781285741551"))
        assertFalse(IsbnValidator.isValid("12345"))
        assertFalse(IsbnValidator.isValid(""))
        assertFalse(IsbnValidator.isValid(null))
        assertFalse(IsbnValidator.isValid("97812857415AB"))
    }

    @Test
    fun normalizeStripsSeparators() {
        assertEquals("9780262046305", IsbnValidator.normalize("978-0 262-04630-5"))
        assertEquals("080442957X", IsbnValidator.normalize("0-8044-2957-x"))
    }

    // Builds a valid 979-prefixed ISBN from 12 digits so the test doesn't rely on a memorised number.
    private fun fixCheckDigit(isbn: String): String {
        val first12 = isbn.take(12)
        val sum = first12.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return first12 + ((10 - sum % 10) % 10)
    }
}
