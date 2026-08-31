package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.DeleteItemCascadeRequest
import com.zackwhye.secondbrain.core.network.dto.ItemDto
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** [responseCodesQueue] is consumed one code per call, then holds at the last entry (default: always succeed). */
class FakeSupabaseItemsApi : SupabaseItemsApi {
    var lastInserted: List<ItemDto>? = null
    val responseCodesQueue: MutableList<Int> = mutableListOf(201)
    var callCount: Int = 0
        private set

    var deleteResponseCode: Int = 204
    var deleteCallCount: Int = 0
        private set
    var lastDeletedItemId: String? = null
        private set

    override suspend fun insertItem(authorization: String, apiKey: String, prefer: String, item: List<ItemDto>): Response<Unit> {
        callCount++
        lastInserted = item
        val code = if (responseCodesQueue.size > 1) responseCodesQueue.removeAt(0) else responseCodesQueue.first()
        return if (code in 200..299) Response.success(Unit) else Response.error(code, "".toResponseBody())
    }

    override suspend fun deleteItemCascade(authorization: String, apiKey: String, body: DeleteItemCascadeRequest): Response<Unit> {
        deleteCallCount++
        lastDeletedItemId = body.itemId
        return if (deleteResponseCode in 200..299) Response.success(Unit) else Response.error(deleteResponseCode, "".toResponseBody())
    }
}

class FakeSupabaseStorageApi : SupabaseStorageApi {
    val deletedPaths: MutableList<String> = mutableListOf()

    override suspend fun upload(bucket: String, path: String, authorization: String, apiKey: String, contentType: String, file: RequestBody): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun delete(bucket: String, path: String, authorization: String, apiKey: String): Response<Unit> {
        deletedPaths.add("$bucket/$path")
        return Response.success(Unit)
    }
}
