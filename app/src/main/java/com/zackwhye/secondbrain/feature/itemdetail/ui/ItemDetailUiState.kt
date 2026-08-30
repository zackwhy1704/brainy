package com.zackwhye.secondbrain.feature.itemdetail.ui

sealed interface ItemDetailUiState {
    data object Loading : ItemDetailUiState
    data class Error(val message: String, val retryable: Boolean) : ItemDetailUiState
    data object Empty : ItemDetailUiState // item not found / no longer exists
    data class Ready(val item: ItemDetailUiModel) : ItemDetailUiState
}

/** Raw content only in this build (Phase 1) — no brief yet, that's Phase 2. */
data class ItemDetailUiModel(
    val title: String,
    val sourceLabel: String,
    val rawContent: String,
)
