package com.matrixmarket.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// Runs on a device/emulator: checks the installed package name matches Firebase's config.
@RunWith(AndroidJUnit4::class)
class AppContextTest {
    @Test
    fun usesTheFirebaseRegisteredPackageName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.matrixmarket.app", context.packageName)
    }
}
