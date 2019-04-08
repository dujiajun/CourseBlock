package com.dujiajun.courseblock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zhuangfei.timetable.model.Schedule;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class CourseActivity extends AppCompatActivity {

    private EditText etCourse, etTeacher, etLocation, etNote;
    private TextView tvWeeks, tvDay, tvStart, tvEnd;
    private int day;
    private int start;
    private int end;
    private CourseManager courseManager;
    private boolean[] isWeekSelected = new boolean[CourseManager.MAX_WEEKS];
    private List<Integer> weekList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);
        initUI();
        courseManager = CourseManager.getInstance(getApplicationContext());
    }

    private void initUI() {
        Intent intent = getIntent();
        day = intent.getIntExtra("day", 0);
        start = intent.getIntExtra("start", 1);
        int week = intent.getIntExtra("week", 1);
        end = start;
        isWeekSelected[week - 1] = true;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etCourse = findViewById(R.id.et_course);
        etTeacher = findViewById(R.id.et_teacher);
        etLocation = findViewById(R.id.et_location);
        etNote = findViewById(R.id.et_note);
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

        refreshTextViewAfterDialog();
    }

    private void refreshTextViewAfterDialog() {
        StringBuilder stringBuilder = new StringBuilder();
        weekList.clear();
        for (int i = 0; i < isWeekSelected.length; i++) {
            if (isWeekSelected[i]) {
                stringBuilder.append(i + 1);
                stringBuilder.append(" ");
                weekList.add(i + 1);
            }
        }

        if (stringBuilder.length() != 0) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            tvWeeks.setText(String.format(getString(R.string.week), stringBuilder.toString()));
        } else {
            tvWeeks.setText("");
        }
    }

    private void showWeekDialog() {

        final String[] items = new String[CourseManager.MAX_WEEKS];
        for (int i = 1; i <= CourseManager.MAX_WEEKS; i++) {
            items[i - 1] = String.format(getString(R.string.week), String.valueOf(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_week));
        builder.setMultiChoiceItems(items, isWeekSelected, (dialog, which, isChecked) -> isWeekSelected[which] = isChecked);
        builder.setNeutralButton(R.string.ok,
                (dialog, which) -> refreshTextViewAfterDialog());
        builder.setNegativeButton(getString(R.string.select_all), (dialog, which) -> {
            for (int i = 0; i < isWeekSelected.length; i++)
                isWeekSelected[i] = true;
            refreshTextViewAfterDialog();
        });
        builder.setPositiveButton(getString(R.string.unselect_all), (dialog, which) -> {
            for (int i = 0; i < isWeekSelected.length; i++)
                isWeekSelected[i] = false;
            refreshTextViewAfterDialog();
        });
        builder.show();
    }

    private void showStartDialog() {
        final String[] items = new String[CourseManager.MAX_STEPS];
        for (int i = 1; i <= CourseManager.MAX_STEPS; i++)
            items[i - 1] = String.format(getString(R.string.period), String.valueOf(i));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_start));
        builder.setSingleChoiceItems(items, start - 1,
                (dialog, which) -> start = which + 1);
        builder.setPositiveButton(R.string.ok,
                (dialog, which) -> tvStart.setText(items[start - 1]));
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showDayDialog() {
        final String[] items = getResources().getStringArray(R.array.days_in_week);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_day));
        builder.setSingleChoiceItems(items, day,
                (dialog, which) -> day = which);
        builder.setPositiveButton(R.string.ok,
                (dialog, which) -> tvDay.setText(items[day]));
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showEndDialog() {
        final String[] items = new String[CourseManager.MAX_STEPS - start + 1];
        for (int i = start; i <= CourseManager.MAX_STEPS; i++)
            items[i - start] = String.format(getString(R.string.period), String.valueOf(i));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_end));
        builder.setSingleChoiceItems(items, end - 1,
                (dialog, which) -> end = which + 1);
        builder.setPositiveButton(R.string.ok,
                (dialog, which) -> tvEnd.setText(items[end - 1]));
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private boolean checkAndApply() {
        if ("".equals(etCourse.getText().toString().trim())) {
            Toast.makeText(this, "Please input the course name!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if ("".equals(tvWeeks.getText().toString().trim())) {
            Toast.makeText(this, "Please input the course name!", Toast.LENGTH_SHORT).show();
            return false;
        }
        Schedule schedule = new Schedule();
        schedule.setName(etCourse.getText().toString().trim());
        schedule.setTeacher(etTeacher.getText().toString().trim());
        schedule.setWeekList(weekList);
        schedule.setStart(start);
        schedule.setStep(end - start + 1);
        schedule.setDay(day+1);
        schedule.setRoom(etLocation.getText().toString().trim());
        courseManager.insertNewCourse(schedule);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_course, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_apply) {
            if (checkAndApply())
                finish();
        }

        return true;
    }
}
