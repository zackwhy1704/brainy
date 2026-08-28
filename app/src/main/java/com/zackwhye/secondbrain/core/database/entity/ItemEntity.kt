package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemSourceType { URL, TEXT, IMAGE, PDF }
enum class ItemSyncState { PENDING, SYNCED, FAILED }

/** Which of the four doors (ARCHITECTURE.md) produced this row — display-only, not branching logic. */
enum class SourceDoor { SHARE, PROCESS_TEXT, ASSIST, MANUAL }

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
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
