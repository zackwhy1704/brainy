package com.zackwhye.secondbrain.feature.ask.ui

import app.cash.turbine.test
import com.zackwhye.secondbrain.core.data.FakeAskRepository
import com.zackwhye.secondbrain.core.model.AskCitation
import com.zackwhye.secondbrain.core.model.AskResult
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AskViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeAskRepository()

    @Test
    fun `starts Idle before any question is asked`() = runTest {
        val viewModel = AskViewModel(fakeRepository)
        assertEquals(AskUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `normal path — Loading then Ready with the answer and citations`() = runTest {
        fakeRepository.result = AskResult.Answered(
            answer = "The K2 compiler shipped with Kotlin 2.0.",
            citations = listOf(AskCitation(itemId = "1", title = "Kotlin (programming language)")),
        )
        val viewModel = AskViewModel(fakeRepository)

        viewModel.uiState.test {
            assertEquals(AskUiState.Idle, awaitItem())
            viewModel.ask("What is the K2 compiler?")
            assertEquals(AskUiState.Loading, awaitItem())
            val ready = awaitItem() as AskUiState.Ready
            assertEquals("The K2 compiler shipped with Kotlin 2.0.", ready.answer)
            assertEquals(1, ready.citations.size)
            assertEquals("1", ready.citations.first().itemId)
        }
    }

    @Test
    fun `empty-retrieval path — Loading then EmptyRetrieval, never a bare answer`() = runTest {
        fakeRepository.result = AskResult.NoResults
        val viewModel = AskViewModel(fakeRepository)

        viewModel.uiState.test {
            assertEquals(AskUiState.Idle, awaitItem())
            viewModel.ask("What is the airspeed velocity of an unladen swallow?")
            assertEquals(AskUiState.Loading, awaitItem())
            assertEquals(AskUiState.EmptyRetrieval, awaitItem())
        }
    }

    @Test
    fun `error path — Loading then a retryable Error, not a crash`() = runTest {
        fakeRepository.shouldThrow = true
        val viewModel = AskViewModel(fakeRepository)

        viewModel.uiState.test {
            assertEquals(AskUiState.Idle, awaitItem())
            viewModel.ask("Anything")
            assertEquals(AskUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is AskUiState.Error)
            assertTrue((error as AskUiState.Error).retryable)
        }
    }

    @Test
    fun `retry re-asks the last question`() = runTest {
        fakeRepository.shouldThrow = true
        val viewModel = AskViewModel(fakeRepository)
        viewModel.ask("What is the K2 compiler?")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.retry()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeRepository.callCount)
        assertEquals("What is the K2 compiler?", fakeRepository.lastQuestion)
    }

    @Test
    fun `a blank question is ignored, never sent`() = runTest {
        val viewModel = AskViewModel(fakeRepository)
        viewModel.ask("   ")
        assertEquals(0, fakeRepository.callCount)
        assertEquals(AskUiState.Idle, viewModel.uiState.value)
    }
}
