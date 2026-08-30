package com.zackwhye.secondbrain.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zackwhye.secondbrain.core.designsystem.CardGap
import com.zackwhye.secondbrain.core.designsystem.ChipShape
import com.zackwhye.secondbrain.core.designsystem.ScreenHorizontalMargin
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.designsystem.SpacingLg
import com.zackwhye.secondbrain.core.designsystem.SpacingMd
import com.zackwhye.secondbrain.core.designsystem.SpacingSm
import com.zackwhye.secondbrain.core.designsystem.SpacingXs
import com.zackwhye.secondbrain.core.designsystem.SpacingXxl
import com.zackwhye.secondbrain.core.designsystem.TextInputShape
import com.zackwhye.secondbrain.core.designsystem.components.SecondBrainCard
import com.zackwhye.secondbrain.core.designsystem.components.SecondBrainChip
import com.zackwhye.secondbrain.core.designsystem.components.SectionHeader

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onItemClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Text(
                text = "Second Brain",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = ScreenHorizontalMargin, vertical = SpacingMd),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                HomeUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is HomeUiState.Error -> HomeErrorContent(uiState, onRetry, Modifier.align(Alignment.Center))
                HomeUiState.Empty -> HomeEmptyContent(Modifier.align(Alignment.Center))
                is HomeUiState.Ready -> HomeReadyContent(uiState, onItemClick, onSearchClick)
            }
        }
    }
}

@Composable
private fun HomeErrorContent(state: HomeUiState.Error, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(ScreenHorizontalMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (state.retryable) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = SpacingLg)) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HomeEmptyContent(modifier: Modifier = Modifier) {
    Text(
        text = "Nothing captured yet. Share something into Second Brain to see it here.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(ScreenHorizontalMargin),
    )
}

@Composable
private fun HomeReadyContent(
    state: HomeUiState.Ready,
    onItemClick: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "search") {
            SearchBarRow(onClick = onSearchClick, modifier = Modifier.padding(horizontal = ScreenHorizontalMargin, vertical = SpacingSm))
        }
        item(key = "section-today") {
            SectionHeader(text = "Today")
        }
        items(items = state.items, key = { it.id }) { item ->
            HomeItemCard(
                item = item,
                onClick = { onItemClick(item.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenHorizontalMargin, vertical = CardGap / 2),
            )
        }
    }
}

@Composable
private fun SearchBarRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = TextInputShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Ask your second brain…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SpacingLg, vertical = SpacingMd),
        )
    }
}

@Composable
private fun HomeItemCard(item: HomeItemUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SecondBrainCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                shape = ChipShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(SpacingXxl),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = item.sourceGlyph,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.padding(start = SpacingMd)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${item.summary} · ${item.capturedAtLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = SpacingXs),
                )
            }
        }
        if (item.topicChips.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                modifier = Modifier.padding(top = SpacingMd),
            ) {
                item.topicChips.forEach { chip -> SecondBrainChip(text = chip) }
            }
        }
    }
}

// ---- Previews ----

private val sampleItems = listOf(
    HomeItemUiModel(
        id = "1",
        title = "AI accounting video",
        summary = "Captured from YouTube",
        sourceGlyph = "▶",
        topicChips = listOf("AI Accounting", "New insight"),
        capturedAtLabel = "4 min ago",
    ),
    HomeItemUiModel(
        id = "2",
        title = "Customer conversation",
        summary = "3 decisions · 2 open questions · 4 tasks",
        sourceGlyph = "◈",
        topicChips = listOf("Product"),
        capturedAtLabel = "2 hr ago",
    ),
)

@Preview(name = "Loading", showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    SecondBrainTheme { HomeScreen(HomeUiState.Loading, {}, {}, {}) }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    SecondBrainTheme {
        HomeScreen(HomeUiState.Error(message = "Couldn't load your captures. Check your connection.", retryable = true), {}, {}, {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    SecondBrainTheme { HomeScreen(HomeUiState.Empty, {}, {}, {}) }
}

@Preview(name = "Ready", showBackground = true)
@Composable
private fun HomeScreenReadyPreview() {
    SecondBrainTheme { HomeScreen(HomeUiState.Ready(items = sampleItems, isRefreshing = false), {}, {}, {}) }
}

@Preview(name = "Ready — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenReadyDarkPreview() {
    SecondBrainTheme(darkTheme = true) { HomeScreen(HomeUiState.Ready(items = sampleItems, isRefreshing = false), {}, {}, {}) }
}
