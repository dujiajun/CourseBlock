package com.dujiajun.courseblock.model;

import com.zhuangfei.timetable.model.Schedule;
import com.zhuangfei.timetable.model.ScheduleEnable;

import org.litepal.crud.LitePalSupport;

import java.util.ArrayList;
import java.util.List;

public class Course extends LitePalSupport implements ScheduleEnable {
    public static final int MAX_STEPS = 14;
    public static final String[] CLASS_TIMES = new String[]{
            "8:00\n8:45", "8:55\n9:40", "10:00\n10:45", "10:55\n11:40",
            "12:00\n12:45", "12:55\n13:40", "14:00\n14:55", "14:55\n15:40",
            "16:00\n16:45", "16:55\n17:40", "18:00\n18:45", "18:55\n19:40",
            "20:00\n20:45", "20:55\n21:40"
    };
    public static final int MAX_WEEKS = 22;
    private int id;
    private String courseId;
    private String courseName;
    private String classId;
    private String teacher;
    private String note;
    private String location;
    private int day = 1;
    private int start = 13;
    private int step = 1;
    private String weekCode;
    private boolean isFromServer;

    public int getId() {
        return id;
    }

    public String getWeekCode() {
        return weekCode;
    }

    public void setWeekCode(String weekCode) {
        this.weekCode = weekCode;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public boolean isFromServer() {
        return isFromServer;
    }

    public void setFromServer(boolean fromServer) {
        isFromServer = fromServer;
    }

    @Override
    public Schedule getSchedule() {
        Schedule s = new Schedule();
        s.setName(courseName);
        s.setRoom(location);
        s.setDay(day);
        s.setStart(start);
        s.setStep(step);
        s.setTeacher(teacher);
        List<Integer> weekList = new ArrayList<>();
        for (int j = 0; j < MAX_WEEKS; j++) {
            if (weekCode.charAt(j) == '1')
                weekList.add(j + 1);
        }
        s.setWeekList(weekList);
        return s;
    }

}
