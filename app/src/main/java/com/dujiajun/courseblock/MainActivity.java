package com.dujiajun.courseblock;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.zhuangfei.timetable.TimetableView;
import com.zhuangfei.timetable.listener.ISchedule;
import com.zhuangfei.timetable.listener.OnSlideBuildAdapter;
import com.zhuangfei.timetable.listener.OnSpaceItemClickAdapter;
import com.zhuangfei.timetable.view.WeekView;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private String cur_year;
    private String cur_term;
    private int cur_week;
    private CourseManager courseManager;
    private TimetableView timetableView;
    private WeekView weekView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        cur_year = preferences.getString("cur_year", "2018");
        cur_term = preferences.getString("cur_term", "3");
        cur_week = preferences.getInt("cur_week", 1);

        timetableView = findViewById(R.id.id_timetableView);
        weekView = findViewById(R.id.id_weekview);

        weekView.curWeek(cur_week)
                .callback(week -> {
                    int cur = timetableView.curWeek();
                    timetableView.onDateBuildListener()
                            .onUpdateDate(cur, week);
                    timetableView.changeWeekOnly(week);
                })
                .callback(this::showCurrentWeekDialog)
                .itemCount(CourseManager.MAX_WEEKS)
                .isShow(false).showView();

        boolean show_weekend = preferences.getBoolean("show_weekend", true);
        boolean show_not_cur_week = preferences.getBoolean("show_not_cur_week", true);
        boolean show_time = preferences.getBoolean("show_course_time", true);

        timetableView.curWeek(cur_week)
                .isShowNotCurWeek(show_not_cur_week)
                .isShowWeekends(show_weekend)
                .maxSlideItem(CourseManager.MAX_STEPS)
                .callback(new OnSpaceItemClickAdapter() {
                    @Override
                    public void onSpaceItemClick(int day, int start) {
                        Intent intent = new Intent(MainActivity.this, CourseActivity.class);
                        intent.putExtra("day", day+1);
                        intent.putExtra("start", start);
                        intent.putExtra("week", timetableView.curWeek());
                        intent.putExtra("action",CourseActivity.ACTION_INSERT);
                        startActivity(intent);
                        //Toast.makeText(MainActivity.this, String.valueOf(day)+" "+String.valueOf(start), Toast.LENGTH_SHORT).show();
                    }
                })
                .callback((ISchedule.OnItemLongClickListener) (v, day, start) -> {
                    Course c = courseManager.findCourseByDayAndStart(day,start);
                    Intent intent = new Intent(MainActivity.this,CourseActivity.class);
                    intent.putExtra("action",CourseActivity.ACTION_DETAIL);
                    intent.putExtra("course",c);
                    startActivity(intent);
                    //Toast.makeText(MainActivity.this, c.getCourseId()+c.getCourseName(), Toast.LENGTH_SHORT).show();
                });
        if (show_time)
            showTime();
        courseManager = CourseManager.getInstance(getApplicationContext());
        courseManager.readFromDatabase();
        timetableView.source(courseManager.getCourseList()).showView();
        weekView.source(courseManager.getCourseList()).updateView();

    }

    protected void showTime() {
        OnSlideBuildAdapter listener = (OnSlideBuildAdapter) timetableView.onSlideBuildListener();
        listener.setTimes(CourseManager.times)
                .setTimeTextColor(Color.BLACK);
        timetableView.updateSlideView();
    }

    private void showCurrentWeekDialog() {

        final String[] items = new String[CourseManager.MAX_WEEKS];
        for (int i = 1; i <= CourseManager.MAX_WEEKS; i++)
            items[i - 1] = String.valueOf(i);
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(MainActivity.this);
        listDialog.setTitle(R.string.current_week);
        listDialog.setItems(items, (dialog, which) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("cur_week", which + 1);
            weekView.curWeek(which + 1).updateView();
            timetableView.changeWeekOnly(which + 1);
            editor.apply();
        });
        listDialog.show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        cur_year = preferences.getString("cur_year", "2018");
        cur_term = preferences.getString("cur_term", "12");
        cur_week = preferences.getInt("cur_week", 1);
        timetableView.source(courseManager.getCourseList()).updateView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_login) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.action_sync) {
            CourseManager.SEMESTER semester = CourseManager.getSemesterFromValue(cur_term);
            courseManager.updateCourseDatabase(cur_year
                    , semester
                    , new CourseManager.ShowInUICallback() {
                        @Override
                        public void onShow(List<Course> schedules) {
                            timetableView.source(schedules).showView();
                            weekView.source(schedules).updateView();
                        }

                        @Override
                        public void onToast(String message) {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else if (id == R.id.action_expand) {
            if (weekView.isShowing())
                item.setIcon(R.mipmap.ic_expand_more_white_24dp);
            else
                item.setIcon(R.mipmap.ic_expand_less_white_24dp);

            weekView.isShow(!weekView.isShowing());
        }

        return true;
    }


}
