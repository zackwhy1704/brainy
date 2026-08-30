package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.zackwhye.secondbrain.core.designsystem.CardPadding
import com.zackwhye.secondbrain.core.designsystem.CardShape
import com.zackwhye.secondbrain.core.designsystem.ScreenHorizontalMargin
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.designsystem.SpacingLg
import com.zackwhye.secondbrain.core.designsystem.SpacingMd
import com.zackwhye.secondbrain.core.designsystem.SpacingSm
import androidx.compose.material3.Surface
import com.zackwhye.secondbrain.core.designsystem.components.SecondBrainChip

@Composable
fun ItemDetailScreen(
    uiState: ItemDetailUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onRetryBrief: () -> Unit,
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
                is ItemDetailUiState.Ready -> ItemDetailReadyContent(uiState.item, onRetryBrief)
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
private fun ItemDetailReadyContent(item: ItemDetailUiModel, onRetryBrief: () -> Unit) {
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
        BriefSection(item.brief, onRetryBrief)
        Text(
            text = item.rawContent,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = SpacingLg),
        )
    }
}

@Composable
private fun BriefSection(brief: BriefUiState, onRetryBrief: () -> Unit) {
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(CardPadding)) {
            when (brief) {
                BriefUiState.Pending -> BriefPendingContent()
                is BriefUiState.Ready -> BriefReadyContent(brief)
                is BriefUiState.Failed -> BriefFailedContent(brief, onRetryBrief)
            }
        }
    }
}

@Composable
private fun BriefPendingContent() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(SpacingLg))
        Text(
            text = "Extracting a brief…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = SpacingMd),
        )
    }
}

@Composable
private fun BriefReadyContent(brief: BriefUiState.Ready) {
    Column {
        Text(brief.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (brief.topics.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                modifier = Modifier.padding(top = SpacingMd),
            ) {
                brief.topics.forEach { topic -> SecondBrainChip(text = topic) }
            }
        }
        if (brief.tasks.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = SpacingMd)) {
                Text("Tasks", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                brief.tasks.forEach { task ->
                    Text(
                        text = "• $task",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = SpacingSm),
                    )
                }
            }
        }
    }
}

@Composable
private fun BriefFailedContent(brief: BriefUiState.Failed, onRetryBrief: () -> Unit) {
    Column {
        Text(
            text = "Couldn't extract a brief for this item.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        if (brief.retryable) {
            Button(onClick = onRetryBrief, modifier = Modifier.padding(top = SpacingMd)) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ---- Previews ----

private fun sampleItem(brief: BriefUiState) = ItemDetailUiModel(
    title = "The Future of AI Accounting",
    sourceLabel = "YouTube · captured today",
    rawContent = "A long-form discussion about automated reconciliation and where AI genuinely changes the workflow versus where it's marketing.",
    brief = brief,
)

@Preview(name = "Loading", showBackground = true)
@Composable
private fun ItemDetailLoadingPreview() {
    SecondBrainTheme { ItemDetailScreen(ItemDetailUiState.Loading, {}, {}, {}) }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun ItemDetailErrorPreview() {
    SecondBrainTheme {
        ItemDetailScreen(ItemDetailUiState.Error(message = "Couldn't load this item.", retryable = true), {}, {}, {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun ItemDetailEmptyPreview() {
    SecondBrainTheme { ItemDetailScreen(ItemDetailUiState.Empty, {}, {}, {}) }
}

@Preview(name = "Ready — brief pending", showBackground = true)
@Composable
private fun ItemDetailReadyBriefPendingPreview() {
    SecondBrainTheme {
        ItemDetailScreen(ItemDetailUiState.Ready(sampleItem(BriefUiState.Pending)), {}, {}, {})
    }
}

@Preview(name = "Ready — brief ready", showBackground = true)
@Composable
private fun ItemDetailReadyBriefReadyPreview() {
    SecondBrainTheme {
        ItemDetailScreen(
            ItemDetailUiState.Ready(
                sampleItem(
                    BriefUiState.Ready(
                        summary = "A discussion of where AI genuinely helps reconciliation versus where it's marketing.",
                        entities = listOf("AI", "Reconciliation"),
                        topics = listOf("ai accounting", "automation"),
                        tasks = listOf("Watch the follow-up video"),
                        importance = 3,
                    ),
                ),
            ),
            {}, {}, {},
        )
    }
}

@Preview(name = "Ready — brief failed", showBackground = true)
@Composable
private fun ItemDetailReadyBriefFailedPreview() {
    SecondBrainTheme {
        ItemDetailScreen(
            ItemDetailUiState.Ready(sampleItem(BriefUiState.Failed(reason = "Anthropic API 529: overloaded", retryable = true))),
            {}, {}, {},
        )
    }
}

@Preview(name = "Ready — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ItemDetailReadyDarkPreview() {
    SecondBrainTheme(darkTheme = true) {
        ItemDetailScreen(
            ItemDetailUiState.Ready(
                sampleItem(
                    BriefUiState.Ready(
                        summary = "A discussion of where AI genuinely helps reconciliation versus where it's marketing.",
                        entities = emptyList(),
                        topics = listOf("ai accounting"),
                        tasks = emptyList(),
                        importance = 3,
                    ),
                ),
            ),
            {}, {}, {},
        )
    }
}
