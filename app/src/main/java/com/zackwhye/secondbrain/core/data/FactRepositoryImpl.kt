package com.zackwhye.secondbrain.core.data

import android.util.Log
import com.zackwhye.secondbrain.BuildConfig
import com.zackwhye.secondbrain.core.database.dao.FactDao
import com.zackwhye.secondbrain.core.database.entity.FactEntity
import com.zackwhye.secondbrain.core.model.Fact
import com.zackwhye.secondbrain.core.network.AuthSessionManager
import com.zackwhye.secondbrain.core.network.api.SupabaseFactsApi
import com.zackwhye.secondbrain.core.network.withAuthRetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject

class FactRepositoryImpl @Inject constructor(
    private val factDao: FactDao,
    private val authSessionManager: AuthSessionManager,
    private val factsApi: SupabaseFactsApi,
) : FactRepository {

    override fun observeFactsForSubject(subject: String): Flow<List<Fact>> =
        factDao.observeBySubject(subject).map { entities -> entities.map { it.toDomain() } }

    override fun observeSubjectsForItem(itemId: String): Flow<List<String>> =
        factDao.observeSubjectsForItem(itemId)

    override suspend fun pollFacts() {
        try {
            val response = authSessionManager.withAuthRetry { bearer ->
                factsApi.getFacts(authorization = bearer, apiKey = BuildConfig.SUPABASE_ANON_KEY)
            }
            if (!response.isSuccessful) throw HttpException(response)
            val entities = response.body().orEmpty().map { dto ->
                FactEntity(
                    id = dto.id,
                    subject = dto.subject,
                    category = dto.category,
                    value = dto.value,
                    quote = dto.quote,
                    confidence = dto.confidence,
                    validFrom = parseInstant(dto.validFrom).toEpochMilli(),
                    supersededBy = dto.supersededBy,
                    sourceItemId = dto.sourceItemId,
                    createdAt = parseInstant(dto.createdAt).toEpochMilli(),
                )
            }
            if (entities.isNotEmpty()) factDao.upsertAll(entities)
            Log.i(TAG, "pollFacts synced ${entities.size} facts")
        } catch (e: Exception) {
            Log.e(TAG, "pollFacts failed", e)
        }
    }

    private companion object {
        const val TAG = "FactRepository"

        /** PostgREST emits "2026-08-31T15:05:22.123456+00:00" — an offset, not a Z, so not Instant.parse. */
        fun parseInstant(iso: String): Instant = OffsetDateTime.parse(iso).toInstant()
    }
}

private fun FactEntity.toDomain(): Fact = Fact(
    id = id,
    subject = subject,
    category = category,
    value = value,
    quote = quote,
    confidence = confidence,
    validFrom = Instant.ofEpochMilli(validFrom),
    supersededBy = supersededBy,
    sourceItemId = sourceItemId,
)
