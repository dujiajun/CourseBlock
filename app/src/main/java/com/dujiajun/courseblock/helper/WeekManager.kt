package com.dujiajun.courseblock.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.model.Course
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeekManager private constructor(context: Context) {
    private val preferences: SharedPreferences
    private var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private var curWeek = 0
    lateinit var firstDate: Date
    lateinit var lastDate: Date

    init {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun updateCurWeek() {
        loadFirstDay()
        loadLastDay()
        val diff = Date().time - firstDate.time
        curWeek = (diff / 1000 / 60 / 60 / 24 / 7 + 1).toInt()
        if (curWeek < 1) curWeek = 1
        if (curWeek > Course.MAX_WEEKS) curWeek = Course.MAX_WEEKS
    }

    fun setFirstDay(year: Int, month: Int, day_of_month: Int) {
        val calendar = Calendar.getInstance(Locale.CHINA)
        calendar[year, month, day_of_month, 0, 0] = 0
        var dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        if (dayOfWeek == 1) dayOfWeek = 8
        calendar.add(Calendar.DATE, 2 - dayOfWeek)
        firstDate = calendar.time
        saveFirstDay()
    }

    fun setLastDay(year: Int, month: Int, day_of_month: Int) {
        val calendar = Calendar.getInstance(Locale.CHINA)
        calendar[year, month, day_of_month, 0, 0] = 0
        var dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        if (dayOfWeek == 1) dayOfWeek = 8
        calendar.add(Calendar.DATE, 8 - dayOfWeek)
        lastDate = calendar.time
        saveLastDay()
    }

    private fun saveFirstDay() {
        val editor = preferences.edit()
        editor.putString("first_monday", showFirstDate)
        editor.apply()
    }

    private fun saveLastDay() {
        val editor = preferences.edit()
        editor.putString("last_sunday", showLastDate)
        editor.apply()
    }

    fun loadFirstDay(): Date {
        preferences.getString("first_monday", FIRST_DATE)?.let { s ->
            simpleDateFormat.parse(s)?.let {
                firstDate = it
            }
        }
        return firstDate
    }

    fun loadLastDay(): Date {
        preferences.getString("last_sunday", LAST_DATE)?.let { s ->
            simpleDateFormat.parse(s)?.let {
                lastDate = it
            }
        }
        return lastDate
    }

    val showFirstDate: String
        get() = simpleDateFormat.format(firstDate)

    val showLastDate: String
        get() = simpleDateFormat.format(lastDate)

    fun getCurWeek(): Int {
        updateCurWeek()
        return curWeek
    }

    companion object {
        const val FIRST_DATE = "2021-09-13"
        const val LAST_DATE = "2021-09-13"
        private var singleton: WeekManager? = null

        @JvmStatic
        fun getInstance(context: Context): WeekManager {
            if (singleton == null) {
                singleton = WeekManager(context)
            }
            return singleton!!
        }
    }
}