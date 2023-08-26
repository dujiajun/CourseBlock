package com.dujiajun.courseblock.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.dujiajun.courseblock.CourseActivity;
import com.dujiajun.courseblock.MainActivity;
import com.dujiajun.courseblock.R;

public class CourseAppWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_COURSE_DETAIL = "com.dujiajun.courseblock.ACTION_COURSE_DETAIL";
    public static final String EXTRA_ITEM = "com.dujiajun.courseblock.EXTRA_ITEM";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(ACTION_COURSE_DETAIL)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int courseId = intent.getIntExtra(EXTRA_ITEM, 0);
            Intent courseIntent = new Intent(context, CourseActivity.class);
            courseIntent.putExtra("action", CourseActivity.ACTION_DETAIL);
            courseIntent.putExtra("course", courseId);
            courseIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(courseIntent);
        }
        super.onReceive(context, intent);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget);

            Intent mainIntent = new Intent("android.intent.action.MAIN");
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            mainIntent.setComponent(new ComponentName(context.getPackageName(),
                    MainActivity.class.getName()));
            PendingIntent mainPending = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.title, mainPending);
            rv.setOnClickPendingIntent(R.id.empty_view, mainPending);

            Intent serviceIntent = new Intent(context, CourseWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setRemoteAdapter(R.id.lv_courses, serviceIntent);
            rv.setEmptyView(R.id.lv_courses, R.id.empty_view);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_courses);

            Intent updateIntent = new Intent(context, CourseAppWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.iv_refresh, pendingIntent);


            Intent toastIntent = new Intent(context, CourseAppWidgetProvider.class);
            toastIntent.setAction(CourseAppWidgetProvider.ACTION_COURSE_DETAIL);
            toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.lv_courses, toastPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

}
