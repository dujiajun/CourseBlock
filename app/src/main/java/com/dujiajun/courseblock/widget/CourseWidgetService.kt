package com.dujiajun.courseblock.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dujiajun.courseblock.R
import com.dujiajun.courseblock.helper.WeekManager.Companion.getInstance
import com.dujiajun.courseblock.model.Course
import org.litepal.LitePal.findAll
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

class CourseWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CourseRemoteViewsFactory(this, intent)
    }

    class CourseRemoteViewsFactory(private val context: Context, intent: Intent) : RemoteViewsFactory {
        private val dayInWeek: Array<String>

        init {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)
            dayInWeek = context.resources.getStringArray(R.array.days_in_week)
        }

        override fun getViewAt(position: Int): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.item_widget_course)
            val course = courses[position]
            rv.setTextViewText(R.id.tv_course, course.courseName)
            rv.setTextViewText(R.id.tv_location, course.location)
            rv.setTextViewText(R.id.tv_day, dayInWeek[course.day - 1])
            val time = String.format(Locale.CHINA, "%s-%s",
                    Course.START_TIMES[course.start - 1],
                    Course.END_TIMES[course.start + course.step - 2])
            rv.setTextViewText(R.id.tv_time, time)
            val extras = Bundle()
            extras.putInt(CourseAppWidgetProvider.EXTRA_ITEM, course.id)
            val fillInIntent = Intent()
            fillInIntent.putExtras(extras)
            rv.setOnClickFillInIntent(R.id.item_widget_course, fillInIntent)
            return rv
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        private fun loadData() {
            courses.clear()
            val calendar = Calendar.getInstance(Locale.CHINA)
            val tomorrow = calendar[Calendar.DAY_OF_WEEK]
            var today = tomorrow - 1
            if (today == 0) today = 7
            val origin_courses = findAll(Course::class.java)
            val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
            val now = Date()
            val curWeek = getInstance(context).getCurWeek()
            for (course in origin_courses) {
                if (course.weekCode[curWeek - 1] == '0') continue
                if (course.day == tomorrow) {
                    courses.add(course)
                    continue
                }
                if (course.day == today) {
                    try {
                        val expectedCalendar = Calendar.getInstance(Locale.CHINA)
                        simpleDateFormat.parse(Course.END_TIMES[course.step + course.start - 2])?.let { expected ->
                            expectedCalendar.time = expected
                            expectedCalendar[Calendar.YEAR] = calendar[Calendar.YEAR]
                            expectedCalendar[Calendar.MONTH] = calendar[Calendar.MONTH]
                            expectedCalendar[Calendar.DAY_OF_MONTH] = calendar[Calendar.DAY_OF_MONTH]
                        }
                        val expected = expectedCalendar.time
                        if (expected.time > now.time) {
                            courses.add(course)
                        }
                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }
                }
            }
            courses.sortWith { o1: Course, o2: Course ->
                if (o1.day == 7 && o2.day == 1) return@sortWith -1
                if (o1.day == 1 && o2.day == 7) return@sortWith 1
                if (o1.day == o2.day) {
                    return@sortWith o1.start.compareTo(o2.start)
                }
                o1.day.compareTo(o2.day)
            }
        }

        override fun onCreate() {}
        override fun onDataSetChanged() {
            loadData()
        }

        override fun getCount(): Int {
            return courses.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun onDestroy() {
            courses.clear()
        }

        companion object {
            private val courses: MutableList<Course> = ArrayList()
        }
    }
}