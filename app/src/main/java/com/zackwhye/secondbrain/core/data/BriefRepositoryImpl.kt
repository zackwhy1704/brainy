package com.zackwhye.secondbrain.core.data

import android.util.Log
import com.zackwhye.secondbrain.BuildConfig
import com.zackwhye.secondbrain.core.database.dao.BriefDao
import com.zackwhye.secondbrain.core.database.dao.ItemDao
import com.zackwhye.secondbrain.core.database.entity.BriefEntity
import com.zackwhye.secondbrain.core.model.Brief
import com.zackwhye.secondbrain.core.model.BriefStatus
import com.zackwhye.secondbrain.core.network.AuthSessionManager
import com.zackwhye.secondbrain.core.network.dto.RetryExtractionRequest
import com.zackwhye.secondbrain.core.network.withAuthRetry
import com.zackwhye.secondbrain.core.network.api.SupabaseBriefsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class BriefRepositoryImpl @Inject constructor(
    private val briefDao: BriefDao,
    private val itemDao: ItemDao,
    private val authSessionManager: AuthSessionManager,
    private val briefsApi: SupabaseBriefsApi,
) : BriefRepository {

    override fun observeBrief(itemId: String): Flow<Brief?> =
        briefDao.observeByItemId(itemId).map { it?.toDomain() }

    override fun observeAllBriefs(): Flow<List<Brief>> =
        briefDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun pollBriefs() {
        val itemIds = itemDao.observeAll().first().map { it.id }
        if (itemIds.isEmpty()) return

        try {
            val filter = "in.(${itemIds.joinToString(",")})"
            val response = authSessionManager.withAuthRetry { bearer ->
                briefsApi.getBriefs(authorization = bearer, apiKey = BuildConfig.SUPABASE_ANON_KEY, itemIdFilter = filter)
            }
            if (!response.isSuccessful) throw HttpException(response)

            val nowMillis = Instant.now().toEpochMilli()
            response.body().orEmpty().forEach { dto ->
                briefDao.upsert(
                    BriefEntity(
                        id = UUID.randomUUID().toString(),
                        itemId = dto.itemId,
                        status = BriefStatus.valueOf(dto.status.uppercase()),
                        summary = dto.summary,
                        entities = dto.entities,
                        topics = dto.topics,
                        tasks = dto.tasks,
                        importance = dto.importance,
                        failureReason = dto.failureReason,
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "pollBriefs failed", e)
        }
    }

    override suspend fun retryExtraction(itemId: String) {
        try {
            val response = authSessionManager.withAuthRetry { bearer ->
                briefsApi.triggerExtraction(
                    authorization = bearer,
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    body = RetryExtractionRequest(itemId),
                )
            }
            if (!response.isSuccessful) throw HttpException(response)
        } catch (e: Exception) {
            Log.e(TAG, "retryExtraction failed for item $itemId", e)
        }
        pollBriefs()
    }

    private companion object {
        const val TAG = "BriefRepository"
    }
}

private fun BriefEntity.toDomain(): Brief = Brief(
    itemId = itemId,
    status = status,
    summary = summary,
    entities = entities,
    topics = topics,
    tasks = tasks,
    importance = importance,
    failureReason = failureReason,
)
