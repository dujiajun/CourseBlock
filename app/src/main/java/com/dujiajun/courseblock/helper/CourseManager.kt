package com.dujiajun.courseblock.helper

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.calendar.AcademicCalendar
import com.dujiajun.courseblock.calendar.AcademicTerm
import com.dujiajun.courseblock.calendar.OfficialCalendars
import com.dujiajun.courseblock.calendar.School
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.downloader.CourseDownloader
import com.dujiajun.courseblock.downloader.GraduateDownloader
import com.dujiajun.courseblock.downloader.MedicineDownloader
import com.dujiajun.courseblock.downloader.UndergraduateDownloader
import com.dujiajun.courseblock.model.Course
import org.litepal.LitePal
import java.time.LocalDate

class CourseManager private constructor(private val context: Context) {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    var courses: List<Course> = ArrayList()

    private var downloader: CourseDownloader = UndergraduateDownloader(context)
    internal var school = School.UNDERGRADUATE
        private set

    internal val suggestedTerm: AcademicTerm
        get() = OfficialCalendars.suggestedTerm(LocalDate.now(AcademicCalendar.ZONE), school)

    internal val selectedTerm: AcademicTerm?
        get() {
            val year = preferences.getString(PreferenceKey.CURRENT_YEAR, null)?.toIntOrNull() ?: return null
            val number = preferences.getString(PreferenceKey.CURRENT_TERM, null)?.toIntOrNull() ?: return null
            val allowedTerms = if (school.isGraduate) 1..2 else 1..3
            return if (number in allowedTerms) AcademicTerm(year, number) else null
        }

    internal val term: AcademicTerm
        get() = selectedTerm ?: suggestedTerm

    internal fun selectTerm(term: AcademicTerm?) {
        val editor = preferences.edit()
        if (term == null) {
            editor.remove(PreferenceKey.CURRENT_YEAR).remove(PreferenceKey.CURRENT_TERM)
        } else {
            editor.putString(PreferenceKey.CURRENT_YEAR, term.year.toString())
                .putString(PreferenceKey.CURRENT_TERM, term.number.toString())
        }
        editor.apply()
    }

    internal val termKey: String
        get() = term.key(school)

    internal val academicCalendar
        get() = OfficialCalendars.find(term, school)

    internal val canSync: Boolean
        get() = when (school) {
            School.GRADUATE, School.PART_TIME_GRADUATE -> term.number != 3
            School.MEDICINE -> academicCalendar != null
            School.UNDERGRADUATE -> true
        }

    internal fun hasSyncedTerm() = preferences.getBoolean("synced:$termKey", false)

    init {
        updateStatus()
        migrateTermSelection()
    }

    private fun migrateTermSelection() {
        if (preferences.getBoolean(TERM_SELECTOR_MIGRATED, false)) return
        val editor = preferences.edit().putBoolean(TERM_SELECTOR_MIGRATED, true)
        // Old Settings wrote the default pair even if the user only opened it.
        if (selectedTerm == suggestedTerm) {
            editor.remove(PreferenceKey.CURRENT_YEAR).remove(PreferenceKey.CURRENT_TERM)
        }
        editor.apply()
    }

    fun updateStatus() {
        val selectedSchool = when (preferences.getString(PreferenceKey.STATUS, "0")) {
            "1" -> School.GRADUATE
            "2" -> School.MEDICINE
            "3" -> School.PART_TIME_GRADUATE
            else -> School.UNDERGRADUATE
        }

        if (selectedSchool != school) {
            school = selectedSchool
            downloader = when (school) {
                School.GRADUATE, School.PART_TIME_GRADUATE -> GraduateDownloader(this.context)
                School.MEDICINE -> MedicineDownloader(this.context)
                else -> UndergraduateDownloader(this.context)
            }
        }
    }

    fun loadCourses() {
        val selection = termKey
        if (!preferences.contains(LEGACY_TERM)) {
            preferences.edit().putString(LEGACY_TERM, selection).apply()
        }
        courses = readCourses(selection)
    }

    internal fun readCourses(key: String): List<Course> {
        val filter = if (preferences.getString(LEGACY_TERM, null) == key) {
            "termKey = ? OR termKey IS NULL OR termKey = ''"
        } else {
            "termKey = ?"
        }
        return LitePal.where(filter, key).find(Course::class.java)
    }

    fun updateCourses(replaceCustom: Boolean, uiHandler: Handler) {
        if (!canSync) {
            uiHandler.sendEmptyMessage(FAILED)
            return
        }
        val selectedTerm = term
        val selectedKey = selectedTerm.key(school)
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val result = if (msg.what == CourseDownloader.DOWNLOADED) {
                    @Suppress("UNCHECKED_CAST")
                    val downloadedCourses = msg.obj as List<Course>
                    try {
                        replaceCourses(selectedKey, downloadedCourses, replaceCustom)
                        preferences.edit().putBoolean("synced:$selectedKey", true).apply()
                        SUCCEEDED
                    } catch (error: RuntimeException) {
                        Log.e("CourseManager", "Could not save timetable", error)
                        FAILED
                    }
                } else {
                    msg.what
                }
                uiHandler.sendEmptyMessage(result)
            }
        }
        downloader.getCourses(selectedTerm.year.toString(), selectedTerm.number.toString(), handler)
    }

    private fun replaceCourses(key: String, newCourses: List<Course>, replaceCustom: Boolean) {
        val includeLegacy = preferences.getString(LEGACY_TERM, null) == key
        val removeCustom = replaceCustom && preferences.getBoolean("remove_customized_when_sync", false)
        val termFilter = if (includeLegacy) "(termKey = ? OR termKey IS NULL OR termKey = '')" else "termKey = ?"
        val replaceFilter = if (removeCustom) termFilter else "$termFilter AND isFromServer = 1"
        val db = LitePal.getDatabase()

        db.beginTransaction()
        try {
            LitePal.deleteAll(Course::class.java, replaceFilter, key)
            newCourses.forEach { it.termKey = key }
            check(newCourses.isEmpty() || LitePal.saveAll(newCourses)) { "Could not save timetable" }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    val loginUrl: String
        get() = downloader.loginUrl
    val afterLoginPattern: String
        get() = downloader.afterLoginPattern

    val startTimes: Array<String>
        get() = downloader.START_TIMES

    val endTimes: Array<String>
        get() = downloader.END_TIMES

    fun setReferer(referer: String) {
        downloader.referer = referer
        preferences.edit().putString(PreferenceKey.REFERER, referer).apply()
    }

    companion object {
        private const val TERM_SELECTOR_MIGRATED = "term_selector_migrated"
        private const val LEGACY_TERM = "legacy_course_term"
        const val SUCCEEDED = CourseDownloader.DOWNLOADED
        const val FAILED = CourseDownloader.FAILED
        const val UNLOGIN = CourseDownloader.UNLOGIN
        private var singleton: CourseManager? = null

        @JvmStatic
        fun getInstance(context: Context): CourseManager {
            if (singleton == null) {
                singleton = CourseManager(context)
            }
            return singleton!!
        }

    }
}
