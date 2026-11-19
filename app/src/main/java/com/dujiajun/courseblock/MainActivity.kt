package com.dujiajun.courseblock

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.calendar.AcademicCalendar
import com.dujiajun.courseblock.calendar.AndroidCalendar
import com.dujiajun.courseblock.calendar.CalendarImport
import com.dujiajun.courseblock.calendar.importRemainingCourses
import com.dujiajun.courseblock.calendar.CourseOccurrences
import com.dujiajun.courseblock.calendar.DisplayedCourse
import com.dujiajun.courseblock.calendar.TimetableDates
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.helper.CourseManager
import com.dujiajun.courseblock.model.Course
import com.google.android.material.snackbar.Snackbar
import com.zhuangfei.timetable.TimetableView
import com.zhuangfei.timetable.listener.IWeekView.OnWeekItemClickedListener
import com.zhuangfei.timetable.listener.OnSlideBuildAdapter
import com.zhuangfei.timetable.listener.OnSpaceItemClickAdapter
import com.zhuangfei.timetable.view.WeekView
import kotlinx.coroutines.launch
import java.time.LocalDate

open class MainActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var courseManager: CourseManager
    private lateinit var toolbar: Toolbar
    private lateinit var timetableView: TimetableView
    private lateinit var weekView: WeekView
    private var displayedCourses = emptyList<DisplayedCourse>()
    private var shownTerm: String? = null
    private var exporting = false
    private var showWeek = 1
    private val courseSync by lazy { ViewModelProvider(this)[CourseSyncViewModel::class.java] }
    private val loginForSync =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            courseSync.waitingForLogin = false
            if (result.resultCode == RESULT_OK) {
                courseSync.start(courseManager, allowLogin = false, replaceCustom = courseSync.replaceCustomOnRetry)
            }
        }

    private val calendarPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (AndroidCalendar.hasPermission(this@MainActivity)) exportCalendar()
            else Snackbar.make(timetableView, R.string.calendar_permission_denied, Snackbar.LENGTH_LONG)
                .setAction(R.string.calendar_open_settings) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                }
                .show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.isFocusable = true
        toolbar.setOnClickListener {
            if (!courseSync.isBusy && !exporting) TermPickerDialog.show(supportFragmentManager)
        }
        supportFragmentManager.setFragmentResultListener(TermPickerDialog.RESULT_KEY, this) { _, _ ->
            refreshSelectedTerm()
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        courseManager = CourseManager.getInstance(applicationContext)
        shownTerm = savedInstanceState?.getString(STATE_SHOWN_TERM)
        timetableView = findViewById(R.id.id_timetableView)
        weekView = findViewById(R.id.id_weekview)
        weekView.curWeek(currentTeachingWeek() ?: 0).selectedWeek(openingWeek())
            .callback(object : OnWeekItemClickedListener {
                override fun onWeekClicked(week: Int) {
                    val cur = timetableView.curWeek()
                    timetableView.curWeek(week)
                    timetableView.onDateBuildListener().onUpdateDate(cur, week)
                    timetableView.changeWeekOnly(week)
                    showWeek = week
                }
            })
            .hideLeftLayout()
            .itemCount(Course.MAX_WEEKS)
            .isShow(false).showView()
        timetableView.callback(TimetableDates { courseManager.academicCalendar })
            .curWeek(openingWeek())
            .maxSlideItem(Course.MAX_STEPS)
            .callback(object : OnSpaceItemClickAdapter() {
                override fun onSpaceItemClick(day: Int, start: Int) {
                    val intent = Intent(this@MainActivity, CourseActivity::class.java)
                    intent.putExtra("day", day + 1)
                    intent.putExtra("start", start)
                    intent.putExtra("week", showWeek)
                    intent.putExtra("action", CourseActivity.ACTION_INSERT)
                    startActivity(intent)
                }
            })
            .callback { _: View?, day: Int, start: Int ->
                val c = displayedCourses.firstOrNull { it.isAt(showWeek, day, start) }?.course
                if (c != null) {
                    val intent = Intent(this@MainActivity, CourseActivity::class.java)
                    intent.putExtra("action", CourseActivity.ACTION_DETAIL)
                    intent.putExtra("course", c.id)
                    startActivity(intent)
                }
            }
        updateWeek()
        updateView()
        courseSync.outcome.observe(this) { outcome ->
            if (outcome != null && !isFinishing) {
                courseSync.clearOutcome()
                handleSyncOutcome(outcome)
            }
        }
    }

    private fun updateSectionLabels() {
        val listener = timetableView.onSlideBuildListener() as OnSlideBuildAdapter
        val showTime = preferences.getBoolean(PreferenceKey.SHOW_COURSE_TIME, true)
        val times = Array(Course.MAX_STEPS) { i ->
            if (showTime) "${courseManager.startTimes[i]}\n${courseManager.endTimes[i]}" else ""
        }
        listener.setTimes(times).setTimeTextColor(Color.BLACK)
        timetableView.updateSlideView()
    }

    override fun onResume() {
        super.onResume()
        val useChiIcon = preferences.getBoolean(PreferenceKey.USE_CHI_ICON, false)
        setIcon(useChiIcon)
        refreshSelectedTerm()
    }

    private fun refreshSelectedTerm() {
        courseManager.updateStatus()
        val previous = shownTerm
        val selectedTerm = courseManager.termKey
        shownTerm = selectedTerm
        updateWeek()
        updateView()
        val hasChangedTerm = previous != null && previous != selectedTerm
        if (!courseSync.isBusy && (hasChangedTerm ||
                (courseSync.lastAutomaticTerm != selectedTerm && !courseManager.hasSyncedTerm()))
        ) {
            courseSync.lastAutomaticTerm = selectedTerm
            syncSelectedTerm()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SHOWN_TERM, shownTerm)
        super.onSaveInstanceState(outState)
    }

    private fun setIcon(useChiIcon: Boolean) {
        val main = MainActivity::class.java.name
        val alias = main.substringBeforeLast('.') + ".MainAliasActivity"
        val disableActivity = if (useChiIcon) main else alias
        val enableActivity = if (!useChiIcon) main else alias
        val packageManager = packageManager
        packageManager.setComponentEnabledSetting(
            ComponentName(this, disableActivity),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            ComponentName(this, enableActivity),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_login -> {
                if (!courseSync.isBusy && !exporting) {
                    courseSync.replaceCustomOnRetry = false
                    launchLogin()
                }
            }

            R.id.action_settings -> {
                if (!courseSync.isBusy && !exporting) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }

            R.id.action_sync -> syncSelectedTerm(explicit = true)

            R.id.action_calendar -> exportCalendar()

            R.id.action_expand -> {
                if (weekView.isShowing) item.setIcon(R.mipmap.ic_expand_more_white_24dp) else item.setIcon(
                    R.mipmap.ic_expand_less_white_24dp
                )
                weekView.isShow(!weekView.isShowing)
            }
        }
        return true
    }

    private fun handleSyncOutcome(outcome: CourseSyncViewModel.Outcome) {
        val feedback = when (outcome) {
            CourseSyncViewModel.Outcome.SYNCED -> {
                updateWeek()
                updateView()
                R.string.succeed_in_sync
            }

            CourseSyncViewModel.Outcome.NEEDS_LOGIN -> {
                launchLogin()
                return
            }

            CourseSyncViewModel.Outcome.STILL_UNAUTHENTICATED -> R.string.please_log_in
            CourseSyncViewModel.Outcome.FAILED -> R.string.failed
        }
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show()
    }

    private fun launchLogin() {
        courseSync.waitingForLogin = true
        loginForSync.launch(Intent(this, LoginActivity::class.java))
    }

    private fun syncSelectedTerm(explicit: Boolean = false) {
        if (exporting) return
        if (courseManager.canSync) {
            courseSync.start(courseManager, allowLogin = true, replaceCustom = explicit)
        } else {
            val message = if (courseManager.school.isGraduate) {
                R.string.graduate_summer_unavailable
            } else {
                R.string.academic_calendar_unavailable
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateView() {
        courseManager.loadCourses()
        displayedCourses = CourseOccurrences.display(courseManager.courses)
        val showWeekend = preferences.getBoolean(PreferenceKey.SHOW_WEEKEND, true)
        val showNotCurWeek = preferences.getBoolean(PreferenceKey.SHOW_NOT_CUR_WEEK, true)
        timetableView.isShowWeekends(showWeekend).isShowNotCurWeek(showNotCurWeek)
            .source(displayedCourses).updateView()
        updateSectionLabels()
        val calendarWeeks = courseManager.academicCalendar?.weekCount ?: Course.MAX_WEEKS
        val latestCourseWeek = displayedCourses.maxOfOrNull { it.course.weekCode.lastIndexOf('1') + 1 } ?: 0
        weekView.itemCount(maxOf(calendarWeeks, latestCourseWeek).coerceAtMost(Course.MAX_WEEKS))
            .source(displayedCourses).showView()
        val termLabel = TermPickerDialog.label(this, courseManager.term)
        val label = if (courseManager.academicCalendar != null) termLabel
        else getString(R.string.display_term_no_dates, termLabel)
        supportActionBar?.subtitle = getString(R.string.edit_term_label, label)
        toolbar.contentDescription = getString(R.string.edit_term_description, label)
    }

    private fun currentTeachingWeek() =
        courseManager.academicCalendar?.weekOf(LocalDate.now(AcademicCalendar.ZONE))

    private fun openingWeek() = currentTeachingWeek() ?: 1

    private fun updateWeek() {
        val currentWeek = currentTeachingWeek()
        showWeek = currentWeek ?: 1
        timetableView.curWeek(showWeek)
        weekView.curWeek(currentWeek ?: 0).selectedWeek(showWeek)
    }


    private fun exportCalendar() {
        if (exporting || courseSync.isBusy) return
        exporting = true
        lifecycleScope.launch {
            try {
                when (val result = importRemainingCourses(this@MainActivity, courseManager)) {
                    is CalendarImport.Message -> Toast.makeText(
                        this@MainActivity, result.text, Toast.LENGTH_LONG,
                    ).show()
                    CalendarImport.PermissionRequired -> calendarPermissions.launch(AndroidCalendar.permissions)
                }
            } finally {
                exporting = false
            }
        }
    }

    private companion object {
        const val STATE_SHOWN_TERM = "shown_term"
    }
}
