package com.dujiajun.courseblock.helper

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.model.Course
import org.litepal.LitePal.deleteAll
import org.litepal.LitePal.findAll
import org.litepal.LitePal.saveAll
import java.util.Calendar
import java.util.Locale

class CourseManager private constructor(private val context: Context) {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    var courses: List<Course> = ArrayList()

    private var downloader: CourseDownloader = UndergraduateDownloader(context)
    private var status: STATUS = STATUS.UNDERGRADUATE

    init {
        updateStatus()
    }

    fun updateStatus() {
        val status = when (preferences.getString(PreferenceKey.STATUS, "0")) {
            "1" -> STATUS.GRADUATE
            "2" -> STATUS.MEDICINE
            else -> STATUS.UNDERGRADUATE
        }

        if (status != this.status) {
            this.status = status
            downloader = when (status) {
                STATUS.GRADUATE -> GraduateDownloader(this.context)
                STATUS.MEDICINE -> MedicineDownloader(this.context)
                else -> UndergraduateDownloader(this.context)
            }
        }
    }

    fun loadCourses() {
        courses = findAll(Course::class.java)
    }

    fun updateCourses(uiHandler: Handler) {
        val handler: Handler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val uiMessage = Message()
                if (msg.what == CourseDownloader.DOWNLOADED) {
                    val downloadedCourses = msg.obj as List<Course>
                    updateCourses(downloadedCourses)
                    uiMessage.what = SUCCEEDED
                    uiMessage.obj = msg.obj
                } else {
                    uiMessage.what = msg.what
                }
                uiHandler.sendMessage(uiMessage)
            }
        }
        val year = preferences.getString("cur_year", defaultYear)
        val term = preferences.getString("cur_term", defaultTerm)
        downloader.getCourses(year!!, term!!, handler)
    }

    private fun updateCourses(courses: List<Course>) {
        if (preferences.getBoolean("remove_customized_when_sync", false)) {
            deleteAll(Course::class.java)
        } else {
            deleteAll(Course::class.java, "isFromServer = ?", "1")
        }
        saveAll(courses)
    }

    val loginUrl: String
        get() = downloader.loginUrl
    val afterLoginPattern: String
        get() = downloader.afterLoginPattern

    val startTimes: Array<String>
        get() = downloader.START_TIMES

    val endTimes: Array<String>
        get() = downloader.END_TIMES

    fun setReferer(referer: String) {
        downloader.referer = referer
        preferences.edit().putString(PreferenceKey.REFERER, referer).apply()
    }

    fun findCourseByDayAndStart(week: Int, day: Int, start: Int): Course? {
        for (course in courses) {
            if (course.weekCode[week - 1] == '1' && course.day == day && course.start <= start && course.start + course.step - 1 >= start) return course
        }
        return null
    }

    enum class STATUS {
        UNDERGRADUATE, GRADUATE, MEDICINE
    }

    companion object {
        const val SUCCEEDED = CourseDownloader.DOWNLOADED
        const val FAILED = CourseDownloader.FAILED
        const val UNLOGIN = CourseDownloader.UNLOGIN
        private var singleton: CourseManager? = null

        @JvmStatic
        fun getInstance(context: Context): CourseManager {
            if (singleton == null) {
                singleton = CourseManager(context)
            }
            return singleton!!
        }

        @JvmStatic
        val defaultYear: String
            get() {
                val calendar = Calendar.getInstance(Locale.CHINA)
                val curRealMonth = calendar[Calendar.MONTH] + 1
                var curRealYear = calendar[Calendar.YEAR]
                if (curRealMonth < 9) {
                    curRealYear-- // 9月前为上一学年
                }
                return curRealYear.toString()
            }

        @JvmStatic
        val defaultTerm: String
            get() {
                val calendar = Calendar.getInstance(Locale.CHINA)
                val curRealMonth = calendar[Calendar.MONTH] + 1
                val term: String = if (curRealMonth >= 8 || curRealMonth < 2) {
                    "1" // 秋季学期
                } else if (curRealMonth < 7) {
                    "2" // 春季学期
                } else {
                    "3" //夏季学期
                }
                return term
            }
    }
}