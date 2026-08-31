package com.xiaoquexing.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xiaoquexing.app.util.ReminderScheduler

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (ReminderScheduler.isEnabled(context)) ReminderScheduler.schedule(context)
            return
        }
        if (!ReminderScheduler.isEnabled(context)) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            2101,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今天记下小确幸了吗")
            .setContentText("一分钟就够，植物会记得你来过。")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(2102, note) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL, "每日提醒", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val CHANNEL = "xqx_daily"
    }
}
