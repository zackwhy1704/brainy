package com.zackwhye.secondbrain.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the Postgres `items` table (ARCHITECTURE.md). Enum fields are lowercase to match the Postgres enum labels. */
@Serializable
data class ItemDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("source_type") val sourceType: String,
    @SerialName("source_door") val sourceDoor: String,
    @SerialName("source_uri") val sourceUri: String? = null,
    @SerialName("raw_text") val rawText: String? = null,
    val title: String? = null,
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
