package com.zackwhye.secondbrain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zackwhye.secondbrain.core.database.dao.ItemDao
import com.zackwhye.secondbrain.core.database.entity.BriefEntity
import com.zackwhye.secondbrain.core.database.entity.DecisionEntity
import com.zackwhye.secondbrain.core.database.entity.EmbeddingEntity
import com.zackwhye.secondbrain.core.database.entity.ItemEntity
import com.zackwhye.secondbrain.core.database.entity.ItemLinkEntity
import com.zackwhye.secondbrain.core.database.entity.PersonEntity
import com.zackwhye.secondbrain.core.database.entity.ProjectEntity

/**
 * Entities mirror ARCHITECTURE.md's schema rule exactly (schema-only for
 * projects/people/decisions/item_links in this build — see SCOPE.md). Only
 * ItemDao exists — the only DAO (c) actually needs.
 */
@Database(
    entities = [
        ItemEntity::class,
        BriefEntity::class,
        EmbeddingEntity::class,
        ProjectEntity::class,
        PersonEntity::class,
        DecisionEntity::class,
        ItemLinkEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
