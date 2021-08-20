package com.dujiajun.courseblock.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.dujiajun.courseblock.model.Course;

import org.litepal.LitePal;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CourseManager {
    public static final String DEFAULT_TERM = "1";

    public static final int SUCCEEDED = CourseDownloader.DOWNLOADED;
    public static final int FAILED = CourseDownloader.FAILED;
    public static final int UNLOGIN = CourseDownloader.UNLOGIN;
    private static CourseManager singleton;
    private final SharedPreferences preferences;
    private List<Course> courses;
    private CourseDownloader downloader;
    private STATUS status;


    private CourseManager(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        updateStatus();
    }

    public static CourseManager getInstance(Context context) {
        if (singleton == null) {
            singleton = new CourseManager(context);
        }
        return singleton;
    }

    public static String getDefaultYear() {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        return String.valueOf(calendar.get(Calendar.YEAR));
    }

    public void updateStatus() {
        String s = preferences.getString("status", "0");
        STATUS status = s.equals("1") ? STATUS.GRADUATE : STATUS.UNDERGRADUATE;
        if (status != this.status) {
            this.status = status;
            if (status == STATUS.GRADUATE) {
                downloader = new GraduateDownloader();
            } else {
                downloader = new UndergraduateDownloader();
            }
        }
    }

    public void loadCourses() {
        courses = LitePal.findAll(Course.class);
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void updateCourses(Handler uiHandler) {
        Handler handler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Message uiMessage = new Message();
                if (msg.what == CourseDownloader.DOWNLOADED) {
                    List<Course> downloadedCourses = (List<Course>) msg.obj;
                    updateCourses(downloadedCourses);
                    uiMessage.what = SUCCEEDED;
                    uiMessage.obj = msg.obj;
                } else {
                    uiMessage.what = msg.what;
                }
                uiHandler.sendMessage(uiMessage);
            }
        };

        String year = preferences.getString("cur_year", getDefaultYear());
        String term = preferences.getString("cur_term", DEFAULT_TERM);
        downloader.getCourses(year, term, handler);
    }

    private void updateCourses(List<Course> courses) {
        if (preferences.getBoolean("remove_customized_when_sync", false)) {
            LitePal.deleteAll(Course.class);
        } else {
            LitePal.deleteAll(Course.class, "isFromServer = ?", "1");
        }
        LitePal.saveAll(courses);
    }

    public String getLoginUrl() {
        return downloader.loginUrl;
    }

    public Course findCourseByDayAndStart(int week, int day, int start) {
        for (Course course : courses) {
            if (course.getWeekCode().charAt(week) == '1' &&
                    course.getDay() == day && course.getStart() <= start &&
                    (course.getStart() + course.getStep() - 1) >= start)
                return course;
        }
        return null;
    }

    public enum STATUS {UNDERGRADUATE, GRADUATE}
}
