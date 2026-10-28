package com.matrixmarket.app.util

// Student Score tiers - mirrors KarmaService.GetTrustTier on the API.
object KarmaTiers {
    val thresholds = listOf(0, 40, 100, 200)
    val names = listOf("New Trader", "Rising Trader", "Trusted Trader", "Campus Legend")

    fun tierFor(points: Int): String {
        val index = thresholds.indexOfLast { points >= it }.coerceAtLeast(0)
        return names[index]
    }

    // 0f..1f progress toward the next tier (1f once the top tier is reached).
    fun progress(points: Int): Float {
        val nextIndex = thresholds.indexOfFirst { it > points }
        if (nextIndex == -1) return 1f
        if (nextIndex == 0) return 0f
        val prev = thresholds[nextIndex - 1]
        val next = thresholds[nextIndex]
        return ((points - prev).toFloat() / (next - prev)).coerceIn(0f, 1f)
    }

    // Points still needed and the name of the next tier, or null at the top tier.
    fun pointsToNext(points: Int): Pair<Int, String>? {
        val nextIndex = thresholds.indexOfFirst { it > points }
        if (nextIndex == -1) return null
        return (thresholds[nextIndex] - points) to names[nextIndex]
    }
}
