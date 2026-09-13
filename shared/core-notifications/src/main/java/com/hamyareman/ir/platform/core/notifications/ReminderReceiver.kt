package com.hamyareman.ir.platform.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.notifications.R

/**
 * یادآور را به اعلان تبدیل می‌کند.
 *
 * دو اصل محصول اینجا اعمال می‌شود:
 *  1) در ساعات سکوت هیچ اعلانی نشان داده نمی‌شود (مگر تماس).
 *  2) بعد از نمایش، یادآور فردا دوباره زمان‌بندی می‌شود (تکرار روزانه).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: NotificationChannels.REMINDERS

        val scheduler = ReminderScheduler(context)
        if (scheduler.quietHours.isQuietNow()) {
            // سکوت یعنی سکوت: فقط فردا دوباره زمان‌بندی می‌کنیم.
            scheduler.find(id)?.let { scheduler.schedule(it) }
            return
        }

        show(context, id, title, body, channel)
        scheduler.find(id)?.let { scheduler.schedule(it) }
    }

    private fun show(context: Context, id: String, title: String, body: String, channel: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                id.hashCode(),
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id.hashCode(), notification) }
        // ذخیره‌ی «آخرین یادآور نمایش‌داده‌شده» برای آمار ملایم (بدون فشار به کاربر)
        LocalStore(context, ReminderScheduler.REMINDER_STORE).putLong("last_shown_${id}", System.currentTimeMillis())
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_BODY = "reminder_body"
        const val EXTRA_CHANNEL = "reminder_channel"
    }
}
