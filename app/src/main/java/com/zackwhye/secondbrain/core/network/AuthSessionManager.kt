package com.zackwhye.secondbrain.core.network

interface AuthSessionManager {
    suspend fun ensureUserId(): String
    suspend fun ensureAccessToken(): String

    /** Forces a fresh token regardless of the stored expiry — used after an unexpected 401 the proactive check missed. */
    suspend fun invalidateAndRefresh(): String
}
