package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [ItemDetailViewModel] to the stateless [ItemDetailScreen]. */
@Composable
fun ItemDetailRoute(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPersonClick: (String) -> Unit = {},
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.pollBriefsWhileActive() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ItemDetailEvent.Deleted -> onBackClick()
                ItemDetailEvent.DeleteFailed ->
                    snackbarHostState.showSnackbar("Couldn't delete — check your connection and try again.")
            }
        }
    }
    ItemDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetry = {},
        onRetryBrief = viewModel::retryBrief,
        onPersonClick = onPersonClick,
        modifier = modifier,
        onDeleteConfirm = viewModel::deleteItem,
        snackbarHostState = snackbarHostState,
    )
}
