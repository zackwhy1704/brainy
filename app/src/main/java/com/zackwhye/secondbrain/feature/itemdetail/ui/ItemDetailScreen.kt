package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.zackwhye.secondbrain.core.designsystem.ScreenHorizontalMargin
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.designsystem.SpacingLg
import com.zackwhye.secondbrain.core.designsystem.SpacingMd
import com.zackwhye.secondbrain.core.designsystem.SpacingSm

@Composable
fun ItemDetailScreen(
    uiState: ItemDetailUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Text(
                text = "‹ Back",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = ScreenHorizontalMargin, vertical = SpacingMd)
                    .clickable(onClick = onBackClick)
                    .semantics { contentDescription = "Back" },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                ItemDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ItemDetailUiState.Error -> ItemDetailErrorContent(uiState, onRetry, Modifier.align(Alignment.Center))
                ItemDetailUiState.Empty -> ItemDetailEmptyContent(Modifier.align(Alignment.Center))
                is ItemDetailUiState.Ready -> ItemDetailReadyContent(uiState.item)
            }
        }
    }
}

@Composable
private fun ItemDetailErrorContent(state: ItemDetailUiState.Error, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(ScreenHorizontalMargin), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(state.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        if (state.retryable) {
            Text(
                text = "Retry",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = SpacingLg).clickable(onClick = onRetry),
            )
        }
    }
}

@Composable
private fun ItemDetailEmptyContent(modifier: Modifier = Modifier) {
    Text(
        text = "This item no longer exists.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(ScreenHorizontalMargin),
    )
}

@Composable
private fun ItemDetailReadyContent(item: ItemDetailUiModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalMargin),
    ) {
        Text(item.title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = item.sourceLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = SpacingSm, bottom = SpacingLg),
        )
        Text(item.rawContent, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---- Previews ----

private val sampleItem = ItemDetailUiModel(
    title = "The Future of AI Accounting",
    sourceLabel = "YouTube · captured today",
    rawContent = "A long-form discussion about automated reconciliation and where AI genuinely changes the workflow versus where it's marketing.",
)

@Preview(name = "Loading", showBackground = true)
@Composable
private fun ItemDetailLoadingPreview() {
    SecondBrainTheme { ItemDetailScreen(ItemDetailUiState.Loading, {}, {}) }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun ItemDetailErrorPreview() {
    SecondBrainTheme {
        ItemDetailScreen(ItemDetailUiState.Error(message = "Couldn't load this item.", retryable = true), {}, {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun ItemDetailEmptyPreview() {
    SecondBrainTheme { ItemDetailScreen(ItemDetailUiState.Empty, {}, {}) }
}

@Preview(name = "Ready", showBackground = true)
@Composable
private fun ItemDetailReadyPreview() {
    SecondBrainTheme { ItemDetailScreen(ItemDetailUiState.Ready(sampleItem), {}, {}) }
}

@Preview(name = "Ready — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ItemDetailReadyDarkPreview() {
    SecondBrainTheme(darkTheme = true) { ItemDetailScreen(ItemDetailUiState.Ready(sampleItem), {}, {}) }
}
