package com.zackwhye.secondbrain.core.model

import java.time.Instant

/**
 * One versioned, provenance-carrying statement about a person. Never mutated: a change is a new
 * Fact whose predecessor has [supersededBy] set to the new id. [subject] is the name exactly as
 * written in the source — string-match identity only, no entity resolution in this build.
 */
data class Fact(
    val id: String,
    val subject: String,
    val category: String,
    val value: String,
    val quote: String,
    val confidence: Float?,
    val validFrom: Instant,
    val supersededBy: String?,
    val sourceItemId: String,
)
