package com.zackwhye.secondbrain

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.zackwhye.secondbrain.core.data.ItemRepository
import com.zackwhye.secondbrain.core.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltAndroidApp
class SecondBrainApp : Application() {

    @Inject lateinit var itemRepository: ItemRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private val startedActivityCount = AtomicInteger(0)

    override fun onCreate() {
        super.onCreate()
        // Retry-on-app-open (cheapest option, decided over WorkManager/a retry UI): a FAILED item
        // stays FAILED forever otherwise — nothing else in this app ever re-attempts it. This
        // fires once per 0->1 transition of started activities, i.e. once per foreground-entry,
        // not once per Activity (rotation/multi-activity would otherwise fire it repeatedly).
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount.getAndIncrement() == 0) {
                    applicationScope.launch { itemRepository.retryFailedSyncs() }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount.decrementAndGet()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
