package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.CapturedContext
import com.zackwhye.secondbrain.core.model.Item
import kotlinx.coroutines.flow.Flow

interface ItemRepository {

    fun observeItems(): Flow<List<Item>>

    fun observeItem(id: String): Flow<Item?>

    /** Writes to Room immediately (instant, offline-safe), then syncs to Supabase in the background. Returns the new item's id. */
    suspend fun saveCapturedItem(context: CapturedContext): String
}
