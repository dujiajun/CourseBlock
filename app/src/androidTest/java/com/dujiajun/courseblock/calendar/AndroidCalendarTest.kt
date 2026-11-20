package com.dujiajun.courseblock.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            val empty = calendar.sync(calendarName, "this", emptyList(), now)
            assertEquals(0, empty.scheduled)
            assertEquals(0, empty.added)
            assertFalse(empty.changed)
            assertTrue(calendarIds(account).isEmpty())

            val first = CalendarClass("First class", "Room 101", now.plusSeconds(86_400), now.plusSeconds(90_000))
            val moved = first.copy(start = first.start.plusSeconds(86_400), end = first.end.plusSeconds(86_400))
            val otherTerm = first.copy(title = "Class from another term")
            fun insertPrevious(
                title: String,
                term: String? = null,
                offset: Long = 0L,
                owner: String = testNamespace,
            ) {
                requireNotNull(resolver.insert(Events.CONTENT_URI, ContentValues().apply {
                    put(Events.CALENDAR_ID, previousId)
                    put(Events.TITLE, title)
                    put(Events.DTSTART, first.start.plusSeconds(offset).toEpochMilli())
                    put(Events.DTEND, first.end.plusSeconds(offset).toEpochMilli())
                    put(Events.EVENT_TIMEZONE, AcademicCalendar.ZONE.id)
                    if (term != null) {
                        put(Events.CUSTOM_APP_PACKAGE, owner)
                        put(Events.CUSTOM_APP_URI, "courseblock://calendar/$term")
                    }
                }))
            }
            insertPrevious(first.title, term = "this")
            insertPrevious("Unrelated")
            insertPrevious("Previous class", term = "this", offset = -172_800)
            insertPrevious("Other term", term = "other")

            calendar.sync(calendarName, "other", listOf(otherTerm), now)
            val firstImport = calendar.sync(calendarName, "this", listOf(first), now, previousId)
            assertEquals(1, firstImport.scheduled)
            assertEquals(1, firstImport.added)
            assertTrue(firstImport.changed)
            val rerun = calendar.sync("Different name", "this", listOf(first), now, previousId)
            assertEquals(calendarName, rerun.calendarName)
            assertEquals(1, rerun.scheduled)
            assertEquals(0, rerun.added)
            assertFalse(rerun.changed)
            assertEquals(setOf("Unrelated", "Previous class", "Other term"), events(previousId).map { it.second }.toSet())

            val id = calendarIds(account).single()
            val saved = events(id).single { it.second == first.title }
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

            val newReminder = calendar.sync(calendarName, "this", listOf(moved), now, reminderMinutes = setOf(30))
            assertEquals(1, newReminder.scheduled)
            assertEquals(0, newReminder.added)
            assertTrue(newReminder.changed)
            requireNotNull(resolver.query(
                Reminders.CONTENT_URI, arrayOf(Reminders.MINUTES),
                "${Reminders.EVENT_ID} = ?", arrayOf(movedEvent.first.toString()), null,
            )).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(30, cursor.getInt(0))
            }

            insertPrevious("Reimported into old calendar", term = "this")
            insertPrevious("Other app", term = "this", owner = "$testNamespace.other")
            assertEquals(5, calendar.removeImportedCourses(previousId))
            assertEquals(0, calendar.removeImportedCourses(previousId))
            assertTrue(events(id).isEmpty())
            assertEquals(setOf("Unrelated", "Other app"), events(previousId).map { it.second }.toSet())
        } finally {
            calendarIds(account).forEach {
                resolver.delete(ContentUris.withAppendedId(cleanupUri, it), null, null)
            }
            resolver.delete(ContentUris.withAppendedId(previousUri, previousId), null, null)
        }
    }

    @Test
    fun previewSharesTheCalendarWithEarlierTestInstalls() {
        val family = "${context.packageName}.migration.${UUID.randomUUID()}"
        val previewPackage = "$family.preview"
        val testPackage = "$family.verify"
        val oldTest = AndroidCalendar(resolver, testPackage)
        val oldPreview = AndroidCalendar(resolver, previewPackage)
        val preview = AndroidCalendar(resolver, previewPackage, family)
        val now = Instant.now()
        val course = CalendarClass("First class", "Room 101", now.plusSeconds(86_400), now.plusSeconds(90_000))
        val otherTerm = course.copy(title = "Other term")
        val accounts = listOf(family, previewPackage, testPackage).map { "$it.courses" }
        try {
            oldTest.sync("CourseBlock", "first", listOf(course), now)
            oldTest.sync("CourseBlock", "second", listOf(otherTerm), now)

            val testCalendar = calendarIds("$testPackage.courses").single()
            val oldEventId = events(testCalendar).single { it.second == course.title }.first
            val reused = preview.sync("CourseBlock", "first", listOf(course), now)
            assertEquals(1, reused.scheduled)
            assertEquals(0, reused.added)
            assertFalse(reused.changed)
            assertTrue(calendarIds("$family.courses").isEmpty())
            assertTrue(calendarIds("$previewPackage.courses").isEmpty())
            assertEquals(oldEventId, events(testCalendar).single { it.second == course.title }.first)

            oldPreview.sync("CourseBlock", "first", listOf(course), now)
            val previewCalendar = calendarIds("$previewPackage.courses").single()
            val deduped = preview.sync("CourseBlock", "first", listOf(course), now)
            assertEquals(1, deduped.scheduled)
            assertEquals(0, deduped.added)
            assertTrue(deduped.changed)
            assertTrue(events(previewCalendar).isEmpty())
            assertEquals(setOf(course.title, otherTerm.title), events(testCalendar).map { it.second }.toSet())

            oldPreview.sync("CourseBlock", "first", listOf(course), now)
            assertEquals(3, preview.removeImportedCourses())
            assertEquals(0, preview.removeImportedCourses())
            assertTrue(events(testCalendar).isEmpty())
            assertTrue(events(previewCalendar).isEmpty())
        } finally {
            accounts.forEach { account ->
                calendarIds(account).forEach { id ->
                    resolver.delete(ContentUris.withAppendedId(localCalendars(account), id), null, null)
                }
            }
        }
    }

    @Test
    fun multipleRemindersUpdateInPlaceAndCanBeTurnedOff() {
        val namespace = "${context.packageName}.reminders.${UUID.randomUUID()}"
        val calendar = AndroidCalendar(resolver, namespace)
        val account = "$namespace.courses"
        val now = Instant.now()
        val course = CalendarClass("Class", "", now.plusSeconds(86_400), now.plusSeconds(90_000))
        try {
            val times = setOf(0, 5, 12, 30, 60)
            assertEquals(1, calendar.sync("CourseBlock", "this", listOf(course), now, reminderMinutes = times).added)
            val id = calendarIds(account).single()
            val eventId = events(id).single().first

            fun alertTimes(): List<Int> = requireNotNull(resolver.query(
                Reminders.CONTENT_URI, arrayOf(Reminders.MINUTES), "${Reminders.EVENT_ID} = ? AND ${Reminders.METHOD} = ?",
                arrayOf(eventId.toString(), Reminders.METHOD_ALERT.toString()), Reminders.MINUTES,
            )).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getInt(0)) }
            }

            assertEquals(times.sorted(), alertTimes())
            val same = calendar.sync("CourseBlock", "this", listOf(course), now, reminderMinutes = times)
            assertEquals(0, same.added)
            assertFalse(same.changed)
            assertEquals(times.sorted(), alertTimes())

            val changed = calendar.sync("CourseBlock", "this", listOf(course), now, reminderMinutes = setOf(0, 10))
            assertEquals(0, changed.added)
            assertTrue(changed.changed)
            assertEquals(listOf(0, 10), alertTimes())

            val disabled = calendar.sync("CourseBlock", "this", listOf(course), now, reminderMinutes = emptySet())
            assertEquals(0, disabled.added)
            assertTrue(disabled.changed)
            assertTrue(alertTimes().isEmpty())
            assertEquals(eventId, events(id).single().first)
        } finally {
            calendarIds(account).forEach { id ->
                resolver.delete(ContentUris.withAppendedId(localCalendars(account), id), null, null)
            }
        }
    }

    @Test
    fun savingDefaultsUpdatesOnlyUnchangedFutureCourseReminders() {
        val namespace = "${context.packageName}.autoupdate.${UUID.randomUUID()}"
        val calendar = AndroidCalendar(resolver, namespace)
        val account = "$namespace.courses"
        val now = Instant.now()
        val course = CalendarClass("Unchanged", "", now.plusSeconds(86_400), now.plusSeconds(90_000))
        val manual = course.copy(title = "Custom")
        val past = course.copy(
            title = "Past", start = now.minusSeconds(86_400), end = now.minusSeconds(82_800),
        )
        try {
            calendar.sync("CourseBlock", "first", listOf(course, manual), now)
            calendar.sync("CourseBlock", "second", listOf(past), now.minusSeconds(172_800))
            val id = calendarIds(account).single()
            val ids = events(id).associate { it.second to it.first }
            val manualId = ids.getValue(manual.title)
            resolver.delete(Reminders.CONTENT_URI, "${Reminders.EVENT_ID} = ?", arrayOf(manualId.toString()))
            resolver.insert(Reminders.CONTENT_URI, ContentValues().apply {
                put(Reminders.EVENT_ID, manualId)
                put(Reminders.MINUTES, 25)
                put(Reminders.METHOD, Reminders.METHOD_ALERT)
            })

            // Simulate an import created before the application started recording applied defaults.
            val regularId = ids.getValue(course.title)
            resolver.update(ContentUris.withAppendedId(Events.CONTENT_URI, regularId),
                ContentValues().apply { put(Events.CUSTOM_APP_URI, "courseblock://calendar/first") }, null, null)

            fun times(eventId: Long) = requireNotNull(resolver.query(
                Reminders.CONTENT_URI, arrayOf(Reminders.MINUTES), "${Reminders.EVENT_ID} = ?",
                arrayOf(eventId.toString()), Reminders.MINUTES,
            )).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getInt(0)) } }

            assertEquals(1, calendar.updateImportedReminders(now, null, setOf(5), setOf(0, 12)))
            assertEquals(listOf(0, 12), times(regularId))
            assertEquals(listOf(25), times(manualId))
            assertEquals(listOf(15), times(ids.getValue(past.title)))
            assertEquals(0, calendar.updateImportedReminders(now, null, setOf(5), setOf(0, 12)))
            calendar.sync("CourseBlock", "first", listOf(course, manual), now, reminderMinutes = setOf(0, 12))
            assertEquals(listOf(25), times(manualId))
            assertEquals(ids, events(id).associate { it.second to it.first })
        } finally {
            calendarIds(account).forEach { id ->
                resolver.delete(ContentUris.withAppendedId(localCalendars(account), id), null, null)
            }
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
