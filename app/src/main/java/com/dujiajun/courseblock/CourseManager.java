package com.dujiajun.courseblock;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class CourseManager {

    static final int MAX_WEEKS = 22;
    static final int MAX_STEPS = 13;
    static final String[] times = new String[]{
            "8:00\n8:45", "8:55\n9:40", "10:00\n10:45", "10:55\n11:40",
            "12:00\n12:45", "12:55\n13:40", "14:00\n14:55", "14:55\n15:40",
            "16:00\n16:45", "16:55\n17:40", "18:00\n18:45", "18:55\n19:40",
            "20:00\n20:20"
    };
    private static CourseManager singleton;
    private final int MSG_TYPE_HEADER = 1;
    private final int MSG_TYPE_BODY = 0;
    private Resources resources;
    private List<Course> courseList;
    private CourseDBHelper dbHelper;
    private OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new WebViewCookieHandler())
            .build();
    private ResponseHandler responseHandler = new ResponseHandler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TYPE_HEADER) {
                if (callback != null)
                    callback.onToast(resources.getString(R.string.please_log_in));
            } else if (msg.what == MSG_TYPE_BODY) {
                String resp = (String) msg.obj;
                parseCourseJson(resp);
                writeToDatabase();
                if (callback != null)
                    callback.onShow(courseList);
            }
        }
    };
    private SharedPreferences preferences;

    private CourseManager(Context context) {
        courseList = new ArrayList<>();
        dbHelper = new CourseDBHelper(context);
        resources = context.getResources();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    static CourseManager getInstance(Context context) {
        if (singleton == null) {
            singleton = new CourseManager(context.getApplicationContext());
        }
        return singleton;
    }

    static SEMESTER getSemesterFromValue(String value) {
        if (value == null)
            return SEMESTER.FIRST;
        switch (value) {
            case "3":
                return SEMESTER.FIRST;
            case "12":
                return SEMESTER.SECOND;
            case "16":
                return SEMESTER.SUMMER;
            default:
                return SEMESTER.FIRST;
        }
    }

    List<Course> getCourseList() {
        return courseList;
    }

    private void writeToDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean removeCustomized = preferences.getBoolean("remove_customized_when_sync", true);
        if (removeCustomized)
            db.execSQL("delete from course");
        else
            db.execSQL("delete from course where from_server = 1");
        for (Course s :
                courseList) {
            ContentValues values = new ContentValues();
            values.put("name", s.getCourseName());
            values.put("room", s.getLocation());
            values.put("teacher", s.getTeacher());
            values.put("start", s.getStart());
            values.put("step", s.getStep());
            values.put("day", s.getDay());
            values.put("weeklist", getStringFromWeekList(s.getWeekList()));
            values.put("note", s.getNote());
            values.put("course_id", s.getCourseId());
            db.insert("course", null, values);
        }
        db.close();
    }

    void readFromDatabase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from course", null);
        courseList.clear();
        if (cursor.moveToFirst()) {
            do {
                Course course = new Course();
                course.setCourseName(cursor.getString(cursor.getColumnIndex("name")));
                course.setLocation(cursor.getString(cursor.getColumnIndex("room")));
                course.setTeacher(cursor.getString(cursor.getColumnIndex("teacher")));
                course.setStart(cursor.getInt(cursor.getColumnIndex("start")));
                course.setStep(cursor.getInt(cursor.getColumnIndex("step")));
                course.setDay(cursor.getInt(cursor.getColumnIndex("day")));
                course.setWeekList(getWeekListFromString(cursor.getString(cursor.getColumnIndex("weeklist"))));
                String note = cursor.getString(cursor.getColumnIndex("note"));
                if (note != null)
                    course.setNote(note.trim());
                String courseId = cursor.getString(cursor.getColumnIndex("course_id"));
                if (courseId != null)
                    course.setCourseId(courseId);
                course.setId(cursor.getInt(cursor.getColumnIndex("id")));
                courseList.add(course);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
    }

    private String getStringFromWeekList(List<Integer> weekList) {
        StringBuilder builder = new StringBuilder();
        for (Integer i :
                weekList) {
            builder.append(i);
            builder.append(',');
        }
        return builder.toString();
    }

    private List<Integer> getWeekListFromString(String s) {
        String[] arr = s.split(",");
        List<Integer> weekList = new ArrayList<>();
        for (String week :
                arr) {
            weekList.add(Integer.valueOf(week));
        }
        return weekList;
    }

    void updateCourseDatabase(String year, SEMESTER semester, ShowInUICallback callback) {

        RequestBody requestBody = new FormBody.Builder()
                .add("xnm", year)
                .add("xqm", semester.getValue())
                .build();
        String courseJsonUrl = "http://i.sjtu.edu.cn/kbcx/xskbcx_cxXsKb.html";
        Request request = new Request.Builder()
                .url(courseJsonUrl)
                .post(requestBody)
                .build();
        Log.i("CourseBlock", request.headers().toString());

        responseHandler.setCallback(callback);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String header = response.header("Content-Type");
                String body = response.body().string();
                Message message = new Message();

                if (!"application/json;charset=UTF-8".equals(header)) {
                    message.what = MSG_TYPE_HEADER;
                } else {
                    message.what = MSG_TYPE_BODY;
                    message.obj = body;
                }
                responseHandler.sendMessage(message);
            }
        });
    }

    public Course findCourseByDayAndStart(int week, int day, int start) {
        for (Course s :
                courseList) {
            if (s.getWeekList().contains(week) && s.getDay() == day && s.getStart() <= start && (s.getStart() + s.getStep() - 1) >= start)
                return s;
        }
        return null;
    }

    public void insertNewCourse(Course s) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", s.getCourseName());
        values.put("room", s.getLocation());
        values.put("teacher", s.getTeacher());
        values.put("start", s.getStart());
        values.put("step", s.getStep());
        values.put("day", s.getDay());
        values.put("weeklist", getStringFromWeekList(s.getWeekList()));
        values.put("note", s.getNote());
        values.put("course_id", s.getCourseId());
        values.put("from_server", s.isFromServer() ? 1 : 0);
        db.insert("course", null, values);
        db.close();
        readFromDatabase();
    }

    public void deleteCourseById(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("course", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        readFromDatabase();
    }

    public void modifyCourse(Course course) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", course.getCourseName());
        values.put("room", course.getLocation());
        values.put("teacher", course.getTeacher());
        values.put("start", course.getStart());
        values.put("step", course.getStep());
        values.put("day", course.getDay());
        values.put("weeklist", getStringFromWeekList(course.getWeekList()));
        values.put("note", course.getNote());
        values.put("course_id", course.getCourseId());
        values.put("from_server", course.isFromServer() ? 1 : 0);
        db.update("course", values, "id = ?", new String[]{String.valueOf(course.getId())});
        db.close();
        readFromDatabase();
    }

    private void parseCourseJson(String json) {
        try {
            courseList.clear();
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArrayKb = jsonObject.getJSONArray("kbList");

            for (int i = 0; i < jsonArrayKb.length(); i++) {
                JSONObject jsonCourse = jsonArrayKb.getJSONObject(i);
                Course course = new Course();
                course.setCourseName(jsonCourse.getString("kcmc"));
                course.setLocation(jsonCourse.getString("cdmc"));
                course.setDay(jsonCourse.getInt("xqj"));
                List<Integer> startAndStep = getStartAndStep(jsonCourse.getString("jcs"));
                course.setStart(startAndStep.get(0));
                course.setStep(startAndStep.get(1));
                course.setTeacher(jsonCourse.getString("xm"));
                course.setWeekList(getWeekList(jsonCourse.getString("zcd")));
                course.setCourseId(jsonCourse.getString("kch_id"));
                course.setNote(jsonCourse.getString("xkbz"));
                course.setFromServer(true);
                courseList.add(course);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getWeekList(String weekString) {
        int isEven = 0;
        if (weekString.contains("(单)")) {
            isEven = 1;
            weekString = weekString.replace("(单)", "");
        }
        if (weekString.contains("(双)")) {
            isEven = 1;
            weekString = weekString.replace("(双)", "");
        }
        String[] weeks = weekString.replace("周", "").split("-");
        List<Integer> weekList = new ArrayList<>();
        for (Integer i = Integer.valueOf(weeks[0]); i <= Integer.valueOf(weeks[1]); i = i + 1 + isEven) {
            weekList.add(i);
        }
        return weekList;
    }

    private List<Integer> getStartAndStep(String jcor) {
        List<Integer> startAndStep = new ArrayList<>();
        String[] jc = jcor.split("-");
        startAndStep.add(Integer.valueOf(jc[0]));
        startAndStep.add(Integer.valueOf(jc[1]) - Integer.valueOf(jc[0]) + 1);
        return startAndStep;
    }

    public enum SEMESTER {
        FIRST("3"), SECOND("12"), SUMMER("16");
        private String value;

        SEMESTER(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public interface ShowInUICallback {
        void onShow(List<Course> courses);

        void onToast(String message);
    }

    public static class WebViewCookieHandler implements CookieJar {
        private CookieManager mCookieManager = CookieManager.getInstance();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            String urlString = url.toString();

            for (Cookie cookie : cookies) {
                mCookieManager.setCookie(urlString, cookie.toString());
            }
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            String urlString = url.toString();
            String cookiesString = mCookieManager.getCookie(urlString);

            if (cookiesString != null && !cookiesString.isEmpty()) {
                String[] cookieHeaders = cookiesString.split(";");
                List<Cookie> cookies = new ArrayList<>(cookieHeaders.length);

                for (String header : cookieHeaders) {
                    cookies.add(Cookie.parse(url, header));
                }
                return cookies;
            }

            return Collections.emptyList();
        }
    }

    private class ResponseHandler extends Handler {
        ShowInUICallback callback;

        ResponseHandler(Looper looper) {
            super(looper);
        }

        void setCallback(ShowInUICallback callback) {
            this.callback = callback;
        }
    }
}
