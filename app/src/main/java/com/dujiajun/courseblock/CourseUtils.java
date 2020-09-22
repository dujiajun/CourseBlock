package com.dujiajun.courseblock;

import java.util.ArrayList;
import java.util.List;

class CourseUtils {

    public static String getStringFromWeekList(List<Integer> weekList) {
        StringBuilder builder = new StringBuilder();
        for (Integer i :
                weekList) {
            builder.append(i);
            builder.append(',');
        }
        return builder.toString();
    }

    public static List<Integer> getWeekListFromDBString(String s) {
        String[] arr = s.split(",");
        List<Integer> weekList = new ArrayList<>();
        for (String week :
                arr) {
            weekList.add(Integer.valueOf(week));
        }
        return weekList;
    }

    public static List<Integer> getWeekListFromOnlineString(String weekString) {
        String[] items = weekString.split(",");
        List<Integer> weekList = new ArrayList<>();
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

            String[] weeks;
            if (item.contains("-")) {
                weeks = item.split("-");
            } else {
                weeks = new String[]{item, item};
            }
            for (int i = Integer.parseInt(weeks[0]); i <= Integer.parseInt(weeks[1]); i = i + step) {
                weekList.add(i);
            }
        }
        return weekList;
    }

    public static List<Integer> getStartAndStep(String jcor) {
        List<Integer> startAndStep = new ArrayList<>();
        String[] jc = jcor.split("-");
        startAndStep.add(Integer.valueOf(jc[0]));
        startAndStep.add(Integer.parseInt(jc[1]) - Integer.parseInt(jc[0]) + 1);
        return startAndStep;
    }
}
