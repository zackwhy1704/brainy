package com.zackwhye.secondbrain.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zackwhye.secondbrain.core.designsystem.ChipShape
import com.zackwhye.secondbrain.core.designsystem.SpacingSm
import com.zackwhye.secondbrain.core.designsystem.SpacingXs

/** Component inventory: topic/status chips. Color is never the only signal — always paired with this text. */
@Composable
fun SecondBrainChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(shape = ChipShape, color = containerColor, contentColor = contentColor, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = SpacingSm, vertical = SpacingXs),
        )
    }
}
