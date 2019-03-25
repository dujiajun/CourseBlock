package com.dujiajun.courseblock;

import android.content.Context;
import android.os.AsyncTask;
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

    private CourseManager(Context context) {
        mContext = context;
        scheduleList = new ArrayList<>();
    }

    public static CourseManager getInstance(Context context) {
        if (singleton == null) {
            singleton = new CourseManager(context);
        }
        return singleton;
    }

    public List<Schedule> getScheduleList() {
        return scheduleList;
    }

    public void writeToDatabase() {

    }

    public void readFromDatabase() {

    }

    public void downloadFromNetwork() {

    }

    private OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new WebViewCookieHandler())
            .build();

    public enum SEMESTER {
        FIRST("3"), SECOND("12"), SUMMER("16");
        private String name;

        SEMESTER(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private void getCourseTable(String year, SEMESTER semester, Callback callback) {
        RequestBody requestBody = new FormBody.Builder()
                .add("xnm", year)
                .add("xqm", semester.getName())
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

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                RequestBody requestBody = new FormBody.Builder()
                        .add("xnm", year)
                        .add("xqm", semester.getName())
                        .build();
                String courseJsonUrl = "http://i.sjtu.edu.cn/kbcx/xskbcx_cxXsKb.html";
                Request request = new Request.Builder()
                        .url(courseJsonUrl)
                        .post(requestBody)
                        .build();
                Log.i("CourseBlock", request.headers().toString());
                Response response = null;
                try {
                    response = client.newCall(request).execute();

                    if (!"application/json;charset=UTF-8".equals(response.header("Content-Type"))) {
                        Toast.makeText(mContext, "请先登录！", Toast.LENGTH_SHORT).show();
                        return null;
                    }
                    if (response.body() != null) {
                        String resp = null;
                        resp = response.body().string();
                        parseCourseJson(resp);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                callback.onShow(scheduleList);
            }
        }.execute();

    }

    public interface ShowInUICallback {
        void onShow(List<Schedule> schedules);
    }

    private void parseCourseJson(String json) {
        try {
            scheduleList.clear();
            JSONObject jsonObject = new JSONObject(json);
            JSONObject jsonObjectJmcMap = jsonObject.getJSONObject("xqjmcMap");
            JSONArray jsonArrayKb = jsonObject.getJSONArray("kbList");
            for (int i = 0; i < jsonArrayKb.length(); i++) {
                JSONObject jsonCourse = jsonArrayKb.getJSONObject(i);
                JSubject subject = new JSubject();
                subject.setName(jsonCourse.getString("kcmc"));
                subject.setRoom(jsonCourse.getString("cdmc"));
                subject.setDay(jsonCourse.getInt("xqj"));
                List<Integer> startAndStep = getStartAndStep(jsonCourse.getString("jcor"));
                subject.setStart(startAndStep.get(0));
                subject.setStep(startAndStep.get(1));
                subject.setTeacher(jsonCourse.getString("xm"));
                subject.setWeekList(getWeekList(jsonCourse.getString("zcd")));
                scheduleList.add(subject.getSchedule());
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
