package com.hereliesaz.meatpiety

/**
 * Posts milestone [Notice]s as operating-system notifications (Android
 * notification shade, browser Notification API). Each notice id is sent once
 * per install; the record survives restarts.
 */
expect object Notifier {
    fun post(notice: Notice)
}

/** Sends the notices not already sent. */
internal fun Notifier.postNew(notices: List<Notice>) = notices.forEach(::post)
