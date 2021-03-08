package com.dujiajun.courseblock.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeekManager {
    private static final String FIRST_DATE = "2021-02-22 00:00:00";
    private static WeekManager singleton;
    private final SharedPreferences preferences;
    public SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private int curWeek;
    private Date firstDate;

    private WeekManager(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static WeekManager getInstance(Context context) {
        if (singleton == null) {
            singleton = new WeekManager(context);
        }
        return singleton;
    }

    public void updateCurWeek() {
        loadFirstDay();
        long diff = new Date().getTime() - firstDate.getTime();
        curWeek = (int) (diff / 1000 / 60 / 60 / 24 / 7 + 1);
        if (curWeek < 1) curWeek = 1;
    }

    public void setFirstDay(int year, int month, int day_of_month) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.set(year, month, day_of_month, 0, 0, 0);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == 1) dayOfWeek = 8;
        calendar.add(Calendar.DATE, 2 - dayOfWeek);
        firstDate = calendar.getTime();
        saveFirstDay();
    }

    private void saveFirstDay() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("first_monday", getShowDate());
        editor.apply();
    }

    private void loadFirstDay() {
        try {
            String s = preferences.getString("first_monday", FIRST_DATE);
            firstDate = simpleDateFormat.parse(s);
        } catch (ParseException ignored) {

        }
    }

    public Date getFirstDate() {
        return firstDate;
    }

    public String getShowDate() {
        return simpleDateFormat.format(firstDate);
    }

    public int getCurWeek() {
        updateCurWeek();
        return curWeek;
    }
}
