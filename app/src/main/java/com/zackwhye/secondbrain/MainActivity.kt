package com.zackwhye.secondbrain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.model.CapturedContext
import com.zackwhye.secondbrain.core.model.ItemSourceType
import com.zackwhye.secondbrain.core.model.SourceDoor
import com.zackwhye.secondbrain.feature.capture.domain.SaveCapturedItemUseCase
import com.zackwhye.secondbrain.navigation.SecondBrainNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Door 1's ACTION_SEND target, and the app's normal entry point. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var saveCapturedItem: SaveCapturedItemUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSendIntent(intent)

        setContent {
            SecondBrainTheme {
                SecondBrainNavHost()
            }
        }
    }

    /**
     * Without this, a second share while Second Brain is already the foreground task (its
     * existing task just gets brought forward, not recreated) is silently dropped — onCreate()
     * never runs again, so the new intent's content never reaches saveCapturedItem. Confirmed
     * on-device: repeated shares in quick succession only captured the first.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSendIntent(intent)
    }

    private fun handleSendIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            intentToCapturedContext(intent)?.let { context ->
                lifecycleScope.launch { saveCapturedItem(context) }
            }
        }
    }

    private fun intentToCapturedContext(intent: Intent): CapturedContext? {
        val mimeType = intent.type ?: return null
        val now = Instant.now()

        return when {
            mimeType == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
                val isUrl = text.startsWith("http://") || text.startsWith("https://")
                CapturedContext(
                    door = SourceDoor.SHARE,
                    sourceType = if (isUrl) ItemSourceType.URL else ItemSourceType.TEXT,
                    sourceUri = if (isUrl) text else null,
                    rawText = if (isUrl) null else text,
                    capturedAt = now,
                )
            }

            mimeType.startsWith("image/") || mimeType == "application/pdf" -> {
                @Suppress("DEPRECATION")
                val streamUri: Uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: return null
                val localFile = copyToLocalFile(streamUri, mimeType) ?: return null
                CapturedContext(
                    door = SourceDoor.SHARE,
                    sourceType = if (mimeType == "application/pdf") ItemSourceType.PDF else ItemSourceType.IMAGE,
                    sourceUri = localFile.absolutePath,
                    rawText = null,
                    capturedAt = now,
                    mimeType = mimeType,
                )
            }

            else -> null
        }
    }

    private fun copyToLocalFile(uri: Uri, mimeType: String): File? {
        val extension = when {
            mimeType == "application/pdf" -> "pdf"
            mimeType == "image/png" -> "png"
            else -> "jpg"
        }
        val captureDir = File(filesDir, "captures").apply { mkdirs() }
        val destination = File(captureDir, "${UUID.randomUUID()}.$extension")
        return runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            destination
        }.onFailure { e -> Log.e("MainActivity", "copyToLocalFile failed for $uri", e) }.getOrNull()
    }
}
