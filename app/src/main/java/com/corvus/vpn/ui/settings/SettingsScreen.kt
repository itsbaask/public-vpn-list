package com.corvus.vpn.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.components.CorvusCard
import com.corvus.vpn.ui.components.CorvusListRow
import com.corvus.vpn.ui.components.CorvusToggle
import com.corvus.vpn.ui.theme.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    killSwitchEnabled: Boolean,
    onKillSwitchChange: (Boolean) -> Unit,
    autoStartEnabled: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
    persistentNotificationEnabled: Boolean,
    onPersistentNotificationChange: (Boolean) -> Unit,
    allowLanDevices: Boolean = false,
    onAllowLanDevicesChange: (Boolean) -> Unit = {},
    mockGpsEnabled: Boolean = false,
    onMockGpsChange: (Boolean) -> Unit = {},
    displaySpeedNotification: Boolean = true,
    onDisplaySpeedNotificationChange: (Boolean) -> Unit = {},
    notificationToggleEnabled: Boolean = true,
    onNotificationToggleChange: (Boolean) -> Unit = {},
    onImportCustomOvpnClick: () -> Unit = {},
    onProClick: () -> Unit = {},
    onMyIpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLanguageSelected: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showKillSwitchInfoDialog by remember { mutableStateOf(false) }
    var showMockGpsPermissionDialog by remember { mutableStateOf(false) }

    val languages = listOf(
        Pair("English", "en"),
        Pair("العربية", "ar"),
        Pair("Español", "es"),
        Pair("Français", "fr"),
        Pair("Deutsch", "de"),
        Pair("日本語", "ja"),
        Pair("Português", "pt"),
        Pair("Русский", "ru"),
        Pair("Türkçe", "tr"),
        Pair("中文", "zh")
    )

    Scaffold(
        containerColor = CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CrowText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_custom_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CrowBlack
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrowBlack)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // VIP Pro Banner at the very top
                ProUpgradeBannerCard(onClick = onProClick)

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_interface))
                CorvusCard {
                    CorvusListRow(
                        title = stringResource(R.string.language),
                        icon = Icons.Default.Language,
                        onClick = { showLanguageDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle(stringResource(R.string.section_power_features))
                CorvusCard {
                    CorvusListRow(
                        title = stringResource(R.string.allow_lan_devices),
                        icon = Icons.Default.Share,
                        onClick = { onAllowLanDevicesChange(!allowLanDevices) },
                        trailingContent = {
                            CorvusToggle(checked = allowLanDevices, onCheckedChange = onAllowLanDevicesChange)
                        }
                    )
                    SettingsDivider()
                    val handleMockGpsToggle = { newState: Boolean ->
                        if (newState) {
                            if (com.corvus.vpn.util.MockLocationManager.isMockLocationAllowed(context)) {
                                onMockGpsChange(true)
                            } else {
                                showMockGpsPermissionDialog = true
                            }
                        } else {
                            onMockGpsChange(false)
                            com.corvus.vpn.util.MockLocationManager.clearMockLocation(context)
                        }
                    }
                    CorvusListRow(
                        title = stringResource(R.string.mock_gps),
                        icon = Icons.Default.LocationOn,
                        onClick = { handleMockGpsToggle(!mockGpsEnabled) },
                        trailingContent = {
                            CorvusToggle(checked = mockGpsEnabled, onCheckedChange = handleMockGpsToggle)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val handleKillSwitchToggle = { newState: Boolean ->
                    onKillSwitchChange(newState)
                    if (newState) {
                        showKillSwitchInfoDialog = true
                    }
                }

                SectionTitle(stringResource(R.string.section_connection))
                CorvusCard {
                    CorvusListRow(
                        title = stringResource(R.string.kill_switch),
                        icon = Icons.Default.PowerSettingsNew,
                        onClick = { handleKillSwitchToggle(!killSwitchEnabled) },
                        trailingContent = {
                            CorvusToggle(checked = killSwitchEnabled, onCheckedChange = handleKillSwitchToggle)
                        }
                    )
                    SettingsDivider()
                    CorvusListRow(
                        title = stringResource(R.string.auto_start),
                        icon = Icons.Default.RocketLaunch,
                        onClick = { onAutoStartChange(!autoStartEnabled) },
                        trailingContent = {
                            CorvusToggle(checked = autoStartEnabled, onCheckedChange = onAutoStartChange)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle(stringResource(R.string.section_speed_notification))
                CorvusCard {
                    CorvusListRow(
                        title = stringResource(R.string.display_speed_notification),
                        icon = Icons.Default.Speed,
                        onClick = { onDisplaySpeedNotificationChange(!displaySpeedNotification) },
                        trailingContent = {
                            CorvusToggle(checked = displaySpeedNotification, onCheckedChange = onDisplaySpeedNotificationChange)
                        }
                    )
                    SettingsDivider()
                    CorvusListRow(
                        title = stringResource(R.string.notification_toggle),
                        icon = Icons.Default.NotificationsActive,
                        onClick = { onNotificationToggleChange(!notificationToggleEnabled) },
                        trailingContent = {
                            CorvusToggle(checked = notificationToggleEnabled, onCheckedChange = onNotificationToggleChange)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle(stringResource(R.string.section_custom_ovpn))
                CorvusCard {
                    CorvusListRow(
                        title = stringResource(R.string.import_custom_ovpn),
                        subtitle = stringResource(R.string.custom_ovpn_subtitle),
                        icon = Icons.Default.FileUpload,
                        onClick = onImportCustomOvpnClick
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle(stringResource(R.string.section_info_diagnostics))
                CorvusCard {
                    CorvusListRow(
                        title = stringResource(R.string.my_ip),
                        icon = Icons.Default.Public,
                        onClick = onMyIpClick
                    )
                    SettingsDivider()
                    CorvusListRow(
                        title = stringResource(R.string.about),
                        icon = Icons.Default.Info,
                        onClick = onAboutClick
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    containerColor = CrowSurface,
                    title = {
                        Text(
                            text = stringResource(R.string.select_language),
                            color = CrowText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            languages.forEach { (name, code) ->
                                TextButton(
                                    onClick = {
                                        showLanguageDialog = false
                                        onLanguageSelected(code)
                                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                                        (context as? Activity)?.recreate()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = name,
                                        color = CrowText,
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(text = stringResource(R.string.cancel_button), color = CrowAccent)
                        }
                    }
                )
            }

            if (showKillSwitchInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showKillSwitchInfoDialog = false },
                    containerColor = CrowSurface,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CrowAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.kill_switch_dialog_title),
                            color = CrowText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.kill_switch_dialog_desc),
                            color = CrowMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showKillSwitchInfoDialog = false
                                try {
                                    context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrowAccent)
                        ) {
                            Text(text = stringResource(R.string.open_vpn_settings), color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showKillSwitchInfoDialog = false }) {
                            Text(text = stringResource(R.string.close_button), color = CrowMuted)
                        }
                    }
                )
            }

            if (showMockGpsPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showMockGpsPermissionDialog = false },
                    containerColor = CrowSurface,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CrowAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.mock_gps_dialog_title),
                            color = CrowText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.mock_gps_dialog_desc),
                            color = CrowMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showMockGpsPermissionDialog = false
                                try {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrowAccent)
                        ) {
                            Text(text = stringResource(R.string.open_developer_options), color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMockGpsPermissionDialog = false }) {
                            Text(text = stringResource(R.string.cancel_button), color = CrowMuted)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProUpgradeBannerCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp),
            shape = RoundedCornerShape(22.dp),
            color = CrowSurface,
            border = BorderStroke(1.dp, CrowBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                CrowAccentGlow.copy(alpha = 0.25f),
                                CrowSurface
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.upgrade_now),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = CrowText
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.enjoy_exclusive_privileges),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CrowMuted,
                                    fontSize = 12.5.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = CrowAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(110.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp)
                .height(145.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.raven_vip),
                contentDescription = "Raven VIP Character",
                modifier = Modifier
                    .height(185.dp)
                    .offset(y = (-45).dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.sp,
            fontSize = 12.sp
        ),
        color = CrowAccent,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        thickness = 0.5.dp,
        color = CrowBorder
    )
}
