package com.dujiajun.courseblock.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AcademicCalendarTest {
    @Test
    fun fallUsesTheOfficialWeekAndMovesTheHolidayClasses() {
        val calendar = requireNotNull(OfficialCalendars.find(AcademicTerm(2026, 1), School.UNDERGRADUATE))
        assertEquals(10, calendar.openingWeek(LocalDate.parse("2026-11-19")))
        assertEquals(1, calendar.openingWeek(LocalDate.parse("2027-03-01")))
        assertEquals(listOf(LocalDate.parse("2026-10-10")), calendar.classDates(4, 2))
        assertEquals(listOf(LocalDate.parse("2026-09-20")), calendar.classDates(3, 5))
        assertTrue(calendar.classDates(2, 5).isEmpty())
    }

    @Test
    fun summerAndMedicineUseTheirOwnCalendar() {
        val summer = requireNotNull(OfficialCalendars.find(AcademicTerm(2025, 3), School.UNDERGRADUATE))
        assertEquals(LocalDate.parse("2026-07-06"), summer.firstMonday)
        assertEquals(4, summer.weekCount)
        val medicine = requireNotNull(OfficialCalendars.find(AcademicTerm(2026, 1), School.MEDICINE))
        assertEquals(LocalDate.parse("2026-09-07"), medicine.firstMonday)
        assertEquals(LocalDate.parse("2027-01-17"), medicine.lastDay)
        assertNull(OfficialCalendars.find(AcademicTerm(2099, 1), School.UNDERGRADUATE))
    }

    @Test
    fun selectionFollowsTheLiveTermAndUsesTheNextTermInBreaks() {
        fun selected(date: String, school: School = School.UNDERGRADUATE) =
            OfficialCalendars.suggestedTerm(LocalDate.parse(date), school)

        assertEquals(AcademicTerm(2026, 1), selected("2026-11-19"))
        assertEquals(AcademicTerm(2026, 1), selected("2026-08-03"))
        assertEquals(AcademicTerm(2025, 3), selected("2026-07-08"))
        assertEquals(AcademicTerm(2026, 1), selected("2026-07-08", School.GRADUATE))
        assertEquals(AcademicTerm(2026, 2), selected("2027-01-20"))
        assertEquals(AcademicTerm(2026, 2), selected("2027-01-20", School.MEDICINE))
    }
}
