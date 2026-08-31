package com.zackwhye.secondbrain.feature.ask.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [AskViewModel] to the stateless [AskScreen]. */
@Composable
fun AskRoute(
    modifier: Modifier = Modifier,
    onCitationClick: (String) -> Unit = {},
    viewModel: AskViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AskScreen(
        uiState = uiState,
        onAsk = viewModel::ask,
        onRetry = viewModel::retry,
        onCitationClick = onCitationClick,
        modifier = modifier,
    )
}
