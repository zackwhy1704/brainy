package com.zackwhye.secondbrain.core.testing

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Proves the two pieces every ViewModel test in this project will rely on:
 * [MainDispatcherRule] swapping Dispatchers.Main, and Turbine asserting Flow emissions.
 * Not a real ViewModel test — there's no ViewModel yet (Phase 1).
 */
class MainDispatcherRuleAndTurbineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `flow emits expected value under test dispatcher`() = runTest {
        val flow = MutableStateFlow("initial")

        flow.test {
            assertEquals("initial", awaitItem())
            flow.value = "updated"
            assertEquals("updated", awaitItem())
        }
    }
}
