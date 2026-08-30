package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [ItemDetailViewModel] to the stateless [ItemDetailScreen]. Nav-args (itemId) land in (c). */
@Composable
fun ItemDetailRoute(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.pollBriefsWhileActive() }
    ItemDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetry = {},
        onRetryBrief = viewModel::retryBrief,
        modifier = modifier,
    )
}
