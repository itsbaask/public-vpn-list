package com.corvus.vpn

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import de.blinkt.openvpn.core.ICSOpenVPNApplication

@HiltAndroidApp
class CorvusApplication : ICSOpenVPNApplication(), Configuration.Provider {

    override fun attachBaseContext(base: android.content.Context) {
        val prefs = base.getSharedPreferences("corvus_settings", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "") ?: ""
        val context = if (lang.isNotBlank()) {
            val locale = java.util.Locale.forLanguageTag(lang)
            java.util.Locale.setDefault(locale)
            val configuration = base.resources.configuration
            configuration.setLocale(locale)
            base.createConfigurationContext(configuration)
        } else {
            base
        }
        super.attachBaseContext(context)
    }

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
            6, java.util.concurrent.TimeUnit.HOURS
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
