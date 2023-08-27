package com.zhuangfei.timetable.model

/**
 * 如果需要自定义周次选择栏，请实现该接口,
 * 它仅仅提供一个规范，可用可不用
 * Created by Liu ZhuangFei on 2018/7/28.
 */
interface WeekViewEnable<T> {
    /**
     * 设置当前周
     *
     * @param curWeek
     * @return
     */
    fun curWeek(curWeek: Int): T

    /**
     * 设置项数
     *
     * @param count
     * @return
     */
    fun itemCount(count: Int): T

    /**
     * 获取项数
     *
     * @return
     */
    fun itemCount(): Int

    /**
     * 设置数据源
     *
     * @param list
     * @return
     */
    fun source(list: List<ScheduleEnable>): T

    /**
     * 设置数据源
     *
     * @param scheduleList
     * @return
     */
    fun data(scheduleList: List<Schedule>): T

    /**
     * 获取数据源
     *
     * @return
     */
    fun dataSource(): List<Schedule>

    /**
     * 初次构建时调用，显示周次选择布局
     */
    fun showView(): T

    /**
     * 当前周被改变后可以调用该方式修正一下底部的文本
     *
     * @return
     */
    fun updateView(): T

    /**
     * 设置控件的可见性
     *
     * @param isShow true:显示，false:隐藏
     */
    fun isShow(isShow: Boolean): T

    /**
     * 判断该控件是否显示
     *
     * @return
     */
    val isShowing: Boolean
}