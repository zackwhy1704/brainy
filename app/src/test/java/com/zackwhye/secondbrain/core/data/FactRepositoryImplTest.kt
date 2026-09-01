package com.zackwhye.secondbrain.core.data

import app.cash.turbine.test
import com.zackwhye.secondbrain.core.network.FakeAuthSessionManager
import com.zackwhye.secondbrain.core.network.api.FakeSupabaseFactsApi
import com.zackwhye.secondbrain.core.network.dto.FactDto
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Covers the real FactRepositoryImpl — the sync-down merge that the Person view reads. The
 * supersede path is the one that matters: when the server flips an existing fact to superseded,
 * the local row must be updated in place (same id) and stay readable with its source intact.
 */
class FactRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeFactDao()
    private val auth = FakeAuthSessionManager()
    private val api = FakeSupabaseFactsApi()
    private val repository = FactRepositoryImpl(dao, auth, api)

    private fun dto(
        id: String,
        value: String,
        sourceItemId: String,
        validFrom: String,
        supersededBy: String? = null,
        subject: String = "Sarah Tan",
    ) = FactDto(
        id = id, subject = subject, category = "location", value = value, quote = "\"$value\"", confidence = 0.9f,
        validFrom = validFrom, supersededBy = supersededBy, sourceItemId = sourceItemId, createdAt = validFrom,
    )

    @Test
    fun `pollFacts maps DTOs into Room, parsing PostgREST offset timestamps`() = runTest {
        api.facts = listOf(dto("f1", "Based in Singapore", sourceItemId = "item-A", validFrom = "2026-08-31T15:05:22.123456+08:00"))

        repository.pollFacts()

        val stored = dao.snapshot().single()
        assertEquals("f1", stored.id)
        assertEquals("Sarah Tan", stored.subject)
        assertEquals("location", stored.category)
        assertEquals("Based in Singapore", stored.value)
        assertEquals("item-A", stored.sourceItemId)
        assertEquals(0.9f, checkNotNull(stored.confidence), 0.0001f)
        assertEquals(Instant.parse("2026-08-31T07:05:22.123456Z").toEpochMilli(), stored.validFrom) // +08:00 honoured
        assertNull(stored.supersededBy)
    }

    @Test
    fun `supersede path - a superseded fact stays readable with its value and source intact`() = runTest {
        // Poll 1: one current fact from item A.
        api.facts = listOf(dto("f1", "Based in Singapore", sourceItemId = "item-A", validFrom = "2026-08-01T00:00:00+00:00"))
        repository.pollFacts()

        // Poll 2: the server superseded f1 with f2 (from item B). f1 comes back with superseded_by set.
        api.facts = listOf(
            dto("f2", "Relocated to Jakarta", sourceItemId = "item-B", validFrom = "2026-08-20T00:00:00+00:00"),
            dto("f1", "Based in Singapore", sourceItemId = "item-A", validFrom = "2026-08-01T00:00:00+00:00", supersededBy = "f2"),
        )
        repository.pollFacts()

        repository.observeFactsForSubject("Sarah Tan").test {
            val facts = awaitItem()
            assertEquals(listOf("f2", "f1"), facts.map { it.id }) // both rows, newest first — history preserved

            val current = facts.single { it.supersededBy == null }
            assertEquals("f2", current.id)
            assertEquals("Relocated to Jakarta", current.value)
            assertEquals("item-B", current.sourceItemId)

            val superseded = facts.single { it.id == "f1" }
            assertEquals("f2", superseded.supersededBy)       // flipped in place, same id
            assertEquals("Based in Singapore", superseded.value) // value untouched
            assertEquals("item-A", superseded.sourceItemId)      // provenance intact
        }
        // The old source item still surfaces its person — the chip into the person view survives.
        repository.observeSubjectsForItem("item-A").test { assertEquals(listOf("Sarah Tan"), awaitItem()) }
    }

    @Test
    fun `subject lookup is case-insensitive, matching the server's string-identity rule`() = runTest {
        api.facts = listOf(dto("f1", "v", sourceItemId = "i", validFrom = "2026-08-01T00:00:00+00:00", subject = "Sarah Tan"))
        repository.pollFacts()

        repository.observeFactsForSubject("sarah tan").test { assertEquals(1, awaitItem().size) }
    }

    @Test
    fun `pollFacts on a non-2xx response leaves the cache untouched and does not throw`() = runTest {
        api.facts = listOf(dto("f1", "kept", sourceItemId = "i", validFrom = "2026-08-01T00:00:00+00:00"))
        repository.pollFacts()

        api.codesQueue.clear(); api.codesQueue.add(500)
        api.facts = listOf(dto("f1", "must not land", sourceItemId = "i", validFrom = "2026-08-01T00:00:00+00:00"))
        repository.pollFacts() // must not throw

        assertEquals("kept", dao.snapshot().single().value)
    }

    @Test
    fun `pollFacts on a network failure leaves the cache untouched and does not throw`() = runTest {
        api.facts = listOf(dto("f1", "kept", sourceItemId = "i", validFrom = "2026-08-01T00:00:00+00:00"))
        repository.pollFacts()

        auth.shouldFail = true
        repository.pollFacts()

        assertEquals("kept", dao.snapshot().single().value)
    }

    @Test
    fun `pollFacts on a 401 refreshes once and retries`() = runTest {
        api.facts = listOf(dto("f1", "v", sourceItemId = "i", validFrom = "2026-08-01T00:00:00+00:00"))
        api.codesQueue.clear(); api.codesQueue.add(401); api.codesQueue.add(200)

        repository.pollFacts()

        assertEquals(1, auth.refreshCallCount)
        assertEquals(2, api.callCount)
        assertEquals(1, dao.snapshot().size)
    }

    @Test
    fun `an empty server response does not wipe the local cache`() = runTest {
        // Documents current behaviour: sync-down is additive; local removal happens only through
        // ItemRepository.deleteItem (which mirrors the server cascade locally).
        api.facts = listOf(dto("f1", "kept", sourceItemId = "i", validFrom = "2026-08-01T00:00:00+00:00"))
        repository.pollFacts()

        api.facts = emptyList()
        repository.pollFacts()

        assertEquals(1, dao.snapshot().size)
    }
}
