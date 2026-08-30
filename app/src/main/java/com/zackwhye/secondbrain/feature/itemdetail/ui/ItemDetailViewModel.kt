package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneId
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 5_000L

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val briefRepository: BriefRepository,
) : ViewModel() {

    private val itemId = checkNotNull(savedStateHandle.get<String>("itemId")) { "ItemDetail route requires itemId" }

    val uiState: StateFlow<ItemDetailUiState> = combine(
        itemRepository.observeItem(itemId),
        briefRepository.observeBrief(itemId),
    ) { item, brief ->
        if (item == null) ItemDetailUiState.Empty else ItemDetailUiState.Ready(item.toUiModel(brief))
    }
        .catch { emit(ItemDetailUiState.Error(message = "Couldn't load this item.", retryable = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailUiState.Loading)

    /**
     * Poll fallback (ARCHITECTURE.md) — called from a `LaunchedEffect(Unit)` in the Route, not
     * launched here in `init`. An unconditional infinite loop started at construction time runs
     * on `viewModelScope` regardless of what's testing this class; a plain ViewModel unit test
     * has no Compose lifecycle to cancel it, so it just spins forever and hangs the test JVM —
     * caught building this by `testDebugUnitTest` actually hanging, not by inspection.
     */
    suspend fun pollBriefsWhileActive() {
        while (true) {
            briefRepository.pollBriefs()
            delay(POLL_INTERVAL_MS)
        }
    }

    fun retryBrief() {
        viewModelScope.launch { briefRepository.retryExtraction(itemId) }
    }
}

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private fun ItemSourceType.toLabel(): String = when (this) {
    ItemSourceType.URL -> "URL"
    ItemSourceType.TEXT -> "Text"
    ItemSourceType.IMAGE -> "Image"
    ItemSourceType.PDF -> "PDF"
}

private fun Brief?.toUiState(): BriefUiState = when {
    this == null || status == BriefStatus.PENDING -> BriefUiState.Pending
    status == BriefStatus.FAILED -> BriefUiState.Failed(reason = failureReason, retryable = true)
    else -> BriefUiState.Ready(
        summary = summary.orEmpty(),
        entities = entities,
        topics = topics,
        tasks = tasks,
        importance = importance,
    )
}

private fun Item.toUiModel(brief: Brief?): ItemDetailUiModel = ItemDetailUiModel(
    title = title ?: when (sourceType) {
        ItemSourceType.URL -> sourceUri ?: "Untitled"
        ItemSourceType.TEXT -> rawText?.take(80) ?: "Untitled"
        ItemSourceType.IMAGE -> "Screenshot"
        ItemSourceType.PDF -> "PDF"
    },
    sourceLabel = "${sourceType.toLabel()} · captured ${dateFormatter.format(capturedAt)}",
    rawContent = when (sourceType) {
        ItemSourceType.URL -> sourceUri ?: ""
        ItemSourceType.TEXT -> rawText.orEmpty()
        ItemSourceType.IMAGE -> "Image capture."
        ItemSourceType.PDF -> "PDF capture."
    },
    brief = brief.toUiState(),
)
