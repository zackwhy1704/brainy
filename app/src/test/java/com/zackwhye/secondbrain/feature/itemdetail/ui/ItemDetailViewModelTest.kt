package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.zackwhye.secondbrain.core.data.FakeBriefRepository
import com.zackwhye.secondbrain.core.data.FakeFactRepository
import com.zackwhye.secondbrain.core.data.FakeItemRepository
import com.zackwhye.secondbrain.core.model.Brief
import com.zackwhye.secondbrain.core.model.BriefStatus
import com.zackwhye.secondbrain.core.model.Fact
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
    private val fakeFactRepository = FakeFactRepository()

    private fun savedStateHandleFor(itemId: String) = SavedStateHandle(mapOf("itemId" to itemId))

    private fun viewModel(itemId: String) =
        ItemDetailViewModel(savedStateHandleFor(itemId), fakeItemRepository, fakeBriefRepository, fakeFactRepository)

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
        val viewModel = viewModel("1")

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val ready = awaitItem() as ItemDetailUiState.Ready
            assertEquals("captured text", ready.item.rawContent)
            assertEquals(BriefUiState.Pending, ready.item.brief)
            assertTrue(ready.item.people.isEmpty())
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
        val viewModel = viewModel("1")

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
    fun `people this item produced facts about surface as tappable subjects`() = runTest {
        putItem("1")
        fakeFactRepository.setFacts(
            listOf(
                Fact("f1", "Sarah Tan", "location", "Singapore", "q", 0.9f, Instant.now(), null, sourceItemId = "1"),
                Fact("f2", "Sarah Tan", "motivation", "Wants regional scope", "q", 0.9f, Instant.now(), null, sourceItemId = "1"),
                Fact("f3", "Ben Ong", "availability", "Free in Q4", "q", 0.9f, Instant.now(), null, sourceItemId = "1"),
                Fact("f4", "Someone Else", "location", "KL", "q", 0.9f, Instant.now(), null, sourceItemId = "other-item"),
            ),
        )
        val viewModel = viewModel("1")

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val ready = awaitItem() as ItemDetailUiState.Ready
            assertEquals(listOf("Ben Ong", "Sarah Tan"), ready.item.people) // distinct, sorted, this item only
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
        val viewModel = viewModel("1")

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
        val viewModel = viewModel("42")

        viewModel.retryBrief()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeBriefRepository.retryCallCount)
        assertEquals("42", fakeBriefRepository.lastRetriedItemId)
    }

    @Test
    fun `Loading then Empty when the item does not exist`() = runTest {
        fakeItemRepository.setItems(emptyList())
        val viewModel = viewModel("missing")

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            assertEquals(ItemDetailUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `deleteItem success emits Deleted so the Route can navigate back`() = runTest {
        putItem("1")
        val viewModel = viewModel("1")
        fakeItemRepository.deleteResult = true

        viewModel.events.test {
            viewModel.deleteItem()
            assertEquals(ItemDetailEvent.Deleted, awaitItem())
        }
        assertEquals(1, fakeItemRepository.deleteCallCount)
        assertEquals("1", fakeItemRepository.lastDeletedId)
    }

    @Test
    fun `deleteItem failure emits DeleteFailed and the screen stays put`() = runTest {
        putItem("1")
        val viewModel = viewModel("1")
        fakeItemRepository.deleteResult = false

        viewModel.events.test {
            viewModel.deleteItem()
            assertEquals(ItemDetailEvent.DeleteFailed, awaitItem())
        }
    }

    @Test
    fun `Loading then Error when repository fails`() = runTest {
        fakeItemRepository.setShouldError(true)
        val viewModel = viewModel("1")

        viewModel.uiState.test {
            assertEquals(ItemDetailUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is ItemDetailUiState.Error)
            assertTrue((error as ItemDetailUiState.Error).retryable)
        }
    }
}
