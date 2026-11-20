package com.dujiajun.courseblock.calendar

import com.zhuangfei.timetable.listener.OnDateBuildAdapter
import java.time.LocalDate

internal class TimetableDates(private val calendar: () -> AcademicCalendar?) : OnDateBuildAdapter() {
    private var displayWeek = 1

    override fun onUpdateDate(curWeek: Int, targetWeek: Int) {
        displayWeek = targetWeek
        val monday = calendar()?.takeIf { targetWeek in 1..it.weekCount }?.date(targetWeek, 1)
        textViews[0]?.text = monday?.let { "${it.monthValue}\n月" }.orEmpty()
        for (day in 1..7) {
            textViews[day]?.text = monday?.plusDays(day - 1L)?.let { "${it.dayOfMonth}日" }.orEmpty()
        }
        onHighLight()
    }

    override fun onHighLight() {
        initDateBackground()
        val today = LocalDate.now(AcademicCalendar.ZONE)
        if (calendar()?.weekOf(today) == displayWeek) activeDateBackground(today.dayOfWeek.value)
    }
}
