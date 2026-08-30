package com.zackwhye.secondbrain.core.network

import retrofit2.Response

/**
 * Attaches the current access token; on a 401 (proactive refresh missed it — clock skew,
 * out-of-band revocation) forces one refresh and retries exactly once. Shared between
 * ItemRepositoryImpl and BriefRepositoryImpl rather than duplicated — same non-trivial logic.
 */
suspend fun <T> AuthSessionManager.withAuthRetry(call: suspend (bearer: String) -> Response<T>): Response<T> {
    val token = ensureAccessToken()
    val response = call("Bearer $token")
    if (response.code() == 401) {
        val refreshedToken = invalidateAndRefresh()
        return call("Bearer $refreshedToken")
    }
    return response
}
