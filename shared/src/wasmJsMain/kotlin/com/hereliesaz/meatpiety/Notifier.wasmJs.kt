package com.hereliesaz.meatpiety

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Hands the service worker (resources/sw.js) a schedule of upcoming
// milestones; its daily Periodic Background Sync shows the due ones while the
// app is closed. Requires an installed PWA in a Chromium browser; elsewhere
// nothing is shown, which is the intended fallback.
private fun writeSchedule(json: String): Unit = js(
    """{
    if (!('caches' in self)) return;
    caches.open('meatpiety-data').then(function (c) {
      return c.put('/__schedule.json', new Response(json, { headers: { 'content-type': 'application/json' } }));
    });
    if (typeof Notification === 'undefined' || !('serviceWorker' in navigator)) return;
    var register = function () {
      navigator.serviceWorker.ready.then(function (reg) {
        if (reg.periodicSync) reg.periodicSync.register('milestones', { minInterval: 24 * 60 * 60 * 1000 }).catch(function () {});
      });
    };
    if (Notification.permission === 'granted') register();
    else if (Notification.permission === 'default') Notification.requestPermission().then(function (p) { if (p === 'granted') register(); });
  }"""
)

actual object Notifier {
    actual fun foreground(snapshot: LedgerSnapshot, notices: List<Notice>) {
        val json = buildJsonObject {
            put("seen", buildJsonArray { notices.forEach { add(JsonPrimitive(it.id)) } })
            put("upcoming", buildJsonArray {
                upcomingNotices(snapshot, notices).forEach { (day, n) ->
                    add(buildJsonObject {
                        put("day", day)
                        put("id", n.id)
                        put("title", n.title)
                        put("body", n.body)
                    })
                }
            })
        }
        writeSchedule(json.toString())
    }
}
