package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The vector itself is Postgres-only (pgvector). Room keeps just enough to
 * know an embedding exists for an item — no on-device similarity search in this build.
 */
@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val chunkIndex: Int,
    val model: String,
    val createdAt: Long,
)
