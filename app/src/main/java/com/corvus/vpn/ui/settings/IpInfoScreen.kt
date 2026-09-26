package com.corvus.vpn.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.corvus.vpn.R
import com.corvus.vpn.data.IpInfo
import com.corvus.vpn.ui.components.CorvusCard
import com.corvus.vpn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpInfoScreen(
    viewModel: IpInfoViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchIpInfo()
    }

    Scaffold(
        containerColor = CrowBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.my_ip),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = CrowText,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_custom_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchIpInfo() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CrowTextSub,
                            modifier = Modifier.size(20.dp)
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
                .padding(horizontal = 20.dp)
        ) {
            when (val state = uiState) {
                is IpInfoUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CrowAccent,
                        strokeWidth = 2.5.dp
                    )
                }
                is IpInfoUiState.Success -> {
                    IpDetailsList(state.ipInfo)
                }
                is IpInfoUiState.Error -> {
                    ErrorState(state.message) { viewModel.fetchIpInfo() }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun IpDetailsList(ipInfo: IpInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Main IP Display Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CrowSurface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, CrowBorderMid)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CrowAccent)
                    )
                    Text(
                        text = stringResource(R.string.ip_address).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = CrowAccentText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ipInfo.query,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = CrowText
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(stringResource(R.string.ip_details))

        CorvusCard {
            InfoRow(Icons.Default.Public, stringResource(R.string.location), "${ipInfo.city}, ${ipInfo.country}")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = CrowBorder)
            InfoRow(Icons.Default.Business, stringResource(R.string.isp), ipInfo.isp ?: "Unknown")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = CrowBorder)
            InfoRow(Icons.Default.Domain, stringResource(R.string.organization), ipInfo.org ?: "Unknown")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = CrowBorder)
            InfoRow(Icons.Default.Schedule, stringResource(R.string.timezone), ipInfo.timezone ?: "Unknown")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = CrowBorder)
            InfoRow(Icons.Default.PinDrop, stringResource(R.string.zip_code), ipInfo.zip ?: "Unknown")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.5.sp,
            fontSize = 11.sp
        ),
        color = CrowTextSub,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(CrowSurface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CrowTextSub,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CrowMuted
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CrowText
            )
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = CrowMuted
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.failed_to_load_ip),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CrowText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = CrowMuted
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = CrowAccent,
                contentColor = CrowBlack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                stringResource(R.string.refresh),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
