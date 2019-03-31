package com.dujiajun.courseblock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.Toast;

import com.zhuangfei.timetable.model.Schedule;

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

public class CourseManager {

    private Context mContext;
    private static CourseManager singleton;
    private List<Schedule> scheduleList;
    private CourseDBHelper dbHelper;

    private CourseManager(Context context) {
        mContext = context;
        scheduleList = new ArrayList<>();
        dbHelper = new CourseDBHelper(context);
    }

    public static CourseManager getInstance(Context context) {
        if (singleton == null) {
            singleton = new CourseManager(context.getApplicationContext());
        }
        return singleton;
    }

    public List<Schedule> getScheduleList() {
        return scheduleList;
    }

    public void writeToDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("delete from course");
        for (Schedule s :
                scheduleList) {
            ContentValues values = new ContentValues();
            values.put("name", s.getName());
            values.put("room", s.getRoom());
            values.put("teacher", s.getTeacher());
            values.put("start", s.getStart());
            values.put("step", s.getStep());
            values.put("day", s.getDay());
            values.put("weeklist", getStringFromWeekList(s.getWeekList()));
            db.insert("course", null, values);
        }
    }

    public void readFromDatabase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from course", null);
        scheduleList.clear();
        if (cursor.moveToFirst()) {
            do {
                Schedule schedule = new Schedule();
                schedule.setName(cursor.getString(cursor.getColumnIndex("name")));
                schedule.setRoom(cursor.getString(cursor.getColumnIndex("room")));
                schedule.setTeacher(cursor.getString(cursor.getColumnIndex("teacher")));
                schedule.setStart(cursor.getInt(cursor.getColumnIndex("start")));
                schedule.setStep(cursor.getInt(cursor.getColumnIndex("step")));
                schedule.setDay(cursor.getInt(cursor.getColumnIndex("day")));
                schedule.setWeekList(getWeekListFromString(cursor.getString(cursor.getColumnIndex("weeklist"))));
                scheduleList.add(schedule);
            } while (cursor.moveToNext());
        }
        cursor.close();
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

    private OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new WebViewCookieHandler())
            .build();

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

    public static SEMESTER getSemesterFromValue(String value) {
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

    private void getCourseTable(String year, SEMESTER semester, Callback callback) {
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
        client.newCall(request).enqueue(callback);
    }

    public void updateCourseDatabase(String year, SEMESTER semester, ShowInUICallback callback) {

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

        handler.setCallback(callback);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Message message = new Message();
                message.obj = response;
                message.what = 0;

                handler.sendMessage(message);
            }
        });


    }

    private class MyHandler extends Handler {
        ShowInUICallback callback;

        public MyHandler(Looper looper) {
            super(looper);
        }

        public void setCallback(ShowInUICallback callback) {
            this.callback = callback;
        }
    }

    private MyHandler handler = new MyHandler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Response response = (Response) msg.obj;

            try {
                if (!"application/json;charset=UTF-8".equals(response.header("Content-Type"))) {
                    Toast.makeText(mContext, R.string.please_log_in, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (response.body() != null) {
                    String resp;
                    resp = response.body().string();
                    parseCourseJson(resp);
                    writeToDatabase();
                    if (callback != null)
                        callback.onShow(scheduleList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public interface ShowInUICallback {
        void onShow(List<Schedule> schedules);
    }

    private void parseCourseJson(String json) {
        try {
            scheduleList.clear();
            JSONObject jsonObject = new JSONObject(json);
            //JSONObject jsonObjectJmcMap = jsonObject.getJSONObject("xqjmcMap");
            JSONArray jsonArrayKb = jsonObject.getJSONArray("kbList");

            for (int i = 0; i < jsonArrayKb.length(); i++) {
                JSONObject jsonCourse = jsonArrayKb.getJSONObject(i);
                Schedule schedule = new Schedule();
                schedule.setName(jsonCourse.getString("kcmc"));
                schedule.setRoom(jsonCourse.getString("cdmc"));
                schedule.setDay(jsonCourse.getInt("xqj"));
                List<Integer> startAndStep = getStartAndStep(jsonCourse.getString("jcor"));
                schedule.setStart(startAndStep.get(0));
                schedule.setStep(startAndStep.get(1));
                schedule.setTeacher(jsonCourse.getString("xm"));
                schedule.setWeekList(getWeekList(jsonCourse.getString("zcd")));
                scheduleList.add(schedule);
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
}
