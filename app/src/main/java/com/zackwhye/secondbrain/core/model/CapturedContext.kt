package com.zackwhye.secondbrain.core.model

import java.time.Instant

/**
 * The coherence contract (ARCHITECTURE.md → "The four doors"): every door's
 * entry point constructs one of these and hands it to
 * SaveCapturedItemUseCase — the one funnel every door writes through.
 */
data class CapturedContext(
    val door: SourceDoor,
    val sourceType: ItemSourceType,
    val sourceUri: String?,
    val rawText: String?,
    val capturedAt: Instant,
    /** Set for IMAGE/PDF only — [sourceUri] is a local file path in that case, and this is its real MIME type for Storage upload. */
    val mimeType: String? = null,
    /** Which Edge Function extraction profile this capture is sent with. "general" unless the user
     * explicitly shared to the person-note share target (see MainActivity / the manifest alias). */
    val extractionProfile: String = "general",
)
