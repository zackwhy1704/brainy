package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.CapturedContext
import com.zackwhye.secondbrain.core.model.Item
import kotlinx.coroutines.flow.Flow

interface ItemRepository {

    fun observeItems(): Flow<List<Item>>

    fun observeItem(id: String): Flow<Item?>

    /** Writes to Room immediately (instant, offline-safe), then syncs to Supabase in the background. Returns the new item's id. */
    suspend fun saveCapturedItem(context: CapturedContext): String

    /** Re-attempts sync for every item whose last sync attempt failed. Called once per app foreground-entry (retry-on-app-open). */
    suspend fun retryFailedSyncs()

    /**
     * Deletes the item everywhere: the remote row (cascading to its brief, embedding, and the facts
     * it produced — facts it merely superseded are restored as current), the uploaded storage object
     * for IMAGE/PDF, the local capture file, and the Room mirror. Remote-first: if the server delete
     * fails, nothing is removed and this returns false so the UI can say so. An item that never
     * synced is deleted locally only. Returns true when the item is gone.
     */
    suspend fun deleteItem(id: String): Boolean
}
