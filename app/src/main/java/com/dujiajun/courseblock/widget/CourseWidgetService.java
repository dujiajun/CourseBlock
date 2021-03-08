package com.dujiajun.courseblock.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.dujiajun.courseblock.R;
import com.dujiajun.courseblock.helper.WeekManager;
import com.dujiajun.courseblock.model.Course;

import org.litepal.LitePal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CourseRemoteViewsFactory(this, intent);
    }

    public static class CourseRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private static final List<Course> courses = new ArrayList<>();
        private final Context context;
        private final String[] dayInWeek;

        public CourseRemoteViewsFactory(Context context, Intent intent) {
            this.context = context;
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            dayInWeek = context.getResources().getStringArray(R.array.days_in_week);
        }

        @Override
        public RemoteViews getViewAt(int position) {

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.item_widget_course);

            Course course = courses.get(position);
            rv.setTextViewText(R.id.tv_course, course.getCourseName());
            rv.setTextViewText(R.id.tv_location, course.getLocation());
            rv.setTextViewText(R.id.tv_day, dayInWeek[course.getDay() - 1]);
            String time = String.format(Locale.CHINA, "%s-%s",
                    Course.START_TIMES[course.getStart() - 1],
                    Course.END_TIMES[course.getStart() + course.getStep() - 2]);
            rv.setTextViewText(R.id.tv_time, time);

            Bundle extras = new Bundle();
            extras.putInt(CourseAppWidgetProvider.EXTRA_ITEM, course.getId());
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.item_widget_course, fillInIntent);

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }


        private void loadData() {
            courses.clear();
            Calendar calendar = Calendar.getInstance(Locale.CHINA);
            int tomorrow = calendar.get(Calendar.DAY_OF_WEEK);
            int today = tomorrow - 1;
            if (today == 0) today = 7;
            List<Course> origin_courses = LitePal.findAll(Course.class);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
            Date now = new Date();
            int curWeek = WeekManager.getInstance(context).getCurWeek();

            for (Course course : origin_courses) {
                if (course.getWeekCode().charAt(curWeek - 1) == '0')
                    continue;
                if (course.getDay() == tomorrow) {
                    courses.add(course);
                    continue;
                }
                if (course.getDay() == today) {
                    try {
                        Calendar expectedCalendar = Calendar.getInstance(Locale.CHINA);
                        Date expected = simpleDateFormat.parse(Course.END_TIMES[course.getStep() + course.getStart() - 2]);
                        expectedCalendar.setTime(expected);
                        expectedCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                        expectedCalendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
                        expectedCalendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
                        expected = expectedCalendar.getTime();

                        if (expected.getTime() > now.getTime()) {
                            courses.add(course);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            Collections.sort(courses, (o1, o2) -> {
                if (o1.getDay() == 7 && o2.getDay() == 1)
                    return -1;
                if (o1.getDay() == 1 && o2.getDay() == 7)
                    return 1;
                if (o1.getDay() == o2.getDay()) {
                    return Integer.compare(o1.getStart(), o2.getStart());
                }
                return Integer.compare(o1.getDay(), o2.getDay());
            });

        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
            loadData();
        }

        @Override
        public int getCount() {
            return courses.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }


        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public void onDestroy() {
            courses.clear();
        }
    }
}