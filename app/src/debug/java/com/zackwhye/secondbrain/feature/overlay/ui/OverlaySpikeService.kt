package com.zackwhye.secondbrain.feature.overlay.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zackwhye.secondbrain.core.designsystem.BubbleDiameter
import com.zackwhye.secondbrain.core.designsystem.CardPadding
import com.zackwhye.secondbrain.core.designsystem.CardShape
import com.zackwhye.secondbrain.core.designsystem.ChipShape
import com.zackwhye.secondbrain.core.designsystem.SecondBrainTheme
import com.zackwhye.secondbrain.core.designsystem.SpacingSm
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Phase 1(d) — Door 3 platform-risk spike ONLY. No bubble state machine, no
 * panel content, no capture wiring. A Service has no Lifecycle/ViewModelStore/
 * SavedStateRegistry of its own; [LifecycleService] supplies the first for
 * free, this class supplies the other two directly (ComposeView hard-requires
 * all three attached via the ViewTree* setters or it throws when composing).
 */
class OverlaySpikeService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    override val viewModelStore: ViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var expandedState = mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.i(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // The caller used startForegroundService() (required by this service's
        // foregroundServiceType), so the platform requires startForeground() to be called
        // regardless of whether we're about to bail out — skipping it here crashes with
        // ForegroundServiceDidNotStartInTimeException even though we call stopSelf() right after.
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted — routing to Settings, not starting the overlay.")
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(settingsIntent)
            stopSelf()
            return START_NOT_STICKY
        }

        if (composeView == null) addBubbleView()
        Log.i(TAG, "onStartCommand: overlay shown")
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "onConfigurationChanged: orientation=${newConfig.orientation}")
        clampPositionToScreen()
    }

    override fun onDestroy() {
        composeView?.let { runCatching { windowManager.removeView(it) } }
        composeView = null
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun addBubbleView() {
        val view = ComposeView(this).apply {
            attachViewTreeOwners(this@OverlaySpikeService)
            setViewTreeSavedStateRegistryOwner(this@OverlaySpikeService)
            setContent {
                SecondBrainTheme {
                    val expanded by expandedState
                    OverlaySpikeContent(
                        expanded = expanded,
                        onTap = { expandedState.value = !expandedState.value },
                        onDrag = ::moveBubbleBy,
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        windowManager.addView(view, params)
        composeView = view
        layoutParams = params
    }

    /**
     * [View.OnTouchListener] set on a [ComposeView] never fires: Compose's internal
     * `AndroidComposeView` child always claims the touch dispatch for its own pointer-input
     * processing before it can reach the parent's listener (confirmed on-device — zero listener
     * callbacks even for a full drag gesture). Dragging is driven from inside the composable via
     * `Modifier.pointerInput` instead; this is the WindowManager-side half of that.
     */
    private fun moveBubbleBy(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        params.x += dx.toInt()
        params.y += dy.toInt()
        composeView?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun clampPositionToScreen() {
        val params = layoutParams ?: return
        val (screenWidth, screenHeight) = screenSize()
        params.x = params.x.coerceIn(0, (screenWidth - BUBBLE_APPROX_PX).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (screenHeight - BUBBLE_APPROX_PX).coerceAtLeast(0))
        composeView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
    }

    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val point = Point().also { windowManager.defaultDisplay.getRealSize(it) }
            point.x to point.y
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "Overlay spike", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Second Brain — overlay spike")
            .setContentText("Phase 1(d) platform-risk test. Stop via adb: am force-stop com.zackwhye.secondbrain")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val TAG = "OverlaySpikeService"
        const val CHANNEL_ID = "overlay_spike_channel"
        const val NOTIFICATION_ID = 1001
        const val BUBBLE_APPROX_PX = 200 // rough on-screen footprint incl. expanded panel width, for clamping
    }
}

/**
 * `ViewTreeLifecycleOwner.set` / `ViewTreeViewModelStoreOwner.set` are the only setters
 * androidx.lifecycle 2.10.0 ships (no extension-function replacement exists in this version), and
 * Kotlin 2.2.21 — this project's pinned compiler, see gradle/libs.versions.toml — resolves them as
 * `HIDDEN`-level deprecated: present in bytecode, invisible to Kotlin source resolution ("Unresolved
 * reference", not a deprecation warning). Rather than reflect into the hidden method, this sets the
 * exact same view tags those methods set internally — resolved by NAME through the resource table
 * instead of importing the library's own (also Kotlin-invisible) `R` class. [requireViewTreeTagId]
 * fails loudly, at attach time, if the platform ever stops shipping these ids — never silently, at
 * render time, with a bubble that has no lifecycle owner.
 */
private fun ComposeView.attachViewTreeOwners(owner: OverlaySpikeService) {
    setTag(requireViewTreeTagId("view_tree_lifecycle_owner"), owner)
    setTag(requireViewTreeTagId("view_tree_view_model_store_owner"), owner)
}

private fun View.requireViewTreeTagId(name: String): Int {
    val id = resources.getIdentifier(name, "id", context.packageName)
    check(id != 0) { "View-tree tag id '$name' not found in the merged resource table" }
    return id
}

private const val DRAG_THRESHOLD_PX = 12f

/** Manual down/move/up loop (rather than `detectTapGestures`+`detectDragGestures` stacked on the
 * same modifier) so a single gesture stream decides once, cleanly, whether it's a tap or a drag —
 * the two detector functions fight over the same pointer events if combined naively. */
private fun Modifier.dragOrTap(onTap: () -> Unit, onDrag: (Float, Float) -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown()
            var dragging = false
            var totalDx = 0f
            var totalDy = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) {
                    if (!dragging) onTap()
                    break
                }
                val dx = change.position.x - change.previousPosition.x
                val dy = change.position.y - change.previousPosition.y
                totalDx += dx
                totalDy += dy
                if (!dragging && (abs(totalDx) > DRAG_THRESHOLD_PX || abs(totalDy) > DRAG_THRESHOLD_PX)) {
                    dragging = true
                }
                if (dragging) {
                    change.consume()
                    onDrag(dx, dy)
                }
            }
        }
    }

@Composable
private fun OverlaySpikeContent(expanded: Boolean, onTap: () -> Unit, onDrag: (Float, Float) -> Unit) {
    if (expanded) {
        Surface(
            shape = CardShape,
            tonalElevation = SpacingSm,
            modifier = Modifier.width(240.dp).dragOrTap(onTap, onDrag),
        ) {
            Column(modifier = Modifier.padding(CardPadding)) {
                Text("Overlay spike", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Phase 1(d) platform-risk check — no product logic here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        Surface(shape = ChipShape, modifier = Modifier.size(BubbleDiameter).dragOrTap(onTap, onDrag)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(BubbleDiameter)) {
                Text("✦", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
