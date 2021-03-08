package com.dujiajun.courseblock;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.dujiajun.courseblock.helper.CourseManager;
import com.dujiajun.courseblock.helper.WeekManager;
import com.dujiajun.courseblock.model.Course;
import com.zhuangfei.timetable.TimetableView;
import com.zhuangfei.timetable.listener.ISchedule;
import com.zhuangfei.timetable.listener.OnSlideBuildAdapter;
import com.zhuangfei.timetable.listener.OnSpaceItemClickAdapter;
import com.zhuangfei.timetable.view.WeekView;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private CourseManager courseManager;
    private TimetableView timetableView;
    private WeekView weekView;
    private WeekManager weekManager;
    private int showWeek = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        weekManager = WeekManager.getInstance(this);

        timetableView = findViewById(R.id.id_timetableView);
        weekView = findViewById(R.id.id_weekview);
        weekView.curWeek(weekManager.getCurWeek())
                .callback(week -> {
                    int cur = timetableView.curWeek();
                    timetableView.onDateBuildListener()
                            .onUpdateDate(cur, week);
                    timetableView.changeWeekOnly(week);
                    showWeek = week;
                })
                .hideLeftLayout()
                .itemCount(Course.MAX_WEEKS)
                .isShow(false).showView();

        boolean show_weekend = preferences.getBoolean("show_weekend", true);
        boolean show_not_cur_week = preferences.getBoolean("show_not_cur_week", true);
        boolean show_time = preferences.getBoolean("show_course_time", true);

        timetableView.curWeek(weekManager.getCurWeek())
                .isShowNotCurWeek(show_not_cur_week)
                .isShowWeekends(show_weekend)
                .maxSlideItem(Course.MAX_STEPS)
                .callback(new OnSpaceItemClickAdapter() {
                    @Override
                    public void onSpaceItemClick(int day, int start) {
                        Intent intent = new Intent(MainActivity.this, CourseActivity.class);
                        intent.putExtra("day", day + 1);
                        intent.putExtra("start", start);
                        intent.putExtra("week", showWeek);
                        intent.putExtra("action", CourseActivity.ACTION_INSERT);
                        startActivity(intent);
                    }
                })
                .callback((ISchedule.OnItemLongClickListener) (v, day, start) -> {
                    Course c = courseManager.findCourseByDayAndStart(showWeek, day, start);
                    if (c != null) {
                        Intent intent = new Intent(MainActivity.this, CourseActivity.class);
                        intent.putExtra("action", CourseActivity.ACTION_DETAIL);
                        intent.putExtra("course", c.getId());
                        startActivity(intent);
                    }
                });
        if (show_time)
            showTime();
        courseManager = CourseManager.getInstance(getApplicationContext());
        updateView();
        updateWeek();
    }

    protected void showTime() {
        OnSlideBuildAdapter listener = (OnSlideBuildAdapter) timetableView.onSlideBuildListener();
        String[] showTimes = new String[Course.MAX_STEPS];
        for (int i = 0; i < Course.MAX_STEPS; i++) {
            showTimes[i] = Course.START_TIMES[i] + "\n" + Course.END_TIMES[i];
        }
        listener.setTimes(showTimes)
                .setTimeTextColor(Color.BLACK);
        timetableView.updateSlideView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean use_chi_icon = preferences.getBoolean("use_chi_icon", false);
        setIcon(use_chi_icon);

        courseManager.updateStatus();

        showWeek = timetableView.curWeek();
        updateView();
    }

    private void setIcon(boolean use_chi_icon) {
        String disable_activity = use_chi_icon ? ".MainActivity" : ".MainAliasActivity";
        String enable_activity = (!use_chi_icon) ? ".MainActivity" : ".MainAliasActivity";
        PackageManager packageManager = getPackageManager();
        packageManager.setComponentEnabledSetting(new ComponentName(this, getPackageName() +
                disable_activity), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager
                .DONT_KILL_APP);
        packageManager.setComponentEnabledSetting(new ComponentName(this, getPackageName() +
                enable_activity), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager
                .DONT_KILL_APP);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_login) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.action_sync) {
            courseManager.updateCourses(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == CourseManager.SUCCEEDED) {
                        updateView();
                        Toast.makeText(MainActivity.this, R.string.succeed_in_sync, Toast.LENGTH_SHORT).show();
                    } else if (msg.what == CourseManager.UNLOGIN) {
                        Toast.makeText(MainActivity.this, R.string.please_log_in, Toast.LENGTH_SHORT).show();
                    } else if (msg.what == CourseManager.FAILED) {
                        Toast.makeText(MainActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
                    }
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

    private void updateView() {
        courseManager.loadCourses();
        timetableView.source(courseManager.getCourses()).updateView();
        weekView.source(courseManager.getCourses()).showView();
    }

    private void updateWeek() {
        weekManager.updateCurWeek();
        timetableView.curWeek(weekManager.getCurWeek());
        weekView.curWeek(weekManager.getCurWeek());
    }
}
