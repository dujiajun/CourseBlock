package com.dujiajun.courseblock

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.helper.CourseManager
import com.dujiajun.courseblock.helper.WeekManager
import com.dujiajun.courseblock.model.Course
import com.zhuangfei.timetable.TimetableView
import com.zhuangfei.timetable.listener.IWeekView.OnWeekItemClickedListener
import com.zhuangfei.timetable.listener.OnSlideBuildAdapter
import com.zhuangfei.timetable.listener.OnSpaceItemClickAdapter
import com.zhuangfei.timetable.view.WeekView

open class MainActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var courseManager: CourseManager
    private lateinit var timetableView: TimetableView
    private lateinit var weekView: WeekView
    private lateinit var weekManager: WeekManager
    private var showWeek = 1
    private val courseSync by lazy { ViewModelProvider(this)[CourseSyncViewModel::class.java] }
    private val loginForSync =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            courseSync.waitingForLogin = false
            if (result.resultCode == RESULT_OK) {
                courseSync.start(courseManager, allowLogin = false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        weekManager = WeekManager.getInstance(this)
        courseManager = CourseManager.getInstance(applicationContext)
        timetableView = findViewById(R.id.id_timetableView)
        weekView = findViewById(R.id.id_weekview)
        weekView.curWeek(weekManager.getCurWeek())
            .callback(object : OnWeekItemClickedListener {
                override fun onWeekClicked(week: Int) {
                    val cur = timetableView.curWeek()
                    timetableView.onDateBuildListener().onUpdateDate(cur, week)
                    timetableView.changeWeekOnly(week)
                    showWeek = week
                }
            })
            .hideLeftLayout()
            .itemCount(Course.MAX_WEEKS)
            .isShow(false).showView()
        val showWeekend = preferences.getBoolean(PreferenceKey.SHOW_WEEKEND, true)
        val showNotCurWeek = preferences.getBoolean(PreferenceKey.SHOW_NOT_CUR_WEEK, true)
        val showTime = preferences.getBoolean(PreferenceKey.SHOW_COURSE_TIME, true)
        timetableView.curWeek(weekManager.getCurWeek())
            .isShowNotCurWeek(showNotCurWeek)
            .isShowWeekends(showWeekend)
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
                val c = courseManager.findCourseByDayAndStart(showWeek, day, start)
                if (c != null) {
                    val intent = Intent(this@MainActivity, CourseActivity::class.java)
                    intent.putExtra("action", CourseActivity.ACTION_DETAIL)
                    intent.putExtra("course", c.id)
                    startActivity(intent)
                }
            }
        if (showTime) showTime()
        updateWeek()
        updateView()
        courseSync.outcome.observe(this) { outcome ->
            if (outcome != null && !isFinishing) {
                courseSync.clearOutcome()
                handleSyncOutcome(outcome)
            }
        }
    }

    private fun showTime() {
        val listener = timetableView.onSlideBuildListener() as OnSlideBuildAdapter
        val showTimes = Array(Course.MAX_STEPS) { "" }
        for (i in 0 until Course.MAX_STEPS) {
            showTimes[i] = """
                ${courseManager.startTimes[i]}
                ${courseManager.endTimes[i]}
                """.trimIndent()
        }
        listener.setTimes(showTimes).setTimeTextColor(Color.BLACK)
        timetableView.updateSlideView()
    }

    override fun onResume() {
        super.onResume()
        val useChiIcon = preferences.getBoolean(PreferenceKey.USE_CHI_ICON, false)
        setIcon(useChiIcon)
        courseManager.updateStatus()
        updateWeek()
        updateView()
        val showTime = preferences.getBoolean(PreferenceKey.SHOW_COURSE_TIME, true)
        if (showTime) showTime()
    }

    private fun setIcon(useChiIcon: Boolean) {
        val disableActivity = if (useChiIcon) ".MainActivity" else ".MainAliasActivity"
        val enableActivity = if (!useChiIcon) ".MainActivity" else ".MainAliasActivity"
        val packageManager = packageManager
        packageManager.setComponentEnabledSetting(
            ComponentName(this, packageName + disableActivity),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            ComponentName(this, packageName + enableActivity),
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
                if (!courseSync.isBusy) {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }

            R.id.action_settings -> {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }

            R.id.action_sync -> {
                courseSync.start(courseManager, allowLogin = true)
            }

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
                updateView()
                R.string.succeed_in_sync
            }

            CourseSyncViewModel.Outcome.NEEDS_LOGIN -> {
                courseSync.waitingForLogin = true
                loginForSync.launch(Intent(this, LoginActivity::class.java))
                return
            }

            CourseSyncViewModel.Outcome.STILL_UNAUTHENTICATED -> R.string.please_log_in
            CourseSyncViewModel.Outcome.FAILED -> R.string.failed
        }
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show()
    }

    private fun updateView() {
        courseManager.loadCourses()
        timetableView.source(courseManager.courses).updateView()
        weekView.source(courseManager.courses).showView()
    }

    private fun updateWeek() {
        val currentWeek = weekManager.getCurWeek()
        showWeek = currentWeek
        timetableView.curWeek(currentWeek)
        weekView.curWeek(currentWeek)
    }
}
