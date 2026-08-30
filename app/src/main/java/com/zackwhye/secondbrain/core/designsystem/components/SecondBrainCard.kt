package com.zackwhye.secondbrain.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zackwhye.secondbrain.core.designsystem.CardPadding
import com.zackwhye.secondbrain.core.designsystem.CardShape
import com.zackwhye.secondbrain.core.designsystem.ElevationLevel1

/** Component inventory #1/#6 — capture card / evidence card share this shape. */
@Composable
fun SecondBrainCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = ElevationLevel1)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = CardShape, colors = colors, elevation = elevation) {
            Column(modifier = Modifier.padding(CardPadding), content = content)
        }
    } else {
        Card(modifier = modifier, shape = CardShape, colors = colors, elevation = elevation) {
            Column(modifier = Modifier.padding(CardPadding), content = content)
        }
    }
}
