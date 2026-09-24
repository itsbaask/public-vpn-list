package com.corvus.vpn

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import de.blinkt.openvpn.core.ICSOpenVPNApplication

@HiltAndroidApp
class CorvusApplication : ICSOpenVPNApplication(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleDailySync()
    }

    private fun scheduleDailySync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = androidx.work.PeriodicWorkRequestBuilder<com.corvus.vpn.worker.SyncWorker>(
            4, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PeriodicVpnSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }
}
