package com.matrixmarket.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun emailValidation() {
        assertTrue(InputValidator.isValidEmail("thabo.mokoena@myemeris.edu.za"))
        assertTrue(InputValidator.isValidEmail("  st10084625@vcconnect.edu.za "))
        assertFalse(InputValidator.isValidEmail("not-an-email"))
        assertFalse(InputValidator.isValidEmail("a@b"))
        assertFalse(InputValidator.isValidEmail(""))
    }

    @Test
    fun passwordAndNameRules() {
        assertTrue(InputValidator.isValidPassword("secret1"))
        assertFalse(InputValidator.isValidPassword("12345"))
        assertTrue(InputValidator.isValidName("Caleb Naidoo"))
        assertFalse(InputValidator.isValidName(" A "))
    }

    @Test
    fun priceParsingAcceptsDotOrCommaAndRejectsBadValues() {
        assertEquals(450.0, InputValidator.parsePrice("450")!!, 0.001)
        assertEquals(450.5, InputValidator.parsePrice("450,50")!!, 0.001)
        assertEquals(0.0, InputValidator.parsePrice("0")!!, 0.001)
        assertNull(InputValidator.parsePrice(""))
        assertNull(InputValidator.parsePrice("1.2.3"))
        assertNull(InputValidator.parsePrice("2000000"))
    }

    @Test
    fun priceInputFilterRemovesLetters() {
        assertEquals("12.50", InputValidator.filterPriceInput("R12.50abc"))
    }

    @Test
    fun titleAndDescriptionLengths() {
        assertTrue(InputValidator.isValidTitle("Calculus"))
        assertFalse(InputValidator.isValidTitle("ab"))
        assertTrue(InputValidator.isValidDescription("Good condition"))
        assertFalse(InputValidator.isValidDescription("ok"))
    }
}
