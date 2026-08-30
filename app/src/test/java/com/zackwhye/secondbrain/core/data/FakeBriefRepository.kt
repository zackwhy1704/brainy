package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.Brief
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Fakes over mocks, per CLAUDE.md — a real implementation of [BriefRepository] backed by in-memory state. */
class FakeBriefRepository : BriefRepository {

    private val briefs = MutableStateFlow<List<Brief>>(emptyList())
    var retryCallCount = 0
        private set
    var pollCallCount = 0
        private set
    var lastRetriedItemId: String? = null
        private set

    fun setBriefs(newBriefs: List<Brief>) {
        briefs.value = newBriefs
    }

    override fun observeBrief(itemId: String): Flow<Brief?> = briefs.map { list -> list.firstOrNull { it.itemId == itemId } }

    override fun observeAllBriefs(): Flow<List<Brief>> = briefs

    override suspend fun pollBriefs() {
        pollCallCount++
    }

    override suspend fun retryExtraction(itemId: String) {
        retryCallCount++
        lastRetriedItemId = itemId
    }
}
