package com.zackwhye.secondbrain.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the Postgres `briefs` table (ARCHITECTURE.md). */
@Serializable
data class BriefDto(
    @SerialName("item_id") val itemId: String,
    val status: String,
    val summary: String? = null,
    val entities: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val tasks: List<String> = emptyList(),
    val importance: Int? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class RetryExtractionRequest(@SerialName("item_id") val itemId: String)
