package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.CapturedContext
import com.zackwhye.secondbrain.core.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Fakes over mocks, per CLAUDE.md — a real implementation of [ItemRepository] backed by in-memory state. */
class FakeItemRepository : ItemRepository {

    private val items = MutableStateFlow<List<Item>>(emptyList())
    private var shouldError = false

    fun setItems(newItems: List<Item>) {
        items.value = newItems
    }

    fun setShouldError(value: Boolean) {
        shouldError = value
    }

    override fun observeItems(): Flow<List<Item>> = items.map {
        if (shouldError) throw IllegalStateException("Simulated repository failure")
        it
    }

    override fun observeItem(id: String): Flow<Item?> = items.map { list ->
        if (shouldError) throw IllegalStateException("Simulated repository failure")
        list.firstOrNull { it.id == id }
    }

    override suspend fun saveCapturedItem(context: CapturedContext): String {
        throw NotImplementedError("Not needed by current ViewModel tests")
    }
}
