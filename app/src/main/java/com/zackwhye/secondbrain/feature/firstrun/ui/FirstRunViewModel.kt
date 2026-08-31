package com.zackwhye.secondbrain.feature.firstrun.ui

import androidx.lifecycle.ViewModel
import com.zackwhye.secondbrain.core.prefs.FirstRunStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FirstRunViewModel @Inject constructor(
    private val firstRunStore: FirstRunStore,
) : ViewModel() {

    /** Persists the acknowledgement so the screen never shows again, then the Route navigates on. */
    fun acknowledge() {
        firstRunStore.markFirstRunSeen()
    }
}
