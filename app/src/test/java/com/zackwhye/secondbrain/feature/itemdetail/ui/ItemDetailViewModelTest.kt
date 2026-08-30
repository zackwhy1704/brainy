package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.lifecycle.SavedStateHandle
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

class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeItemRepository = FakeItemRepository()
    private val fakeBriefRepository = FakeBriefRepository()

    private fun savedStateHandleFor(itemId: String) = SavedStateHandle(mapOf("itemId" to itemId))

    private fun putItem(id: String) {
        fakeItemRepository.setItems(
            listOf(
                Item(
                    id = id,
                    sourceType = ItemSourceType.TEXT,
                    sourceDoor = SourceDoor.SHARE,
                    sourceUri = null,
                    rawText = "captured text",
                    title = null,
                    capturedAt = Instant.now(),
                ),
            ),
        )
    }

    @Test
    fun `Loading then Ready with a Pending brief when no brief row exists yet`() = runTest {
        putItem("1")
        val viewModel = ItemDetailViewModel(savedStateHandleFor("1"), fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val ready = awaitItem() as ItemDetailUiState.Ready
            assertEquals("captured text", ready.item.rawContent)
            assertEquals(BriefUiState.Pending, ready.item.brief)
        }
    }

    @Test
    fun `Ready brief renders summary, topics, and tasks`() = runTest {
        putItem("1")
        fakeBriefRepository.setBriefs(
            listOf(
                Brief(
                    itemId = "1",
                    status = BriefStatus.READY,
                    summary = "A concise summary.",
                    entities = listOf("Second Brain"),
                    topics = listOf("productivity"),
                    tasks = listOf("Follow up next week"),
                    importance = 4,
                    failureReason = null,
                ),
            ),
        )
        val viewModel = ItemDetailViewModel(savedStateHandleFor("1"), fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val ready = awaitItem() as ItemDetailUiState.Ready
            val brief = ready.item.brief as BriefUiState.Ready
            assertEquals("A concise summary.", brief.summary)
            assertEquals(listOf("productivity"), brief.topics)
            assertEquals(listOf("Follow up next week"), brief.tasks)
            assertEquals(4, brief.importance)
        }
    }

    @Test
    fun `Failed brief is retryable and never silently absent`() = runTest {
        putItem("1")
        fakeBriefRepository.setBriefs(
            listOf(
                Brief(
                    itemId = "1",
                    status = BriefStatus.FAILED,
                    summary = null,
                    entities = emptyList(),
                    topics = emptyList(),
                    tasks = emptyList(),
                    importance = null,
                    failureReason = "Anthropic API 529: overloaded",
                ),
            ),
        )
        val viewModel = ItemDetailViewModel(savedStateHandleFor("1"), fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val ready = awaitItem() as ItemDetailUiState.Ready
            val brief = ready.item.brief as BriefUiState.Failed
            assertEquals("Anthropic API 529: overloaded", brief.reason)
            assertTrue(brief.retryable)
        }
    }

    @Test
    fun `retryBrief invokes BriefRepository retryExtraction for this item`() = runTest {
        putItem("42")
        val viewModel = ItemDetailViewModel(savedStateHandleFor("42"), fakeItemRepository, fakeBriefRepository)

        viewModel.retryBrief()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeBriefRepository.retryCallCount)
        assertEquals("42", fakeBriefRepository.lastRetriedItemId)
    }

    @Test
    fun `Loading then Empty when the item does not exist`() = runTest {
        fakeItemRepository.setItems(emptyList())
        val viewModel = ItemDetailViewModel(savedStateHandleFor("missing"), fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            assertEquals(ItemDetailUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `Loading then Error when repository fails`() = runTest {
        fakeItemRepository.setShouldError(true)
        val viewModel = ItemDetailViewModel(savedStateHandleFor("1"), fakeItemRepository, fakeBriefRepository)

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is ItemDetailUiState.Error)
            assertTrue((error as ItemDetailUiState.Error).retryable)
        }
    }
}
