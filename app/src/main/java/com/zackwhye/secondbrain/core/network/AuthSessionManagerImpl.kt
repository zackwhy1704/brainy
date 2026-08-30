package com.zackwhye.secondbrain.core.network

import android.content.Context
import com.zackwhye.secondbrain.BuildConfig
import com.zackwhye.secondbrain.core.network.api.SupabaseAuthApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anonymous sign-in only (SCOPE.md decision — no login UI in this build).
 * One session per install, persisted in SharedPreferences and created lazily
 * on first use, not at app startup.
 */
@Singleton
class AuthSessionManagerImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val authApi: SupabaseAuthApi,
) : AuthSessionManager {
    private val prefs = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun ensureUserId(): String = ensureSession().first

    override suspend fun ensureAccessToken(): String = ensureSession().second

    private suspend fun ensureSession(): Pair<String, String> = mutex.withLock {
        val storedUserId = prefs.getString(KEY_USER_ID, null)
        val storedAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (storedUserId != null && storedAccessToken != null) {
            return storedUserId to storedAccessToken
        }
        val session = authApi.signInAnonymously(apiKey = BuildConfig.SUPABASE_ANON_KEY)
        prefs.edit()
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .apply()
        session.user.id to session.accessToken
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
