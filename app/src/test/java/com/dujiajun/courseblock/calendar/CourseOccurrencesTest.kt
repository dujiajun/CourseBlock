package com.dujiajun.courseblock.calendar

import com.dujiajun.courseblock.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseOccurrencesTest {
    private val term = AcademicTerm(2026, 1)

    @Test
    fun timetableKeepsTheTeachingSlotsFromTheCourseSource() {
        val course = Course().apply {
            courseName = "数学"
            location = "101"
            day = 2
            start = 1
            step = 2
            weekCode = "0001000001"
            isFromServer = true
        }
        val blocks = CourseOccurrences.display(listOf(course))
        assertTrue(blocks.none { it.isAt(4, 6, 1) })
        assertTrue(blocks.any { it.isAt(4, 2, 1) })
        assertTrue(blocks.any { it.isAt(10, 2, 2) })

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
