package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.Fact
import kotlinx.coroutines.flow.Flow

interface FactRepository {

    /** Every fact ever recorded for this subject (current and superseded), newest first. */
    fun observeFactsForSubject(subject: String): Flow<List<Fact>>

    /** Distinct person names whose facts originated from this item — the entry point into the person view. */
    fun observeSubjectsForItem(itemId: String): Flow<List<String>>

    /** Poll fallback (ARCHITECTURE.md): fetches the caller's facts and upserts them into Room. */
    suspend fun pollFacts()
}
