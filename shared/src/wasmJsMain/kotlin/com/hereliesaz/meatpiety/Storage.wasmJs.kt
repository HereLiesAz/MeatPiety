package com.hereliesaz.meatpiety

private fun lsGet(key: String): String? = js("{ try { return localStorage.getItem(key); } catch (e) { return null; } }")
private fun lsSet(key: String, value: String): Unit = js("{ try { localStorage.setItem(key, value); } catch (e) {} }")
private fun localEpochDay(): Double = js("{ const d = new Date(); return Math.floor((d.getTime() - d.getTimezoneOffset() * 60000) / 86400000); }")

actual object Storage {
    actual fun get(key: String): String? = lsGet(key)
    actual fun set(key: String, value: String) = lsSet(key, value)
}

actual fun todayEpochDay(): Long = localEpochDay().toLong()
