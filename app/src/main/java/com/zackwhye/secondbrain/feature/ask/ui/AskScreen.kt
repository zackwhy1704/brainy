package com.zackwhye.secondbrain.feature.ask.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.zackwhye.secondbrain.core.designsystem.CardPadding
import com.zackwhye.secondbrain.core.designsystem.CardShape
import com.zackwhye.secondbrain.core.designsystem.ScreenHorizontalMargin
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.designsystem.SpacingLg
import com.zackwhye.secondbrain.core.designsystem.SpacingMd
import com.zackwhye.secondbrain.core.designsystem.SpacingSm

@Composable
fun AskScreen(
    uiState: AskUiState,
    onAsk: (String) -> Unit,
    onRetry: () -> Unit,
    onCitationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AskInputRow(enabled = uiState !is AskUiState.Loading, onAsk = onAsk)
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState) {
                    AskUiState.Idle -> AskIdleContent(Modifier.align(Alignment.Center))
                    AskUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is AskUiState.Ready -> AskReadyContent(uiState, onCitationClick)
                    AskUiState.EmptyRetrieval -> AskEmptyRetrievalContent(Modifier.align(Alignment.Center))
                    is AskUiState.Error -> AskErrorContent(uiState, onRetry, Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun AskInputRow(enabled: Boolean, onAsk: (String) -> Unit) {
    var question by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(horizontal = ScreenHorizontalMargin, vertical = SpacingSm)) {
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            enabled = enabled,
            singleLine = true,
            // The keyboard's own action key submits too — a stranger won't hunt for the arrow glyph.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (question.isNotBlank()) onAsk(question) }),
            placeholder = { Text("Ask your second brain…") },
            trailingIcon = {
                IconButton(onClick = { if (question.isNotBlank()) onAsk(question) }, enabled = enabled) {
                    Text("↗", style = MaterialTheme.typography.titleMedium)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AskIdleContent(modifier: Modifier = Modifier) {
    Text(
        text = "Ask a question about anything you've captured.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(ScreenHorizontalMargin),
    )
}

@Composable
private fun AskEmptyRetrievalContent(modifier: Modifier = Modifier) {
    Text(
        text = "Nothing you've captured answers that yet.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(ScreenHorizontalMargin),
    )
}

@Composable
private fun AskErrorContent(state: AskUiState.Error, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(ScreenHorizontalMargin), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(state.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        if (state.retryable) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = SpacingLg)) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AskReadyContent(state: AskUiState.Ready, onCitationClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalMargin),
    ) {
        Text(state.answer, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (state.citations.isNotEmpty()) {
            Text(
                text = "Sources",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = SpacingLg, bottom = SpacingSm),
            )
            Column(verticalArrangement = Arrangement.spacedBy(SpacingSm)) {
                state.citations.forEach { citation ->
                    Surface(
                        onClick = { onCitationClick(citation.itemId) },
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = citation.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(CardPadding),
                        )
                    }
                }
            }
        }
    }
}

// ---- Previews ----

@Preview(name = "Idle", showBackground = true)
@Composable
private fun AskScreenIdlePreview() {
    SecondBrainTheme { AskScreen(AskUiState.Idle, {}, {}, {}) }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun AskScreenLoadingPreview() {
    SecondBrainTheme { AskScreen(AskUiState.Loading, {}, {}, {}) }
}

@Preview(name = "Ready", showBackground = true)
@Composable
private fun AskScreenReadyPreview() {
    SecondBrainTheme {
        AskScreen(
            AskUiState.Ready(
                answer = "The Kotlin K2 compiler shipped with Kotlin 2.0 in May 2024, offering significant compilation speed improvements.",
                citations = listOf(AskCitationUiModel(itemId = "1", title = "https://en.wikipedia.org/wiki/Kotlin_programming_language")),
            ),
            {}, {}, {},
        )
    }
}

@Preview(name = "Empty retrieval", showBackground = true)
@Composable
private fun AskScreenEmptyRetrievalPreview() {
    SecondBrainTheme { AskScreen(AskUiState.EmptyRetrieval, {}, {}, {}) }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun AskScreenErrorPreview() {
    SecondBrainTheme { AskScreen(AskUiState.Error(message = "Couldn't get an answer.", retryable = true), {}, {}, {}) }
}

@Preview(name = "Ready — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AskScreenReadyDarkPreview() {
    SecondBrainTheme(darkTheme = true) {
        AskScreen(
            AskUiState.Ready(
                answer = "The Kotlin K2 compiler shipped with Kotlin 2.0 in May 2024.",
                citations = listOf(AskCitationUiModel(itemId = "1", title = "https://en.wikipedia.org/wiki/Kotlin_programming_language")),
            ),
            {}, {}, {},
        )
    }
}
