package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.AnonymousSignInRequest
import com.zackwhye.secondbrain.core.network.dto.AuthSessionDto
import com.zackwhye.secondbrain.core.network.dto.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    suspend fun signInAnonymously(
        @Header("apikey") apiKey: String,
        @Body body: AnonymousSignInRequest = AnonymousSignInRequest(),
    ): AuthSessionDto

    @POST("auth/v1/token")
    suspend fun refreshToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Header("apikey") apiKey: String,
        @Body body: RefreshTokenRequest,
    ): AuthSessionDto
}
