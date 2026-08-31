package com.zackwhye.secondbrain.feature.person.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [PersonViewModel] to the stateless [PersonScreen]. */
@Composable
fun PersonRoute(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSourceClick: (String) -> Unit = {},
    viewModel: PersonViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.pollFactsWhileActive() }
    PersonScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSourceClick = onSourceClick,
        modifier = modifier,
    )
}
