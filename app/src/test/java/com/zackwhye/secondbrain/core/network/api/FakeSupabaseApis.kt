package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.ItemDto
import okhttp3.RequestBody
import retrofit2.Response

class FakeSupabaseItemsApi : SupabaseItemsApi {
    var lastInserted: List<ItemDto>? = null

    override suspend fun insertItem(authorization: String, apiKey: String, prefer: String, item: List<ItemDto>): Response<Unit> {
        lastInserted = item
        return Response.success(Unit)
    }
}

class FakeSupabaseStorageApi : SupabaseStorageApi {
    override suspend fun upload(bucket: String, path: String, authorization: String, apiKey: String, contentType: String, file: RequestBody): Response<Unit> {
        return Response.success(Unit)
    }
}
