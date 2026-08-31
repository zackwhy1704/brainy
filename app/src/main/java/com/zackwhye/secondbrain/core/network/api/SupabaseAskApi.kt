package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.AskRequestDto
import com.zackwhye.secondbrain.core.network.dto.AskResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseAskApi {

    @POST("functions/v1/ask")
    suspend fun ask(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Body body: AskRequestDto,
    ): Response<AskResponseDto>
}
