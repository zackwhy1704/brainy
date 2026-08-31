package com.zackwhye.secondbrain.core.prefs

/** Fakes over mocks, per CLAUDE.md — a real in-memory implementation of [FirstRunStore]. */
class FakeFirstRunStore(private var seen: Boolean = false) : FirstRunStore {
    var markCallCount = 0
        private set

    override fun hasSeenFirstRun(): Boolean = seen

    override fun markFirstRunSeen() {
        seen = true
        markCallCount++
    }
}
