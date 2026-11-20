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

internal class AndroidCalendar(
    private val resolver: ContentResolver,
    private val packageName: String,
    familyPackage: String = if (packageName in COURSEBLOCK_PACKAGES) COURSEBLOCK_PACKAGE else packageName,
) {
    data class Synced(
        val scheduled: Int,
        val added: Int,
        val changed: Boolean,
        val calendarName: String,
    )

    class HiddenCalendar(val calendarName: String) : IllegalStateException()

    private data class LocalCalendar(val id: Long, val visible: Boolean, val displayName: String)

    private val packageNames = courseBlockPackages(familyPackage)
    private val accountNames = packageNames.map { "$it.courses" }
    private val accountName = accountNames.first()
    private val packagePlaceholders = packageNames.joinToString(", ") { "?" }

    fun sync(
        calendarName: String,
        termKey: String,
        upcoming: List<CalendarClass>,
        now: Instant,
        previousCalendarId: Long? = null,
        reminderMinutes: Set<Int> = CalendarReminderTimes.default,
        legacyReminderMinutes: Set<Int> = reminderMinutes,
    ): Synced = synchronized(writeLock) {
        require(reminderMinutes.size <= CalendarReminderTimes.MAX_COUNT &&
            reminderMinutes.all(CalendarReminderTimes::isAllowed))
        val marker = termMarker(termKey)
        val desired = upcoming.filter { it.start > now }.associateBy { it.identity() }
        val calendars = findCalendars(calendarName)
        val target = calendars.firstOrNull() ?: if (desired.isNotEmpty()) createCalendar(calendarName) else null
        if (target?.visible == false && desired.isNotEmpty()) throw HiddenCalendar(target.displayName)
        val calendarId = target?.id
        val existing = calendarId?.let { existing(it, marker, now) }.orEmpty()
        val retainedIds = desired.keys.mapNotNull { existing[it]?.firstOrNull()?.id }
        val savedReminders = remindersFor(retainedIds)
        val operations = arrayListOf<ContentProviderOperation>()

        val staleIds = existing.flatMap { (identity, ids) ->
            if (identity in desired) ids.drop(1) else ids
        }
        if (calendarId != null) {
            staleIds.forEach { operations.add(deleteManagedEvent(calendarId, marker, now, it.id)) }
        }
        calendarIds(calendars, previousCalendarId).filterNot { it == calendarId }.forEach { otherId ->
            existing(otherId, marker, now).values.flatten().forEach { eventId ->
                operations.add(deleteManagedEvent(otherId, marker, now, eventId.id))
            }
        }
        for ((identity, occurrence) in desired) {
            val event = existing[identity]?.firstOrNull()
            if (event != null) {
                updateDefaultReminders(operations, event, savedReminders[event.id].orEmpty(), legacyReminderMinutes, reminderMinutes)
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
                put(Events.CUSTOM_APP_URI, CalendarReminderTimes.eventMarker(marker, reminderMinutes))
                put(Events.HAS_ALARM, if (reminderMinutes.isEmpty()) 0 else 1)
            }).build())
            reminderMinutes.sorted().forEach { minutes ->
                operations.add(ContentProviderOperation.newInsert(Reminders.CONTENT_URI)
                    .withValueBackReference(Reminders.EVENT_ID, eventIndex)
                    .withValues(reminderValues(minutes))
                    .build())
            }
        }
        val changed = operations.isNotEmpty() && resolver.applyBatch(CalendarContract.AUTHORITY, operations)
            .any { it.uri != null || (it.count ?: 0) > 0 }
        Synced(
            scheduled = desired.size,
            added = desired.size - retainedIds.size,
            changed = changed,
            calendarName = target?.displayName ?: calendarName,
        )
    }

    fun removeImportedCourses(previousCalendarId: Long? = null): Int = synchronized(writeLock) {
        val calendarIds = calendarIds(findCalendars(""), previousCalendarId)
        val operations = calendarIds.map { id ->
            ContentProviderOperation.newDelete(Events.CONTENT_URI)
                .withSelection(
                    "$managedInCalendar AND ${Events.CUSTOM_APP_URI} GLOB ?",
                    managedEventArgs(id, "$TERM_MARKER_PREFIX*"),
                )
                .build()
        }
        if (operations.isEmpty()) 0
        else resolver.applyBatch(CalendarContract.AUTHORITY, ArrayList(operations)).sumOf { it.count ?: 0 }
    }

    fun updateImportedReminders(
        now: Instant,
        previousCalendarId: Long?,
        legacyReminderMinutes: Set<Int>,
        reminderMinutes: Set<Int>,
    ): Int = synchronized(writeLock) {
        val calendarIds = calendarIds(findCalendars(""), previousCalendarId)
        val events = calendarIds.flatMap { id ->
            val args = managedEventArgs(id, "$TERM_MARKER_PREFIX*") + now.toEpochMilli().toString()
            val fields = arrayOf(Events._ID, Events.CUSTOM_APP_URI)
            requireNotNull(resolver.query(
                Events.CONTENT_URI, fields, "$managedInCalendar AND ${Events.CUSTOM_APP_URI} GLOB ? AND ${Events.DTSTART} > ?",
                args, Events._ID,
            )).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(SavedEvent(cursor.getLong(0), cursor.getString(1)))
                }
            }
        }
        val reminders = remindersFor(events.map { it.id })
        val operations = arrayListOf<ContentProviderOperation>()
        val updated = events.count { event ->
            updateDefaultReminders(operations, event, reminders[event.id].orEmpty(), legacyReminderMinutes, reminderMinutes)
        }
        if (operations.isNotEmpty()) resolver.applyBatch(CalendarContract.AUTHORITY, operations)
        updated
    }

    private fun termMarker(termKey: String) = TERM_MARKER_PREFIX + termKey

    private fun calendarIds(calendars: List<LocalCalendar>, previousId: Long?): List<Long> =
        (calendars.map { it.id } + listOfNotNull(previousId)).distinct()

    private fun findCalendars(fallbackName: String): List<LocalCalendar> {
        val fields = arrayOf(Calendars._ID, Calendars.VISIBLE, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME)
        val filter = "${Calendars.ACCOUNT_TYPE} = ? AND ${Calendars.NAME} = ? AND " +
            "${Calendars.ACCOUNT_NAME} IN ($packagePlaceholders) AND ${Calendars.OWNER_ACCOUNT} = ${Calendars.ACCOUNT_NAME}"
        val args = arrayOf(CalendarContract.ACCOUNT_TYPE_LOCAL, CALENDAR_NAME, *accountNames.toTypedArray())
        return requireNotNull(resolver.query(Calendars.CONTENT_URI, fields, filter, args, Calendars._ID)).use { cursor ->
            val preferred = mutableListOf<LocalCalendar>()
            val legacy = mutableListOf<LocalCalendar>()
            while (cursor.moveToNext()) {
                val name = cursor.getString(2).orEmpty().ifBlank { fallbackName }
                val calendar = LocalCalendar(cursor.getLong(0), cursor.getInt(1) == 1, name)
                // Keep using the oldest existing variant calendar when there is no shared one yet.
                if (cursor.getString(3) == accountName) preferred.add(calendar) else legacy.add(calendar)
            }
            preferred + legacy
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
            put(Calendars.MAX_REMINDERS, CalendarReminderTimes.MAX_COUNT)
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

    private val managedInCalendar = "${Events.CALENDAR_ID} = ? AND " +
        "${Events.CUSTOM_APP_PACKAGE} IN ($packagePlaceholders) AND ${Events.DELETED} = 0"
    private val managedEvent = "$managedInCalendar AND (${Events.CUSTOM_APP_URI} = ? OR ${Events.CUSTOM_APP_URI} GLOB ?)"
    private val managedFutureEvent = "$managedEvent AND ${Events.DTSTART} > ?"

    private fun managedEventArgs(calendarId: Long, marker: String) =
        arrayOf(calendarId.toString(), *packageNames.toTypedArray(), marker)

    private fun managedEventArgs(calendarId: Long, marker: String, now: Instant) =
        managedEventArgs(calendarId, marker) + arrayOf(CalendarReminderTimes.eventPattern(marker), now.toEpochMilli().toString())

    private fun existing(calendarId: Long, marker: String, now: Instant): Map<EventIdentity, List<SavedEvent>> {
        val fields = arrayOf(Events._ID, Events.TITLE, Events.EVENT_LOCATION, Events.DTSTART, Events.DTEND, Events.CUSTOM_APP_URI)
        val args = managedEventArgs(calendarId, marker, now)
        return requireNotNull(resolver.query(Events.CONTENT_URI, fields, managedFutureEvent, args, Events._ID))
            .use { cursor ->
                val events = mutableMapOf<EventIdentity, MutableList<SavedEvent>>()
                while (cursor.moveToNext()) {
                    val identity = EventIdentity(
                        cursor.getString(1).orEmpty(), cursor.getString(2).orEmpty(),
                        cursor.getLong(3), cursor.getLong(4),
                    )
                    events.getOrPut(identity) { mutableListOf() }.add(SavedEvent(cursor.getLong(0), cursor.getString(5)))
                }
                events
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

    private fun updateDefaultReminders(
        operations: MutableList<ContentProviderOperation>,
        event: SavedEvent,
        reminders: List<SavedReminder>,
        legacy: Set<Int>,
        desired: Set<Int>,
    ): Boolean {
        if (reminders.any { it.method != Reminders.METHOD_ALERT }) return false
        val current = reminders.map { it.minutes }.toSet()
        if (reminders.size != current.size || current == desired) return false

        val term = CalendarReminderTimes.termFromEvent(event.marker)
        val usesDefault = if (term == event.marker) {
            // Older users could change settings without re-importing. Their events may still use the original 15 minutes.
            current == legacy || current == CalendarReminderTimes.default
        } else {
            current == CalendarReminderTimes.appliedToEvent(event.marker)
        }
        if (!usesDefault) return false

        operations.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(Events.CONTENT_URI, event.id))
            .withValue(Events.CUSTOM_APP_URI, CalendarReminderTimes.eventMarker(term, desired))
            .withExpectedCount(1)
            .build())
        syncReminders(operations, event.id, reminders, desired)
        return true
    }

    private fun syncReminders(
        operations: MutableList<ContentProviderOperation>,
        event: Long,
        reminders: List<SavedReminder>,
        desired: Set<Int>,
    ) {
        val remaining = desired.toMutableSet()
        reminders.filter { it.method == Reminders.METHOD_ALERT }.forEach { reminder ->
            if (!remaining.remove(reminder.minutes)) {
                operations.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Reminders.CONTENT_URI, reminder.id))
                    .build())
            }
        }
        remaining.sorted().forEach { minutes ->
            operations.add(ContentProviderOperation.newInsert(Reminders.CONTENT_URI)
                .withValue(Reminders.EVENT_ID, event)
                .withValues(reminderValues(minutes))
                .build())
        }
    }

    private fun reminderValues(minutes: Int) = ContentValues().apply {
        put(Reminders.MINUTES, minutes)
        put(Reminders.METHOD, Reminders.METHOD_ALERT)
    }

    private data class SavedEvent(val id: Long, val marker: String)

    private data class SavedReminder(val id: Long, val minutes: Int, val method: Int)

    private fun CalendarClass.identity() = EventIdentity(title, location, start.toEpochMilli(), end.toEpochMilli())

    private data class EventIdentity(val title: String, val location: String, val start: Long, val end: Long)

    companion object {
        private const val COURSEBLOCK_PACKAGE = "com.dujiajun.courseblock"
        private val COURSEBLOCK_PACKAGES = courseBlockPackages(COURSEBLOCK_PACKAGE)

        private fun courseBlockPackages(base: String) = listOf(base, "$base.preview", "$base.verify")

        private const val CALENDAR_NAME = "courseblock-classes"
        private const val TERM_MARKER_PREFIX = "courseblock://calendar/"
        val permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

        fun hasPermission(context: Context) = permissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        private val writeLock = Any()
    }
}
