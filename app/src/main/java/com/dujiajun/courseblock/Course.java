package com.dujiajun.courseblock;

import com.zhuangfei.timetable.model.Schedule;
import com.zhuangfei.timetable.model.ScheduleEnable;

import java.io.Serializable;
import java.util.List;

public class Course implements ScheduleEnable, Serializable {
    private String courseId;
    private String courseName;
    private String teacher;
    private String note;
    private String location;
    private int id;
    private int day;
    private int start;
    private int step;
    private List<Integer> weekList;
    private boolean isFromServer;

    public Course() {
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public List<Integer> getWeekList() {
        return weekList;
    }

    public void setWeekList(List<Integer> weekList) {
        this.weekList = weekList;
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
        s.setWeekList(weekList);
        return s;
    }

    public boolean isFromServer() {
        return isFromServer;
    }

    public void setFromServer(boolean fromServer) {
        isFromServer = fromServer;
    }
}
