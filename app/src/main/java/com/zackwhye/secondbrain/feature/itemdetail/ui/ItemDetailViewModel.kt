package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * No repository and no nav-args plumbing yet (both land in (c)) — state
 * stays Loading. Not fake data: the honest state of "not wired yet."
 */
@HiltViewModel
class ItemDetailViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()
}
