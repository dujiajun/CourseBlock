package com.dujiajun.courseblock;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ParseCourseUnitTest {
    @Test
    public void case_one() {
        List<Integer> weeks = CourseUtils.getWeekListFromOnlineString("3周,7周,11周,15周");
        int[] actual_weeks = new int[]{3, 7, 11, 15};
        for (int i = 0; i < actual_weeks.length; i++) {
            assertEquals(weeks.get(i).intValue(), actual_weeks[i]);
        }
    }

    @Test
    public void case_two() {
        List<Integer> weeks = CourseUtils.getWeekListFromOnlineString("1-3周(单),4-16周");
        int[] actual_weeks = new int[]{1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < actual_weeks.length; i++) {
            assertEquals(weeks.get(i).intValue(), actual_weeks[i]);
        }
    }

    @Test
    public void case_three() {
        List<Integer> weeks = CourseUtils.getWeekListFromOnlineString("2周");
        int[] actual_weeks = new int[]{2};
        for (int i = 0; i < actual_weeks.length; i++) {
            assertEquals(weeks.get(i).intValue(), actual_weeks[i]);
        }
    }
}