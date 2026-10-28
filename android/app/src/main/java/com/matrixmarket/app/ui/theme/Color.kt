package com.matrixmarket.app.ui.theme

import androidx.compose.ui.graphics.Color

// Palette from the Part 1 mockups: dark neon-purple onboarding/login screens and
// a light lavender palette for the main marketplace screens.
val MatrixBlack = Color(0xFF0D0D0F)
val MatrixGold = Color(0xFFD4AF37)
val RoyalPurple = Color(0xFF6B4EFF)
val DeepPurple = Color(0xFF3A1F9E)
val NeonPurple = Color(0xFF9D7BFF)
val LavenderLight = Color(0xFFEDE7FA)
val LavenderCard = Color(0xFFD8CCF5)
val TextDark = Color(0xFF1A1A1D)
val TextMuted = Color(0xFF6E6A7C)
val SuccessGreen = Color(0xFF1E9E5A)
val WarningAmber = Color(0xFFF2A93B)
val ErrorRed = Color(0xFFD93B3B)
val CodeBackground = Color(0xFFFFF3D6)
val White = Color(0xFFFFFFFF)

// Each listing type gets its own accent so Sell / Trade / Lend read distinctly.
val SellPurple = RoyalPurple
val TradeTeal = Color(0xFF1FA7A0)
val LendAmber = Color(0xFFE08A3C)
val MeetingTeal = Color(0xFF2E8B8B)

fun listingTypeColor(listingType: String): Color = when (listingType.lowercase()) {
    "trade" -> TradeTeal
    "lend" -> LendAmber
    else -> SellPurple
}

// Student Score tier colours - the progress card visibly "levels up".
val NewTraderGray = Color(0xFF9C92C2)
val RisingTraderTeal = TradeTeal
val TrustedTraderGold = MatrixGold
val CampusLegendPurple = Color(0xFF4B2E9E)

fun tierColor(tier: String): Color = when (tier) {
    "Campus Legend" -> CampusLegendPurple
    "Trusted Trader" -> TrustedTraderGold
    "Rising Trader" -> RisingTraderTeal
    else -> NewTraderGray
}

fun badgeColor(badgeType: String): Color = when (badgeType) {
    "First Trade" -> RisingTraderTeal
    "Top Seller" -> TrustedTraderGold
    "Trusted Trader" -> CampusLegendPurple
    else -> RoyalPurple
}
