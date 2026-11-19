package com.dujiajun.courseblock.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class AndroidCalendarTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver

    @Test
    fun importsIntoOneLocalCalendarAndUpdatesOnlyCourseBlockEvents() {
        val testNamespace = "${context.packageName}.test.${UUID.randomUUID()}"
        val calendar = AndroidCalendar(resolver, testNamespace)
        val calendarName = "CourseBlock test"
        val account = "$testNamespace.courses"
        val cleanupUri = localCalendars(account)
        val previousAccount = "CourseBlock-test-${UUID.randomUUID()}"
        val previousUri = localCalendars(previousAccount)
        val previousId = ContentUris.parseId(requireNotNull(resolver.insert(previousUri, ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, previousAccount)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, previousAccount)
            put(Calendars.CALENDAR_DISPLAY_NAME, previousAccount)
            put(Calendars.CALENDAR_COLOR, 0xFF009688.toInt())
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, previousAccount)
            put(Calendars.CALENDAR_TIME_ZONE, AcademicCalendar.ZONE.id)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
        })))
        try {
            val now = Instant.now()
            assertEquals(0, calendar.sync(calendarName, "this", emptyList(), now).count)
            assertTrue(calendarIds(account).isEmpty())

            val first = CalendarClass("First class", "Room 101", now.plusSeconds(86_400), now.plusSeconds(90_000))
            val moved = first.copy(start = first.start.plusSeconds(86_400), end = first.end.plusSeconds(86_400))
            val otherTerm = first.copy(title = "Class from another term")
            fun insertPrevious(title: String, term: String? = null, offset: Long = 0L) {
                requireNotNull(resolver.insert(Events.CONTENT_URI, ContentValues().apply {
                    put(Events.CALENDAR_ID, previousId)
                    put(Events.TITLE, title)
                    put(Events.DTSTART, first.start.plusSeconds(offset).toEpochMilli())
                    put(Events.DTEND, first.end.plusSeconds(offset).toEpochMilli())
                    put(Events.EVENT_TIMEZONE, AcademicCalendar.ZONE.id)
                    if (term != null) {
                        put(Events.CUSTOM_APP_PACKAGE, testNamespace)
                        put(Events.CUSTOM_APP_URI, "courseblock://calendar/$term")
                    }
                }))
            }
            insertPrevious(first.title, term = "this")
            insertPrevious("Unrelated")
            insertPrevious("Previous class", term = "this", offset = -172_800)
            insertPrevious("Other term", term = "other")

            calendar.sync(calendarName, "other", listOf(otherTerm), now)
            calendar.sync(calendarName, "this", listOf(first), now, previousId)
            val rerun = calendar.sync("Different name", "this", listOf(first), now, previousId)
            assertEquals(calendarName, rerun.calendarName)
            assertEquals(setOf("Unrelated", "Previous class", "Other term"), events(previousId).map { it.second }.toSet())

            val id = calendarIds(account).single()
            val saved = events(id).single { it.second == first.title }
            resolver.delete(Reminders.CONTENT_URI, "${Reminders.EVENT_ID} = ?", arrayOf(saved.first.toString()))
            calendar.sync(calendarName, "this", listOf(first), now)
            requireNotNull(resolver.query(
                Reminders.CONTENT_URI, arrayOf(Reminders.MINUTES, Reminders.METHOD),
                "${Reminders.EVENT_ID} = ?", arrayOf(saved.first.toString()), null,
            )).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(15, cursor.getInt(0))
                assertEquals(Reminders.METHOD_ALERT, cursor.getInt(1))
            }

            calendar.sync(calendarName, "this", listOf(moved), now)
            val movedEvent = events(id).single { it.second == moved.title }
            assertEquals(moved.start.toEpochMilli(), movedEvent.third)
            assertEquals(2, events(id).size)

            calendar.sync(calendarName, "this", listOf(moved), now, reminderMinutes = 30)
            requireNotNull(resolver.query(
                Reminders.CONTENT_URI, arrayOf(Reminders.MINUTES),
                "${Reminders.EVENT_ID} = ?", arrayOf(movedEvent.first.toString()), null,
            )).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(30, cursor.getInt(0))
            }

            insertPrevious("Reimported into old calendar", term = "this")
            assertEquals(3, calendar.removeTerm("this", previousId))
            assertEquals(0, calendar.removeTerm("this", previousId))
            assertEquals(listOf(otherTerm.title), events(id).map { it.second })
            assertEquals(setOf("Unrelated", "Other term"), events(previousId).map { it.second }.toSet())
        } finally {
            calendarIds(account).forEach {
                resolver.delete(ContentUris.withAppendedId(cleanupUri, it), null, null)
            }
            resolver.delete(ContentUris.withAppendedId(previousUri, previousId), null, null)
        }
    }

    private fun localCalendars(account: String) = Calendars.CONTENT_URI.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    private fun calendarIds(account: String): List<Long> = requireNotNull(resolver.query(
        Calendars.CONTENT_URI, arrayOf(Calendars._ID),
        "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ?",
        arrayOf(account, CalendarContract.ACCOUNT_TYPE_LOCAL), null,
    )).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
    }

    private fun events(calendarId: Long) = requireNotNull(resolver.query(
        Events.CONTENT_URI, arrayOf(Events._ID, Events.TITLE, Events.DTSTART),
        "${Events.CALENDAR_ID} = ? AND ${Events.DELETED} = 0", arrayOf(calendarId.toString()), null,
    )).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(Triple(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
        }
    }
}
