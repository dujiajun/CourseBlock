package com.dujiajun.courseblock.helper;

import android.os.Handler;

import com.dujiajun.courseblock.model.Course;

import java.util.List;

import okhttp3.Callback;
import okhttp3.OkHttpClient;

public abstract class CourseDownloader {
    public static final int DOWNLOADED = 0;
    public static final int FAILED = 1;
    public static final int UNLOGIN = 2;

    protected final OkHttpClient client;
    protected String loginUrl;
    protected String courseUrl;
    protected List<Course> courses;

    public CourseDownloader() {
        client = new OkHttpClient.Builder()
                .cookieJar(new WebViewCookieHandler())
                .build();
    }

    public abstract void getCourses(String year, String term, Handler handler);

    protected abstract List<Course> parseFrom(String json);

    protected abstract void download(String year, String term, Callback callback);

}
