package com.zackwhye.secondbrain.feature.person.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.zackwhye.secondbrain.core.designsystem.CardGap
import com.zackwhye.secondbrain.core.designsystem.CardPadding
import com.zackwhye.secondbrain.core.designsystem.CardShape
import com.zackwhye.secondbrain.core.designsystem.ScreenHorizontalMargin
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.designsystem.SpacingLg
import com.zackwhye.secondbrain.core.designsystem.SpacingMd
import com.zackwhye.secondbrain.core.designsystem.SpacingSm
import com.zackwhye.secondbrain.core.designsystem.SpacingXs
import com.zackwhye.secondbrain.core.designsystem.components.SecondBrainCard

@Composable
fun PersonScreen(
    uiState: PersonUiState,
    onBackClick: () -> Unit,
    onSourceClick: (String) -> Unit,
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
                PersonUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is PersonUiState.Error -> Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center).padding(ScreenHorizontalMargin),
                )
                is PersonUiState.Empty -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(ScreenHorizontalMargin),
                    verticalArrangement = Arrangement.spacedBy(SpacingSm),
                ) {
                    Text(
                        text = "Nothing recorded about ${uiState.subject} yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Facts come from what you share: capture a conversation or notes that mention them, and what they said about their situation will be kept here — with the quote it came from.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is PersonUiState.Ready -> PersonReadyContent(uiState.person, onSourceClick)
            }
        }
    }
}

@Composable
private fun PersonReadyContent(person: PersonUiModel, onSourceClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = SpacingLg)) {
        item(key = "header") {
            Text(
                text = person.subject,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = ScreenHorizontalMargin, vertical = SpacingSm),
            )
        }
        items(items = person.facts, key = { "${it.category}:${it.value}:${it.sourceItemId}" }) { fact ->
            FactCard(fact, onSourceClick, Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalMargin, vertical = CardGap / 2))
        }
    }
}

@Composable
private fun FactCard(fact: CurrentFactUiModel, onSourceClick: (String) -> Unit, modifier: Modifier = Modifier) {
    SecondBrainCard(onClick = { onSourceClick(fact.sourceItemId) }, modifier = modifier) {
        Text(
            text = fact.category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = fact.value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = SpacingXs),
        )
        Quote(quote = fact.quote, dateLabel = fact.validFromLabel, modifier = Modifier.padding(top = SpacingSm))

        fact.previous?.let { previous ->
            Column(modifier = Modifier.padding(top = SpacingMd)) {
                Text(
                    text = "Changed — previously:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = previous.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = SpacingXs),
                )
                Surface(
                    onClick = { onSourceClick(previous.sourceItemId) },
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = SpacingSm),
                ) {
                    Quote(quote = previous.quote, dateLabel = previous.validFromLabel, modifier = Modifier.padding(CardPadding))
                }
            }
        }
    }
}

@Composable
private fun Quote(quote: String, dateLabel: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SpacingXs)) {
        Text(
            text = "“$quote”",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Previews ----

private val samplePerson = PersonUiModel(
    subject = "Sarah Tan",
    facts = listOf(
        CurrentFactUiModel(
            category = "availability",
            value = "Open to hearing about opportunities with regional scope",
            quote = "I'd be open to hearing about something with a regional remit now.",
            validFromLabel = "Aug 17, 2026",
            sourceItemId = "2",
            previous = PreviousFactUiModel(
                value = "Not actively looking",
                quote = "I'm not actively looking right now.",
                validFromLabel = "Jul 29, 2026",
                sourceItemId = "1",
            ),
        ),
        CurrentFactUiModel(
            category = "location",
            value = "Based in Singapore",
            quote = "I'm in Singapore for the foreseeable future.",
            validFromLabel = "Jul 29, 2026",
            sourceItemId = "1",
            previous = null,
        ),
    ),
)

@Preview(name = "Loading", showBackground = true)
@Composable
private fun PersonLoadingPreview() {
    SecondBrainTheme { PersonScreen(PersonUiState.Loading, {}, {}) }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun PersonEmptyPreview() {
    SecondBrainTheme { PersonScreen(PersonUiState.Empty("Sarah Tan"), {}, {}) }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun PersonErrorPreview() {
    SecondBrainTheme { PersonScreen(PersonUiState.Error("Couldn't load this person.", retryable = true), {}, {}) }
}

@Preview(name = "Ready — with a change", showBackground = true)
@Composable
private fun PersonReadyPreview() {
    SecondBrainTheme { PersonScreen(PersonUiState.Ready(samplePerson), {}, {}) }
}

@Preview(name = "Ready — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PersonReadyDarkPreview() {
    SecondBrainTheme(darkTheme = true) { PersonScreen(PersonUiState.Ready(samplePerson), {}, {}) }
}
