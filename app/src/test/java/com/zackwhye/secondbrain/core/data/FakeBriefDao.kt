package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.database.dao.BriefDao
import com.zackwhye.secondbrain.core.database.entity.BriefEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBriefDao : BriefDao {

    private val briefs = MutableStateFlow<Map<String, BriefEntity>>(emptyMap())

    fun snapshot(): List<BriefEntity> = briefs.value.values.toList()

    override suspend fun upsert(brief: BriefEntity) {
        briefs.value = briefs.value + (brief.itemId to brief)
    }

    override fun observeByItemId(itemId: String): Flow<BriefEntity?> = briefs.map { it[itemId] }

    override fun observeAll(): Flow<List<BriefEntity>> = briefs.map { it.values.toList() }

    override suspend fun deleteByItemId(itemId: String) {
        briefs.value = briefs.value - itemId
    }
}
