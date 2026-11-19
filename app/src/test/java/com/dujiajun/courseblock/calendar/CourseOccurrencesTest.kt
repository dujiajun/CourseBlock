package com.dujiajun.courseblock.calendar

import com.dujiajun.courseblock.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class CourseOccurrencesTest {
    private val term = AcademicTerm(2026, 1)

    @Test
    fun timetableKeepsTheTeachingSlotsAndExportUsesTheMakeupDate() {
        val course = Course().apply {
            courseName = "数学"
            location = "101"
            day = 2
            start = 1
            step = 2
            weekCode = "0001000001"
            isFromServer = true
        }
        val calendar = requireNotNull(OfficialCalendars.find(term, School.UNDERGRADUATE))
        val blocks = CourseOccurrences.display(listOf(course))
        assertTrue(blocks.none { it.isAt(4, 6, 1) })
        assertTrue(blocks.any { it.isAt(4, 2, 1) })
        assertTrue(blocks.any { it.isAt(10, 2, 2) })

        val start = arrayOf("8:00", "8:55")
        val end = arrayOf("8:45", "9:40")
        val upcoming = CourseOccurrences.upcoming(
            listOf(course), calendar, start, end, Instant.parse("2026-10-07T00:00:00Z"),
        )
        assertEquals(listOf("2026-10-10T00:00:00Z", "2026-11-17T00:00:00Z"), upcoming.map { it.start.toString() })
        assertEquals("2026-10-10T01:40:00Z", upcoming.first().end.toString())
        assertEquals(course.location, upcoming.first().location)
    }

    @Test
    fun medicineKeepsTheServerDateAndTime() {
        val actual = LocalDateTime.parse("2026-11-19T08:03:00")
        val finish = actual.plusMinutes(79)
        val course = Course().apply {
            courseName = "医学"
            start = 1
            step = 2
            day = 4
            weekCode = "00000000001"
            actualSessions = "$actual/$finish"
            isFromServer = true
        }
        val calendar = requireNotNull(OfficialCalendars.find(term, School.MEDICINE))
        val occurrence = CourseOccurrences.upcoming(
            listOf(course), calendar, arrayOf("08:00"), arrayOf("08:40", "09:30"), Instant.EPOCH,
        ).single()
        assertEquals(actual.atZone(AcademicCalendar.ZONE).toInstant(), occurrence.start)
        assertEquals(finish.atZone(AcademicCalendar.ZONE).toInstant(), occurrence.end)
        assertTrue(CourseOccurrences.display(listOf(course)).single().isAt(11, 4, 1))
    }

    @Test
    fun oldCourseThatRanPastTheFinalPeriodUsesTheFinalPeriodsEnd() {
        val course = Course().apply {
            courseName = "晚课"
            day = 2
            start = 14
            step = 2
            weekCode = "1"
        }
        val calendar = requireNotNull(OfficialCalendars.find(term, School.UNDERGRADUATE))
        val occurrence = CourseOccurrences.upcoming(
            listOf(course), calendar, Array(14) { "20:55" }, Array(14) { "21:40" }, Instant.EPOCH,
        ).single()
        assertEquals("2026-09-15T13:40:00Z", occurrence.end.toString())
    }

    @Test
    fun legacyMedicalRowWithUnknownPeriodStaysVisibleAndSelectable() {
        val course = Course().apply {
            courseName = "原有课程"
            day = 1
            start = 0
            step = 2
            weekCode = "1"
            isFromServer = true
        }
        val displayed = CourseOccurrences.display(listOf(course)).single()
        assertEquals(1, displayed.schedule.start)
        assertEquals(2, displayed.schedule.step)
        assertTrue(displayed.isAt(1, 1, 1))
    }

    @Test
    fun theTimetableIgnoresInvalidSourceWeekdays() {
        val valid = Course().apply { day = 5 }
        val invalid = Course().apply { day = 0 }
        assertEquals(listOf(valid), CourseOccurrences.display(listOf(invalid, valid)).map { it.course })
    }

    @Test
    fun graduateCalendarsUseThePublishedRulesForTheirEnrollmentMode() {
        val full = requireNotNull(OfficialCalendars.find(term, School.GRADUATE))
        val part = requireNotNull(OfficialCalendars.find(term, School.PART_TIME_GRADUATE))
        assertEquals("2026-10-10", full.classDates(4, 2).single().toString())
        assertTrue(part.classDates(4, 2).isEmpty())
        assertEquals("2026-10-06", part.classDates(4, 6).single().toString())
    }
}
