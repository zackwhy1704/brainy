package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.lifecycle.SavedStateHandle
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    itemRepository: ItemRepository,
) : ViewModel() {

    val uiState: StateFlow<ItemDetailUiState> = itemRepository
        .observeItem(checkNotNull(savedStateHandle.get<String>("itemId")) { "ItemDetail route requires itemId" })
        .map<Item?, ItemDetailUiState> { item ->
            if (item == null) ItemDetailUiState.Empty else ItemDetailUiState.Ready(item.toUiModel())
        }
        .catch { emit(ItemDetailUiState.Error(message = "Couldn't load this item.", retryable = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailUiState.Loading)
}

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private fun ItemSourceType.toLabel(): String = when (this) {
    ItemSourceType.URL -> "URL"
    ItemSourceType.TEXT -> "Text"
    ItemSourceType.IMAGE -> "Image"
    ItemSourceType.PDF -> "PDF"
}

private fun Item.toUiModel(): ItemDetailUiModel = ItemDetailUiModel(
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
        ItemSourceType.IMAGE -> "Image capture — brief and preview arrive in Phase 2."
        ItemSourceType.PDF -> "PDF capture — brief and preview arrive in Phase 2."
    },
)
