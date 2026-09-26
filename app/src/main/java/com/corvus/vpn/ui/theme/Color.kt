package com.corvus.vpn.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// CORVUS VPN — OBSIDIAN CYBER NOIR Palette
// Primary: #050505 Void | Accent: #39FF88 Emerald (used sparingly ~3%)
// ============================================================================

// ── Backgrounds & Surfaces ──────────────────────────────────────────────────
val CrowBlack          = Color(0xFF050505) // True Void Black — primary canvas
val CrowSurface        = Color(0xFF0D0D0D) // Elevated surface — cards, panels
val CrowCore           = Color(0xFF0A0A0A) // Inner core — deepest surface
val CrowSurface2       = Color(0xFF141414) // Second-level surface — dialogs
val CrowSurface3       = Color(0xFF1A1A1A) // Third-level — list items hover

// ── Borders & Strokes ───────────────────────────────────────────────────────
val CrowBorder         = Color(0xFF1E1E1E) // Subtle border — default
val CrowBorderMid      = Color(0xFF282828) // Mid border — focused states
val CrowBorderBright   = Color(0xFF333333) // Bright border — active items

// ── Text Hierarchy ──────────────────────────────────────────────────────────
val CrowText           = Color(0xFFF0F0F0) // Primary text — near-white
val CrowTextSub        = Color(0xFFAAAAAA) // Secondary text — muted
val CrowMuted          = Color(0xFF666666) // Muted / placeholder
val CrowFaint          = Color(0xFF333333) // Faint / disabled

// ── Primary Accent — Emerald Green (use VERY SPARINGLY ~3%) ─────────────────
val CrowAccent         = Color(0xFF39FF88) // Emerald neon — primary CTA only
val CrowAccentDim      = Color(0xFF1A7A45) // Dimmed emerald — backgrounds
val CrowAccentGlow     = Color(0xFF0D3D22) // Deep emerald glow
val CrowAccentBorder   = Color(0xFF0F2A1A) // Accent border tint
val CrowAccentText     = Color(0xFF6EFFA8) // Lighter emerald text

// ── Connect Ring States ──────────────────────────────────────────────────────
val CrowRingOuterOff   = Color(0xFF141414)
val CrowRingInnerOff   = Color(0xFF1C1C1C)
val CrowRingOuterOn    = Color(0xFF0F2A1A)
val CrowRingInnerOn    = Color(0xFF1A4A2E)
val CrowButtonRingOff  = Color(0xFF252525)
val CrowLogoOff        = Color(0xFF555555)
val CrowLogoOn         = Color(0xFFE0FFE8)
val CrowDotOff         = Color(0xFF404040)

// ── Status Colors ────────────────────────────────────────────────────────────
val CrowSuccess        = Color(0xFF39FF88) // Connected — same as accent
val CrowGold           = Color(0xFFE8A020) // Warning / best value
val CrowBlood          = Color(0xFFE53935) // Error / danger

// ── Utility ──────────────────────────────────────────────────────────────────
fun getLatencyColor(pingMs: Int?): Color {
    if (pingMs == null || pingMs <= 0) return CrowMuted
    return when (pingMs) {
        in 0..149  -> CrowAccent
        in 150..299 -> CrowGold
        else       -> CrowBlood
    }
}