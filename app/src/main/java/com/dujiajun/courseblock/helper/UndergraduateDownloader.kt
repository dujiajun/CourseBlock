package com.dujiajun.courseblock.helper;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dujiajun.courseblock.model.Course;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;

public class UndergraduateDownloader extends CourseDownloader {
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");


    public UndergraduateDownloader() {
        super();
        this.loginUrl = "https://i.sjtu.edu.cn/jaccountlogin";
        this.courseUrl = "https://i.sjtu.edu.cn/kbcx/xskbcx_cxXsKb.html";
    }

    public static String getWeekCode(String week) {
        String[] items = week.split(",");
        char[] code = new char[Course.MAX_WEEKS];
        Arrays.fill(code, '0');
        for (String item : items) {

            int step = 1;
            if (item.contains("(单)")) {
                step = 2;
                item = item.replace("(单)", "");
            }
            if (item.contains("(双)")) {
                step = 2;
                item = item.replace("(双)", "");
            }
            item = item.replace("周", "");

            String[] weekStartAndEnd;
            if (item.contains("-")) {
                weekStartAndEnd = item.split("-");
            } else {
                weekStartAndEnd = new String[]{item, item};
            }
            for (int i = Integer.parseInt(weekStartAndEnd[0]); i <= Integer.parseInt(weekStartAndEnd[1]); i = i + step) {
                code[i - 1] = '1';
            }
        }
        return new String(code);
    }

    protected static List<Integer> getStartAndStep(String jcor) {
        List<Integer> startAndStep = new ArrayList<>();
        String[] jc = jcor.split("-");
        startAndStep.add(Integer.valueOf(jc[0]));
        startAndStep.add(Integer.parseInt(jc[1]) - Integer.parseInt(jc[0]) + 1);
        return startAndStep;
    }

    private static String convertParams(String term) {
        switch (term) {
            case "2":
                return "12";
            case "3":
                return "16";
            default:
                return "3";
        }
    }

    @Override
    public void getCourses(String year, String term, Handler handler) {
        download(year, term, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Message message = new Message();
                message.what = FAILED;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                int code = response.code();
                String body = response.body().string();
                Log.d("DEBUG", code + "\n" + body);
                Message message = new Message();
                if (body.contains("jAccount")) {
                    message.what = UNLOGIN;
                } else {
                    courses = parseFrom(body);
                    if (courses != null) {
                        message.what = DOWNLOADED;
                        message.obj = courses;
                    } else {
                        message.what = FAILED;
                    }
                }

                handler.sendMessage(message);
            }
        });
    }

    @Override
    protected List<Course> parseFrom(String json) {
        Log.d("DEBUG", json);
        List<Course> courses = new ArrayList<>();
        try {

            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArrayKb = jsonObject.getJSONArray("kbList");

            for (int i = 0; i < jsonArrayKb.length(); i++) {
                JSONObject jsonCourse = jsonArrayKb.getJSONObject(i);
                Course course = new Course();
                course.setCourseId(jsonCourse.getString("kch_id"));
                course.setClassId(jsonCourse.getString("jxbmc"));
                course.setCourseName(jsonCourse.getString("kcmc"));
                course.setLocation(jsonCourse.getString("cdmc"));
                course.setDay(jsonCourse.getInt("xqj"));
                List<Integer> startAndStep = getStartAndStep(jsonCourse.getString("jcs"));
                course.setStart(startAndStep.get(0));
                course.setStep(startAndStep.get(1));
                course.setTeacher(jsonCourse.getString("xm"));
                course.setWeekCode(getWeekCode(jsonCourse.getString("zcd")));
                course.setNote(jsonCourse.getString("xkbz"));
                course.setFromServer(true);
                courses.add(course);
            }
            return courses;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void download(String year, String term, Callback callback) {
        FormBody body = new FormBody.Builder()
                .add("xnm", year)
                .add("xqm", convertParams(term))
                .build();
        Request request = new Request.Builder()
                .url(courseUrl)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
