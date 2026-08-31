package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.database.dao.ItemDao
import com.zackwhye.secondbrain.core.database.entity.ItemEntity
import com.zackwhye.secondbrain.core.database.entity.ItemSyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeItemDao : ItemDao {

    private val items = MutableStateFlow<Map<String, ItemEntity>>(emptyMap())

    override suspend fun insert(item: ItemEntity) {
        items.value = items.value + (item.id to item)
    }

    override suspend fun update(item: ItemEntity) {
        items.value = items.value + (item.id to item)
    }

    override fun observeAll(): Flow<List<ItemEntity>> =
        items.map { it.values.sortedByDescending { entity -> entity.capturedAt } }

    override fun observeById(id: String): Flow<ItemEntity?> = items.map { it[id] }

    override suspend fun getById(id: String): ItemEntity? = items.value[id]

    override suspend fun getFailed(): List<ItemEntity> =
        items.value.values.filter { it.syncState == ItemSyncState.FAILED }

    override suspend fun delete(id: String) {
        items.value = items.value - id
    }
}
