package com.hereliesaz.meatpiety

/** What the background check needs to recompute the ledger while the user is away. */
data class LedgerSnapshot(val days: Int, val diet: Diet, val influencePeople: Double?, val money: Money)

/**
 * Milestone notifications fire only while the user is away from the app. In
 * the foreground, [foreground] records the ledger and marks every milestone
 * already crossed as known, so opening the app or changing a value never
 * notifies. A daily background check advances the day count from the saved
 * start date and notifies for milestones crossed since (Android only; the
 * web has no reliable background execution).
 */
expect object Notifier {
    fun foreground(snapshot: LedgerSnapshot, notices: List<Notice>)
}
