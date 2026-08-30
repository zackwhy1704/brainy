package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.Brief
import kotlinx.coroutines.flow.Flow

interface BriefRepository {

    fun observeBrief(itemId: String): Flow<Brief?>

    fun observeAllBriefs(): Flow<List<Brief>>

    /** Fetches current brief rows for every locally-known item and upserts them into Room. Poll fallback (ARCHITECTURE.md) rather than a Realtime channel. */
    suspend fun pollBriefs()

    /** Re-invokes the same Edge Function the items-insert trigger calls, for one item. */
    suspend fun retryExtraction(itemId: String)
}
