package com.dujiajun.courseblock.helper

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Message
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.model.Course
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.HashSet
import java.util.Locale

class MedicineDownloader(private val context: Context) : CourseDownloader(context) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    init {
        loginUrl = "https://jwstu.shsmu.edu.cn"
        courseUrl = "https://webvpn2.shsmu.edu.cn/Home/GetCurriculumTable"
        afterLoginPattern = "/Home/Index"
        referer = preferences.getString("referer", "").toString()
    }

    companion object {
        val START_TIME = arrayOf("08:00", "08:50", "09:40", "10:30", "11:20", "13:30", "14:20", "15:10", "16:00", "16:50", "17:40", "18:30", "19:20")
    }


    private fun getDayInWeek(date: String): Int {
        val cal = Calendar.getInstance(Locale.CHINA)
        cal.time = SimpleDateFormat("yyyy-MM-dd").parse(date)
        var w = cal[Calendar.DAY_OF_WEEK] - 1;
        if (w == 0) {
            w = 7
        }
        return w
    }

    private fun getCourseWeek(date: String): Int {
        val firstDate = WeekManager.getInstance(this.context).loadFirstDay()
        val thisDate = SimpleDateFormat("yyyy-MM-dd").parse(date)
        return ((thisDate.time - firstDate.time) / 1000 / 3600 / 24 / 7).toInt() + 1
    }

    fun getWeekCode(curWeek: Int): String {
        val code = CharArray(Course.MAX_WEEKS)
        Arrays.fill(code, '0')
        code[curWeek - 1] = '1'
        return String(code)
    }


    override fun getCourses(year: String, term: String, handler: Handler) {
        download(year, term, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val message = Message()
                message.what = FAILED
                handler.sendMessage(message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body!!.string()
                val message = Message()
                when (code) {
                    500 -> {
                        message.what = FAILED
                    }

                    403, 302 -> {
                        message.what = UNLOGIN
                    }

                    else -> {
                        if (body.contains("访问出错")) {
                            message.what = FAILED
                        } else {
                            courses = parseFrom(body)
                            message.what = DOWNLOADED
                            message.obj = courses
                        }

                    }
                }
                handler.sendMessage(message)
            }
        })
    }

    fun dealWeekCodeWithMultiple(weekCodeMap :HashMap<String, CharArray>,code:String,  week :Int){
        if (!weekCodeMap.contains(code)){
            val weekcode = CharArray(Course.MAX_WEEKS)
            Arrays.fill(weekcode, '0')
            weekCodeMap[code] = weekcode
        }

        weekCodeMap[code]?.set(week-1, '1')
    }

    override fun parseFrom(json: String): List<Course> {
        val courses: MutableList<Course> = ArrayList()
        val timeMap = START_TIME.mapIndexed { index, it -> it to index+1 }.toMap()
        val weekCodeMap = HashMap<String, CharArray>()
        try {
            val jsonList = JSONObject(json).getJSONArray("List")
            for (i in 0 until jsonList.length()) {
                val jsonObj = jsonList.getJSONObject(i)
                val course = Course()
                course.courseName = jsonObj.getString("Curriculum")
                course.courseId = jsonObj.getString("CourseCode")
                course.location = jsonObj.getString("Classroom")
                course.step = jsonObj.getInt("CourseCount")
                val (date, start) = jsonObj.getString("Start").split("T")
                course.day = getDayInWeek(date)
                course.start = timeMap[start.substring(0, start.length - 3)] ?: 0
                dealWeekCodeWithMultiple(weekCodeMap, course.courseId, getCourseWeek(date))
                course.isFromServer = true
                courses.add(course)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val finalCourses = ArrayList<Course>()
        val addedMap = HashSet<String>()
        for (course in courses){
            if(addedMap.contains(course.courseId))
                continue
            if (!weekCodeMap.contains(course.courseId)) continue
            weekCodeMap[course.courseId]?.let { course.weekCode = String(it) }
            finalCourses.add(course)
            addedMap.add(course.courseId)
        }
        return finalCourses
    }

    override fun download(year: String, term: String, callback: Callback) {
        val start = preferences.getString("first_monday", WeekManager.FIRST_DATE)?.split(" ")?.get(0)
        val end = preferences.getString("last_sunday", WeekManager.LAST_DATE)?.split(" ")?.get(0)
        val url = "$courseUrl?Start=$start&End=$end"
        val request: Request = Request.Builder()
                .url(url)
                .addHeader("Referer", referer)
                .get()
                .build()
        client.newCall(request).enqueue(callback)
    }
}