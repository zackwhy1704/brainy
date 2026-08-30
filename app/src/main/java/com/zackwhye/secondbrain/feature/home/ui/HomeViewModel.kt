package com.zackwhye.secondbrain.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackwhye.secondbrain.core.data.BriefRepository
import com.zackwhye.secondbrain.core.data.ItemRepository
import com.zackwhye.secondbrain.core.model.Brief
import com.zackwhye.secondbrain.core.model.BriefStatus
import com.zackwhye.secondbrain.core.model.Item
import com.zackwhye.secondbrain.core.model.ItemSourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 5_000L

@HiltViewModel
class HomeViewModel @Inject constructor(
    itemRepository: ItemRepository,
    private val briefRepository: BriefRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        itemRepository.observeItems(),
        briefRepository.observeAllBriefs(),
    ) { items, briefs ->
        val briefsByItemId = briefs.associateBy { it.itemId }
        if (items.isEmpty()) HomeUiState.Empty
        else HomeUiState.Ready(items = items.map { it.toUiModel(briefsByItemId[it.id]) }, isRefreshing = false)
    }
        .catch { emit(HomeUiState.Error(message = "Couldn't load your captures.", retryable = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /**
     * Poll fallback (ARCHITECTURE.md) — called from a `LaunchedEffect(Unit)` in the Route, not
     * launched in `init`. See ItemDetailViewModel.pollBriefsWhileActive for why: an unconditional
     * infinite loop started at construction time has no Compose lifecycle to cancel it under a
     * plain ViewModel unit test, and hangs the test JVM instead of failing loudly.
     */
    suspend fun pollBriefsWhileActive() {
        while (true) {
            briefRepository.pollBriefs()
            delay(POLL_INTERVAL_MS)
        }
    }
}

private fun Item.toUiModel(brief: Brief?): HomeItemUiModel = HomeItemUiModel(
    id = id,
    title = title ?: when (sourceType) {
        ItemSourceType.URL -> sourceUri ?: "Untitled"
        ItemSourceType.TEXT -> rawText?.take(60) ?: "Untitled"
        ItemSourceType.IMAGE -> "Screenshot"
        ItemSourceType.PDF -> "PDF"
    },
    summary = when {
        brief != null && brief.status == BriefStatus.READY -> brief.summary.orEmpty()
        brief != null && brief.status == BriefStatus.FAILED -> "Brief failed to generate"
        brief != null && brief.status == BriefStatus.PENDING -> "Extracting a brief…"
        else -> when (sourceType) {
            ItemSourceType.URL -> sourceUri ?: ""
            ItemSourceType.TEXT -> rawText.orEmpty()
            ItemSourceType.IMAGE -> "Screenshot"
            ItemSourceType.PDF -> "PDF"
        }
    },
    sourceGlyph = when (sourceType) {
        ItemSourceType.URL -> "↗"
        ItemSourceType.TEXT -> "✎"
        ItemSourceType.IMAGE -> "▣"
        ItemSourceType.PDF -> "▤"
    },
    topicChips = if (brief?.status == BriefStatus.READY) brief.topics else emptyList(),
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
