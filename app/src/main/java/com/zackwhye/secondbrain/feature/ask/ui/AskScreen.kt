package com.zackwhye.secondbrain.feature.ask.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zackwhye.secondbrain.core.designsystem.ScreenHorizontalMargin
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme

/** Stub only — Phase 3 wires question → embed → retrieval → cited answer. */
@Composable
fun AskScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Text(
                text = "Ask",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(ScreenHorizontalMargin),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                text = "Ask arrives in Phase 3, with citations.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ScreenHorizontalMargin),
            )
        }
    }
}

@Preview(name = "Stub", showBackground = true)
@Composable
private fun AskScreenPreview() {
    SecondBrainTheme { AskScreen() }
}

@Preview(name = "Stub — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AskScreenDarkPreview() {
    SecondBrainTheme(darkTheme = true) { AskScreen() }
}
