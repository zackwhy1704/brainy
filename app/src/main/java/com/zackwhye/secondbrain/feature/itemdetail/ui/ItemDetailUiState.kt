package com.zackwhye.secondbrain.feature.itemdetail.ui

/** One-off events (never state): navigation and transient feedback after a delete attempt. */
sealed interface ItemDetailEvent {
    data object Deleted : ItemDetailEvent
    data object DeleteFailed : ItemDetailEvent
}

sealed interface ItemDetailUiState {
    data object Loading : ItemDetailUiState
    data class Error(val message: String, val retryable: Boolean) : ItemDetailUiState
    data object Empty : ItemDetailUiState // item not found / no longer exists
    data class Ready(val item: ItemDetailUiModel) : ItemDetailUiState
}

data class ItemDetailUiModel(
    val title: String,
    val sourceLabel: String,
    val rawContent: String,
    val brief: BriefUiState,
    /** People this item produced facts about — tappable into the person view. Empty until extraction ran. */
    val people: List<String>,
)

/** Never silently absent (ARCHITECTURE.md): a brief is always exactly one of these three. */
sealed interface BriefUiState {
    data object Pending : BriefUiState
    data class Ready(
        val summary: String,
        val entities: List<String>,
        val topics: List<String>,
        val tasks: List<String>,
        val importance: Int?,
    ) : BriefUiState
    data class Failed(val reason: String?, val retryable: Boolean) : BriefUiState
}
