package com.zackwhye.secondbrain.feature.home.ui

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String, val retryable: Boolean) : HomeUiState
    data object Empty : HomeUiState
    data class Ready(val items: List<HomeItemUiModel>, val isRefreshing: Boolean) : HomeUiState
}

/**
 * [sourceGlyph] is a placeholder for a Material Symbols icon (DESIGN.md →
 * Iconography) — no icon library is wired yet, so a single Unicode
 * character stands in, same as the earlier design-mockup artifact used.
 */
data class HomeItemUiModel(
    val id: String,
    val title: String,
    val summary: String,
    val sourceGlyph: String,
    val topicChips: List<String>,
    val capturedAtLabel: String,
)
