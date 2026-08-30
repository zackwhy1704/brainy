package com.zackwhye.secondbrain.feature.home.ui

import app.cash.turbine.test
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is Loading — no repository wired yet`() = runTest {
        val viewModel = HomeViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
        }
    }
}
