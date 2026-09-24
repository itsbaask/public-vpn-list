package com.corvus.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import com.corvus.vpn.MainActivity
import com.corvus.vpn.R
import com.corvus.vpn.vpn.engines.VpnConnectionEngine
import com.corvus.vpn.vpn.model.VpnCommand
import com.corvus.vpn.vpn.model.VpnState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class CorvusVpnService : VpnService() {

    @Inject
    lateinit var connectionEngine: VpnConnectionEngine

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeConnectionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            connectionEngine.processCommand(VpnCommand.Disconnect)
        }
        return START_STICKY
    }

    private fun observeConnectionState() {
        connectionEngine.state
            .onEach { state ->
                updateNotification(state)
            }
            .launchIn(serviceScope)
    }

    private fun updateNotification(state: VpnState) {
        val notification = createNotification(state)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(state: VpnState): Notification {
        val title = when (state) {
            is VpnState.Idle -> "Corvus VPN - Disconnected"
            is VpnState.Connecting -> "Connecting to ${state.server.name}..."
            is VpnState.Connected -> "Connected to ${state.server.name}"
            is VpnState.Cancelling -> "Cancelling connection..."
            is VpnState.Disconnecting -> "Disconnecting..."
            is VpnState.Switching -> "Switching to ${state.toServer.name}..."
            is VpnState.Error -> "Connection Error"
        }

        val contentText = when (state) {
            is VpnState.Connected -> "Tap to manage connection"
            is VpnState.Error -> state.reason
            else -> "Protecting your privacy"
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn_outline) // From :openvpn module
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (state is VpnState.Connected) {
            builder.setUsesChronometer(true)
            builder.setWhen(state.connectedSince)
            
            val disconnectIntent = Intent(this, CorvusVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            val disconnectPendingIntent = PendingIntent.getService(
                this, 1, disconnectIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "Disconnect", disconnectPendingIntent)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "corvus_vpn_status"
        const val NOTIFICATION_CHANNEL_NAME = "Connection Status"
        const val ACTION_DISCONNECT = "com.corvus.vpn.ACTION_DISCONNECT"
    }
}
