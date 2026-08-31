package com.xiaoquexing.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xiaoquexing.app.util.ReminderScheduler

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.ensure(context)
            return
        }
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            2101,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (ReminderScheduler.isEnabled(context)) {
            val note = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("今天记下小确幸了吗")
                .setContentText("一分钟就够，植物会记得你来过。")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            notifySafely(context, 2102, note)
        }
        com.xiaoquexing.app.data.remote.AnniversaryStore(context).today().forEachIndexed { index, item ->
            val note = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("今天是「${item.title}」")
                .setContentText("写一条小确幸，把这一天留下来。")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            notifySafely(context, 2200 + index, note)
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(context: Context, id: Int, note: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching { NotificationManagerCompat.from(context).notify(id, note) }
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
