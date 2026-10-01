package com.corvus.vpn.vpn.engines

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class CorvusXrayVpnService : VpnService() {

    companion object {
        private const val TAG = "CorvusXrayVpnService"
        var tunFd: ParcelFileDescriptor? = null
            private set
        var instance: CorvusXrayVpnService? = null
            private set

        const val ACTION_START = "com.corvus.vpn.xray.START"
        const val ACTION_STOP = "com.corvus.vpn.xray.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "CorvusXrayVpnService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "CorvusXrayVpnService ACTION_START received")
                establishTun()
            }
            ACTION_STOP -> {
                Log.d(TAG, "CorvusXrayVpnService ACTION_STOP received")
                stopTun()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    fun establishTun(): Int {
        try {
            stopTun()
            val builder = Builder()
                .setSession("Corvus VPN Xray")
                .addAddress("26.26.26.1", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1500)

            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not disallow package: ${e.message}")
            }

            val pfd = builder.establish()
            tunFd = pfd
            val fd = pfd?.fd ?: -1
            Log.d(TAG, "TUN interface established successfully with fd=$fd")
            return fd
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish TUN interface", e)
            return -1
        }
    }

    fun stopTun() {
        try {
            tunFd?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing tunFd: ${e.message}")
        }
        tunFd = null
    }

    override fun onDestroy() {
        stopTun()
        instance = null
        super.onDestroy()
    }
}
