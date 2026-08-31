package com.zackwhye.secondbrain.feature.firstrun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zackwhye.secondbrain.core.designsystem.CardPadding
import com.zackwhye.secondbrain.core.designsystem.CardShape
import com.zackwhye.secondbrain.core.designsystem.ScreenHorizontalMargin
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.designsystem.SpacingLg
import com.zackwhye.secondbrain.core.designsystem.SpacingMd
import com.zackwhye.secondbrain.core.designsystem.SpacingXxl

/**
 * The single first-run screen: what the app does, what to share into it, and where the share
 * sheet is. No carousel, no permissions, no sign-in. Shown once; the store remembers.
 */
@Composable
fun FirstRunScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenHorizontalMargin, vertical = SpacingXxl),
            verticalArrangement = Arrangement.spacedBy(SpacingLg),
        ) {
            Text(
                text = "Second Brain",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Keeps what you come across on your phone, and answers questions about it later — with the source.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Share links, text, screenshots or PDFs into it from any app. Notes about a person are kept as facts, so you can see what changed over time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(CardPadding), verticalArrangement = Arrangement.spacedBy(SpacingMd)) {
                    Text(
                        text = "How to add something",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "In any app, tap Share, then choose Second Brain. It appears on Home within seconds and gets a short brief.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "Content you share is sent to an AI service to be summarised. Nothing is shared with other people.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(SpacingLg))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Got it", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(name = "First run", showBackground = true)
@Composable
private fun FirstRunPreview() {
    SecondBrainTheme { FirstRunScreen(onContinue = {}) }
}

@Preview(name = "First run — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FirstRunDarkPreview() {
    SecondBrainTheme(darkTheme = true) { FirstRunScreen(onContinue = {}) }
}
