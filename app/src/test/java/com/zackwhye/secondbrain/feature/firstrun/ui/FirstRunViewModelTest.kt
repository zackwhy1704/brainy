package com.zackwhye.secondbrain.feature.firstrun.ui

import com.zackwhye.secondbrain.core.prefs.FakeFirstRunStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunViewModelTest {

    @Test
    fun `acknowledge marks first run as seen exactly once per tap`() {
        val store = FakeFirstRunStore(seen = false)
        val viewModel = FirstRunViewModel(store)
        assertFalse(store.hasSeenFirstRun())

        viewModel.acknowledge()

        assertTrue(store.hasSeenFirstRun())
        assertEquals(1, store.markCallCount)
    }

    @Test
    fun `a fresh store reports first run not seen, so MainActivity starts on FirstRun`() {
        assertFalse(FakeFirstRunStore().hasSeenFirstRun())
    }
}
