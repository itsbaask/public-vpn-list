package com.corvus.vpn.ui.theme

import androidx.compose.ui.graphics.Color

// Ultra World-Class Cyber Obsidian Palette (ProtonVPN / Cloudflare 1.1.1.1 / NordVPN Style)
val CyberDarkObsidian = Color(0xFF08090C)   // Deep OLED Carbon Canvas
val CyberSurfaceDark = Color(0xFF111319)    // Elevated Glassmorphic Container
val CyberSurfaceVariant = Color(0xFF191C26) // Secondary Glass Container
val CyberBorderStroke = Color(0xFF242838)   // Thin Glossy Border

// Vibrant High-Tech Electric Accents
val ElectricCyan = Color(0xFF00F2FE)       // Cyber Electric Cyan Primary
val ElectricBlue = Color(0xFF38BDF8)       // Ocean Blue Secondary
val ElectricNeonGreen = Color(0xFF00E676)  // Radiant Neon Emerald (Connected)
val GlowingAmber = Color(0xFFFFB300)       // Warm Amber (Connecting)
val CyberCrimson = Color(0xFFFF3366)        // Electric Crimson (Error/Disconnect)
val RoyalPurple = Color(0xFF8B5CF6)         // Accent Royal Violet
val PureGold = Color(0xFFFFD700)            // Pure Gold VIP

// Crisp Text Tokens
val TextPrimaryWhite = Color(0xFFFAFAFA)    // 98% Pure Crisp White
val TextSecondaryMuted = Color(0xFF8E95A5)  // Cool Muted Slate

// Light Mode Tokens (Clean White Glass)
val LightCanvas = Color(0xFFF4F6F9)
val LightCard = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEAEFF5)
val LightText = Color(0xFF0F172A)
val LightSecondaryText = Color(0xFF64748B)
val LightAccent = Color(0xFF0284C7)
val LightAccentContainer = Color(0xFFE0F2FE)
val LightSuccess = ElectricNeonGreen
val LightDanger = CyberCrimson
val LightOutline = Color(0xFFCBD5E1)

// Dark Mode Color Tokens
val DarkCanvas = CyberDarkObsidian
val DarkCard = CyberSurfaceDark
val DarkSurfaceVariant = CyberSurfaceVariant
val DarkText = TextPrimaryWhite
val DarkSecondaryText = TextSecondaryMuted
val DarkAccent = ElectricCyan
val DarkAccentContainer = Color(0xFF0C2A38)
val DarkSuccess = ElectricNeonGreen
val DarkDanger = CyberCrimson
val DarkOutline = CyberBorderStroke

// Latency Color Helper (Green <= 99ms, Yellow <= 249ms, Red >= 250ms)
fun getLatencyColor(pingMs: Int?): Color {
    if (pingMs == null || pingMs <= 0) return DarkSecondaryText
    return when (pingMs) {
        in 0..99 -> ElectricNeonGreen
        in 100..249 -> PureGold
        else -> CyberCrimson
    }
}
