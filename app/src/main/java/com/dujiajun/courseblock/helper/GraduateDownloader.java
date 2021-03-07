package com.dujiajun.courseblock.helper;

import android.os.Handler;
import android.os.Message;

import com.dujiajun.courseblock.model.Course;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class GraduateDownloader extends CourseDownloader {


    public GraduateDownloader() {
        super();
        this.loginUrl = "http://yjs.sjtu.edu.cn/gsapp/sys/wdkbapp/*default/index.do";
        this.courseUrl = "http://yjs.sjtu.edu.cn/gsapp/sys/wdkbapp/modules/xskcb/xspkjgcx.do";
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
                Message message = new Message();
                if (code == 500) {
                    message.what = FAILED;
                } else if (code == 403 || code == 302) {
                    message.what = UNLOGIN;
                } else {
                    courses = parseFrom(body);
                    message.what = DOWNLOADED;
                    message.obj = courses;
                }

                handler.sendMessage(message);
            }
        });
    }

    @Override
    protected List<Course> parseFrom(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json).getJSONObject("datas").getJSONObject("xspkjgcx");
            JSONArray courseRows = jsonObject.getJSONArray("rows");
            HashMap<String, Course> map = new HashMap<>();
            for (int i = 0; i < courseRows.length(); i++) {
                JSONObject courseRow = courseRows.getJSONObject(i);
                String classId = courseRow.getString("BJMC");
                Course course = map.get(classId);
                if (course == null) {
                    course = new Course();
                    course.setCourseId(courseRow.getString("KCDM"));
                    course.setClassId(classId);
                    course.setCourseName(courseRow.getString("KCMC"));
                    course.setTeacher(courseRow.getString("JSXM"));
                    course.setDay(courseRow.getInt("XQ"));
                    course.setNote(courseRow.optString("KBBZ", ""));
                    course.setLocation(courseRow.getString("JASMC"));
                    course.setWeekCode(courseRow.getString("ZCBH").substring(0, Course.MAX_WEEKS));
                    course.setFromServer(true);
                    map.put(classId, course);
                }
                int classTime = courseRow.getInt("KSJCDM");
                course.setStart(Math.min(classTime, course.getStart()));
                course.setStep(Math.max(course.getStep(), classTime - course.getStart() + 1));
            }
            return new ArrayList<>(map.values());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String convertParams(String year, String term) {
        if (term.equals("2")) {
            return String.format(Locale.CHINA, "%d02", Integer.parseInt(year) + 1);
        } else {
            return String.format(Locale.CHINA, "%s09", year);
        }

    }

    @Override
    protected void download(String year, String term, Callback callback) {
        FormBody body = new FormBody.Builder()
                .add("XNXQDM", convertParams(year, term))
                .add("XH", "")
                .build();
        Request request = new Request.Builder()
                .url(courseUrl)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }


}
