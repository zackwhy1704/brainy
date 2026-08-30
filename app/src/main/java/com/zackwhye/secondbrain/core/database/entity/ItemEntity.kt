package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zackwhye.secondbrain.core.model.ItemSourceType
import com.zackwhye.secondbrain.core.model.SourceDoor

enum class ItemSyncState { PENDING, SYNCED, FAILED }

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    /** Null until the first successful sync — capture must not block on network auth (see chat: crash fix). */
    val userId: String?,
    val sourceType: ItemSourceType,
    val sourceDoor: SourceDoor,
    val sourceUri: String?,
    val rawText: String?,
    val title: String?,
    val projectId: String?,
    val syncState: ItemSyncState,
    val capturedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
