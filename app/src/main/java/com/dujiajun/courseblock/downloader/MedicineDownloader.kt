package com.dujiajun.courseblock.downloader

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.calendar.AcademicCalendar
import com.dujiajun.courseblock.calendar.AcademicTerm
import com.dujiajun.courseblock.calendar.OfficialCalendars
import com.dujiajun.courseblock.calendar.School
import com.dujiajun.courseblock.model.Course
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalTime

class MedicineDownloader(private val context: Context) : CourseDownloader(context) {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    init {
        loginUrl = "https://jwstu.shsmu.edu.cn"
        courseUrl = "https://webvpn2.shsmu.edu.cn/Home/GetCurriculumTable"
        afterLoginPattern = "/Home/Index"
        referer = preferences.getString(PreferenceKey.REFERER, "").toString()

        START_TIMES = arrayOf(
            "08:00", "08:50", "09:40", "10:30",
            "11:20", "13:30", "14:20", "15:10",
            "16:00", "16:50", "17:40", "18:30",
            "19:20", "20:10"
        )

        END_TIMES = arrayOf(
            "08:40", "09:30", "10:20", "11:10",
            "12:00", "14:10", "15:00", "15:50",
            "16:40", "17:30", "18:20", "19:10",
            "20:00", "20:50"
        )
    }


    override fun getCourses(year: String, term: String, handler: Handler) {
        val calendar = OfficialCalendars.find(AcademicTerm(year.toInt(), term.toInt()), School.MEDICINE)
        if (calendar == null) {
            handler.sendEmptyMessage(FAILED)
            return
        }
        val url = courseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("Start", calendar.firstMonday.toString())
            .addQueryParameter("End", calendar.lastDay.plusDays(1).toString())
            .build()
        val request = Request.Builder().url(url).header("Referer", referer).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.sendEmptyMessage(FAILED)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val body = response.body?.string().orEmpty()
                        when {
                            response.code in listOf(302, 401, 403) -> handler.sendEmptyMessage(UNLOGIN)
                            !response.isSuccessful || body.contains("访问出错") -> handler.sendEmptyMessage(FAILED)
                            body.contains("上海交通大学医学院用户认证中心") -> handler.sendEmptyMessage(UNLOGIN)
                            else -> handler.obtainMessage(DOWNLOADED, parseFrom(body, calendar)).sendToTarget()
                        }
                    } catch (e: Exception) {
                        handler.sendEmptyMessage(FAILED)
                    }
                }
            }
        })
    }

    internal fun parseFrom(json: String, calendar: AcademicCalendar): List<Course> {
        val source = JSONObject(json).getJSONArray("List")
        val sectionTimes = START_TIMES.map(LocalTime::parse)
        val courses = linkedMapOf<List<Any>, Pair<Course, MutableMap<LocalDateTime, LocalDateTime?>>>()
        for (index in 0 until source.length()) {
            val row = source.getJSONObject(index)
            val startAt = LocalDateTime.parse(row.getString("Start"))
            val endAt = row.optString("End").takeIf { it.isNotBlank() && it != "null" }
                ?.let { raw ->
                    runCatching { LocalDateTime.parse(raw) }.getOrNull()
                        ?: runCatching { startAt.toLocalDate().atTime(LocalTime.parse(raw)) }.getOrNull()
                }
                ?.takeIf { it > startAt }
            val date = startAt.toLocalDate()
            calendar.weekOf(date) ?: continue
            val course = Course().apply {
                courseName = row.getString("Curriculum")
                courseId = row.getString("CourseCode")
                location = row.getString("Classroom")
                day = date.dayOfWeek.value
                start = (sectionTimes.indexOfLast { it <= startAt.toLocalTime() } + 1).coerceAtLeast(1)
                step = row.getInt("CourseCount")
                isFromServer = true
            }
            require(course.start in 1..Course.MAX_STEPS && course.step in 1..(Course.MAX_STEPS + 1 - course.start))
            val key = listOf(course.courseId, course.courseName, course.location, course.day, course.start, course.step)
            courses.getOrPut(key) { course to sortedMapOf() }.second[startAt] = endAt
        }
        return courses.values.map { (course, sessions) ->
            val weeks = CharArray(Course.MAX_WEEKS) { '0' }
            sessions.keys.forEach { weeks[requireNotNull(calendar.weekOf(it.toLocalDate())) - 1] = '1' }
            course.weekCode = String(weeks)
            course.actualSessions = sessions.entries.joinToString(",") { (start, end) ->
                if (end == null) start.toString() else "$start/$end"
            }
            course
        }
    }
}
