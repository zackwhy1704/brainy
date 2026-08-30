package com.zackwhye.secondbrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zackwhye.secondbrain.core.database.entity.BriefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BriefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(brief: BriefEntity)

    @Query("SELECT * FROM briefs WHERE itemId = :itemId")
    fun observeByItemId(itemId: String): Flow<BriefEntity?>

    @Query("SELECT * FROM briefs")
    fun observeAll(): Flow<List<BriefEntity>>
}
