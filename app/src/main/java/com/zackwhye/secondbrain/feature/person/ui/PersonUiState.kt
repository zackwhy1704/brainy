package com.zackwhye.secondbrain.feature.person.ui

sealed interface PersonUiState {
    data object Loading : PersonUiState
    data class Error(val message: String, val retryable: Boolean) : PersonUiState
    /** No facts recorded for this name (yet) — carries the name so the screen can say so honestly. */
    data class Empty(val subject: String) : PersonUiState
    data class Ready(val person: PersonUiModel) : PersonUiState
}

data class PersonUiModel(
    val subject: String,
    val facts: List<CurrentFactUiModel>,
)

/** A current (non-superseded) fact, plus — when it replaced an earlier value — what it replaced. */
data class CurrentFactUiModel(
    val category: String,
    val value: String,
    val quote: String,
    val validFromLabel: String,
    val sourceItemId: String,
    val previous: PreviousFactUiModel?,
)

data class PreviousFactUiModel(
    val value: String,
    val quote: String,
    val validFromLabel: String,
    val sourceItemId: String,
)
