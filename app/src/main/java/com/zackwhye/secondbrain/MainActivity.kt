package com.zackwhye.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Phase 0 skeleton entry point only — no screens yet. Also the future
 * ACTION_SEND / ACTION_SEND_MULTIPLE target once feature/capture lands.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecondBrainTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Text(text = "Second Brain — skeleton builds. Screens start in Phase 1.")
                }
            }
        }
    }
}
