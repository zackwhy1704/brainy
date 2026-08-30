package com.zackwhye.secondbrain.core.network

import android.content.Context
import android.util.Log
import com.zackwhye.secondbrain.BuildConfig
import com.zackwhye.secondbrain.core.network.api.SupabaseAuthApi
import com.zackwhye.secondbrain.core.network.dto.AuthSessionDto
import com.zackwhye.secondbrain.core.network.dto.RefreshTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anonymous sign-in only (SCOPE.md decision — no login UI in this build).
 * One session per install, persisted in SharedPreferences and created lazily
 * on first use, not at app startup. Refreshes proactively near expiry and
 * on-demand after an unexpected 401.
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

    override suspend fun invalidateAndRefresh(): String = mutex.withLock {
        val storedRefreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val session = storedRefreshToken?.let { refresh(it) } ?: signInAnonymously()
        persist(session)
        session.accessToken
    }

    private suspend fun ensureSession(): Pair<String, String> = mutex.withLock {
        val storedUserId = prefs.getString(KEY_USER_ID, null)
        val storedAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val storedRefreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        if (storedUserId != null && storedAccessToken != null) {
            val expiry = JwtExpiry.decodeExpiryEpochSeconds(storedAccessToken)
            val nowSeconds = System.currentTimeMillis() / 1000
            val stillValid = expiry != null && expiry > nowSeconds + EXPIRY_BUFFER_SECONDS
            if (stillValid) return storedUserId to storedAccessToken

            if (storedRefreshToken != null) {
                val refreshed = runCatching { refresh(storedRefreshToken) }.getOrNull()
                if (refreshed != null) {
                    persist(refreshed)
                    return refreshed.user.id to refreshed.accessToken
                }
                // refresh token itself rejected (revoked/expired) — fall through to a fresh anonymous session
            }
        }

        val session = signInAnonymously()
        persist(session)
        session.user.id to session.accessToken
    }

    private suspend fun signInAnonymously(): AuthSessionDto =
        authApi.signInAnonymously(apiKey = BuildConfig.SUPABASE_ANON_KEY)

    private suspend fun refresh(refreshToken: String): AuthSessionDto =
        authApi.refreshToken(apiKey = BuildConfig.SUPABASE_ANON_KEY, body = RefreshTokenRequest(refreshToken))

    /**
     * A refresh-token rejection (revoked/expired) falls through to a fresh anonymous sign-in,
     * which mints a NEW user id (see [ensureSession] / [invalidateAndRefresh]). Every item this
     * install previously synced belongs to the old id and RLS hides it from the new session —
     * silently, since Room still shows the local rows. This is the account-recovery gap recorded
     * in ARCHITECTURE.md; not fixed here (that's the Phase 4 account-upgrade flow), only made
     * detectable: the orphaned id is kept and the change is logged loudly.
     */
    private fun persist(session: AuthSessionDto) {
        val priorUserId = prefs.getString(KEY_USER_ID, null)
        if (priorUserId != null && priorUserId != session.user.id) {
            Log.e(
                TAG,
                "Identity changed: user id $priorUserId -> ${session.user.id}. The refresh token " +
                    "was rejected and a new anonymous session was minted. Every item synced under " +
                    "$priorUserId is now hidden from this session by RLS (known gap, see ARCHITECTURE.md).",
            )
            val orphaned = prefs.getStringSet(KEY_ORPHANED_USER_IDS, emptySet()).orEmpty() + priorUserId
            prefs.edit().putStringSet(KEY_ORPHANED_USER_IDS, orphaned).apply()
        }
        prefs.edit()
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .apply()
    }

    private companion object {
        const val TAG = "AuthSessionManager"
        const val KEY_USER_ID = "user_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ORPHANED_USER_IDS = "orphaned_user_ids"

        /** Refresh a little before actual expiry rather than racing a request against it. */
        const val EXPIRY_BUFFER_SECONDS = 60L
    }
}
