package com.corvus.vpn.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.components.CorvusCard
import com.corvus.vpn.ui.components.CorvusListRow
import com.corvus.vpn.ui.components.CorvusToggle

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
    onMyIpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

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
                CorvusListRow(
                    title = stringResource(R.string.mock_gps),
                    icon = Icons.Default.LocationOn,
                    onClick = { onMockGpsChange(!mockGpsEnabled) },
                    trailingContent = {
                        CorvusToggle(checked = mockGpsEnabled, onCheckedChange = onMockGpsChange)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(stringResource(R.string.section_connection))
            CorvusCard {
                CorvusListRow(
                    title = stringResource(R.string.kill_switch),
                    icon = Icons.Default.PowerSettingsNew,
                    onClick = { onKillSwitchChange(!killSwitchEnabled) },
                    trailingContent = {
                        CorvusToggle(checked = killSwitchEnabled, onCheckedChange = onKillSwitchChange)
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
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}
