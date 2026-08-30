package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zackwhye.secondbrain.core.model.BriefStatus

@Entity(tableName = "briefs", indices = [Index(value = ["itemId"], unique = true)])
data class BriefEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val status: BriefStatus,
    val summary: String?,
    val entities: List<String>,
    val topics: List<String>,
    val tasks: List<String>,
    val importance: Int?,
    val failureReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
