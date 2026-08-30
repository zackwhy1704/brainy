package com.zackwhye.secondbrain.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [HomeViewModel] to the stateless [HomeScreen]. No navigation graph yet — callbacks are no-ops until it exists. */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Cancelled automatically when this leaves composition — the Compose-lifecycle-driven
    // counterpart to the polling loop the ViewModel deliberately does not self-start.
    LaunchedEffect(Unit) { viewModel.pollBriefsWhileActive() }
    HomeScreen(
        uiState = uiState,
        onItemClick = onItemClick,
        onSearchClick = onSearchClick,
        onRetry = {},
        modifier = modifier,
    )
}
