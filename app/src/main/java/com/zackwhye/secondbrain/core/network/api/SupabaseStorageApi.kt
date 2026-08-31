package com.zackwhye.secondbrain.core.network.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface SupabaseStorageApi {

    @PUT("storage/v1/object/{bucket}/{path}")
    suspend fun upload(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Header("Content-Type") contentType: String,
        @Body file: RequestBody,
    ): Response<Unit>

    @DELETE("storage/v1/object/{bucket}/{path}")
    suspend fun delete(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
    ): Response<Unit>
}
