package com.corvus.vpn.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = CrowBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.about_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = CrowText,
                        letterSpacing = (-0.3).sp
                    )
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CrowBlack)
                .padding(paddingValues)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            // Logo circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(CrowSurface)
                    .border(0.5.dp, CrowBorderMid, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logo_no_bg),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = CrowText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Corvus VPN",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = CrowText,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${stringResource(R.string.version_label)} 1.0.0",
                fontSize = 13.sp,
                color = CrowMuted
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Description card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CrowSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CrowBorderMid),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.about_description),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = CrowTextSub,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoChip(label = "AES-256", sub = "Encryption", modifier = Modifier.weight(1f))
                InfoChip(label = "No-Log", sub = "Policy", modifier = Modifier.weight(1f))
                InfoChip(label = "Multi", sub = "Protocol", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.copyright_text),
                fontSize = 11.sp,
                color = CrowMuted.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CrowSurface)
            .border(0.5.dp, CrowBorderMid, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CrowAccent
        )
        Text(
            text = sub,
            fontSize = 10.sp,
            color = CrowMuted
        )
    }
}
