package com.zackwhye.secondbrain.feature.home.ui

import app.cash.turbine.test
import com.zackwhye.secondbrain.core.data.FakeBriefRepository
import com.zackwhye.secondbrain.core.data.FakeItemRepository
import com.zackwhye.secondbrain.core.model.Brief
import com.zackwhye.secondbrain.core.model.BriefStatus
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

    private val fakeItemRepository = FakeItemRepository()
    private val fakeBriefRepository = FakeBriefRepository()

    @Test
    fun `Loading then Ready when repository has items`() = runTest {
        fakeItemRepository.setItems(
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
        val viewModel = HomeViewModel(fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val ready = awaitItem()
            assertTrue(ready is HomeUiState.Ready)
            assertEquals(1, (ready as HomeUiState.Ready).items.size)
        }
    }

    @Test
    fun `Loading then Empty when repository has no items`() = runTest {
        fakeItemRepository.setItems(emptyList())
        val viewModel = HomeViewModel(fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `Loading then Error when repository fails`() = runTest {
        fakeItemRepository.setShouldError(true)
        val viewModel = HomeViewModel(fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is HomeUiState.Error)
            assertTrue((error as HomeUiState.Error).retryable)
        }
    }

    @Test
    fun `card shows brief summary and topic chips once the brief is ready`() = runTest {
        fakeItemRepository.setItems(
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
        fakeBriefRepository.setBriefs(
            listOf(
                Brief(
                    itemId = "1",
                    status = BriefStatus.READY,
                    summary = "A one-line summary.",
                    entities = emptyList(),
                    topics = listOf("ai", "accounting"),
                    tasks = emptyList(),
                    importance = 3,
                    failureReason = null,
                ),
            ),
        )
        val viewModel = HomeViewModel(fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val ready = awaitItem() as HomeUiState.Ready
            val card = ready.items.single()
            assertEquals("A one-line summary.", card.summary)
            assertEquals(listOf("ai", "accounting"), card.topicChips)
        }
    }
}
