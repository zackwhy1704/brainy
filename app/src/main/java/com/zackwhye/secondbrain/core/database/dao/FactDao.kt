package com.zackwhye.secondbrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zackwhye.secondbrain.core.database.entity.FactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(facts: List<FactEntity>)

    /** Case-insensitive string match on the name as written — the only identity we have. */
    @Query("SELECT * FROM facts WHERE subject = :subject COLLATE NOCASE ORDER BY validFrom DESC")
    fun observeBySubject(subject: String): Flow<List<FactEntity>>

    @Query("SELECT DISTINCT subject FROM facts WHERE sourceItemId = :itemId ORDER BY subject")
    fun observeSubjectsForItem(itemId: String): Flow<List<String>>

    // Local mirror of the server's delete_item_cascade splice — see ItemRepositoryImpl.deleteItem.

    @Query("SELECT * FROM facts WHERE sourceItemId = :itemId")
    suspend fun getBySourceItem(itemId: String): List<FactEntity>

    /** Repoints any fact superseded by [oldId] to [newValue] (null restores it as current). */
    @Query("UPDATE facts SET supersededBy = :newValue WHERE supersededBy = :oldId")
    suspend fun reassignSupersededBy(oldId: String, newValue: String?)

    @Query("DELETE FROM facts WHERE sourceItemId = :itemId")
    suspend fun deleteBySourceItem(itemId: String)
}
