package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.database.entity.ItemSyncState
import com.zackwhye.secondbrain.core.model.CapturedContext
import com.zackwhye.secondbrain.core.model.ItemSourceType
import com.zackwhye.secondbrain.core.model.SourceDoor
import com.zackwhye.secondbrain.core.network.FakeAuthSessionManager
import com.zackwhye.secondbrain.core.network.api.FakeSupabaseItemsApi
import com.zackwhye.secondbrain.core.network.api.FakeSupabaseStorageApi
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Regression test for the crash caught on-device: saveCapturedItem() used to
 * call AuthSessionManager.ensureUserId() synchronously and unguarded, so a
 * network failure (e.g. no DNS) crashed the app instead of just failing the
 * background sync. Fails without the fix (saveCapturedItem throws); passes
 * with it (item persists locally with syncState = FAILED).
 */
class ItemRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeDao = FakeItemDao()
    private val fakeAuth = FakeAuthSessionManager()
    private val fakeItemsApi = FakeSupabaseItemsApi()
    private val fakeStorageApi = FakeSupabaseStorageApi()
    private val fakeFactDao = FakeFactDao()
    private val fakeBriefDao = FakeBriefDao()

    private fun repository(scope: kotlinx.coroutines.CoroutineScope) =
        ItemRepositoryImpl(fakeDao, fakeAuth, fakeItemsApi, fakeStorageApi, fakeFactDao, fakeBriefDao, scope)

    private val sampleContext = CapturedContext(
        door = SourceDoor.SHARE,
        sourceType = ItemSourceType.URL,
        sourceUri = "https://example.com",
        rawText = null,
        capturedAt = Instant.now(),
    )

    @Test
    fun `saveCapturedItem does not throw and persists locally when auth network call fails`() = runTest(UnconfinedTestDispatcher()) {
        fakeAuth.shouldFail = true
        val repository = repository(scope = backgroundScope)

        val id = repository.saveCapturedItem(sampleContext) // must not throw
        advanceUntilIdle() // let the background sync coroutine run to completion

        val stored = fakeDao.getById(id)
        assertEquals(ItemSyncState.FAILED, stored?.syncState)
        assertNull(stored?.userId) // never resolved — auth failed
    }

    @Test
    fun `saveCapturedItem syncs successfully when auth and network succeed`() = runTest(UnconfinedTestDispatcher()) {
        fakeAuth.shouldFail = false
        val repository = repository(scope = backgroundScope)

        val id = repository.saveCapturedItem(sampleContext)
        advanceUntilIdle()

        val stored = fakeDao.getById(id)
        assertEquals(ItemSyncState.SYNCED, stored?.syncState)
        assertEquals("fake-user-id", stored?.userId)
        assertEquals(1, fakeItemsApi.lastInserted?.size)
    }

    @Test
    fun `a non-2xx response is treated as a failure, not silently marked SYNCED`() = runTest(UnconfinedTestDispatcher()) {
        // Regression: insertItem() returns Response<Unit>, which Retrofit does NOT throw on for
        // non-2xx — the old code never checked .isSuccessful, so a real server error (500 here,
        // distinct from the 401-retry path below) was silently recorded as a successful sync.
        fakeItemsApi.responseCodesQueue.clear()
        fakeItemsApi.responseCodesQueue.add(500)
        val repository = repository(scope = backgroundScope)

        val id = repository.saveCapturedItem(sampleContext)
        advanceUntilIdle()

        assertEquals(ItemSyncState.FAILED, fakeDao.getById(id)?.syncState)
    }

    @Test
    fun `a 401 triggers exactly one token refresh and retry, then succeeds`() = runTest(UnconfinedTestDispatcher()) {
        fakeItemsApi.responseCodesQueue.clear()
        fakeItemsApi.responseCodesQueue.add(401) // first attempt
        fakeItemsApi.responseCodesQueue.add(201) // retry after refresh
        val repository = repository(scope = backgroundScope)

        val id = repository.saveCapturedItem(sampleContext)
        advanceUntilIdle()

        assertEquals(ItemSyncState.SYNCED, fakeDao.getById(id)?.syncState)
        assertEquals(1, fakeAuth.refreshCallCount)
        assertEquals(2, fakeItemsApi.callCount)
    }

    @Test
    fun `a 401 where the refresh itself fails ends FAILED, not crashed`() = runTest(UnconfinedTestDispatcher()) {
        fakeItemsApi.responseCodesQueue.clear()
        fakeItemsApi.responseCodesQueue.add(401)
        fakeAuth.shouldRefreshFail = true
        val repository = repository(scope = backgroundScope)

        val id = repository.saveCapturedItem(sampleContext) // must not throw
        advanceUntilIdle()

        assertEquals(ItemSyncState.FAILED, fakeDao.getById(id)?.syncState)
        assertEquals(1, fakeAuth.refreshCallCount)
    }

    @Test
    fun `retryFailedSyncs re-attempts a FAILED item and flips it to SYNCED once the network recovers`() =
        runTest(UnconfinedTestDispatcher()) {
            fakeAuth.shouldFail = true
            val repository = repository(scope = backgroundScope)
            val id = repository.saveCapturedItem(sampleContext)
            advanceUntilIdle()
            assertEquals(ItemSyncState.FAILED, fakeDao.getById(id)?.syncState) // sanity: really failed first

            fakeAuth.shouldFail = false // "network recovers" / "app comes to foreground"
            repository.retryFailedSyncs()
            advanceUntilIdle()

            assertEquals(ItemSyncState.SYNCED, fakeDao.getById(id)?.syncState)
            assertEquals("fake-user-id", fakeDao.getById(id)?.userId)
        }

    @Test
    fun `retryFailedSyncs does not touch items that already synced`() = runTest(UnconfinedTestDispatcher()) {
        fakeAuth.shouldFail = false
        val repository = repository(scope = backgroundScope)
        val id = repository.saveCapturedItem(sampleContext)
        advanceUntilIdle()
        assertEquals(ItemSyncState.SYNCED, fakeDao.getById(id)?.syncState)
        val callsBeforeRetry = fakeItemsApi.callCount

        repository.retryFailedSyncs()
        advanceUntilIdle()

        assertEquals(callsBeforeRetry, fakeItemsApi.callCount) // no re-sync of an already-SYNCED item
    }

    // ---- per-capture extraction profile ----

    @Test
    fun `capture syncs with the context's extraction profile, defaulting to general`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository(scope = backgroundScope)

        repository.saveCapturedItem(sampleContext) // default profile
        advanceUntilIdle()
        assertEquals("general", fakeItemsApi.lastInserted?.single()?.profile)

        repository.saveCapturedItem(sampleContext.copy(extractionProfile = "relationship"))
        advanceUntilIdle()
        assertEquals("relationship", fakeItemsApi.lastInserted?.single()?.profile)
    }

    @Test
    fun `retryFailedSyncs preserves the original capture's profile instead of downgrading it`() =
        runTest(UnconfinedTestDispatcher()) {
            fakeAuth.shouldFail = true
            val repository = repository(scope = backgroundScope)
            val id = repository.saveCapturedItem(sampleContext.copy(extractionProfile = "relationship"))
            advanceUntilIdle()
            assertEquals(ItemSyncState.FAILED, fakeDao.getById(id)?.syncState)

            fakeAuth.shouldFail = false
            repository.retryFailedSyncs()
            advanceUntilIdle()

            assertEquals(ItemSyncState.SYNCED, fakeDao.getById(id)?.syncState)
            assertEquals("relationship", fakeItemsApi.lastInserted?.single()?.profile)
        }

    // ---- deleteItem ----

    private fun fact(id: String, sourceItemId: String, supersededBy: String? = null) =
        com.zackwhye.secondbrain.core.database.entity.FactEntity(
            id = id, subject = "Sarah Tan", category = "location", value = "v-$id", quote = "q",
            confidence = 0.9f, validFrom = 0L, supersededBy = supersededBy, sourceItemId = sourceItemId, createdAt = 0L,
        )

    @Test
    fun `deleteItem on a synced item calls the cascade RPC, removes local rows, and restores superseded facts`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = repository(scope = backgroundScope)
            val id = repository.saveCapturedItem(sampleContext)
            advanceUntilIdle()
            assertEquals(ItemSyncState.SYNCED, fakeDao.getById(id)?.syncState)
            // f-old (another item) was superseded by f-new (this item): deleting the item must
            // restore f-old as current — the exact local mirror of the server splice.
            fakeFactDao.upsertAll(listOf(fact("f-new", sourceItemId = id), fact("f-old", sourceItemId = "other", supersededBy = "f-new")))

            val deleted = repository.deleteItem(id)

            assertEquals(true, deleted)
            assertEquals(1, fakeItemsApi.deleteCallCount)
            assertEquals(id, fakeItemsApi.lastDeletedItemId)
            assertNull(fakeDao.getById(id))
            val remaining = fakeFactDao.snapshot()
            assertEquals(listOf("f-old"), remaining.map { it.id })
            assertNull(remaining.single().supersededBy) // current again
        }

    @Test
    fun `deleteItem returns false and removes nothing when the remote delete fails`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository(scope = backgroundScope)
        val id = repository.saveCapturedItem(sampleContext)
        advanceUntilIdle()
        fakeFactDao.upsertAll(listOf(fact("f1", sourceItemId = id)))
        fakeItemsApi.deleteResponseCode = 500

        val deleted = repository.deleteItem(id)

        assertEquals(false, deleted)
        assertEquals(ItemSyncState.SYNCED, fakeDao.getById(id)?.syncState) // still there
        assertEquals(1, fakeFactDao.snapshot().size) // facts untouched
    }

    @Test
    fun `deleteItem on a never-synced item deletes locally without any network call`() = runTest(UnconfinedTestDispatcher()) {
        fakeAuth.shouldFail = true
        val repository = repository(scope = backgroundScope)
        val id = repository.saveCapturedItem(sampleContext)
        advanceUntilIdle()
        assertEquals(ItemSyncState.FAILED, fakeDao.getById(id)?.syncState)

        val deleted = repository.deleteItem(id)

        assertEquals(true, deleted)
        assertEquals(0, fakeItemsApi.deleteCallCount)
        assertNull(fakeDao.getById(id))
    }
}
