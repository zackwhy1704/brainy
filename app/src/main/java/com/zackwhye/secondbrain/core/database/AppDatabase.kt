package com.zackwhye.secondbrain.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zackwhye.secondbrain.core.database.dao.BriefDao
import com.zackwhye.secondbrain.core.database.dao.FactDao
import com.zackwhye.secondbrain.core.database.dao.ItemDao
import com.zackwhye.secondbrain.core.database.entity.BriefEntity
import com.zackwhye.secondbrain.core.database.entity.DecisionEntity
import com.zackwhye.secondbrain.core.database.entity.EmbeddingEntity
import com.zackwhye.secondbrain.core.database.entity.FactEntity
import com.zackwhye.secondbrain.core.database.entity.ItemEntity
import com.zackwhye.secondbrain.core.database.entity.ItemLinkEntity
import com.zackwhye.secondbrain.core.database.entity.PersonEntity
import com.zackwhye.secondbrain.core.database.entity.ProjectEntity

/**
 * Entities mirror ARCHITECTURE.md's schema rule (schema-only for projects/people/decisions/
 * item_links in this build — see SCOPE.md). Phase 2 added BriefDao. v2 adds `facts` — versioned,
 * provenance-carrying statements about a person, synced down read-only like briefs. v3 adds
 * items.profile (per-capture extraction profile, default "general").
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
        FactEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun briefDao(): BriefDao
    abstract fun factDao(): FactDao
}
