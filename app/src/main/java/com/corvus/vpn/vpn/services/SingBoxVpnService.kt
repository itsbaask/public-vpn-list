package com.corvus.vpn.vpn.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.corvus.vpn.MainActivity
import com.corvus.vpn.data.ServerEntity
import de.blinkt.openvpn.core.CorvusNotificationHelper
import io.nekohasekai.libbox.BoxService
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification as LibboxNotification
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SingBoxVpnService — real sing-box engine (libbox v1.10.7) for:
 * VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC, WireGuard.
 *
 * API matched exactly from javap decompilation of libbox.aar classes.
 */
class SingBoxVpnService : VpnService() {

    companion object {
        private const val TAG = "SingBoxVpnService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "singbox_vpn_channel"

        const val ACTION_START = "com.corvus.vpn.SINGBOX_START"
        const val ACTION_STOP  = "com.corvus.vpn.SINGBOX_STOP"
        const val EXTRA_CONNECTION_URI = "connection_uri"
        const val EXTRA_SERVER_NAME    = "server_name"
        const val EXTRA_SERVER_HOST    = "server_host"
        const val EXTRA_SERVER_PORT    = "server_port"
        const val EXTRA_SERVER_ID      = "server_id"
        const val EXTRA_PROTOCOL       = "protocol"

        private val _tunnelState = MutableStateFlow(TunnelState.DISCONNECTED)
        fun observeState(): StateFlow<TunnelState> = _tunnelState.asStateFlow()

        fun startTunnel(context: Context, connectionUri: String, server: ServerEntity): Boolean {
            return try {
                val intent = Intent(context, SingBoxVpnService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_CONNECTION_URI, connectionUri)
                    putExtra(EXTRA_SERVER_NAME, server.name)
                    putExtra(EXTRA_SERVER_HOST, server.host)
                    putExtra(EXTRA_SERVER_PORT, server.port)
                    putExtra(EXTRA_SERVER_ID, server.id)
                    putExtra(EXTRA_PROTOCOL, server.protocol.lowercase())
                }
                _tunnelState.value = TunnelState.CONNECTING
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(intent)
                else
                    context.startService(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start: ${e.message}")
                _tunnelState.value = TunnelState.ERROR
                false
            }
        }

        fun stopTunnel(context: Context) {
            context.startService(Intent(context, SingBoxVpnService::class.java).apply { action = ACTION_STOP })
        }
    }

    enum class TunnelState { CONNECTING, CONNECTED, DISCONNECTED, ERROR }

    private var boxService: BoxService? = null
    private var tunFd: ParcelFileDescriptor? = null
    private var serverName = "VPN"

    override fun onCreate() { super.onCreate(); CorvusNotificationHelper.createNotificationChannel(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val uri = intent.getStringExtra(EXTRA_CONNECTION_URI) ?: run {
                    _tunnelState.value = TunnelState.ERROR; stopSelf(); return START_NOT_STICKY
                }
                serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "VPN"
                startForeground(CorvusNotificationHelper.NOTIFICATION_ID, CorvusNotificationHelper.buildNotification(this, "Connecting…", false))
                startLibbox(uri)
            }
            ACTION_STOP -> { 
                stopLibbox()
                CorvusNotificationHelper.cancelNotification(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf() 
            }
        }
        return START_NOT_STICKY
    }

    private fun startLibbox(connectionUri: String) {
        try {
            val handler = SingBoxTunnelHandler.fromUri(connectionUri)
            val config  = handler.buildSingBoxConfig()
            Log.d(TAG, "sing-box starting: ${handler.scheme}://${handler.remoteHost}:${handler.remotePort}")

            // Validate config — checkConfig() throws on error, returns void on success
            try { Libbox.checkConfig(config) }
            catch (e: Exception) {
                Log.e(TAG, "Config invalid: ${e.message}")
                _tunnelState.value = TunnelState.ERROR
                return
            }

            val svc = this

            val platform = object : PlatformInterface {

                // Protect socket fd from the VPN (prevents routing loop)
                override fun autoDetectInterfaceControl(fd: Int) { svc.protect(fd) }

                // Build Android TUN device — fd is handed to sing-box
                override fun openTun(options: TunOptions): Int {
                    val builder = Builder()
                        .setSession("Corvus VPN — $serverName")
                        .setMtu(options.getMTU().coerceAtLeast(1500))
                        .addRoute("0.0.0.0", 0)
                        .addDnsServer("8.8.8.8")
                        .addDnsServer("1.1.1.1")

                    // Add IPv4 addresses
                    var hasAddr = false
                    val v4 = options.getInet4Address()
                    if (v4 != null) {
                        while (v4.hasNext()) {
                            val p = v4.next()
                            builder.addAddress(p.address(), p.prefix())
                            hasAddr = true
                        }
                    }
                    // Add IPv6 addresses
                    val v6 = options.getInet6Address()
                    if (v6 != null) {
                        while (v6.hasNext()) {
                            val p = v6.next()
                            builder.addAddress(p.address(), p.prefix())
                            builder.addRoute("::", 0)
                            hasAddr = true
                        }
                    }
                    if (!hasAddr) builder.addAddress("172.19.0.2", 30)

                    val fd = builder.establish()
                    if (fd == null) {
                        Log.e(TAG, "TUN establish() null — no VPN permission?")
                        _tunnelState.value = TunnelState.ERROR
                        return -1
                    }
                    tunFd = fd
                    Log.d(TAG, "TUN fd=${fd.fd} ✅")
                    return fd.detachFd()
                }

                // ── Required interface methods ────────────────────────────────
                override fun clearDNSCache() {}
                override fun useProcFS(): Boolean = false
                override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
                override fun usePlatformDefaultInterfaceMonitor(): Boolean = false
                override fun usePlatformInterfaceGetter(): Boolean = false
                override fun underNetworkExtension(): Boolean = false
                override fun includeAllNetworks(): Boolean = false

                override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}
                override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}

                override fun getInterfaces(): NetworkInterfaceIterator? = null

                override fun readWIFIState(): WIFIState = WIFIState("", "")

                override fun findConnectionOwner(p0: Int, p1: String?, p2: Int, p3: String?, p4: Int): Int = -1
                override fun packageNameByUid(p0: Int): String = ""
                override fun uidByPackageName(p0: String?): Int = -1

                override fun sendNotification(n: LibboxNotification?) {}
                override fun writeLog(message: String?) {
                    if (message != null && message.isNotBlank()) Log.d(TAG, "[sb] $message")
                }
            }

            boxService = Libbox.newService(config, platform)
            boxService?.start()

            _tunnelState.value = TunnelState.CONNECTED
            updateNotification(true)
            Log.d(TAG, "sing-box started ✅")

        } catch (e: Exception) {
            Log.e(TAG, "startLibbox() failed: ${e.message}", e)
            _tunnelState.value = TunnelState.ERROR
        }
    }

    private fun stopLibbox() {
        try { boxService?.close() } catch (_: Exception) {}
        boxService = null
        try { tunFd?.close() } catch (_: Exception) {}
        tunFd = null
        CorvusNotificationHelper.cancelNotification(this)
        _tunnelState.value = TunnelState.DISCONNECTED
    }

    override fun onDestroy() { stopLibbox(); super.onDestroy() }
    override fun onRevoke() { stopLibbox(); super.onRevoke() }

    private fun updateNotification(isConnected: Boolean) {
        val notification = CorvusNotificationHelper.buildNotification(
            this,
            if (isConnected) "Connected" else "Connecting",
            isConnected
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
            CorvusNotificationHelper.NOTIFICATION_ID,
            notification
        )
    }
}
