package com.corvus.vpn.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("corvus_settings", Context.MODE_PRIVATE)

    var killSwitchEnabled: Boolean
        get() = prefs.getBoolean("kill_switch", false)
        set(value) = prefs.edit().putBoolean("kill_switch", value).apply()

    var autoStartEnabled: Boolean
        get() = prefs.getBoolean("auto_start", false)
        set(value) = prefs.edit().putBoolean("auto_start", value).apply()

    var persistentNotificationEnabled: Boolean
        get() = prefs.getBoolean("persistent_notification", true)
        set(value) = prefs.edit().putBoolean("persistent_notification", value).apply()

    var allowLanDevices: Boolean
        get() = prefs.getBoolean("allow_lan_devices", false)
        set(value) = prefs.edit().putBoolean("allow_lan_devices", value).apply()

    var mockGpsEnabled: Boolean
        get() = prefs.getBoolean("mock_gps_enabled", false)
        set(value) = prefs.edit().putBoolean("mock_gps_enabled", value).apply()

    var displaySpeedInNotification: Boolean
        get() = prefs.getBoolean("display_speed_notification", true)
        set(value) = prefs.edit().putBoolean("display_speed_notification", value).apply()

    var notificationToggleEnabled: Boolean
        get() = prefs.getBoolean("notification_toggle_enabled", true)
        set(value) = prefs.edit().putBoolean("notification_toggle_enabled", value).apply()

    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var vpnProtocolMode: String
        get() = prefs.getString("vpn_protocol_mode", "smart") ?: "smart"
        set(value) = prefs.edit().putString("vpn_protocol_mode", value).apply()

    private val _languageFlow = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("language", "") ?: "")
    val languageFlow: kotlinx.coroutines.flow.StateFlow<String> = _languageFlow

    var language: String
        get() = prefs.getString("language", "") ?: ""
        set(value) {
            prefs.edit().putString("language", value).commit()
            _languageFlow.value = value
        }

}
