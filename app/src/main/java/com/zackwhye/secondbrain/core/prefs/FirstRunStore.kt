package com.zackwhye.secondbrain.core.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the one-time first-run screen has been acknowledged. Interface so ViewModels can be tested with a fake. */
interface FirstRunStore {
    fun hasSeenFirstRun(): Boolean
    fun markFirstRunSeen()
}

@Singleton
class SharedPrefsFirstRunStore @Inject constructor(@ApplicationContext context: Context) : FirstRunStore {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    override fun hasSeenFirstRun(): Boolean = prefs.getBoolean(KEY_SEEN, false)

    override fun markFirstRunSeen() {
        prefs.edit().putBoolean(KEY_SEEN, true).apply()
    }

    private companion object {
        const val KEY_SEEN = "first_run_seen"
    }
}
