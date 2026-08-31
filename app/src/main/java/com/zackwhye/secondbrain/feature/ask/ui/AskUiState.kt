package com.zackwhye.secondbrain.feature.ask.ui

sealed interface AskUiState {
    data object Idle : AskUiState
    data object Loading : AskUiState
    data class Ready(val answer: String, val citations: List<AskCitationUiModel>) : AskUiState
    /** Retrieval found nothing relevant — an explicit state, never silently absent, never a
     * fallback to answering from model knowledge without a citation. */
    data object EmptyRetrieval : AskUiState
    data class Error(val message: String, val retryable: Boolean) : AskUiState
}

data class AskCitationUiModel(val itemId: String, val title: String)
