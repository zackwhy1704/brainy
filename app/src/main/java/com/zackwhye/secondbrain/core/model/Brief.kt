package com.zackwhye.secondbrain.core.model

enum class BriefStatus { PENDING, READY, FAILED }

/** Pure domain shape — what the UI layer reads, mapped from BriefEntity. */
data class Brief(
    val itemId: String,
    val status: BriefStatus,
    val summary: String?,
    val entities: List<String>,
    val topics: List<String>,
    val tasks: List<String>,
    val importance: Int?,
    val failureReason: String?,
)
