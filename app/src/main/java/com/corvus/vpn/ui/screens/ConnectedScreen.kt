package com.corvus.vpn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.util.CountryUtils
import kotlin.random.Random

// ---------- Crow palette ----------
private val CrowAccent = Color(0xFF7C6CF0)
private val CrowAccentText = Color(0xFFA79CF7)
private val CrowBlack = Color(0xFF000000)
private val CrowText = Color(0xFFD8D2F5)
private val CrowMuted = Color(0xFF8E88A3)
private val CrowFaint = Color(0xFF46435C)
private val CrowDim = Color(0xFF615C74)
private val MatrixGreen = Color(0xFF00FF8C)

private val MonoFont = FontFamily.Monospace
private const val MATRIX_GLYPHS = "ﾊﾐﾋｰｳｼﾅﾓﾆｻﾜﾂｵﾘｱﾎﾃﾏｹﾒｴｶｷﾑﾕﾗﾔﾖﾙﾚﾛﾝ0123456789ABCDEF"

private data class ColumnSpec(
    val xFrac: Float,
    val durationMs: Int,
    val delayMs: Int,
    val fontSizeSp: Float,
    val color: Color,
    val text: String
)

@Composable
fun ConnectedScreen(
    server: Server? = null,
    serverName: String = "",
    downloadSpeed: String = "4.8 MB/s",
    uploadSpeed: String = "1.2 MB/s",
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "connected")

    // Dynamic extraction bound to actual server
    val resolvedServerName = remember(server, serverName) {
        if (server != null) {
            val flag = CountryUtils.getFlagEmoji(server.countryCode ?: "")
            val cleanName = server.name.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
            val finalName = if (cleanName.isNotBlank()) cleanName else server.countryName ?: "SECURE GATEWAY"
            "$flag $finalName".uppercase()
        } else if (serverName.isNotBlank()) {
            serverName.uppercase()
        } else {
            "🌐 SECURE_GATEWAY — DE•FRANKFURT"
        }
    }

    val encryptionLine = remember(server) {
        val proto = server?.protocol?.uppercase() ?: "OPENVPN"
        when (proto) {
            "OPENVPN" -> "// ENCRYPTION :: AES-256-GCM :: ACTIVE"
            "VLESS" -> "// ENCRYPTION :: REALITY/TLS :: ACTIVE"
            "VMESS" -> "// ENCRYPTION :: AES-128-GCM :: ACTIVE"
            "SHADOWSOCKS", "SS" -> "// ENCRYPTION :: CHACHA20-POLY1305 :: ACTIVE"
            "TROJAN" -> "// ENCRYPTION :: TLS-1.3 :: ACTIVE"
            "HYSTERIA2", "HY2" -> "// ENCRYPTION :: QUIC/TLS-1.3 :: ACTIVE"
            "WIREGUARD", "WG" -> "// ENCRYPTION :: CHACHA20 :: ACTIVE"
            else -> "// ENCRYPTION :: AES-256 :: ACTIVE"
        }
    }

    val pingValue = remember(server) {
        val p = server?.ping ?: 18
        if (p > 0) "${p}ms" else "14ms"
    }

    val serverLoad = remember(server) {
        val sig = server?.signal ?: 3
        val loadPercent = (100 - (sig * 18)).coerceIn(15, 68)
        "${loadPercent}%"
    }

    val dataStreamTxRx = remember(uploadSpeed, downloadSpeed) {
        val tx = if (uploadSpeed.isNotBlank() && uploadSpeed != "0.0 Mbps") uploadSpeed else "1.2 MB/s"
        val rx = if (downloadSpeed.isNotBlank() && downloadSpeed != "0.0 Mbps") downloadSpeed else "4.8 MB/s"
        "tx $tx ↑    rx $rx ↓"
    }

    // Long, independently-timed columns so the rain has no gaps and always
    // reaches the very bottom edge of the screen, not just the top portion.
    val columnCount = 13
    val columns = remember {
        List(columnCount) { i ->
            val rnd = Random(i * 97 + 11)
            ColumnSpec(
                xFrac = (i + 0.5f) / columnCount,
                durationMs = rnd.nextInt(5200, 8600),
                delayMs = rnd.nextInt(0, 2400),
                fontSizeSp = if (i % 2 == 0) 13f else 12f,
                color = if (i % 2 == 0) CrowAccent else CrowAccentText,
                text = buildString { repeat(50) { append(MATRIX_GLYPHS.random(rnd)) } }
            )
        }
    }
    val fallProgress = columns.map { spec ->
        infinite.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(spec.durationMs, delayMillis = spec.delayMs, easing = LinearEasing)
            ),
            label = "fall"
        )
    }

    val flicker by infinite.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "flicker"
    )

    // Drives the staggered "materialize" reveal of each line, once, on entry.
    var elapsedMs by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            elapsedMs = (now - start).toInt()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CrowBlack)) {

        // Matrix rain: fills the entire screen edge-to-edge, reaching the bottom.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineHeightPx = 15.dp.toPx()
            columns.forEachIndexed { i, spec ->
                val x = size.width * spec.xFrac
                val totalTextHeight = lineHeightPx * spec.text.length
                // Loops from above the top to below the bottom, so the column
                // always spans the full 0..height range — never a gap at the bottom.
                val y0 = fallProgress[i].value * (size.height + totalTextHeight) - totalTextHeight
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(
                        217,
                        (spec.color.red * 255).toInt(),
                        (spec.color.green * 255).toInt(),
                        (spec.color.blue * 255).toInt()
                    )
                    textSize = spec.fontSizeSp.sp.toPx()
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                spec.text.forEachIndexed { idx, ch ->
                    val y = y0 + idx * lineHeightPx
                    if (y > -lineHeightPx && y < size.height + lineHeightPx) {
                        drawContext.canvas.nativeCanvas.drawText(ch.toString(), x, y, paint)
                    }
                }
            }
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.9f)),
                    center = Offset(size.width / 2f, size.height * 0.46f),
                    radius = size.width * 1.2f
                )
            )
        }

        // Pure text content — no boxes, no icons, no borders.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MatLine("[ CORVUS_VPN ]", elapsedMs, 150, CrowDim, 11.sp)

            Spacer(Modifier.height(18.dp))
            MatLine(">_ INITIATING SECURE SESSION_", elapsedMs, 400, CrowAccentText, 13.sp, letterSpacing = 1.4.sp)

            Spacer(Modifier.height(26.dp))
            MatLine(
                "CONNECTED", elapsedMs, 750, CrowText, 34.sp,
                weight = FontWeight.Bold, letterSpacing = 2.sp,
                flickerAlpha = flicker, flickerAfterMs = 750
            )

            Spacer(Modifier.height(10.dp))
            MatLine(encryptionLine, elapsedMs, 1050, CrowAccent, 11.sp, letterSpacing = 1.5.sp)

            Spacer(Modifier.height(34.dp))
            MatLine("you are securely connected to", elapsedMs, 1300, CrowMuted, 13.sp)

            Spacer(Modifier.height(8.dp))
            MatLine("< $resolvedServerName >", elapsedMs, 1550, CrowText, 16.sp, weight = FontWeight.Bold)

            Spacer(Modifier.height(6.dp))
            MatLine("ping $pingValue · load $serverLoad · uptime 99.98%", elapsedMs, 1850, CrowFaint, 10.5.sp)

            Spacer(Modifier.height(40.dp))
            MatLine("━━━━━━━━━ DATA STREAM ━━━━━━━━━", elapsedMs, 2150, CrowDim, 11.sp)
            Spacer(Modifier.height(8.dp))
            MatLine(dataStreamTxRx, elapsedMs, 2300, CrowAccent, 10.5.sp)

            Spacer(Modifier.height(44.dp))
            MatMultiline(
                listOf("> the VPN works perfectly", "> and you can trust it_"),
                elapsedMs, 2700, CrowText.copy(alpha = 0.85f), 13.sp
            )

            Spacer(Modifier.height(30.dp))
            MatLine(
                "[ SHARE_WITH_FRIENDS ]", elapsedMs, 3000, CrowAccentText, 13.sp,
                letterSpacing = 1.sp, onClick = onShare
            )

            Spacer(Modifier.height(56.dp))
            MatLine(
                "[ BACK ]", elapsedMs, 3300, CrowText, 15.sp,
                weight = FontWeight.Bold, letterSpacing = 1.5.sp, onClick = onBack
            )
        }
    }
}

/**
 * One line of text that "materializes" out of the matrix rain: it snaps in
 * bright matrix-green at [appearAtMs], then settles into its real brand
 * color — mirroring the web mockup's materialize animation.
 */
@Composable
private fun MatLine(
    text: String,
    elapsedMs: Int,
    appearAtMs: Int,
    settledColor: Color,
    fontSize: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.sp,
    flickerAlpha: Float? = null,
    flickerAfterMs: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val sinceAppear = elapsedMs - appearAtMs
    if (sinceAppear < 0) {
        Spacer(Modifier.height(with(LocalDensity.current) { fontSize.toDp() }))
        return
    }
    val color = if (sinceAppear < 220) MatrixGreen else settledColor
    val alpha = if (flickerAfterMs != null && sinceAppear > 2250 && flickerAlpha != null) flickerAlpha else 1f

    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        color = color.copy(alpha = alpha),
        fontFamily = MonoFont,
        fontSize = fontSize,
        fontWeight = weight,
        letterSpacing = letterSpacing,
        textAlign = TextAlign.Center,
        modifier = if (onClick != null) {
            Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        } else Modifier
    )
}

@Composable
private fun MatMultiline(
    lines: List<String>,
    elapsedMs: Int,
    appearAtMs: Int,
    settledColor: Color,
    fontSize: TextUnit
) {
    val sinceAppear = elapsedMs - appearAtMs
    if (sinceAppear < 0) return
    val color = if (sinceAppear < 220) MatrixGreen else settledColor
    Text(
        text = lines.joinToString("\n"),
        color = color,
        fontFamily = MonoFont,
        fontSize = fontSize,
        lineHeight = fontSize * 1.7f,
        textAlign = TextAlign.Center
    )
}
