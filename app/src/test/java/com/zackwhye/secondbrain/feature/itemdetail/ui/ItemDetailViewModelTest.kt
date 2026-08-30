package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.lifecycle.SavedStateHandle
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

class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeItemRepository()

    private fun savedStateHandleFor(itemId: String) = SavedStateHandle(mapOf("itemId" to itemId))

    @Test
    fun `Loading then Ready when the item exists`() = runTest {
        fakeRepository.setItems(
            listOf(
                Item(
                    id = "1",
                    sourceType = ItemSourceType.TEXT,
                    sourceDoor = SourceDoor.SHARE,
                    sourceUri = null,
                    rawText = "captured text",
                    title = null,
                    capturedAt = Instant.now(),
                ),
            ),
        )
        val viewModel = ItemDetailViewModel(savedStateHandleFor("1"), fakeRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val ready = awaitItem()
            assertTrue(ready is ItemDetailUiState.Ready)
            assertEquals("captured text", (ready as ItemDetailUiState.Ready).item.rawContent)
        }
    }

    @Test
    fun `Loading then Empty when the item does not exist`() = runTest {
        fakeRepository.setItems(emptyList())
        val viewModel = ItemDetailViewModel(savedStateHandleFor("missing"), fakeRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            assertEquals(ItemDetailUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `Loading then Error when repository fails`() = runTest {
        fakeRepository.setShouldError(true)
        val viewModel = ItemDetailViewModel(savedStateHandleFor("1"), fakeRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is ItemDetailUiState.Error)
            assertTrue((error as ItemDetailUiState.Error).retryable)
        }
    }
}
