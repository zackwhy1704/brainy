package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.ItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseItemsApi {

    @POST("rest/v1/items")
    suspend fun insertItem(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body item: List<ItemDto>,
    ): Response<Unit>
}
