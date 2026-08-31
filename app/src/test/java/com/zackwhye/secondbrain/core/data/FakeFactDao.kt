package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.database.dao.FactDao
import com.zackwhye.secondbrain.core.database.entity.FactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFactDao : FactDao {

    private val facts = MutableStateFlow<Map<String, FactEntity>>(emptyMap())

    fun snapshot(): List<FactEntity> = facts.value.values.toList()

    override suspend fun upsertAll(newFacts: List<FactEntity>) {
        facts.value = facts.value + newFacts.associateBy { it.id }
    }

    override fun observeBySubject(subject: String): Flow<List<FactEntity>> =
        facts.map { map -> map.values.filter { it.subject.equals(subject, ignoreCase = true) }.sortedByDescending { it.validFrom } }

    override fun observeSubjectsForItem(itemId: String): Flow<List<String>> =
        facts.map { map -> map.values.filter { it.sourceItemId == itemId }.map { it.subject }.distinct().sorted() }

    override suspend fun getBySourceItem(itemId: String): List<FactEntity> =
        facts.value.values.filter { it.sourceItemId == itemId }

    override suspend fun reassignSupersededBy(oldId: String, newValue: String?) {
        facts.value = facts.value.mapValues { (_, fact) ->
            if (fact.supersededBy == oldId) fact.copy(supersededBy = newValue) else fact
        }
    }

    override suspend fun deleteBySourceItem(itemId: String) {
        facts.value = facts.value.filterValues { it.sourceItemId != itemId }
    }
}
