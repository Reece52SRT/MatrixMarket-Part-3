package com.matrixmarket.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.matrixmarket.app.R

// The API sends English codes ("Sell", "Trusted Trader", "First Trade"...).
// These helpers show them in the user's chosen language.

@Composable
fun listingTypeLabel(type: String): String = when (type.lowercase()) {
    "trade" -> stringResource(R.string.type_trade)
    "lend" -> stringResource(R.string.type_lend)
    else -> stringResource(R.string.type_sell)
}

@Composable
fun tierLabel(tier: String): String = when (tier) {
    "Campus Legend" -> stringResource(R.string.tier_campus_legend)
    "Trusted Trader" -> stringResource(R.string.tier_trusted_trader)
    "Rising Trader" -> stringResource(R.string.tier_rising_trader)
    else -> stringResource(R.string.tier_new_trader)
}

@Composable
fun badgeLabel(badge: String): String = when (badge) {
    "First Trade" -> stringResource(R.string.badge_first_trade)
    "Top Seller" -> stringResource(R.string.badge_top_seller)
    "Trusted Trader" -> stringResource(R.string.badge_trusted_trader)
    else -> badge
}

@Composable
fun roleLabel(role: String): String =
    if (role.equals("Seller", ignoreCase = true)) stringResource(R.string.role_seller) else stringResource(R.string.role_buyer)

@Composable
fun categoryLabel(categoryId: Int, fallback: String): String = when (categoryId) {
    1 -> stringResource(R.string.category_textbooks)
    2 -> stringResource(R.string.category_electronics)
    3 -> stringResource(R.string.category_residence)
    4 -> stringResource(R.string.category_tutoring)
    5 -> stringResource(R.string.category_stationery)
    else -> fallback
}
