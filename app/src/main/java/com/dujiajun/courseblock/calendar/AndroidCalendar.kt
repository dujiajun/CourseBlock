package com.dujiajun.courseblock.calendar

import android.Manifest
import android.content.Context
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import java.time.Instant

internal class AndroidCalendar(private val resolver: ContentResolver, private val packageName: String) {
    data class Synced(val count: Int, val calendarName: String)

    class HiddenCalendar(val calendarName: String) : IllegalStateException()

    private data class LocalCalendar(val id: Long, val visible: Boolean, val displayName: String)

    private val accountName = "$packageName.courses"

    fun sync(
        calendarName: String,
        termKey: String,
        upcoming: List<CalendarClass>,
        now: Instant,
        previousCalendarId: Long? = null,
        reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    ): Synced = synchronized(writeLock) {
        val marker = termMarker(termKey)
        val desired = upcoming.filter { it.start > now }.associateBy { it.identity() }
        val target = findCalendar(calendarName) ?: if (desired.isNotEmpty()) createCalendar(calendarName) else null
        if (target?.visible == false && desired.isNotEmpty()) throw HiddenCalendar(target.displayName)
        val calendarId = target?.id
        val existing = calendarId?.let { existing(it, marker, now) }.orEmpty()
        val retainedIds = desired.keys.mapNotNull { existing[it]?.firstOrNull() }
        val savedReminders = remindersFor(retainedIds)
        val operations = arrayListOf<ContentProviderOperation>()

        val staleIds = existing.flatMap { (identity, ids) ->
            if (identity in desired) ids.drop(1) else ids
        }
        if (calendarId != null) {
            staleIds.forEach { operations.add(deleteManagedEvent(calendarId, marker, now, it)) }
        }
        if (previousCalendarId != null && previousCalendarId != calendarId) {
            existing(previousCalendarId, marker, now).values.flatten().forEach {
                operations.add(deleteManagedEvent(previousCalendarId, marker, now, it))
            }
        }
        for ((identity, occurrence) in desired) {
            val eventId = existing[identity]?.firstOrNull()
            if (eventId != null) {
                ensureReminder(operations, eventId, savedReminders[eventId].orEmpty(), reminderMinutes)
                continue
            }
            val eventIndex = operations.size
            operations.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(ContentValues().apply {
                put(Events.CALENDAR_ID, requireNotNull(calendarId))
                put(Events.TITLE, occurrence.title)
                put(Events.EVENT_LOCATION, occurrence.location)
                put(Events.DTSTART, occurrence.start.toEpochMilli())
                put(Events.DTEND, occurrence.end.toEpochMilli())
                put(Events.EVENT_TIMEZONE, AcademicCalendar.ZONE.id)
                put(Events.CUSTOM_APP_PACKAGE, packageName)
                put(Events.CUSTOM_APP_URI, marker)
                put(Events.HAS_ALARM, 1)
            }).build())
            operations.add(ContentProviderOperation.newInsert(Reminders.CONTENT_URI)
                .withValueBackReference(Reminders.EVENT_ID, eventIndex)
                .withValues(reminderValues(reminderMinutes))
                .build())
        }
        if (operations.isNotEmpty()) resolver.applyBatch(CalendarContract.AUTHORITY, operations)
        Synced(desired.size, target?.displayName ?: calendarName)
    }

    fun removeTerm(termKey: String, previousCalendarId: Long? = null): Int = synchronized(writeLock) {
        val marker = termMarker(termKey)
        val calendarIds = listOfNotNull(findCalendar("")?.id, previousCalendarId).distinct()
        val operations = calendarIds.map { id ->
            ContentProviderOperation.newDelete(Events.CONTENT_URI)
                .withSelection(managedEvent, managedEventArgs(id, marker))
                .build()
        }
        if (operations.isEmpty()) 0
        else resolver.applyBatch(CalendarContract.AUTHORITY, ArrayList(operations)).sumOf { it.count ?: 0 }
    }

    private fun termMarker(termKey: String) = "courseblock://calendar/$termKey"

    private fun findCalendar(fallbackName: String): LocalCalendar? {
        val fields = arrayOf(Calendars._ID, Calendars.VISIBLE, Calendars.CALENDAR_DISPLAY_NAME)
        val filter = "${Calendars.ACCOUNT_TYPE} = ? AND ${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.NAME} = ?"
        val args = arrayOf(CalendarContract.ACCOUNT_TYPE_LOCAL, accountName, CALENDAR_NAME)
        return requireNotNull(resolver.query(Calendars.CONTENT_URI, fields, filter, args, Calendars._ID)).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val displayName = cursor.getString(2).orEmpty().ifBlank { fallbackName }
            LocalCalendar(cursor.getLong(0), cursor.getInt(1) == 1, displayName)
        }
    }

    private fun createCalendar(displayName: String): LocalCalendar {
        val uri = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, accountName)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, CALENDAR_NAME)
            put(Calendars.CALENDAR_DISPLAY_NAME, displayName)
            put(Calendars.CALENDAR_COLOR, 0xFF008577.toInt())
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, accountName)
            put(Calendars.CALENDAR_TIME_ZONE, AcademicCalendar.ZONE.id)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
            put(Calendars.MAX_REMINDERS, 5)
            put(Calendars.ALLOWED_REMINDERS, Reminders.METHOD_ALERT.toString())
        }
        val id = ContentUris.parseId(requireNotNull(resolver.insert(uri, values)))
        return LocalCalendar(id, visible = true, displayName = displayName)
    }

    private fun deleteManagedEvent(calendarId: Long, marker: String, now: Instant, eventId: Long) =
        ContentProviderOperation.newDelete(Events.CONTENT_URI)
            .withSelection("$managedFutureEvent AND ${Events._ID} = ?",
                managedEventArgs(calendarId, marker, now) + eventId.toString())
            .build()

    private val managedEvent = "${Events.CALENDAR_ID} = ? AND ${Events.CUSTOM_APP_PACKAGE} = ? AND " +
        "${Events.CUSTOM_APP_URI} = ? AND ${Events.DELETED} = 0"
    private val managedFutureEvent = "$managedEvent AND ${Events.DTSTART} > ?"

    private fun managedEventArgs(calendarId: Long, marker: String) = arrayOf(calendarId.toString(), packageName, marker)

    private fun managedEventArgs(calendarId: Long, marker: String, now: Instant) =
        managedEventArgs(calendarId, marker) + now.toEpochMilli().toString()

    private fun existing(calendarId: Long, marker: String, now: Instant): Map<EventIdentity, List<Long>> {
        val fields = arrayOf(Events._ID, Events.TITLE, Events.EVENT_LOCATION, Events.DTSTART, Events.DTEND)
        val args = managedEventArgs(calendarId, marker, now)
        return requireNotNull(resolver.query(Events.CONTENT_URI, fields, managedFutureEvent, args, Events._ID))
            .use { cursor ->
                val ids = mutableMapOf<EventIdentity, MutableList<Long>>()
                while (cursor.moveToNext()) {
                    val identity = EventIdentity(
                        cursor.getString(1).orEmpty(), cursor.getString(2).orEmpty(),
                        cursor.getLong(3), cursor.getLong(4),
                    )
                    ids.getOrPut(identity) { mutableListOf() }.add(cursor.getLong(0))
                }
                ids
            }
    }

    private fun remindersFor(eventIds: List<Long>): Map<Long, List<SavedReminder>> {
        val result = mutableMapOf<Long, MutableList<SavedReminder>>()
        val projection = arrayOf(Reminders.EVENT_ID, Reminders._ID, Reminders.MINUTES, Reminders.METHOD)
        for (chunk in eventIds.chunked(200)) {
            val placeholders = chunk.joinToString(",") { "?" }
            requireNotNull(resolver.query(
                Reminders.CONTENT_URI, projection, "${Reminders.EVENT_ID} IN ($placeholders)",
                chunk.map(Long::toString).toTypedArray(), Reminders._ID,
            )).use { cursor ->
                while (cursor.moveToNext()) {
                    result.getOrPut(cursor.getLong(0)) { mutableListOf() }
                        .add(SavedReminder(cursor.getLong(1), cursor.getInt(2), cursor.getInt(3)))
                }
            }
        }
        return result
    }

    private fun ensureReminder(
        operations: MutableList<ContentProviderOperation>,
        event: Long,
        reminders: List<SavedReminder>,
        reminderMinutes: Int,
    ) {
        if (reminders.any { it.minutes == reminderMinutes && it.method == Reminders.METHOD_ALERT }) return
        val existing = reminders.firstOrNull { it.method == Reminders.METHOD_ALERT }
        val operation = if (existing == null) {
            ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValue(Reminders.EVENT_ID, event)
        } else {
            ContentProviderOperation.newUpdate(ContentUris.withAppendedId(Reminders.CONTENT_URI, existing.id))
        }
        operations.add(operation.withValues(reminderValues(reminderMinutes)).build())
    }

    private fun reminderValues(minutes: Int) = ContentValues().apply {
        put(Reminders.MINUTES, minutes)
        put(Reminders.METHOD, Reminders.METHOD_ALERT)
    }

    private data class SavedReminder(val id: Long, val minutes: Int, val method: Int)

    private fun CalendarClass.identity() = EventIdentity(title, location, start.toEpochMilli(), end.toEpochMilli())

    private data class EventIdentity(val title: String, val location: String, val start: Long, val end: Long)

    companion object {
        private const val CALENDAR_NAME = "courseblock-classes"
        const val DEFAULT_REMINDER_MINUTES = 15
        val reminderTimes = listOf(5, 10, DEFAULT_REMINDER_MINUTES, 30, 60)
        val permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

        fun hasPermission(context: Context) = permissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        private val writeLock = Any()
    }
}
