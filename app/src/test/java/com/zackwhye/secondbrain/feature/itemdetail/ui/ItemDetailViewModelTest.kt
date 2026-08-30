package com.zackwhye.secondbrain.feature.itemdetail.ui

import app.cash.turbine.test
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is Loading — no repository or nav-args wired yet`() = runTest {
        val viewModel = ItemDetailViewModel()

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
        }
    }
}
