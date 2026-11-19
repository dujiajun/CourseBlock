package com.dujiajun.courseblock.downloader

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
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

open class UndergraduateDownloader(context: Context) : CourseDownloader(context) {
    private val JSON: MediaType = "application/json; charset=utf-8".toMediaType()


    init {
        loginUrl = "https://i.sjtu.edu.cn/jaccountlogin"
        courseUrl = "https://i.sjtu.edu.cn/kbcx/xskbcx_cxXsKb.html"
        afterLoginPattern = "index_initMenu.html"

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

            override fun onResponse(call: Call, response: Response) {
                val message = Message()
                response.use {
                    Log.i(TAG, "UndergraduateDownloader code: ${response.code}")
                    try {
                        val body = response.body?.string().orEmpty()
                        message.what = when {
                            response.code in listOf(401, 403) ||
                                (body.trimStart().startsWith("<") && body.contains("jAccount", ignoreCase = true)) -> UNLOGIN
                            !response.isSuccessful -> FAILED
                            else -> {
                                val downloaded = parseFrom(body)
                                courses = downloaded
                                message.obj = downloaded
                                DOWNLOADED
                            }
                        }
                    } catch (error: IOException) {
                        message.what = FAILED
                    } catch (error: JSONException) {
                        message.what = FAILED
                    } catch (error: RuntimeException) {
                        message.what = FAILED
                    }
                }
                handler.sendMessage(message)
            }
        })
    }

    internal fun parseFrom(json: String): List<Course> {
        val courses: MutableList<Course> = ArrayList()
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
            course.note = jsonCourse.optString("xkbz", "")
            course.isFromServer = true
            courses.add(course)
        }
        return courses
    }

    private fun download(year: String, term: String, callback: Callback) {
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

    private fun getWeekCode(weeks: String): String {
        val code = CharArray(Course.MAX_WEEKS) { '0' }
        for (entry in weeks.split(',', '，')) {
            if (entry.isBlank()) continue
            val parity = when {
                '单' in entry -> 1
                '双' in entry -> 0
                else -> null
            }
            val range = Regex("[0-9]+").findAll(entry).map { it.value.toInt() }.toList()
            require(range.isNotEmpty() && range.size <= 2)
            val first = range.first()
            val last = range.last()
            require(first in 1..Course.MAX_WEEKS && last in first..Course.MAX_WEEKS)
            for (week in first..last) {
                if (parity == null || week % 2 == parity) code[week - 1] = '1'
            }
        }
        return String(code)
    }

    private fun getStartAndStep(jcor: String): List<Int> {
        val startAndStep: MutableList<Int> = ArrayList()
        val jc = jcor.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        startAndStep.add(Integer.valueOf(jc[0]))
        startAndStep.add((jc.getOrNull(1) ?: jc[0]).toInt() - jc[0].toInt() + 1)
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
