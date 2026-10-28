package com.matrixmarket.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorParserTest {

    @Test
    fun readsApiMessage() {
        assertEquals("Incorrect confirmation code.", ErrorParser.parse("{\"message\":\"Incorrect confirmation code.\"}", 400, "x"))
    }

    @Test
    fun readsFirstValidationError() {
        val body = "{\"title\":\"One or more validation errors occurred.\",\"errors\":{\"Price\":[\"Price is invalid\"]}}"
        assertEquals("Price is invalid", ErrorParser.parse(body, 400, "x"))
    }

    @Test
    fun handlesPlainTextAndEmptyBodies() {
        assertEquals("Invalid email or password.", ErrorParser.parse("Invalid email or password.", 401, "x"))
        assertEquals("fallback", ErrorParser.parse(null, 418, "fallback"))
        assertEquals("The server had a problem. Please try again in a moment.", ErrorParser.parse("", 500, "x"))
    }
}
