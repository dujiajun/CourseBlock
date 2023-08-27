package com.zhuangfei.timetable.model

import android.content.Context
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

/**
 * 课程表的工具包，主要提供几个便捷的方法
 */
object ScheduleSupport {
    private const val TAG = "ScheduleSupport"
    //*****************
    // 日期相关方法
    // getDateStringFromWeek()
    // getWeekDate()
    //*****************
    /**
     * 根据需要算的周数和当前周数计算日期,
     * 用于周次切换时对日期的更新
     *
     * @param targetWeek 需要算的周数
     * @param curWeek    当前周数
     * @return 当周日期集合，共8个元素，第一个为月份（高亮日期的月份），之后7个为周一至周日的日期
     */
    @JvmStatic
    fun getDateStringFromWeek(curWeek: Int, targetWeek: Int): List<String> {
        val calendar = Calendar.getInstance()
        if (targetWeek == curWeek) return getDateStringFromCalendar(calendar)
        val amount = targetWeek - curWeek
        calendar.add(Calendar.WEEK_OF_YEAR, amount)
        return getDateStringFromCalendar(calendar)
    }

    /**
     * 根据周一的时间获取当周的日期
     *
     * @param calendar 周一的日期
     * @return 当周日期数组
     */
    private fun getDateStringFromCalendar(calendar: Calendar): List<String> {
        val dateList: MutableList<String> = ArrayList()
        while (calendar[Calendar.DAY_OF_WEEK] != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        calendar.firstDayOfWeek = Calendar.MONDAY
        dateList.add((calendar[Calendar.MONTH] + 1).toString() + "")
        for (i in 0..6) {
            dateList.add(calendar[Calendar.DAY_OF_MONTH].toString() + "")
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dateList
    }

    @JvmStatic
    val weekDate: List<String>
        /**
         * 获取本周的周一-周日的所有日期
         *
         * @return 8个元素的集合，第一个为月份，之后7个依次为周一-周日
         */
        get() {
            val calendar1 = Calendar.getInstance()
            calendar1.firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = calendar1[Calendar.DAY_OF_WEEK] // 获得当前日期是一个星期的第几天
            if (1 == dayOfWeek) {
                calendar1.add(Calendar.DAY_OF_MONTH, -1)
            }
            val day = calendar1[Calendar.DAY_OF_WEEK]
            calendar1.add(Calendar.DATE, 0)
            calendar1.add(Calendar.DATE, calendar1.firstDayOfWeek - day)
            val beginDate = calendar1.time
            calendar1.add(Calendar.DATE, 6)
            val endDate = calendar1.time
            return getBetweenDates(beginDate, endDate)
        }

    /**
     * 获取两个日期之间的日期集合
     *
     * @param start
     * @param end
     * @return
     */
    private fun getBetweenDates(start: Date, end: Date): List<String> {
        val sdf = SimpleDateFormat("dd", Locale.CHINA)
        val sdf2 = SimpleDateFormat("MM", Locale.CHINA)
        val result: MutableList<String> = ArrayList()
        result.add(sdf2.format(Date()))
        result.add(sdf.format(start))
        val tempStart = Calendar.getInstance()
        tempStart.time = start
        tempStart.add(Calendar.DAY_OF_YEAR, 1)
        val tempEnd = Calendar.getInstance()
        tempEnd.time = end
        while (tempStart.before(tempEnd)) {
            result.add(sdf.format(tempStart.time))
            tempStart.add(Calendar.DAY_OF_YEAR, 1)
        }
        result.add(sdf.format(end))
        return result
    }

    /**
     * 根据开学时间计算当前周
     *
     * @param startTime 满足"yyyy-MM-dd HH:mm:ss"模式的字符串
     * @return
     */
    @JvmStatic
    fun timeTransform(startTime: String?): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return try {
            val start = sdf.parse(startTime).time
            val end = Date().time
            val seconds = (end - start) / 1000
            val day = seconds / (24 * 3600)
            (floor((day / 7).toDouble()) + 1).toInt()
        } catch (e: ParseException) {
            -1
        }
    }
    //*****************
    // 课程工具方法
    //*****************
    /**
     * 模拟分配颜色，将源数据的colorRandom属性赋值，
     * 然后根据该属性值在颜色池中查找颜色即可
     *
     * @param schedules 源数据
     * @return colorRandom属性已有值
     */
    @JvmStatic
    fun getColorReflect(schedules: List<Schedule>): List<Schedule>? {
        if (schedules.isEmpty()) return null

        //保存课程名、颜色的对应关系
        val colorMap: MutableMap<String, Int> = HashMap()
        var colorCount = 1

        //开始转换
        for (i in schedules.indices) {
            val mySubject = schedules[i]
            //计算课程颜色
            var color: Int
            if (colorMap.containsKey(mySubject.name)) {
                color = colorMap[mySubject.name]!!
            } else {
                colorMap[mySubject.name] = colorCount
                color = colorCount
                colorCount++
            }
            mySubject.colorRandom = color
        }
        return schedules
    }

    /**
     * 内部使用的是:context.getResources().getDimensionPixelSize(dp);
     *
     * @param context
     * @param dp
     * @return
     */
    fun getPx(context: Context, dp: Int): Int {
        return context.resources.getDimensionPixelSize(dp)
    }

    /**
     * 转换，将自定义类型转换为List<Schedule>
     *
     * @param dataSource 源数据集合
     * @return
    </Schedule> */
    @JvmStatic
    fun transform(dataSource: List<ScheduleEnable>): List<Schedule> {
        val data: MutableList<Schedule> = ArrayList()
        for (i in dataSource.indices) {
            data.add(dataSource[i].schedule)
        }
        return data
    }

    /**
     * 将源数据拆分为数组的七个元素，每个元素为一个集合，
     * 依次为周一-周日的课程集合
     *
     * @param dataSource 源数据
     * @return
     */
    fun splitSubjectWithDay(dataSource: List<Schedule>): Array<MutableList<Schedule>> {
        val data: Array<MutableList<Schedule>> = Array(7) { ArrayList() }
        for (i in data.indices) {
            data[i] = ArrayList()
        }
        for (i in dataSource.indices) {
            val bean = dataSource[i]
            if (bean.day != -1) data[bean.day - 1].add(bean)
        }
        sortList(data)
        return data
    }

    /**
     * 获取某天有课的课程
     *
     * @param scheduleList 数据集合
     * @param curWeek      当前周，以1开始
     * @param day          星期几，0：周一，1：周二，依次类推..周日：6
     * @return
     */
    @JvmStatic
    fun getHaveSubjectsWithDay(
        scheduleList: List<Schedule>,
        curWeek: Int,
        day: Int
    ): List<Schedule> {
        val subjectBeanList = getAllSubjectsWithDay(scheduleList, day)
        val result: MutableList<Schedule> = ArrayList()
        for (bean in subjectBeanList) {
            if (isThisWeek(bean, curWeek)) {
                result.add(bean)
            }
        }
        return result
    }

    /**
     * 获取某天的所有课程
     *
     * @param scheduleList 数据集合
     * @param day          星期几，0：周一，1：周二，依次类推..周日：6
     * @return
     */
    @JvmStatic
    fun getAllSubjectsWithDay(
        scheduleList: List<Schedule>,
        day: Int
    ): List<Schedule> {
        return splitSubjectWithDay(scheduleList)[day]
    }
    //****************
    // 课程查找
    //****************
    /**
     * 在data中查找与subject的start相同的课程集合
     *
     * @param subject
     * @param data
     * @return
     */
    @JvmStatic
    fun findSubjects(subject: Schedule, data: List<Schedule>): List<Schedule> {
        val result: MutableList<Schedule> = ArrayList()
        for (i in data.indices) {
            val bean = data[i]
            if (bean.start >= subject.start && bean.start < subject.start + subject.step) result.add(
                data[i]
            )
        }
        return result
    }

    /**
     * 按照上课节次排序
     *
     * @param data
     */
    @JvmStatic
    fun sortList(data: Array<MutableList<Schedule>>) {
        for (i in data.indices) sortList(data[i])
    }

    fun sortList(data: MutableList<Schedule>) {
        var min: Int
        var tmp: Schedule
        for (m in 0 until data.size - 1) {
            min = m
            for (k in m + 1 until data.size) {
                if (data[min].start > data[k].start) {
                    min = k
                }
            }
            tmp = data[m]
            data[m] = data[min]
            data[min] = tmp
        }
    }

    /**
     * 判断该课是否为本周的
     *
     * @param curWeek
     * @return
     */
    @JvmStatic
    fun isThisWeek(subject: Schedule, curWeek: Int): Boolean {
        val weekList = subject.weekList
        return weekList.contains(curWeek)
    }

    /**
     * 根据当前周过滤课程，获取本周有效的课程（忽略重叠的）
     *
     * @param data
     * @param curWeek
     * @return
     */
    @JvmStatic
    fun filterSchedule(
        data: List<Schedule>,
        curWeek: Int,
        isShowNotCurWeek: Boolean
    ): List<Schedule> {
        var data = data
        val result: MutableSet<Schedule> = HashSet()
        if (!isShowNotCurWeek) {
            val filter: MutableList<Schedule> = ArrayList()
            for (i in data.indices) {
                val s = data[i]
                if (isThisWeek(s, curWeek)) filter.add(s)
            }
            data = filter
        }
        if (data.isNotEmpty()) {
            result.add(data[0])
        }
        for (i in 1 until data.size) {
            val s = data[i]
            var `is` = true
            for (j in 0 until i) {
                val s2 = data[j]
                if (s.start >= s2.start && s.start <= s2.start + s2.step - 1) {
                    `is` = false
                    if (isThisWeek(s2, curWeek)) {
                        break
                    } else if (isThisWeek(s, curWeek)) {
                        result.remove(s2)
                        result.add(s)
                    }
                }
            }
            if (`is`) result.add(s)
        }
        val list: MutableList<Schedule> = ArrayList(result)
        sortList(list)
        return list
    }
}