package com.dujiajun.courseblock.helper

import android.content.Context
import android.os.Handler
import android.os.Message
import com.dujiajun.courseblock.model.Course
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Arrays

open class UndergraduateDownloader(context: Context) : CourseDownloader(context) {
    private val JSON: MediaType = "application/json; charset=utf-8".toMediaType()

    init {
        loginUrl = "https://i.sjtu.edu.cn/jaccountlogin"
        courseUrl = "https://i.sjtu.edu.cn/kbcx/xskbcx_cxXsKb.html"
        afterLoginPattern = "index_initMenu.html"
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
                if (body.contains("jAccount")) {
                    message.what = UNLOGIN
                } else {
                    courses = parseFrom(body)
                    if (courses.isEmpty()) {
                        message.what = DOWNLOADED
                        message.obj = courses
                    } else {
                        message.what = FAILED
                    }
                }
                handler.sendMessage(message)
            }
        })
    }

    override fun parseFrom(json: String): List<Course> {
        val courses: MutableList<Course> = ArrayList()
        try {
            val jsonObject = JSONObject(json)
            val jsonArrayKb = jsonObject.getJSONArray("kbList")
            for (i in 0 until jsonArrayKb.length()) {
                val jsonCourse = jsonArrayKb.getJSONObject(i)
                val course = Course()
                course.courseId = jsonCourse.getString("kch_id")
                course.classId = jsonCourse.getString("jxbmc")
                course.courseName = jsonCourse.getString("kcmc")
                course.location = jsonCourse.getString("cdmc")
                course.day = jsonCourse.getInt("xqj")
                val startAndStep = getStartAndStep(jsonCourse.getString("jcs"))
                course.start = startAndStep[0]
                course.step = startAndStep[1]
                course.teacher = jsonCourse.getString("xm")
                course.weekCode = getWeekCode(jsonCourse.getString("zcd"))
                course.note = jsonCourse.getString("xkbz")
                course.isFromServer = true
                courses.add(course)
            }
            return courses
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return ArrayList()
    }

    override fun download(year: String, term: String, callback: Callback) {
        val body: FormBody = FormBody.Builder()
                .add("xnm", year)
                .add("xqm", convertParams(term))
                .build()
        val request: Request = Request.Builder()
                .url(courseUrl)
                .post(body)
                .build()
        client.newCall(request).enqueue(callback)
    }

    companion object {
        fun getWeekCode(week: String): String {
            val items = week.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val code = CharArray(Course.MAX_WEEKS)
            Arrays.fill(code, '0')
            for (rawitem in items) {
                var step = 1
                var item = rawitem
                if (item.contains("(单)")) {
                    step = 2
                    item = item.replace("(单)", "")
                }
                if (item.contains("(双)")) {
                    step = 2
                    item = item.replace("(双)", "")
                }
                item = item.replace("周", "")
                val weekStartAndEnd: Array<String> = if (item.contains("-")) {
                    item.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                } else {
                    arrayOf(item, item)
                }
                var i = weekStartAndEnd[0].toInt()
                while (i <= weekStartAndEnd[1].toInt()) {
                    code[i - 1] = '1'
                    i += step
                }
            }
            return String(code)
        }

        protected fun getStartAndStep(jcor: String): List<Int> {
            val startAndStep: MutableList<Int> = ArrayList()
            val jc = jcor.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            startAndStep.add(Integer.valueOf(jc[0]))
            startAndStep.add(jc[1].toInt() - jc[0].toInt() + 1)
            return startAndStep
        }

        private fun convertParams(term: String?): String {
            return when (term) {
                "2" -> "12"
                "3" -> "16"
                else -> "3"
            }
        }
    }
}