package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.AskResult

/** Fakes over mocks, per CLAUDE.md — a real implementation of [AskRepository] backed by in-memory state. */
class FakeAskRepository : AskRepository {

    var result: AskResult = AskResult.NoResults
    var shouldThrow = false
    var lastQuestion: String? = null
        private set
    var callCount = 0
        private set

    override suspend fun ask(question: String): AskResult {
        callCount++
        lastQuestion = question
        if (shouldThrow) throw IllegalStateException("Simulated network failure")
        return result
    }
}
