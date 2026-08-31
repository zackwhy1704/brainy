package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.Fact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Fakes over mocks, per CLAUDE.md — a real implementation of [FactRepository] backed by in-memory state. */
class FakeFactRepository : FactRepository {

    private val facts = MutableStateFlow<List<Fact>>(emptyList())
    private var shouldError = false
    var pollCallCount = 0
        private set

    fun setFacts(newFacts: List<Fact>) {
        facts.value = newFacts
    }

    fun setShouldError(value: Boolean) {
        shouldError = value
    }

    override fun observeFactsForSubject(subject: String): Flow<List<Fact>> = facts.map { list ->
        if (shouldError) throw IllegalStateException("Simulated repository failure")
        list.filter { it.subject.equals(subject, ignoreCase = true) }.sortedByDescending { it.validFrom }
    }

    override fun observeSubjectsForItem(itemId: String): Flow<List<String>> = facts.map { list ->
        list.filter { it.sourceItemId == itemId }.map { it.subject }.distinct().sorted()
    }

    override suspend fun pollFacts() {
        pollCallCount++
    }
}
