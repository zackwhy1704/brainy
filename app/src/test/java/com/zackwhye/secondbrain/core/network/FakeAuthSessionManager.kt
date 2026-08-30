package com.zackwhye.secondbrain.core.network

class FakeAuthSessionManager : AuthSessionManager {

    var shouldFail: Boolean = false
    var shouldRefreshFail: Boolean = false
    var accessToken: String = "fake-access-token"
    var refreshCallCount: Int = 0
        private set

    override suspend fun ensureUserId(): String {
        if (shouldFail) throw java.net.UnknownHostException("Simulated DNS failure")
        return "fake-user-id"
    }

    override suspend fun ensureAccessToken(): String {
        if (shouldFail) throw java.net.UnknownHostException("Simulated DNS failure")
        return accessToken
    }

    override suspend fun invalidateAndRefresh(): String {
        refreshCallCount++
        if (shouldRefreshFail) throw java.net.UnknownHostException("Simulated refresh failure")
        accessToken = "refreshed-access-token"
        return accessToken
    }
}
