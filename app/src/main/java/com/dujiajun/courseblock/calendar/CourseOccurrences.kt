package com.dujiajun.courseblock.calendar

import com.dujiajun.courseblock.model.Course
import com.zhuangfei.timetable.model.Schedule
import com.zhuangfei.timetable.model.ScheduleEnable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

internal data class CalendarClass(
    val title: String,
    val location: String,
    val start: Instant,
    val end: Instant,
)

internal object CourseOccurrences {
    class InvalidTiming(val courseName: String) : IllegalArgumentException(courseName)

    private val sectionTime = DateTimeFormatter.ofPattern("H:mm")

    fun display(courses: List<Course>): List<DisplayedCourse> =
        courses.filter { it.day in 1..7 }.map(::DisplayedCourse)

    fun upcoming(
        courses: List<Course>,
        calendar: AcademicCalendar,
        starts: Array<String>,
        ends: Array<String>,
        now: Instant,
    ): List<CalendarClass> =
        courses.flatMap { course ->
            val dates = dates(course, calendar)
            if (dates.isEmpty()) return@flatMap emptyList()
            val start = starts.getOrNull(course.start - 1)?.let { LocalTime.parse(it, sectionTime) }
                ?: throw InvalidTiming(course.courseName)
            val end = ends.getOrNull(course.lastSection - 1)?.let { LocalTime.parse(it, sectionTime) }
                ?: throw InvalidTiming(course.courseName)
            val sessions = actualSessions(course).ifEmpty { dates.map { it.atTime(start) to null } }
            sessions.mapNotNull { (startAt, endAt) ->
                val startInstant = startAt.atZone(AcademicCalendar.ZONE).toInstant()
                val finish = endAt ?: startAt.toLocalDate().atTime(end)
                val endInstant = finish.atZone(AcademicCalendar.ZONE).toInstant()
                if (startInstant <= now) return@mapNotNull null
                if (endInstant <= startInstant) throw InvalidTiming(course.courseName)
                CalendarClass(course.courseName, course.location, startInstant, endInstant)
            }
        }.distinct().sortedBy { it.start }

    private fun dates(course: Course, calendar: AcademicCalendar): List<LocalDate> {
        val exact = actualSessions(course)
        if (exact.isNotEmpty()) return exact.map { it.first.toLocalDate() }
        return course.schedule.weekList.filter { it <= calendar.weekCount }.flatMap { week ->
            if (course.isFromServer || course.followsAcademicCalendar) calendar.classDates(week, course.day)
            else listOf(calendar.date(week, course.day))
        }
    }

    private fun actualSessions(course: Course): List<Pair<LocalDateTime, LocalDateTime?>> =
        course.actualSessions.orEmpty().split(',').filter(String::isNotBlank).map { session ->
            val start = LocalDateTime.parse(session.substringBefore('/'))
            val end = session.substringAfter('/', "").takeIf(String::isNotBlank)?.let(LocalDateTime::parse)
            start to end
        }
}
