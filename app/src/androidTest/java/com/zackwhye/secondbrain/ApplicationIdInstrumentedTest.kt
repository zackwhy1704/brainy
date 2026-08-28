package com.zackwhye.secondbrain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Trivial instrumented-test proof: runs on a real device/emulator, not the JVM. */
@RunWith(AndroidJUnit4::class)
class ApplicationIdInstrumentedTest {

    @Test
    fun appContext_hasExpectedPackageName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.zackwhye.secondbrain", context.packageName)
    }
}
