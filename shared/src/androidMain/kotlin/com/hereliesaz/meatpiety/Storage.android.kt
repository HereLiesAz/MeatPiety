package com.hereliesaz.meatpiety

import android.content.Context
import java.time.LocalDate

actual object Storage {
    private fun prefs() = AppContext.get().getSharedPreferences("ledger", Context.MODE_PRIVATE)
    actual fun get(key: String): String? = prefs().getString(key, null)
    actual fun set(key: String, value: String) = prefs().edit().putString(key, value).apply()
}

actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/** Application context, set once by the activity. */
object AppContext {
    private lateinit var context: Context
    fun init(context: Context) { this.context = context.applicationContext }
    fun get(): Context = context
}
