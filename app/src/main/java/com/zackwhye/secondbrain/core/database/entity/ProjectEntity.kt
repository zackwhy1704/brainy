package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Schema-only in this build: nothing creates, assigns, or displays a project yet. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)
