package com.zackwhye.secondbrain.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the Postgres `facts` table. Timestamps arrive as ISO-8601 with an offset (PostgREST). */
@Serializable
data class FactDto(
    val id: String,
    val subject: String,
    val category: String,
    val value: String,
    val quote: String,
    val confidence: Float? = null,
    @SerialName("valid_from") val validFrom: String,
    @SerialName("superseded_by") val supersededBy: String? = null,
    @SerialName("source_item_id") val sourceItemId: String,
    @SerialName("created_at") val createdAt: String,
)
