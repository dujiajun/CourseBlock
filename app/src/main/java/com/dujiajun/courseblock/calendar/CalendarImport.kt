package com.dujiajun.courseblock.calendar

import android.content.Context
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.R
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.helper.CourseManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

internal sealed interface CalendarImport {
    data class Message(val text: String) : CalendarImport
    object PermissionRequired : CalendarImport
}

internal suspend fun importRemainingCourses(context: Context, manager: CourseManager): CalendarImport {
    val term = manager.termKey
    val calendar = manager.academicCalendar
        ?: return CalendarImport.Message(context.getString(R.string.academic_calendar_unavailable))
    if (!manager.hasSyncedTerm()) {
        return CalendarImport.Message(context.getString(R.string.calendar_sync_first))
    }

    val startTimes = manager.startTimes
    val endTimes = manager.endTimes
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val previousCalendar = preferences.getLong(PreferenceKey.LEGACY_CALENDAR_ID, -1L).takeIf { it >= 0 }
    val minutes = preferences.getString(PreferenceKey.CALENDAR_REMINDER_MINUTES, null)
        ?.toIntOrNull()?.takeIf { it in AndroidCalendar.reminderTimes }
        ?: AndroidCalendar.DEFAULT_REMINDER_MINUTES
    val calendarName = context.getString(R.string.courseblock_calendar_name)

    return withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()
            val courses = manager.readCourses(term)
            val occurrences = CourseOccurrences.upcoming(courses, calendar, startTimes, endTimes, now)
            if (!AndroidCalendar.hasPermission(context)) {
                return@withContext if (occurrences.isEmpty()) {
                    CalendarImport.Message(context.getString(R.string.calendar_no_remaining))
                } else CalendarImport.PermissionRequired
            }
            val result = AndroidCalendar(context.contentResolver, context.packageName)
                .sync(calendarName, term, occurrences, now, previousCalendar, minutes)
            CalendarImport.Message(
                if (result.count == 0) context.getString(R.string.calendar_no_remaining)
                else context.getString(R.string.calendar_synced, result.count, result.calendarName, minutes),
            )
        } catch (invalid: CourseOccurrences.InvalidTiming) {
            CalendarImport.Message(context.getString(R.string.calendar_invalid_timing, invalid.courseName))
        } catch (hidden: AndroidCalendar.HiddenCalendar) {
            CalendarImport.Message(context.getString(R.string.calendar_hidden, hidden.calendarName))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            CalendarImport.Message(context.getString(R.string.calendar_failed))
        }
    }
}
