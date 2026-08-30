package com.zackwhye.secondbrain.core.network

interface AuthSessionManager {
    suspend fun ensureUserId(): String
    suspend fun ensureAccessToken(): String
}
