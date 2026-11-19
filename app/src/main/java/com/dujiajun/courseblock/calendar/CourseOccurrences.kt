package com.dujiajun.courseblock.calendar

import com.dujiajun.courseblock.model.Course
import com.zhuangfei.timetable.model.Schedule
import com.zhuangfei.timetable.model.ScheduleEnable

internal class DisplayedCourse(val course: Course) : ScheduleEnable {
    override val schedule: Schedule
        get() = course.schedule.apply {
            start = course.displayStart
            step = course.lastSection - start + 1
        }

    fun isAt(week: Int, day: Int, section: Int): Boolean =
        course.weekCode.getOrNull(week - 1) == '1' && course.day == day &&
            section in course.displayStart..course.lastSection
}

private val Course.displayStart: Int
    get() = start.coerceIn(1, Course.MAX_STEPS)

private val Course.lastSection: Int
    get() = displayStart + step.coerceIn(1, Course.MAX_STEPS - displayStart + 1) - 1

internal object CourseOccurrences {
    fun display(courses: List<Course>): List<DisplayedCourse> =
        courses.filter { it.day in 1..7 }.map(::DisplayedCourse)
}
