package com.zackwhye.secondbrain.feature.itemdetail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    onPersonClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteConfirm: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this capture?") },
            text = { Text("Its brief and the facts recorded from it are removed too. Facts they replaced become current again. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteConfirm()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "‹ Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = ScreenHorizontalMargin, vertical = SpacingMd)
                        .clickable(onClick = onBackClick)
                        .semantics { contentDescription = "Back" },
                )
                Spacer(modifier = Modifier.weight(1f))
                if (uiState is ItemDetailUiState.Ready) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(horizontal = ScreenHorizontalMargin, vertical = SpacingMd)
                            .clickable(onClick = { showDeleteDialog = true })
                            .semantics { contentDescription = "Delete this capture" },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                ItemDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ItemDetailUiState.Error -> ItemDetailErrorContent(uiState, onRetry, Modifier.align(Alignment.Center))
                ItemDetailUiState.Empty -> ItemDetailEmptyContent(Modifier.align(Alignment.Center))
                is ItemDetailUiState.Ready -> ItemDetailReadyContent(uiState.item, onRetryBrief, onPersonClick)
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
private fun ItemDetailReadyContent(item: ItemDetailUiModel, onRetryBrief: () -> Unit, onPersonClick: (String) -> Unit) {
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
        if (item.people.isNotEmpty()) {
            Text(
                text = "People",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = SpacingLg, bottom = SpacingSm),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                verticalArrangement = Arrangement.spacedBy(SpacingSm),
            ) {
                item.people.forEach { person ->
                    Surface(
                        onClick = { onPersonClick(person) },
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = person,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = SpacingMd, vertical = SpacingSm),
                        )
                    }
                }
            }
        }
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                verticalArrangement = Arrangement.spacedBy(SpacingSm),
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
    people = listOf("Sarah Tan"),
)

@Preview(name = "Loading", showBackground = true)
@Composable
private fun ItemDetailLoadingPreview() {
    SecondBrainTheme { ItemDetailScreen(ItemDetailUiState.Loading, {}, {}, {}, {}) }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun ItemDetailErrorPreview() {
    SecondBrainTheme {
        ItemDetailScreen(ItemDetailUiState.Error(message = "Couldn't load this item.", retryable = true), {}, {}, {}, {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun ItemDetailEmptyPreview() {
    SecondBrainTheme { ItemDetailScreen(ItemDetailUiState.Empty, {}, {}, {}, {}) }
}

@Preview(name = "Ready — brief pending", showBackground = true)
@Composable
private fun ItemDetailReadyBriefPendingPreview() {
    SecondBrainTheme {
        ItemDetailScreen(ItemDetailUiState.Ready(sampleItem(BriefUiState.Pending)), {}, {}, {}, {})
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
            {}, {}, {}, {},
        )
    }
}

@Preview(name = "Ready — brief failed", showBackground = true)
@Composable
private fun ItemDetailReadyBriefFailedPreview() {
    SecondBrainTheme {
        ItemDetailScreen(
            ItemDetailUiState.Ready(sampleItem(BriefUiState.Failed(reason = "Anthropic API 529: overloaded", retryable = true))),
            {}, {}, {}, {},
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
            {}, {}, {}, {},
        )
    }
}
