package com.corvus.vpn.util

import android.app.AppOpsManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log

object MockLocationManager {
    private const val TAG = "MockLocationManager"
    private var isMockingActive = false

    fun isMockLocationAllowed(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking mock location permission: ${e.message}")
            false
        }
    }

    fun setMockLocation(context: Context, latitude: Double, longitude: Double) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                try {
                    lm.addTestProvider(
                        provider,
                        false, false, false, false,
                        true, true, true,
                        android.location.Criteria.POWER_LOW,
                        android.location.Criteria.ACCURACY_FINE
                    )
                } catch (ignored: Exception) {
                    // Test provider may already exist
                }

                try {
                    lm.setTestProviderEnabled(provider, true)
                } catch (ignored: Exception) {}

                val mockLoc = Location(provider).apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    this.altitude = 12.0
                    this.time = System.currentTimeMillis()
                    this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    this.accuracy = 2.5f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        this.bearingAccuracyDegrees = 0.1f
                        this.verticalAccuracyMeters = 0.1f
                        this.speedAccuracyMetersPerSecond = 0.01f
                    }
                }
                lm.setTestProviderLocation(provider, mockLoc)
                isMockingActive = true
                Log.d(TAG, "Mock location applied to $provider: ($latitude, $longitude)")
            } catch (e: SecurityException) {
                Log.w(TAG, "Mock location security exception (app not selected as mock location app): ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting mock location for $provider: ${e.message}")
            }
        }
    }

    fun clearMockLocation(context: Context) {
        if (!isMockingActive) return
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                lm.setTestProviderEnabled(provider, false)
                lm.removeTestProvider(provider)
                Log.d(TAG, "Mock test provider removed: $provider")
            } catch (ignored: Exception) {}
        }
        isMockingActive = false
    }
}
