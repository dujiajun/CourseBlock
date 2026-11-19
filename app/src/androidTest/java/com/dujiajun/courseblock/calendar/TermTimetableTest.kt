package com.dujiajun.courseblock.calendar

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import com.dujiajun.courseblock.R
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.helper.CourseManager
import com.dujiajun.courseblock.model.Course
import com.zhuangfei.timetable.view.WeekView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.litepal.LitePal

class TermTimetableTest {
    @Test
    fun switchingTermsKeepsCoursesInTheirOwnTerm() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val priorYear = preferences.getString(PreferenceKey.CURRENT_YEAR, null)
        val priorTerm = preferences.getString(PreferenceKey.CURRENT_TERM, null)
        val priorStatus = preferences.getString(PreferenceKey.STATUS, null)
        val firstTerm = AcademicTerm(2040, 1).key(School.UNDERGRADUATE)
        val secondTerm = AcademicTerm(2040, 3).key(School.UNDERGRADUATE)

        try {
            assertTrue(Course().apply {
                courseName = "Fall fixture"
                termKey = firstTerm
                weekCode = "1"
            }.save())
            assertTrue(Course().apply {
                courseName = "Summer fixture"
                termKey = secondTerm
                weekCode = "1"
            }.save())

            val manager = CourseManager.getInstance(context)
            preferences.edit()
                .putString(PreferenceKey.STATUS, "0")
                .putString(PreferenceKey.CURRENT_YEAR, "2040")
                .putString(PreferenceKey.CURRENT_TERM, "1")
                .commit()
            manager.updateStatus()
            manager.loadCourses()
            assertEquals(listOf("Fall fixture"), manager.courses.map { it.courseName })

            preferences.edit().putString(PreferenceKey.CURRENT_TERM, "3").commit()
            manager.loadCourses()
            assertEquals(listOf("Summer fixture"), manager.courses.map { it.courseName })
        } finally {
            LitePal.deleteAll(Course::class.java, "termKey = ? OR termKey = ?", firstTerm, secondTerm)
            preferences.edit()
                .putString(PreferenceKey.CURRENT_YEAR, priorYear)
                .putString(PreferenceKey.CURRENT_TERM, priorTerm)
                .putString(PreferenceKey.STATUS, priorStatus)
                .commit()
        }
    }

    @Test
    fun weekPickerCanMoveFromAFullTermToAShortTerm() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val themed = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
            val layout = LayoutInflater.from(themed).inflate(R.layout.content_main, null)
            val picker = layout.findViewById<WeekView>(R.id.id_weekview)
            picker.curWeek(10).selectedWeek(10).itemCount(18).showView()
            picker.curWeek(0).itemCount(4).showView()
            picker.resetBackground()
            assertEquals(4, picker.container.childCount)
        }
    }
}
