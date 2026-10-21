package com.hereliesaz.meatpiety

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

actual object Notifier {
    private const val CHANNEL = "milestones"
    private const val GROUP = "com.hereliesaz.meatpiety.MILESTONES"
    private const val PREFS = "notifier"
    private const val SEEN = "seen_notice_ids"
    private lateinit var context: Context

    /** Called from the activity before any composition. */
    fun init(context: Context) {
        this.context = context.applicationContext
        this.context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Milestones", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Thresholds your ledger crosses while you're away."
            },
        )
        WorkManager.getInstance(this.context).enqueueUniquePeriodicWork(
            "milestones",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MilestoneWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    actual fun foreground(snapshot: LedgerSnapshot, notices: List<Notice>) {
        if (!::context.isInitialized) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("diet", snapshot.diet.name)
            .putString("influence", snapshot.influencePeople?.toString())
            .putString("currency", snapshot.money.code)
            .putFloat("per_usd", snapshot.money.perUsd.toFloat())
            .putStringSet(SEEN, prefs.getStringSet(SEEN, emptySet()).orEmpty() + notices.map { it.id })
            .apply()
    }

    /** Recomputes the ledger at today's day count and notifies for anything new. */
    internal fun checkInBackground(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        AppContext.init(context)
        val days = StartDate.days() ?: return
        // Never notify over the open app; the next foreground pass absorbs these silently.
        if (ActivityManager.RunningAppProcessInfo().also(ActivityManager::getMyMemoryState).importance <=
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        ) return
        val diet = runCatching { Diet.valueOf(prefs.getString("diet", null)!!) }.getOrDefault(Diet.VEGAN)
        val money = Money(prefs.getString("currency", "USD")!!, prefs.getFloat("per_usd", 1f).toDouble())
        val personal = personalLayer(days, diet)
        val influence = prefs.getString("influence", null)?.toDoubleOrNull()?.let { influenceLayer(it, days, diet) }
        val seen = prefs.getStringSet(SEEN, emptySet()).orEmpty()
        val fresh = milestones(days, personal, influence, worldLayer(days), money).filterNot { it.id in seen }
        if (fresh.isEmpty()) return
        // Without permission nothing is shown, so leave these unseen for when it's granted.
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        prefs.edit().putStringSet(SEEN, seen + fresh.map { it.id }).apply()
        fresh.forEach { post(context, it, summary = false) }
        if (fresh.size > 1) post(context, fresh.first(), summary = true)
    }

    private fun post(context: Context, notice: Notice, summary: Boolean) {
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val builder = Notification.Builder(context, CHANNEL)
            .setSmallIcon(statusIcon(context))
            .setGroup(GROUP)
            .setContentIntent(open)
            .setAutoCancel(true)
        if (summary) {
            builder.setContentTitle("Meat Piety").setGroupSummary(true)
        } else {
            builder.setContentTitle(notice.title).setContentText(notice.body)
                .setStyle(Notification.BigTextStyle().bigText(notice.body))
        }
        context.getSystemService(NotificationManager::class.java)
            .notify(if (summary) GROUP.hashCode() else notice.id.hashCode(), builder.build())
    }
}

// Single-colour status-bar icon from the app module, by name since shared can't see app's R.
private fun statusIcon(context: Context): Int =
    context.resources.getIdentifier("ic_stat_meatpiety", "drawable", context.packageName)
        .takeIf { it != 0 } ?: context.applicationInfo.icon

class MilestoneWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Notifier.checkInBackground(applicationContext)
        return Result.success()
    }
}
