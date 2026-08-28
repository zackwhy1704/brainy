package com.zackwhye.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemLinkType { RELATES_TO, SUPPORTS, CONTRADICTS }

/** Written by nobody in this build — exists so a future linker feature is additive. */
@Entity(tableName = "item_links")
data class ItemLinkEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fromItemId: String,
    val toItemId: String,
    val linkType: ItemLinkType,
    val createdAt: Long,
)
