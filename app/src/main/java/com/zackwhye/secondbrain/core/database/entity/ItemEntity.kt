package com.zackwhye.secondbrain.core.database.entity

import androidx.room.ColumnInfo
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
    /** Extraction profile the capture was (or will be, on retry) synced with — persisted so
     * retryFailedSyncs doesn't silently downgrade a person-note share to "general". */
    @ColumnInfo(defaultValue = "general") val profile: String = "general",
    val capturedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
