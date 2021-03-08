package com.dujiajun.courseblock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dujiajun.courseblock.model.Course;

import org.litepal.LitePal;

import java.util.Arrays;

public class CourseActivity extends AppCompatActivity {

    public static final int ACTION_INSERT = 0;
    public static final int ACTION_MODIFY = 1;
    public static final int ACTION_DETAIL = 2;
    private final boolean[] isWeekSelected = new boolean[Course.MAX_WEEKS];
    private final String[] weekItems = new String[Course.MAX_WEEKS];
    private final String[] startItems = new String[Course.MAX_STEPS];
    private EditText etCourse, etTeacher, etLocation, etNote, etCourseId, etClassId;
    private TextView tvWeeks, tvDay, tvStart, tvEnd;
    private int day;
    private int start;
    private int end;
    private int step;
    private String[] dayItems;
    private String[] endItems;
    private char[] weekCode = new char[Course.MAX_WEEKS];
    private int action;
    private Course course;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);
        initUI();
    }

    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etCourse = findViewById(R.id.et_course);
        etTeacher = findViewById(R.id.et_teacher);
        etLocation = findViewById(R.id.et_location);
        etNote = findViewById(R.id.et_note);
        etCourseId = findViewById(R.id.et_course_id);
        etClassId = findViewById(R.id.et_class_id);
        tvWeeks = findViewById(R.id.tv_weeks);
        tvDay = findViewById(R.id.tv_day);
        tvDay.setText(getResources().getStringArray(R.array.days_in_week)[day]);
        tvDay.setOnClickListener(v -> showDayDialog());
        tvStart = findViewById(R.id.tv_start);
        tvStart.setText(String.format(getString(R.string.period), String.valueOf(start)));
        tvStart.setOnClickListener(v -> showStartDialog());
        tvEnd = findViewById(R.id.tv_end);
        tvEnd.setText(String.format(getString(R.string.period), String.valueOf(end)));
        tvEnd.setOnClickListener(v -> showEndDialog());
        tvWeeks.setOnClickListener(v -> showWeekDialog());

        Intent intent = getIntent();
        action = intent.getIntExtra("action", ACTION_INSERT);
        if (action == ACTION_INSERT) {
            day = intent.getIntExtra("day", 1);
            start = intent.getIntExtra("start", 1);
            int week = intent.getIntExtra("week", 1);
            end = start;
            step = 1;
            isWeekSelected[week - 1] = true;
            course = new Course();
        } else if (action == ACTION_DETAIL) {
            course = LitePal.find(Course.class, intent.getIntExtra("course", 0));
        }
        actionChange();
        refreshTextViewAfterDialog();
    }

    private void loadInitData() {
        if (action == ACTION_DETAIL) {
            etClassId.setText(course.getClassId());
            etCourse.setText(course.getCourseName());
            etLocation.setText(course.getLocation());
            etTeacher.setText(course.getTeacher());
            etNote.setText(course.getNote());
            etCourseId.setText(course.getCourseId());
            day = course.getDay();
            start = course.getStart();
            step = course.getStep();
            end = start + step - 1;
            weekCode = course.getWeekCode().toCharArray();
            for (int i = 0; i < weekCode.length; i++) {
                isWeekSelected[i] = weekCode[i] == '1';
            }
        }

        for (int i = 1; i <= Course.MAX_WEEKS; i++) {
            weekItems[i - 1] = String.format(getString(R.string.week), String.valueOf(i));
        }
        dayItems = getResources().getStringArray(R.array.days_in_week);
        for (int i = 1; i <= Course.MAX_STEPS; i++)
            startItems[i - 1] = String.format(getString(R.string.period), String.valueOf(i));
        endItems = new String[Course.MAX_STEPS - start + 1];
        for (int i = start; i <= Course.MAX_STEPS; i++)
            endItems[i - start] = String.format(getString(R.string.period), String.valueOf(i));

        tvStart.setText(startItems[start - 1]);
        tvDay.setText(dayItems[day - 1]);
        tvEnd.setText(endItems[end - start]);
    }

    private void refreshTextViewAfterDialog() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < isWeekSelected.length; i++) {
            if (isWeekSelected[i]) {
                stringBuilder.append(i + 1);
                stringBuilder.append(" ");
            }
        }
        tvWeeks.setText(stringBuilder.toString().trim());
    }

    private void showWeekDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_week));
        builder.setMultiChoiceItems(weekItems, isWeekSelected, (dialog, which, isChecked) -> {
            isWeekSelected[which] = isChecked;
            weekCode[which] = isChecked ? '1' : '0';
        });
        builder.setNeutralButton(R.string.ok,
                (dialog, which) -> refreshTextViewAfterDialog());
        builder.setNegativeButton(getString(R.string.select_all), (dialog, which) -> {
            Arrays.fill(isWeekSelected, true);
            Arrays.fill(weekCode, '1');
            refreshTextViewAfterDialog();
        });
        builder.setPositiveButton(getString(R.string.unselect_all), (dialog, which) -> {
            Arrays.fill(isWeekSelected, false);
            Arrays.fill(weekCode, '0');
            refreshTextViewAfterDialog();
        });
        builder.show();
    }

    private void showStartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_start));
        builder.setSingleChoiceItems(startItems, start - 1,
                (dialog, which) -> start = which + 1);
        builder.setPositiveButton(R.string.ok,
                (dialog, which) -> {
                    tvStart.setText(startItems[start - 1]);
                    endItems = new String[Course.MAX_STEPS - start + 1];
                    for (int i = start; i <= Course.MAX_STEPS; i++)
                        endItems[i - start] = String.format(getString(R.string.period), String.valueOf(i));
                    if (step - 1 < Course.MAX_STEPS - start)
                        tvEnd.setText(endItems[step - 1]);
                    else
                        tvEnd.setText(endItems[0]);
                    end = start + step - 1;
                });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showDayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_day));
        builder.setSingleChoiceItems(dayItems, day - 1,
                (dialog, which) -> day = which + 1);
        builder.setPositiveButton(R.string.ok,
                (dialog, which) -> tvDay.setText(dayItems[day - 1]));
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showEndDialog() {
        endItems = new String[Course.MAX_STEPS - start + 1];
        for (int i = start; i <= Course.MAX_STEPS; i++)
            endItems[i - start] = String.format(getString(R.string.period), String.valueOf(i));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_end));
        builder.setSingleChoiceItems(endItems, step - 1,
                (dialog, which) -> {
                    end = which + start;
                    step = end - start + 1;
                });
        builder.setPositiveButton(R.string.ok,
                (dialog, which) -> tvEnd.setText(endItems[step - 1]));
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }


    private boolean checkCourse(boolean insert) {
        if ("".equals(etCourse.getText().toString().trim())) {
            Toast.makeText(this, getString(R.string.please_input_name), Toast.LENGTH_SHORT).show();
            return false;
        }
        if ("".equals(tvWeeks.getText().toString().trim())) {
            Toast.makeText(this, getString(R.string.please_select_weeks), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (end < start) {
            Toast.makeText(this, getString(R.string.please_select_period), Toast.LENGTH_SHORT).show();
            return false;
        }
        course.setClassId(etClassId.getText().toString().trim());
        course.setCourseName(etCourse.getText().toString().trim());
        course.setTeacher(etTeacher.getText().toString().trim());
        course.setWeekCode(new String(weekCode));
        course.setStart(start);
        course.setStep(step);
        course.setDay(day);
        course.setLocation(etLocation.getText().toString().trim());
        course.setNote(etNote.getText().toString().trim());
        course.setCourseId(etCourseId.getText().toString().trim());
        course.setWeekCode(new String(weekCode));
        course.setToDefault("isFromServer");
        return course.save();
    }


    private void deleteCourse() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.warning))
                .setMessage(String.format(getString(R.string.sure_to_delete), course.getCourseName()))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    course.delete();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_course, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_apply).setVisible(action != ACTION_DETAIL);
        menu.findItem(R.id.action_modify).setVisible(action == ACTION_DETAIL);
        menu.findItem(R.id.action_delete).setVisible(action == ACTION_MODIFY);
        menu.findItem(R.id.action_cancel).setVisible(action == ACTION_MODIFY);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_apply) {
            if (action == ACTION_INSERT || action == ACTION_MODIFY) {
                if (checkCourse(action == ACTION_INSERT))
                    finish();
            }
        } else if (item.getItemId() == R.id.action_cancel) {
            action = ACTION_DETAIL;
            invalidateOptionsMenu();
            actionChange();
        } else if (item.getItemId() == R.id.action_delete) {
            deleteCourse();
        } else if (item.getItemId() == R.id.action_modify) {
            action = ACTION_MODIFY;
            invalidateOptionsMenu();
            actionChange();

        }
        return true;
    }

    private void actionChange() {
        etClassId.setEnabled(action != ACTION_DETAIL);
        etCourse.setEnabled(action != ACTION_DETAIL);
        etNote.setEnabled(action != ACTION_DETAIL);
        etTeacher.setEnabled(action != ACTION_DETAIL);
        etLocation.setEnabled(action != ACTION_DETAIL);
        etCourseId.setEnabled(action != ACTION_DETAIL);
        tvWeeks.setEnabled(action != ACTION_DETAIL);
        tvEnd.setEnabled(action != ACTION_DETAIL);
        tvStart.setEnabled(action != ACTION_DETAIL);
        tvDay.setEnabled(action != ACTION_DETAIL);

        loadInitData();
    }
}
