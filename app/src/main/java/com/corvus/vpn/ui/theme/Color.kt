package com.corvus.vpn.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Crow Palette — Raven Black + Crow-Eye Violet as the Primary Accent
// (نفس البنية بتاعتك، الألوان اتظبطت تطابق تصميم Corvus Pro / عين الغراب)
// ============================================================================

val CrowBlack          = Color(0xFF0A0A0D) // Void Black Canvas — نفس --void بالـ mock
val CrowSurface        = Color(0xFF121117) // Elevated Surface Container — --feather-1
val CrowCore           = Color(0xFF0D0C11) // Inner Core Surface — أغمق شوية من الـ surface
val CrowBorder         = Color(0xFF211F2C) // Feather Border Stroke — تباين هادي على الأسود

val CrowText           = Color(0xFFEEECE6) // Primary Bone White Text — --bone
val CrowMuted          = Color(0xFF9C98AB) // Smoke Gray Muted Text — --smoke
val CrowFaint          = Color(0xFF46435C) // Dark Violet-Slate Faint Text

val CrowAccent         = Color(0xFF7C6CF0) // Crow-Eye Violet Accent — --violet
val CrowAccentText     = Color(0xFFA79CF7) // Lighter Violet Text
val CrowAccentBorder   = Color(0xFF2A2440) // Dark Accent Border
val CrowAccentGlow     = Color(0xFF3E3470) // Ambient Eye Glow

val CrowRingOuterOff   = Color(0xFF16151C) // --feather-2 tone
val CrowRingInnerOff   = Color(0xFF1D1B26)
val CrowRingOuterOn    = Color(0xFF2A2440)
val CrowRingInnerOn    = Color(0xFF453B85)

val CrowButtonRingOff  = Color(0xFF2E2A4C)
val CrowLogoOff        = Color(0xFF847E9C)
val CrowLogoOn         = Color(0xFFE6E1FF)
val CrowDotOff         = Color(0xFF5C5771)

// لمسة ذهبية (Best Value / تفاصيل فاخرة) — --gold بالـ mock
val CrowGold           = Color(0xFFC9A23A)

// لمسة حمراء نادرة (تحذيرات / حالة خطأ) — --blood بالـ mock
val CrowBlood          = Color(0xFF7A2033)

// Latency Color Helper (Crow Accent للبينج الكويس، Gold للمتوسط، Blood للعالي)
fun getLatencyColor(pingMs: Int?): Color {
    if (pingMs == null || pingMs <= 0) return CrowMuted
    return when (pingMs) {
        in 0..149 -> CrowAccent
        in 150..299 -> CrowGold
        else -> CrowBlood
    }
}