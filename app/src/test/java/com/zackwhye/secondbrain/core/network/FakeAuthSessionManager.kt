package com.zackwhye.secondbrain.core.network

class FakeAuthSessionManager : AuthSessionManager {

    var shouldFail: Boolean = false

    override suspend fun ensureUserId(): String {
        if (shouldFail) throw java.net.UnknownHostException("Simulated DNS failure")
        return "fake-user-id"
    }

    override suspend fun ensureAccessToken(): String {
        if (shouldFail) throw java.net.UnknownHostException("Simulated DNS failure")
        return "fake-access-token"
    }
}
