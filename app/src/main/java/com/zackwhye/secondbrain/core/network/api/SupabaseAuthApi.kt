package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.AnonymousSignInRequest
import com.zackwhye.secondbrain.core.network.dto.AuthSessionDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    suspend fun signInAnonymously(
        @Header("apikey") apiKey: String,
        @Body body: AnonymousSignInRequest = AnonymousSignInRequest(),
    ): AuthSessionDto
}
