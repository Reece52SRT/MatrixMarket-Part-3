package com.matrixmarket.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class DateAndUrlTest {

    @Test
    fun formatsApiDates() {
        assertEquals("Fri 23 Oct, 14:00", DateFormats.meeting("2026-10-23T14:00:00", Locale.UK))
        assertEquals("October 2026", DateFormats.monthYear("2026-10-01T08:30:00.1234567Z", Locale.UK))
        assertEquals("not a date", DateFormats.meeting("not a date", Locale.UK))
    }

    @Test
    fun sendsMeetingTimeWithoutOffsetOrNanos() {
        assertEquals("2026-10-23T14:00:00", DateFormats.toApi(LocalDateTime.of(2026, 10, 23, 14, 0, 0, 999)))
    }

    @Test
    fun formatsRand() {
        assertEquals("R 450.00", DateFormats.formatRand(450.0))
    }

    @Test
    fun resolvesListingPhotoUrls() {
        val base = "https://api.example.net/"
        assertEquals("https://api.example.net/api/v1/media/abc.jpg", UrlUtils.resolveImageUrl(base, "/api/v1/media/abc.jpg"))
        assertEquals("https://cdn.x/y.png", UrlUtils.resolveImageUrl(base, "https://cdn.x/y.png"))
        assertNull(UrlUtils.resolveImageUrl(base, null))
        assertNull(UrlUtils.resolveImageUrl(base, " "))
    }
}
