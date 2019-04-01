package com.dujiajun.courseblock;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.zhuangfei.timetable.TimetableView;
import com.zhuangfei.timetable.listener.OnSlideBuildAdapter;
import com.zhuangfei.timetable.view.WeekView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private CourseManager courseManager;
    private TimetableView timetableView;
    private WeekView weekView;

    private final int MAX_WEEKS = 22;
    private final int MAX_STEPS = 13;
    SharedPreferences preferences;

    String cur_year;// = preferences.getString("cur_year","2018");
    String cur_term;// = preferences.getString("cur_term","3");
    int cur_week;// = preferences.getInt("cur_week",1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        cur_year = preferences.getString("cur_year","2018");
        cur_term = preferences.getString("cur_term","3");
        cur_week = preferences.getInt("cur_week",1);

        timetableView = findViewById(R.id.id_timetableView);
        weekView = findViewById(R.id.id_weekview);

        //Toast.makeText(this, "test", Toast.LENGTH_SHORT).show();
        weekView.curWeek(cur_week)
                .callback(week -> {
                    int cur = timetableView.curWeek();
                    //更新切换后的日期，从当前周cur->切换的周week
                    timetableView.onDateBuildListener()
                            .onUpdateDate(cur, week);
                    timetableView.changeWeekOnly(week);
                })
                .callback(this::showCurrentWeekDialog)
                .itemCount(MAX_WEEKS)
                .isShow(false).showView();

        boolean show_weekend = preferences.getBoolean("show_weekend",true);
        boolean show_not_cur_week = preferences.getBoolean("show_not_cur_week",true);
        boolean show_time = preferences.getBoolean("show_course_time",true);

        timetableView.curWeek(cur_week)
                .isShowNotCurWeek(show_not_cur_week)
                .isShowWeekends(show_weekend)
                .maxSlideItem(MAX_STEPS);
        if (show_time)
            showTime();
        courseManager = CourseManager.getInstance(getApplicationContext());
        courseManager.readFromDatabase();
        timetableView.data(courseManager.getScheduleList()).showView();
        weekView.data(courseManager.getScheduleList()).updateView();

    }

    /**
     * 显示时间
     * 设置侧边栏构建监听，TimeSlideAdapter是控件实现的可显示时间的侧边栏
     */
    protected void showTime() {
        String[] times = new String[]{
                "8:00\n8:45", "8:55\n9:40", "10:00\n10:45", "10:55\n11:40",
                "12:00\n12:45", "12:55\n13:40", "14:00\n14:55", "14:55\n13:40",
                "16:00\n16:45", "16:55\n17:40", "18:00\n18:45","18:55\n19:40",
                "20:00\n20:20"
        };
        OnSlideBuildAdapter listener= (OnSlideBuildAdapter) timetableView.onSlideBuildListener();
        listener.setTimes(times)
                .setTimeTextColor(Color.BLACK);
        timetableView.updateSlideView();
    }

    private void showCurrentWeekDialog() {

        final String[] items = new String[MAX_WEEKS];
        for (int i = 1;i <= MAX_WEEKS;i++)
            items[i-1] = String.valueOf(i);
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(MainActivity.this);
        listDialog.setTitle(R.string.current_week);
        listDialog.setItems(items, (dialog, which) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("cur_week",which+1);
            weekView.curWeek(which+1).updateView();
            timetableView.changeWeekOnly(which+1);
            editor.apply();
        });
        listDialog.show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        cur_year = preferences.getString("cur_year","2018");
        cur_term = preferences.getString("cur_term","12");
        cur_week = preferences.getInt("cur_week",1);
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
                    , schedules -> {
                        timetableView.data(schedules).showView();
                        weekView.data(schedules).updateView();
                    });
        } else if (id == R.id.action_expand){
            if (weekView.isShowing())
                item.setIcon(R.mipmap.ic_expand_more_black_24dp);
            else
                item.setIcon(R.mipmap.ic_expand_less_black_24dp);

            weekView.isShow(!weekView.isShowing());
        }

        return true;
    }


}
