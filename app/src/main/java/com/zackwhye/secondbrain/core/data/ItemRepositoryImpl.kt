package com.zackwhye.secondbrain.core.data

import android.util.Log
import com.zackwhye.secondbrain.BuildConfig
import com.zackwhye.secondbrain.core.database.dao.ItemDao
import com.zackwhye.secondbrain.core.database.entity.ItemEntity
import com.zackwhye.secondbrain.core.database.entity.ItemSyncState
import com.zackwhye.secondbrain.core.di.ApplicationScope
import com.zackwhye.secondbrain.core.model.CapturedContext
import com.zackwhye.secondbrain.core.model.Item
import com.zackwhye.secondbrain.core.model.ItemSourceType
import com.zackwhye.secondbrain.core.network.AuthSessionManager
import com.zackwhye.secondbrain.core.network.withAuthRetry
import com.zackwhye.secondbrain.core.network.api.SupabaseItemsApi
import com.zackwhye.secondbrain.core.network.api.SupabaseStorageApi
import com.zackwhye.secondbrain.core.network.dto.ItemDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val authSessionManager: AuthSessionManager,
    private val itemsApi: SupabaseItemsApi,
    private val storageApi: SupabaseStorageApi,
    @ApplicationScope private val externalScope: CoroutineScope,
) : ItemRepository {

    override fun observeItems(): Flow<List<Item>> =
        itemDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeItem(id: String): Flow<Item?> =
        itemDao.observeById(id).map { it?.toDomain() }

    override suspend fun saveCapturedItem(context: CapturedContext): String {
        // No network call here — capture must be instant and work offline (ARCHITECTURE.md capture
        // flow). userId is unknown until the first successful sync resolves it (see sync() below).
        val id = UUID.randomUUID().toString()
        val now = Instant.now()
        val entity = ItemEntity(
            id = id,
            userId = null,
            sourceType = context.sourceType,
            sourceDoor = context.door,
            sourceUri = context.sourceUri,
            rawText = context.rawText,
            title = null,
            projectId = null,
            syncState = ItemSyncState.PENDING,
            capturedAt = context.capturedAt.toEpochMilli(),
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )
        itemDao.insert(entity)
        externalScope.launch { sync(id, context) }
        return id
    }

    override suspend fun retryFailedSyncs() {
        itemDao.getFailed().forEach { entity ->
            val context = CapturedContext(
                door = entity.sourceDoor,
                sourceType = entity.sourceType,
                sourceUri = entity.sourceUri,
                rawText = entity.rawText,
                capturedAt = Instant.ofEpochMilli(entity.capturedAt),
                mimeType = entity.recoveredMimeType(),
            )
            sync(entity.id, context)
        }
    }

    private suspend fun sync(id: String, context: CapturedContext) {
        try {
            val userId = authSessionManager.ensureUserId()

            val remoteSourceUri = if (context.sourceType == ItemSourceType.IMAGE || context.sourceType == ItemSourceType.PDF) {
                context.sourceUri?.let { localPath -> uploadToStorage(userId, id, localPath, context.mimeType) }
            } else {
                context.sourceUri
            }

            val isoNow = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val dto = ItemDto(
                id = id,
                userId = userId,
                sourceType = context.sourceType.name.lowercase(),
                sourceDoor = context.door.name.lowercase(),
                sourceUri = remoteSourceUri,
                rawText = context.rawText,
                title = null,
                capturedAt = DateTimeFormatter.ISO_INSTANT.format(context.capturedAt),
                createdAt = isoNow,
                updatedAt = isoNow,
            )

            val insertResponse = authSessionManager.withAuthRetry { bearer ->
                itemsApi.insertItem(authorization = bearer, apiKey = BuildConfig.SUPABASE_ANON_KEY, item = listOf(dto))
            }
            if (!insertResponse.isSuccessful) throw HttpException(insertResponse)

            itemDao.getById(id)?.let { itemDao.update(it.copy(userId = userId, syncState = ItemSyncState.SYNCED)) }
            Log.i(TAG, "sync succeeded for item $id (user $userId)")
        } catch (e: CancellationException) {
            throw e // structured concurrency — a cancelled sync is not a failed sync
        } catch (e: Exception) {
            Log.e(TAG, "sync failed for item $id", e)
            itemDao.getById(id)?.let { itemDao.update(it.copy(syncState = ItemSyncState.FAILED)) }
        }
    }

    private suspend fun uploadToStorage(userId: String, itemId: String, localPath: String, mimeType: String?): String {
        val file = File(localPath)
        val contentType = mimeType ?: "application/octet-stream"
        val objectPath = "$userId/$itemId"
        val response = authSessionManager.withAuthRetry { bearer ->
            storageApi.upload(
                bucket = "captures",
                path = objectPath,
                authorization = bearer,
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                contentType = contentType,
                file = file.asRequestBody(contentType.toMediaType()),
            )
        }
        if (!response.isSuccessful) throw HttpException(response)
        return objectPath
    }

    private companion object {
        const val TAG = "ItemRepository"
    }
}

/** Recovers the original upload Content-Type for a retried IMAGE/PDF sync — ItemEntity never
 * persists mimeType itself (only CapturedContext carries it at initial-capture time), but
 * MainActivity.copyToLocalFile names the file by extension, so the local path recovers it. */
private fun ItemEntity.recoveredMimeType(): String? = when (sourceType) {
    ItemSourceType.PDF -> "application/pdf"
    ItemSourceType.IMAGE -> if (sourceUri?.endsWith(".png") == true) "image/png" else "image/jpeg"
    else -> null
}

private fun ItemEntity.toDomain(): Item = Item(
    id = id,
    sourceType = sourceType,
    sourceDoor = sourceDoor,
    sourceUri = sourceUri,
    rawText = rawText,
    title = title,
    capturedAt = Instant.ofEpochMilli(capturedAt),
)
