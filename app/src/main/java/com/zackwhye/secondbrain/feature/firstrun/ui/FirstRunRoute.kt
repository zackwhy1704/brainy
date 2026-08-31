package com.zackwhye.secondbrain.feature.firstrun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/** Wires [FirstRunViewModel] to the stateless [FirstRunScreen]. */
@Composable
fun FirstRunRoute(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FirstRunViewModel = hiltViewModel(),
) {
    FirstRunScreen(
        onContinue = {
            viewModel.acknowledge()
            onContinue()
        },
        modifier = modifier,
    )
}
