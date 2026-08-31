package com.zackwhye.secondbrain.feature.person.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.zackwhye.secondbrain.core.data.FakeFactRepository
import com.zackwhye.secondbrain.core.model.Fact
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PersonViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeFactRepository()

    private fun handle(subject: String) = SavedStateHandle(mapOf("subject" to subject))

    private val weekAgo: Instant = Instant.parse("2026-08-17T10:00:00Z")
    private val today: Instant = weekAgo.plus(7, ChronoUnit.DAYS)

    private fun fact(
        id: String,
        category: String,
        value: String,
        quote: String,
        validFrom: Instant,
        sourceItemId: String,
        supersededBy: String? = null,
        subject: String = "Sarah Tan",
    ) = Fact(
        id = id, subject = subject, category = category, value = value, quote = quote,
        confidence = 0.9f, validFrom = validFrom, supersededBy = supersededBy, sourceItemId = sourceItemId,
    )

    @Test
    fun `a superseded fact surfaces as the previous value with both quotes and both sources`() = runTest {
        fakeRepository.setFacts(
            listOf(
                fact("old", "availability", "Not actively looking", "I'm not actively looking right now.", weekAgo, "item-1", supersededBy = "new"),
                fact("new", "availability", "Open to regional roles", "I'd be open to something with a regional remit.", today, "item-2"),
                fact("loc", "location", "Based in Singapore", "I'm in Singapore.", weekAgo, "item-1"),
            ),
        )
        val viewModel = PersonViewModel(handle("Sarah Tan"), fakeRepository)

        viewModel.uiState.test {
            assertEquals(PersonUiState.Loading, awaitItem())
            val ready = awaitItem() as PersonUiState.Ready
            assertEquals("Sarah Tan", ready.person.subject)
            // Only current (non-superseded) facts are listed: availability (new) + location.
            assertEquals(listOf("availability", "location"), ready.person.facts.map { it.category })

            val changed = ready.person.facts.first { it.category == "availability" }
            assertEquals("Open to regional roles", changed.value)
            assertEquals("I'd be open to something with a regional remit.", changed.quote)
            assertEquals("item-2", changed.sourceItemId)
            val previous = requireNotNull(changed.previous) { "changed fact must carry its predecessor" }
            assertEquals("Not actively looking", previous.value)
            assertEquals("I'm not actively looking right now.", previous.quote)
            assertEquals("item-1", previous.sourceItemId)
            assertTrue(changed.validFromLabel != previous.validFromLabel) // a week apart → different dates

            val unchanged = ready.person.facts.first { it.category == "location" }
            assertNull(unchanged.previous)
        }
    }

    @Test
    fun `subject match is case-insensitive string equality, nothing smarter`() = runTest {
        fakeRepository.setFacts(listOf(fact("a", "location", "Singapore", "q", today, "item-1", subject = "sarah tan")))
        val viewModel = PersonViewModel(handle("Sarah Tan"), fakeRepository)

        viewModel.uiState.test {
            assertEquals(PersonUiState.Loading, awaitItem())
            assertTrue(awaitItem() is PersonUiState.Ready)
        }
    }

    @Test
    fun `Loading then Empty when no facts exist for the subject`() = runTest {
        fakeRepository.setFacts(emptyList())
        val viewModel = PersonViewModel(handle("Nobody"), fakeRepository)

        viewModel.uiState.test {
            assertEquals(PersonUiState.Loading, awaitItem())
            assertEquals(PersonUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `Loading then Error when repository fails`() = runTest {
        fakeRepository.setShouldError(true)
        val viewModel = PersonViewModel(handle("Sarah Tan"), fakeRepository)

        viewModel.uiState.test {
            assertEquals(PersonUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is PersonUiState.Error)
            assertTrue((error as PersonUiState.Error).retryable)
        }
    }
}
