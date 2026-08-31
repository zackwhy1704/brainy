package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.FactDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SupabaseFactsApi {

    /** All of the caller's facts — RLS scopes the table to auth.uid(), so no explicit user filter. */
    @GET("rest/v1/facts")
    suspend fun getFacts(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "valid_from.desc",
    ): Response<List<FactDto>>
}
