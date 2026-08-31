package com.xiaoquexing.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.xiaoquexing.app.MainActivity
import com.xiaoquexing.app.R
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.util.DateKeys
import kotlinx.coroutines.runBlocking

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        Thread {
            try {
                val snap = load(context)
                ids.forEach { id -> manager.updateAppWidget(id, views(context, snap)) }
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val snap = load(context)
            ids.forEach { id -> manager.updateAppWidget(id, views(context, snap)) }
        }

        fun load(context: Context): WidgetSnapshot = runBlocking {
            val db = AppDatabase.getInstance(context)
            val today = DateKeys.epochDay(System.currentTimeMillis())
            val count = db.recordDao().countOnDateAll(today)
            val latest = db.recordDao().latestRecord()
            val gp = db.spaceDao().getDefaultSpace()?.let { db.recordDao().sumAllGp(it.localId) } ?: 0
            WidgetCopy.of(count, latest?.moodTag, latest?.contentText, gp)
        }

        private fun views(context: Context, snap: WidgetSnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            views.setTextViewText(R.id.widget_title, snap.title)
            views.setTextViewText(R.id.widget_body, snap.body)
            views.setTextViewText(R.id.widget_footer, snap.footer)
            val open = PendingIntent.getActivity(
                context,
                31,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            val compose = PendingIntent.getActivity(
                context,
                32,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_COMPOSE, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_compose, compose)
            return views
        }
    }
}
