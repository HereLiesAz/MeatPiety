package com.hereliesaz.meatpiety

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

actual object Notifier {
    private const val CHANNEL = "milestones"
    private const val GROUP = "com.hereliesaz.meatpiety.MILESTONES"
    private const val SENT = "sent_notice_ids"
    private lateinit var context: Context

    /** Called once from the activity; notices posted earlier are dropped. */
    fun init(context: Context) {
        this.context = context.applicationContext
        val manager = this.context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Milestones", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Thresholds the ledger crosses: animals spared, prices moved, lives outlasted."
            },
        )
    }

    actual fun post(notice: Notice) {
        if (!::context.isInitialized) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = context.getSharedPreferences("notifier", Context.MODE_PRIVATE)
        val sent = prefs.getStringSet(SENT, emptySet()).orEmpty()
        if (notice.id in sent) return
        prefs.edit().putStringSet(SENT, sent + notice.id).apply()

        val manager = context.getSystemService(NotificationManager::class.java)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val icon = context.applicationInfo.icon
        manager.notify(
            notice.id.hashCode(),
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(icon)
                .setContentTitle(notice.title)
                .setContentText(notice.body)
                .setStyle(Notification.BigTextStyle().bigText(notice.body))
                .setGroup(GROUP)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
        // Several milestones often land at once; bundle them under one summary.
        manager.notify(
            GROUP.hashCode(),
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(icon)
                .setContentTitle("Meat Piety")
                .setGroup(GROUP)
                .setGroupSummary(true)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}
