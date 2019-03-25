package com.dujiajun.courseblock;

import com.zhuangfei.timetable.model.Schedule;
import com.zhuangfei.timetable.model.ScheduleEnable;

import java.util.List;

public class JSubject implements ScheduleEnable {

    private int id = 0;
    //private String courseId;
    private String time;
    private String name;
    private String room;
    private String teacher;
    private int start;
    private int step;
    private int day;
    private List<Integer> weekList;
    private String term;
    private int colorRandom = 0;

    public JSubject() {
    }

    public JSubject(String time, String name, String room, String teacher, int start, int step, int day, List<Integer> weekList, String term, int colorRandom) {
        this.time = time;
        this.name = name;
        this.room = room;
        this.teacher = teacher;
        this.start = start;
        this.step = step;
        this.day = day;
        this.weekList = weekList;
        this.term = term;
        this.colorRandom = colorRandom;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
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

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public List<Integer> getWeekList() {
        return weekList;
    }

    public void setWeekList(List<Integer> weekList) {
        this.weekList = weekList;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getColorRandom() {
        return colorRandom;
    }

    public void setColorRandom(int colorRandom) {
        this.colorRandom = colorRandom;
    }

    @Override
    public Schedule getSchedule() {
        Schedule schedule = new Schedule();
        schedule.setDay(getDay());
        schedule.setName(getName());
        schedule.setRoom(getRoom());
        schedule.setStart(getStart());
        schedule.setStep(getStep());
        schedule.setTeacher(getTeacher());
        schedule.setWeekList(getWeekList());
        schedule.setColorRandom(2);
        //schedule.putExtras(EXTRAS_ID,getId());
        //schedule.putExtras(EXTRAS_AD_URL,getUrl());
        return schedule;
    }
}
