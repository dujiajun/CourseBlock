package com.dujiajun.courseblock

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dujiajun.courseblock.model.Course
import org.litepal.LitePal.find
import java.util.Arrays

class CourseActivity : AppCompatActivity() {
    private val isWeekSelected = BooleanArray(Course.MAX_WEEKS)
    private val weekItems = arrayOfNulls<String>(Course.MAX_WEEKS)
    private val startItems = arrayOfNulls<String>(Course.MAX_STEPS)
    private lateinit var etCourse: EditText
    private lateinit var etTeacher: EditText
    private lateinit var etLocation: EditText
    private lateinit var etNote: EditText
    private lateinit var etCourseId: EditText
    private lateinit var etClassId: EditText
    private lateinit var tvWeeks: TextView
    private lateinit var tvDay: TextView
    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private var day = 0
    private var start = 0
    private var end = 0
    private var step = 0
    private lateinit var dayItems: Array<String>
    private lateinit var endItems: Array<String>
    private var weekCode = CharArray(Course.MAX_WEEKS)
    private var action = 0
    private var course: Course = Course()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course)
        initUI()
    }

    private fun initUI() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        etCourse = findViewById(R.id.et_course)
        etTeacher = findViewById(R.id.et_teacher)
        etLocation = findViewById(R.id.et_location)
        etNote = findViewById(R.id.et_note)
        etCourseId = findViewById(R.id.et_course_id)
        etClassId = findViewById(R.id.et_class_id)
        tvWeeks = findViewById(R.id.tv_weeks)
        tvDay = findViewById(R.id.tv_day)
        tvDay.text = resources.getStringArray(R.array.days_in_week)[day]
        tvDay.setOnClickListener { showDayDialog() }
        tvStart = findViewById(R.id.tv_start)
        tvStart.text = String.format(getString(R.string.period), start.toString())
        tvStart.setOnClickListener { showStartDialog() }
        tvEnd = findViewById(R.id.tv_end)
        tvEnd.text = String.format(getString(R.string.period), end.toString())
        tvEnd.setOnClickListener { showEndDialog() }
        tvWeeks.setOnClickListener { showWeekDialog() }
        val intent = intent
        action = intent.getIntExtra("action", ACTION_INSERT)
        if (action == ACTION_INSERT) {
            day = intent.getIntExtra("day", 1)
            start = intent.getIntExtra("start", 1)
            val week = intent.getIntExtra("week", 1)
            end = start
            step = 1
            isWeekSelected[week - 1] = true
            course = Course()
        } else if (action == ACTION_DETAIL) {
            course = find(Course::class.java, intent.getIntExtra("course", 0).toLong())
        }
        actionChange()
        refreshTextViewAfterDialog()
    }

    private fun loadInitData() {
        if (action == ACTION_DETAIL) {
            etClassId.setText(course.classId)
            etCourse.setText(course.courseName)
            etLocation.setText(course.location)
            etTeacher.setText(course.teacher)
            etNote.setText(course.note)
            etCourseId.setText(course.courseId)
            day = course.day
            start = course.start
            step = course.step
            end = start + step - 1
            weekCode = course.weekCode.toCharArray()
            for (i in weekCode.indices) {
                isWeekSelected[i] = weekCode[i] == '1'
            }
        }
        for (i in 1..Course.MAX_WEEKS) {
            weekItems[i - 1] = String.format(getString(R.string.week), i.toString())
        }
        dayItems = resources.getStringArray(R.array.days_in_week)
        for (i in 1..Course.MAX_STEPS) startItems[i - 1] =
            String.format(getString(R.string.period), i.toString())
        endItems = Array(Course.MAX_STEPS - start + 1) { "" }
        for (i in start..Course.MAX_STEPS) endItems[i - start] =
            String.format(getString(R.string.period), i.toString())
        tvStart.text = startItems[start - 1]
        tvDay.text = dayItems[day - 1]
        tvEnd.text = endItems[end - start]
    }

    private fun refreshTextViewAfterDialog() {
        val stringBuilder = StringBuilder()
        for (i in isWeekSelected.indices) {
            if (isWeekSelected[i]) {
                stringBuilder.append(i + 1)
                stringBuilder.append(" ")
            }
        }
        tvWeeks.text = stringBuilder.toString().trim { it <= ' ' }
    }

    private fun showWeekDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_week))
        builder.setMultiChoiceItems(
            weekItems,
            isWeekSelected
        ) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            isWeekSelected[which] = isChecked
            weekCode[which] = if (isChecked) '1' else '0'
        }
        builder.setNeutralButton(R.string.ok) { _: DialogInterface?, _: Int -> refreshTextViewAfterDialog() }
        builder.setNegativeButton(getString(R.string.select_all)) { _: DialogInterface?, _: Int ->
            Arrays.fill(isWeekSelected, true)
            Arrays.fill(weekCode, '1')
            refreshTextViewAfterDialog()
        }
        builder.setPositiveButton(getString(R.string.unselect_all)) { _: DialogInterface?, _: Int ->
            Arrays.fill(isWeekSelected, false)
            Arrays.fill(weekCode, '0')
            refreshTextViewAfterDialog()
        }
        builder.show()
    }

    private fun showStartDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_start))
        builder.setSingleChoiceItems(
            startItems,
            start - 1
        ) { _: DialogInterface?, which: Int -> start = which + 1 }
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            tvStart.text = startItems[start - 1]
            endItems = Array(Course.MAX_STEPS - start + 1) { "" }
            for (i in start..Course.MAX_STEPS) endItems[i - start] =
                String.format(getString(R.string.period), i.toString())
            if (step - 1 < Course.MAX_STEPS - start) tvEnd.text =
                endItems[step - 1] else tvEnd.text = endItems[0]
            end = start + step - 1
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun showDayDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_day))
        builder.setSingleChoiceItems(
            dayItems, day - 1
        ) { _: DialogInterface?, which: Int -> day = which + 1 }
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            tvDay.text = dayItems[day - 1]
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun showEndDialog() {
        endItems = Array(Course.MAX_STEPS - start + 1) { "" }
        for (i in start..Course.MAX_STEPS)
            endItems[i - start] = String.format(getString(R.string.period), i.toString())
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_end))
        builder.setSingleChoiceItems(
            endItems, step - 1
        ) { _: DialogInterface?, which: Int ->
            end = which + start
            step = end - start + 1
        }
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            tvEnd.text = endItems[step - 1]
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun checkCourse(insert: Boolean): Boolean {
        if ("" == etCourse.text.toString().trim { it <= ' ' }) {
            Toast.makeText(this, getString(R.string.please_input_name), Toast.LENGTH_SHORT).show()
            return false
        }
        if ("" == tvWeeks.text.toString().trim { it <= ' ' }) {
            Toast.makeText(this, getString(R.string.please_select_weeks), Toast.LENGTH_SHORT).show()
            return false
        }
        if (end < start) {
            Toast.makeText(this, getString(R.string.please_select_period), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        course.classId = etClassId.text.toString().trim { it <= ' ' }
        course.courseName = etCourse.text.toString().trim { it <= ' ' }
        course.teacher = etTeacher.text.toString().trim { it <= ' ' }
        course.weekCode = String(weekCode)
        course.start = start
        course.step = step
        course.day = day
        course.location = etLocation.text.toString().trim { it <= ' ' }
        course.note = etNote.text.toString().trim { it <= ' ' }
        course.courseId = etCourseId.text.toString().trim { it <= ' ' }
        course.weekCode = String(weekCode)
        course.setToDefault("isFromServer")
        return course.save()
    }

    private fun deleteCourse() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.warning))
            .setMessage(String.format(getString(R.string.sure_to_delete), course.courseName))
            .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                course.delete()
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_course, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_apply).isVisible = action != ACTION_DETAIL
        menu.findItem(R.id.action_modify).isVisible = action == ACTION_DETAIL
        menu.findItem(R.id.action_delete).isVisible = action == ACTION_MODIFY
        menu.findItem(R.id.action_cancel).isVisible = action == ACTION_MODIFY
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_apply) {
            if (action == ACTION_INSERT || action == ACTION_MODIFY) {
                if (checkCourse(action == ACTION_INSERT)) finish()
            }
        } else if (item.itemId == R.id.action_cancel) {
            action = ACTION_DETAIL
            invalidateOptionsMenu()
            actionChange()
        } else if (item.itemId == R.id.action_delete) {
            deleteCourse()
        } else if (item.itemId == R.id.action_modify) {
            action = ACTION_MODIFY
            invalidateOptionsMenu()
            actionChange()
        }
        return true
    }

    private fun actionChange() {
        etClassId.isEnabled = action != ACTION_DETAIL
        etCourse.isEnabled = action != ACTION_DETAIL
        etNote.isEnabled = action != ACTION_DETAIL
        etTeacher.isEnabled = action != ACTION_DETAIL
        etLocation.isEnabled = action != ACTION_DETAIL
        etCourseId.isEnabled = action != ACTION_DETAIL
        tvWeeks.isEnabled = action != ACTION_DETAIL
        tvEnd.isEnabled = action != ACTION_DETAIL
        tvStart.isEnabled = action != ACTION_DETAIL
        tvDay.isEnabled = action != ACTION_DETAIL
        loadInitData()
    }

    companion object {
        const val ACTION_INSERT = 0
        const val ACTION_MODIFY = 1
        const val ACTION_DETAIL = 2
    }
}