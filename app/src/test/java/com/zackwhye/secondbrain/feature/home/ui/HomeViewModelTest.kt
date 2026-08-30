package com.zackwhye.secondbrain.feature.home.ui

import app.cash.turbine.test
import com.zackwhye.secondbrain.core.data.FakeItemRepository
import com.zackwhye.secondbrain.core.model.Item
import com.zackwhye.secondbrain.core.model.ItemSourceType
import com.zackwhye.secondbrain.core.model.SourceDoor
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeItemRepository()

    @Test
    fun `Loading then Ready when repository has items`() = runTest {
        fakeRepository.setItems(
            listOf(
                Item(
                    id = "1",
                    sourceType = ItemSourceType.URL,
                    sourceDoor = SourceDoor.SHARE,
                    sourceUri = "https://example.com",
                    rawText = null,
                    title = null,
                    capturedAt = Instant.now(),
                ),
            ),
        )
        val viewModel = HomeViewModel(fakeRepository)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val ready = awaitItem()
            assertTrue(ready is HomeUiState.Ready)
            assertEquals(1, (ready as HomeUiState.Ready).items.size)
        }
    }

    @Test
    fun `Loading then Empty when repository has no items`() = runTest {
        fakeRepository.setItems(emptyList())
        val viewModel = HomeViewModel(fakeRepository)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `Loading then Error when repository fails`() = runTest {
        fakeRepository.setShouldError(true)
        val viewModel = HomeViewModel(fakeRepository)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is HomeUiState.Error)
            assertTrue((error as HomeUiState.Error).retryable)
        }
    }
}
