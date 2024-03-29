package com.dujiajun.courseblock.downloader

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import com.dujiajun.courseblock.model.Course
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

open class GraduateDownloader(context: Context) : CourseDownloader(context) {
    init {
        loginUrl = "http://yjs.sjtu.edu.cn/gsapp/sys/wdkbapp/*default/index.do"
        courseUrl = "http://yjs.sjtu.edu.cn/gsapp/sys/wdkbapp/modules/xskcb/xspkjgcx.do"
        afterLoginPattern = "http://yjs.sjtu.edu.cn:81"

        START_TIMES = arrayOf(
            "8:00", "8:55", "10:00", "10:55",
            "12:00", "12:55", "14:00", "14:55",
            "16:00", "16:55", "18:00", "18:55",
            "20:00", "20:55"
        )
        END_TIMES = arrayOf(
            "8:45", "9:40", "10:45", "11:40",
            "12:45", "13:40", "14:45", "15:40",
            "16:45", "17:40", "18:45", "19:40",
            "20:45", "21:40"
        )
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
                Log.i(TAG, "GraduateDownloader code: $code, body: $body")
                val message = Message()
                when (code) {
                    500 -> {
                        message.what = FAILED
                    }

                    403, 302 -> {
                        message.what = UNLOGIN
                    }

                    else -> {
                        courses = parseFrom(body)
                        message.what = DOWNLOADED
                        message.obj = courses
                    }
                }
                handler.sendMessage(message)
            }
        })
    }

    override fun parseFrom(json: String): List<Course> {
        try {
            val jsonObject = JSONObject(json).getJSONObject("datas").getJSONObject("xspkjgcx")
            val courseRows = jsonObject.getJSONArray("rows")
            val endTime = HashMap<String, Int>()
            val map = HashMap<String, Course>()
            for (i in 0 until courseRows.length()) {
                val courseRow = courseRows.getJSONObject(i)
                val classId = courseRow.getString("BJMC")
                var course = map[classId]
                if (course == null) {
                    course = Course()
                    course.courseId = courseRow.getString("KCDM")
                    course.classId = classId
                    course.courseName = courseRow.getString("KCMC")
                    course.teacher = courseRow.getString("JSXM")
                    course.day = courseRow.getInt("XQ")
                    course.note = courseRow.optString("KBBZ", "")
                    course.location = courseRow.getString("JASMC")
                    course.weekCode = courseRow.getString("ZCBH").substring(0, Course.MAX_WEEKS)
                    course.isFromServer = true
                    map[classId] = course
                }
                val classTime = courseRow.getInt("KSJCDM")
                course.start = classTime.coerceAtMost(course.start)
                val end = endTime[classId]
                if (end == null || end < classTime) {
                    endTime[classId] = classTime
                }
            }
            val courses: List<Course> = map.values.toList()
            for (c in courses) {
                var end = endTime[c.classId]
                if (end == null) end = Course.MAX_STEPS
                c.step = end - c.start + 1
            }
            return courses
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return ArrayList()
    }

    private fun convertParams(year: String, term: String): String {
        return if (term == "2") {
            String.format(Locale.CHINA, "%d02", year.toInt() + 1)
        } else {
            String.format(Locale.CHINA, "%s09", year)
        }
    }

    override fun download(year: String, term: String, callback: Callback) {
        val body: FormBody = FormBody.Builder()
            .add("XNXQDM", convertParams(year, term))
            .add("XH", "")
            .build()
        val request: Request = Request.Builder()
            .url(courseUrl)
            .post(body)
            .build()
        client.newCall(request).enqueue(callback)
    }
}