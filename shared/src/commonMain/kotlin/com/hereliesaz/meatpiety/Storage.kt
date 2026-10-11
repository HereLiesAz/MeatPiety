package com.hereliesaz.meatpiety

/** Small key-value store that survives restarts (SharedPreferences / localStorage). */
expect object Storage {
    fun get(key: String): String?
    fun set(key: String, value: String)
}

/** Days since the Unix epoch in the device's local calendar. */
expect fun todayEpochDay(): Long

/**
 * The day count advances by itself: the app stores the day the user started
 * (today − days entered) and derives days from it on every launch.
 */
internal object StartDate {
    private const val KEY = "start_epoch_day"

    fun days(): Int? = Storage.get(KEY)?.toLongOrNull()?.let { (todayEpochDay() - it).toInt().coerceAtLeast(0) }

    fun setDays(days: Int) = Storage.set(KEY, (todayEpochDay() - days).toString())
}
