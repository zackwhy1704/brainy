package com.zackwhye.secondbrain.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackwhye.secondbrain.core.data.ItemRepository
import com.zackwhye.secondbrain.core.model.Item
import com.zackwhye.secondbrain.core.model.ItemSourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    itemRepository: ItemRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = itemRepository.observeItems()
        .map<List<Item>, HomeUiState> { items ->
            if (items.isEmpty()) HomeUiState.Empty
            else HomeUiState.Ready(items = items.map { it.toUiModel() }, isRefreshing = false)
        }
        .catch { emit(HomeUiState.Error(message = "Couldn't load your captures.", retryable = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}

private fun Item.toUiModel(): HomeItemUiModel = HomeItemUiModel(
    id = id,
    title = title ?: when (sourceType) {
        ItemSourceType.URL -> sourceUri ?: "Untitled"
        ItemSourceType.TEXT -> rawText?.take(60) ?: "Untitled"
        ItemSourceType.IMAGE -> "Screenshot"
        ItemSourceType.PDF -> "PDF"
    },
    summary = when (sourceType) {
        ItemSourceType.URL -> sourceUri ?: ""
        ItemSourceType.TEXT -> rawText.orEmpty()
        ItemSourceType.IMAGE -> "Screenshot"
        ItemSourceType.PDF -> "PDF"
    },
    sourceGlyph = when (sourceType) {
        ItemSourceType.URL -> "↗"
        ItemSourceType.TEXT -> "✎"
        ItemSourceType.IMAGE -> "▣"
        ItemSourceType.PDF -> "▤"
    },
    topicChips = emptyList(), // no brief yet — Phase 2
    capturedAtLabel = capturedAt.toRelativeLabel(),
)

private fun Instant.toRelativeLabel(): String {
    val minutes = Duration.between(this, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} hr ago"
        else -> "${minutes / (24 * 60)} d ago"
    }
}
