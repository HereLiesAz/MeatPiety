package com.hereliesaz.meatpiety

/** Small key-value store that survives restarts (SharedPreferences / localStorage). */
expect object Storage {
    fun get(key: String): String?
    fun set(key: String, value: String)
}

/** Days since the Unix epoch in the device's local calendar. */
expect fun todayEpochDay(): Long

/**
 * The day count advances by itself: the user picks the day they started and
 * days are derived from it on every launch.
 */
internal object StartDate {
    private const val KEY = "start_epoch_day"

    fun days(): Int? = Storage.get(KEY)?.toLongOrNull()?.let { (todayEpochDay() - it).toInt().coerceAtLeast(0) }

    fun epochDay(): Long? = Storage.get(KEY)?.toLongOrNull()

    /** Days in the default ledger when the user never picks a start date. */
    const val DEFAULT_DAYS = 365

    /**
     * The stored start day, or a default of [DEFAULT_DAYS] ago that is persisted
     * on first use so background work and widgets see the same ledger as the app.
     */
    fun epochDayOrDefault(): Long = epochDay() ?: (todayEpochDay() - DEFAULT_DAYS).also(::setEpochDay)

    fun setEpochDay(day: Long) = Storage.set(KEY, day.toString())

    /** "12 Mar 2024" for an epoch day (proleptic Gregorian, UTC). */
    fun label(day: Long): String {
        // Civil-from-days (Howard Hinnant), so no date library is needed.
        val z = day + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val y = yoe + era * 400 + if (m <= 2) 1 else 0
        val month = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[(m - 1).toInt()]
        return "$d $month $y"
    }
}
