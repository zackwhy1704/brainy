package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors Postgres `facts`. Append-only server-side; Room just caches whatever synced down. */
@Entity(
    tableName = "facts",
    indices = [Index(value = ["subject"]), Index(value = ["sourceItemId"])],
)
data class FactEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val category: String,
    val value: String,
    val quote: String,
    val confidence: Float?,
    val validFrom: Long,
    val supersededBy: String?,
    val sourceItemId: String,
    val createdAt: Long,
)
