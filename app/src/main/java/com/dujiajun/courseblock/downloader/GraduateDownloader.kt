package com.dujiajun.courseblock.downloader

import android.content.Context
import android.os.Handler
import android.os.Message
import com.dujiajun.courseblock.model.Course
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
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
                response.use {
                    try {
                        when (response.code) {
                            302, 401, 403 -> handler.sendEmptyMessage(UNLOGIN)
                            200 -> handler.obtainMessage(
                                DOWNLOADED, parseFrom(response.body?.string().orEmpty()),
                            ).sendToTarget()
                            else -> handler.sendEmptyMessage(FAILED)
                        }
                    } catch (e: Exception) {
                        handler.sendEmptyMessage(FAILED)
                    }
                }
            }
        })
    }

    internal fun parseFrom(json: String): List<Course> {
        val rows = JSONObject(json).getJSONObject("datas").getJSONObject("xspkjgcx").getJSONArray("rows")
        val bySlot = (0 until rows.length()).map { rows.getJSONObject(it) }.groupBy {
            listOf(it.getString("KCDM"), it.getString("BJMC"), it.getInt("XQ"),
                it.getString("ZCBH"), it.getString("JASMC"), it.getString("JSXM"))
        }
        return bySlot.values.flatMap { sections ->
            val sample = sections.first()
            val periods = sections.map { it.getInt("KSJCDM") }.distinct().sorted()
            val blocks = mutableListOf<IntRange>()
            periods.forEach { period ->
                require(period in 1..Course.MAX_STEPS)
                val previous = blocks.lastOrNull()
                if (previous != null && previous.last + 1 == period) {
                    blocks[blocks.lastIndex] = previous.first..period
                } else {
                    blocks.add(period..period)
                }
            }
            blocks.map { block ->
                Course().apply {
                    courseId = sample.getString("KCDM")
                    classId = sample.getString("BJMC")
                    courseName = sample.getString("KCMC")
                    teacher = sample.getString("JSXM")
                    day = sample.getInt("XQ")
                    note = sample.optString("KBBZ", "")
                    location = sample.getString("JASMC")
                    weekCode = sample.getString("ZCBH").take(Course.MAX_WEEKS).padEnd(Course.MAX_WEEKS, '0')
                    start = block.first
                    step = block.last - block.first + 1
                    isFromServer = true
                }
            }
        }
    }

    private fun convertParams(year: String, term: String): String {
        return when (term) {
            "1" -> String.format(Locale.CHINA, "%s09", year)
            "2" -> String.format(Locale.CHINA, "%d02", year.toInt() + 1)
            else -> error("Unsupported graduate term: $term")
        }
    }

    private fun download(year: String, term: String, callback: Callback) {
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