package com.dujiajun.courseblock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.dujiajun.courseblock.CourseActivity
import com.dujiajun.courseblock.MainActivity
import com.dujiajun.courseblock.R

class CourseAppWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val mgr = AppWidgetManager.getInstance(context)
        if (intent.action == ACTION_COURSE_DETAIL) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)
            val courseId = intent.getIntExtra(EXTRA_ITEM, 0)
            val courseIntent = Intent(context, CourseActivity::class.java)
            courseIntent.putExtra("action", CourseActivity.ACTION_DETAIL)
            courseIntent.putExtra("course", courseId)
            courseIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(courseIntent)
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val rv = RemoteViews(context.packageName, R.layout.appwidget)
            val mainIntent = Intent("android.intent.action.MAIN")
            mainIntent.addCategory("android.intent.category.LAUNCHER")
            mainIntent.component = ComponentName(context.packageName,
                    MainActivity::class.java.name)
            val mainPending = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            rv.setOnClickPendingIntent(R.id.title, mainPending)
            rv.setOnClickPendingIntent(R.id.empty_view, mainPending)
            val serviceIntent = Intent(context, CourseWidgetService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            rv.setRemoteAdapter(R.id.lv_courses, serviceIntent)
            rv.setEmptyView(R.id.lv_courses, R.id.empty_view)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_courses)
            val updateIntent = Intent(context, CourseAppWidgetProvider::class.java)
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            rv.setOnClickPendingIntent(R.id.iv_refresh, pendingIntent)
            val toastIntent = Intent(context, CourseAppWidgetProvider::class.java)
            toastIntent.action = ACTION_COURSE_DETAIL
            toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            rv.setPendingIntentTemplate(R.id.lv_courses, toastPendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        const val ACTION_COURSE_DETAIL = "com.dujiajun.courseblock.ACTION_COURSE_DETAIL"
        const val EXTRA_ITEM = "com.dujiajun.courseblock.EXTRA_ITEM"
    }
}