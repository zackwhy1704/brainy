package com.zackwhye.secondbrain.core.model

import java.time.Instant

/** Pure domain shape — what the UI layer reads, mapped from ItemEntity. */
data class Item(
    val id: String,
    val sourceType: ItemSourceType,
    val sourceDoor: SourceDoor,
    val sourceUri: String?,
    val rawText: String?,
    val title: String?,
    val capturedAt: Instant,
)
