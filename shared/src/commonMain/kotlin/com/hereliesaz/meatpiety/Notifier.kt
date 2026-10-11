package com.hereliesaz.meatpiety

/** What the background check needs to recompute the ledger while the user is away. */
data class LedgerSnapshot(val days: Int, val diet: Diet, val influencePeople: Double?, val money: Money)

/**
 * Milestone notifications fire only while the user is away from the app. In
 * the foreground, [foreground] records the ledger and marks every milestone
 * already crossed as known, so opening the app or changing a value never
 * notifies. A daily background check advances the day count from the saved
 * start date and notifies for milestones crossed since: WorkManager on
 * Android; on the web, the installed PWA's service worker with Periodic
 * Background Sync (Chromium only).
 */
expect object Notifier {
    fun foreground(snapshot: LedgerSnapshot, notices: List<Notice>)
}

/**
 * Milestones the ledger will cross over the next [horizonDays] days if the
 * user keeps going, each with the epoch day it lands on. The web build hands
 * this to its service worker, which can't run the calculator itself.
 */
internal fun upcomingNotices(snapshot: LedgerSnapshot, notices: List<Notice>, horizonDays: Int = 60): List<Pair<Long, Notice>> {
    val known = notices.map { it.id }.toMutableSet()
    val today = todayEpochDay()
    return (1..horizonDays).flatMap { k ->
        val d = snapshot.days + k
        val personal = personalLayer(d, snapshot.diet)
        val influence = snapshot.influencePeople?.let { influenceLayer(it, d, snapshot.diet) }
        milestones(d, personal, influence, worldLayer(d), snapshot.money)
            .filter { known.add(it.id) }
            .map { today + k to it }
    }
}
