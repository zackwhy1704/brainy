package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.BriefDto
import com.zackwhye.secondbrain.core.network.dto.RetryExtractionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseBriefsApi {

    /** [itemIdFilter] is a PostgREST filter value, e.g. "in.(id1,id2,id3)". */
    @GET("rest/v1/briefs")
    suspend fun getBriefs(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Query("item_id") itemIdFilter: String,
    ): Response<List<BriefDto>>

    /** Same endpoint the items-insert Postgres trigger calls; the app calls it directly to retry. */
    @POST("functions/v1/extract-brief")
    suspend fun triggerExtraction(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Body body: RetryExtractionRequest,
    ): Response<Unit>
}
