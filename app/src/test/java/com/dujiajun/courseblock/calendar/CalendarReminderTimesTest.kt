package com.dujiajun.courseblock.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarReminderTimesTest {
    @Test
    fun legacySingleChoiceAndDefaultArePreserved() {
        assertEquals(setOf(15), CalendarReminderTimes.parse(null))
        assertEquals(setOf(30), CalendarReminderTimes.parse("30"))
        assertEquals(emptySet<Int>(), CalendarReminderTimes.parse(""))
    }

    @Test
    fun storedChoicesAreValidatedDeduplicatedAndLimited() {
        assertEquals(setOf(0, 5, 12, 15, 30), CalendarReminderTimes.parse("60,15,0,5,12,12,4,61,30,bad"))
        assertEquals(setOf(15), CalendarReminderTimes.parse("4,61,bad"))
        assertEquals("0,5,12", CalendarReminderTimes.encode(setOf(12, 5, 0)))
    }
}
