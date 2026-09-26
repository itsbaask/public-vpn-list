package com.corvus.vpn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------- Crow palette ----------
private val CrowAccent = Color(0xFF7C6CF0)
private val CrowAccentText = Color(0xFFA79CF7)
private val CrowAccentBorder = Color(0xFF2A2440)
private val CrowAccentGlow = Color(0xFF3E3470)
private val CrowBlack = Color(0xFF0A0A0D)
private val CrowSurface = Color(0xFF121117)
private val CrowCore = Color(0xFF0D0C11)
private val CrowBorder = Color(0xFF211F2C)
private val CrowText = Color(0xFFEEECE6)
private val CrowMuted = Color(0xFF9C98AB)
private val CrowFaint = Color(0xFF46435C)

// Font families
private val DisplayFont = FontFamily.Serif
private val MonoFont = FontFamily.Monospace

@Composable
fun ConnectedScreen(
    serverName: String = "Free Server — DE-Frankfurt",
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "connected")

    // grid drift
    val gridOffset by infinite.animateFloat(
        initialValue = 0f, targetValue = 42f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "grid"
    )
    // core pulse
    val corePulse by infinite.animateFloat(
        initialValue = 0.9f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "core"
    )
    // expanding rings (three staggered)
    val ringProgress = List(3) { i ->
        infinite.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(2400, delayMillis = i * 800, easing = LinearOutSlowInEasing)
            ),
            label = "ring$i"
        )
    }
    // dash flow for tunnel + connection lines
    val dashPhase by infinite.animateFloat(
        initialValue = 0f, targetValue = 240f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "dash"
    )
    // packet position along tunnel (0..1), three staggered packets
    val packetProgress = List(3) { i ->
        infinite.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(2400, delayMillis = i * 800, easing = LinearEasing)
            ),
            label = "packet$i"
        )
    }
    // subtle text glow shimmer
    val shimmer by infinite.animateFloat(
        initialValue = 0.9f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )
    // falling hex code columns
    val codeFall by infinite.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "codeFall"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrowBlack)
    ) {
        // Falling hex code background
        HexRainBackground(progress = codeFall, modifier = Modifier.fillMaxSize())

        // Animated grid + ambient glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ambient radial glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(CrowAccentGlow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width / 2f, -size.height * 0.1f),
                    radius = size.width * 1.4f
                )
            )
            // drifting grid lines, masked toward center-top
            val step = 42.dp.toPx()
            val offset = gridOffset.dp.toPx() % step
            var x = -offset
            while (x < size.width) {
                drawLine(
                    color = CrowAccent.copy(alpha = 0.08f),
                    start = Offset(x, 0f), end = Offset(x, size.height * 0.5f),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = -offset
            while (y < size.height * 0.5f) {
                drawLine(
                    color = CrowAccent.copy(alpha = 0.08f),
                    start = Offset(0f, y), end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {

            Spacer(modifier = Modifier.height(46.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Encrypted lock core with expanding rings
                Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    ringProgress.forEach { anim ->
                        val p = anim.value
                        Box(
                            modifier = Modifier
                                .size(110.dp * (0.6f + p * 1.8f))
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, CrowAccent.copy(alpha = 0.7f * (1f - p)), RoundedCornerShape(999.dp))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(62.dp * corePulse.coerceIn(0.95f, 1.05f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.radialGradient(listOf(CrowAccentBorder, CrowCore))
                            )
                            .border(1.dp, CrowAccentGlow, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        LockIcon()
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Connected",
                    color = CrowText.copy(alpha = shimmer),
                    fontFamily = DisplayFont,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "AES-256 · TUNNEL ACTIVE",
                    color = CrowAccent,
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You are securely connected to",
                    color = CrowMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1B1730), CrowSurface))
                        )
                        .border(1.dp, CrowAccentBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(CrowAccent)
                    )
                    Text(serverName, color = CrowText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Encrypted tunnel with flowing packets
            EncryptedTunnel(
                dashPhase = dashPhase,
                packetProgress = packetProgress.map { it.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Text(
                text = "ENCRYPTING PACKETS",
                color = CrowFaint,
                fontFamily = MonoFont,
                fontSize = 9.5.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Lower panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(CrowSurface.copy(alpha = 0.85f))
                    .border(1.dp, CrowBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CrowAccentBorder)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "The VPN works perfectly and you can trust it.",
                    color = Color(0xFFC9C6D6),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CrowCore)
                        .border(1.dp, CrowAccentBorder, RoundedCornerShape(999.dp))
                        .clickableNoRipple(onClick = onShare)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Share with Friends", color = CrowAccentText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.linearGradient(listOf(CrowAccent, Color(0xFF4A3B8C))))
                        .clickableNoRipple(onClick = onBack)
                        .padding(vertical = 17.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Back", color = CrowBlack, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(width = 134.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(CrowFaint)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LockIcon() {
    Canvas(modifier = Modifier.size(26.dp)) {
        val strokeColor = CrowAccentText
        val bodyTop = size.height * 0.44f
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(size.width * 0.2f, bodyTop),
            size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.42f),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(size.width * 0.2f, bodyTop),
            size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.42f),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )
        // shackle
        val shackleRadius = size.width * 0.17f
        drawArc(
            color = strokeColor,
            startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(size.width * 0.5f - shackleRadius, bodyTop - shackleRadius * 2f),
            size = androidx.compose.ui.geometry.Size(shackleRadius * 2f, shackleRadius * 2f),
            style = Stroke(width = 1.8.dp.toPx())
        )
        // keyhole dot
        drawCircle(color = CrowAccent, radius = 1.8.dp.toPx(), center = Offset(size.width / 2f, bodyTop + size.height * 0.2f))
    }
}

@Composable
private fun EncryptedTunnel(
    dashPhase: Float,
    packetProgress: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val start = Offset(w * 0.065f, h * 0.7f)
        val end = Offset(w * 0.935f, h * 0.7f)
        val c1 = Offset(w * 0.28f, h * 0.15f)
        val c2 = Offset(w * 0.72f, h * 0.15f)

        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
        }

        // thick tunnel base
        drawPath(path, color = CrowBorder, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
        // dashed flowing line
        drawPath(
            path,
            color = CrowAccentGlow,
            style = Stroke(
                width = 1.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 11f), -dashPhase)
            )
        )
        // endpoints
        drawCircle(color = CrowCore, radius = 6.dp.toPx(), center = start)
        drawCircle(color = CrowAccent, radius = 6.dp.toPx(), center = start, style = Stroke(2.dp.toPx()))
        drawCircle(color = CrowCore, radius = 6.dp.toPx(), center = end)
        drawCircle(color = CrowAccent, radius = 6.dp.toPx(), center = end, style = Stroke(2.dp.toPx()))

        // flowing packets along the bezier
        val measure = android.graphics.PathMeasure(path.asAndroidPath(), false)
        val length = measure.length
        val colors = listOf(CrowAccentText, CrowAccent, CrowText)
        packetProgress.forEachIndexed { i, t ->
            val pos = FloatArray(2)
            measure.getPosTan(t * length, pos, null)
            val fadeIn = (t / 0.08f).coerceIn(0f, 1f)
            val fadeOut = ((1f - t) / 0.08f).coerceIn(0f, 1f)
            val alpha = minOf(fadeIn, fadeOut)
            drawCircle(
                color = colors[i % colors.size].copy(alpha = alpha),
                radius = 4.dp.toPx(),
                center = Offset(pos[0], pos[1])
            )
        }
    }
}

@Composable
private fun HexRainBackground(progress: Float, modifier: Modifier = Modifier) {
    // Deterministic pseudo-random hex columns for a code-rain effect
    val columns = remember {
        listOf(0.06f, 0.22f, 0.72f, 0.87f).map { xFrac ->
            val rnd = Random(xFrac.hashCode())
            xFrac to List(10) { rnd.nextInt(0, 0xFFFF).toString(16).uppercase().padStart(4, '0') }
        }
    }
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(140, 62, 52, 112)
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    Canvas(modifier = modifier) {
        textPaint.textSize = 11.sp.toPx()
        columns.forEach { (xFrac, lines) ->
            val x = size.width * xFrac
            val lineHeight = 16.dp.toPx()
            val totalHeight = lineHeight * lines.size
            val y0 = progress * (size.height + totalHeight) - totalHeight
            lines.forEachIndexed { idx, text ->
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    x,
                    y0 + idx * lineHeight,
                    textPaint
                )
            }
        }
    }
}

// Simple no-ripple clickable helper to keep the dark UI clean.
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    )
}
