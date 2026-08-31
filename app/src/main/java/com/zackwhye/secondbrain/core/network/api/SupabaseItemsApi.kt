package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.DeleteItemCascadeRequest
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

    /** Server-side cascade (SQL function, SECURITY INVOKER + RLS): deletes the item, everything
     * derived from it, and restores facts its facts had superseded. See the migration for the splice. */
    @POST("rest/v1/rpc/delete_item_cascade")
    suspend fun deleteItemCascade(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Body body: DeleteItemCascadeRequest,
    ): Response<Unit>
}
