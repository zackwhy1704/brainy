package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Written by extraction sync; no screen reads this table in this build. */
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val itemId: String,
    val name: String,
    val createdAt: Long,
)
