package com.dujiajun.courseblock.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeekManager private constructor(context: Context) {
    private val preferences: SharedPreferences
    private var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private var curWeek = 0
    lateinit var firstDate: Date

    init {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun updateCurWeek() {
        loadFirstDay()
        val diff = Date().time - firstDate.time
        curWeek = (diff / 1000 / 60 / 60 / 24 / 7 + 1).toInt()
        if (curWeek < 1) curWeek = 1
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

    private fun saveFirstDay() {
        val editor = preferences.edit()
        editor.putString("first_monday", showDate)
        editor.apply()
    }

    private fun loadFirstDay() {
        preferences.getString("first_monday", FIRST_DATE)?.let { s ->
            simpleDateFormat.parse(s)?.let {
                firstDate = it
            }
        }
    }

    val showDate: String
        get() = simpleDateFormat.format(firstDate)

    fun getCurWeek(): Int {
        updateCurWeek()
        return curWeek
    }

    companion object {
        private const val FIRST_DATE = "2021-09-13 00:00:00"
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