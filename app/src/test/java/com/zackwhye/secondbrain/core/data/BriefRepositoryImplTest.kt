package com.zackwhye.secondbrain.core.data

import app.cash.turbine.test
import com.zackwhye.secondbrain.core.database.entity.ItemEntity
import com.zackwhye.secondbrain.core.database.entity.ItemSyncState
import com.zackwhye.secondbrain.core.model.BriefStatus
import com.zackwhye.secondbrain.core.model.ItemSourceType
import com.zackwhye.secondbrain.core.model.SourceDoor
import com.zackwhye.secondbrain.core.network.FakeAuthSessionManager
import com.zackwhye.secondbrain.core.network.api.FakeSupabaseBriefsApi
import com.zackwhye.secondbrain.core.network.dto.BriefDto
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Covers the real BriefRepositoryImpl: the poll-merge into Room, its failure paths, and retry. */
class BriefRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val briefDao = FakeBriefDao()
    private val itemDao = FakeItemDao()
    private val auth = FakeAuthSessionManager()
    private val api = FakeSupabaseBriefsApi()
    private val repository = BriefRepositoryImpl(briefDao, itemDao, auth, api)

    private suspend fun seedItem(id: String) = itemDao.insert(
        ItemEntity(
            id = id, userId = "u", sourceType = ItemSourceType.TEXT, sourceDoor = SourceDoor.SHARE,
            sourceUri = null, rawText = "t", title = null, projectId = null, syncState = ItemSyncState.SYNCED,
            capturedAt = 1, createdAt = 1, updatedAt = 1,
        ),
    )

    private fun dto(itemId: String, status: String, summary: String? = null, failureReason: String? = null) = BriefDto(
        itemId = itemId, status = status, summary = summary, entities = listOf("E"), topics = listOf("t1", "t2"),
        tasks = listOf("do it"), importance = 4, failureReason = failureReason, updatedAt = "2026-09-01T00:00:00+00:00",
    )

    @Test
    fun `pollBriefs with no local items makes no network call`() = runTest {
        repository.pollBriefs()
        assertEquals(0, api.getCallCount)
    }

    @Test
    fun `pollBriefs asks only for local item ids and upserts each returned brief with its fields mapped`() = runTest {
        seedItem("a"); seedItem("b")
        api.briefs = listOf(dto("a", "ready", summary = "Sum A"), dto("b", "failed", failureReason = "API 529"))

        repository.pollBriefs()

        assertEquals("in.(a,b)", api.lastItemIdFilter)
        val stored = briefDao.snapshot().associateBy { it.itemId }
        assertEquals(2, stored.size)
        assertEquals(BriefStatus.READY, stored.getValue("a").status)
        assertEquals("Sum A", stored.getValue("a").summary)
        assertEquals(listOf("t1", "t2"), stored.getValue("a").topics)
        assertEquals(listOf("do it"), stored.getValue("a").tasks)
        assertEquals(4, stored.getValue("a").importance)
        assertEquals(BriefStatus.FAILED, stored.getValue("b").status)
        assertEquals("API 529", stored.getValue("b").failureReason)

        repository.observeBrief("a").test {
            val brief = checkNotNull(awaitItem())
            assertEquals(BriefStatus.READY, brief.status)
            assertEquals("Sum A", brief.summary)
        }
    }

    @Test
    fun `a re-poll replaces the brief for an item instead of duplicating it`() = runTest {
        seedItem("a")
        api.briefs = listOf(dto("a", "pending"))
        repository.pollBriefs()
        assertEquals(BriefStatus.PENDING, briefDao.snapshot().single().status)

        api.briefs = listOf(dto("a", "ready", summary = "done"))
        repository.pollBriefs()

        val only = briefDao.snapshot().single() // one row per item, never two
        assertEquals(BriefStatus.READY, only.status)
        assertEquals("done", only.summary)
    }

    @Test
    fun `pollBriefs on a non-2xx response leaves local briefs untouched and does not throw`() = runTest {
        seedItem("a")
        api.briefs = listOf(dto("a", "ready", summary = "kept"))
        repository.pollBriefs()

        api.getCodesQueue.clear(); api.getCodesQueue.add(500)
        api.briefs = listOf(dto("a", "failed", failureReason = "must not land"))
        repository.pollBriefs() // must not throw

        assertEquals("kept", briefDao.snapshot().single().summary)
        assertEquals(BriefStatus.READY, briefDao.snapshot().single().status)
    }

    @Test
    fun `pollBriefs on a network failure leaves local briefs untouched and does not throw`() = runTest {
        seedItem("a")
        api.briefs = listOf(dto("a", "ready", summary = "kept"))
        repository.pollBriefs()

        auth.shouldFail = true // ensureAccessToken throws UnknownHostException
        repository.pollBriefs()

        assertEquals("kept", briefDao.snapshot().single().summary)
    }

    @Test
    fun `pollBriefs on a 401 refreshes the token once and retries`() = runTest {
        seedItem("a")
        api.briefs = listOf(dto("a", "ready"))
        api.getCodesQueue.clear(); api.getCodesQueue.add(401); api.getCodesQueue.add(200)

        repository.pollBriefs()

        assertEquals(1, auth.refreshCallCount)
        assertEquals(2, api.getCallCount)
        assertEquals(BriefStatus.READY, briefDao.snapshot().single().status)
    }

    @Test
    fun `retryExtraction posts the item id, then polls`() = runTest {
        seedItem("a")
        api.briefs = listOf(dto("a", "ready", summary = "after retry"))

        repository.retryExtraction("a")

        assertEquals(listOf("a"), api.triggeredItemIds)
        assertEquals(1, api.getCallCount)
        assertEquals("after retry", briefDao.snapshot().single().summary)
    }

    @Test
    fun `retryExtraction whose trigger fails still polls and does not throw`() = runTest {
        seedItem("a")
        api.triggerCode = 500
        api.briefs = emptyList()

        repository.retryExtraction("a") // must not throw

        assertEquals(listOf("a"), api.triggeredItemIds)
        assertEquals(1, api.getCallCount)
        assertTrue(briefDao.snapshot().isEmpty())
        assertNull(briefDao.snapshot().firstOrNull())
    }
}
