package com.hereliesaz.meatpiety

private fun notificationsSupported(): Boolean = js("typeof Notification !== 'undefined'")
private fun permission(): String = js("Notification.permission")
private fun requestPermission(): Unit = js("{ Notification.requestPermission(); }")
private fun show(title: String, body: String, tag: String): Unit = js("{ new Notification(title, { body: body, tag: tag }); }")
private fun wasSent(id: String): Boolean = js("{ try { return localStorage.getItem('notice:' + id) !== null; } catch (e) { return false; } }")
private fun markSent(id: String): Unit = js("{ try { localStorage.setItem('notice:' + id, '1'); } catch (e) {} }")

actual object Notifier {
    actual fun post(notice: Notice) {
        if (!notificationsSupported()) return
        when (permission()) {
            // Not yet asked: ask, and keep the notice unsent so it fires on a later pass.
            "default" -> requestPermission()
            "granted" -> if (!wasSent(notice.id)) {
                markSent(notice.id)
                show(notice.title, notice.body, notice.id)
            }
        }
    }
}
