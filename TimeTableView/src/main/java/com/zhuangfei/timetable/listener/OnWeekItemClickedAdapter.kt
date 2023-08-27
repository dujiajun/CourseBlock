package com.zhuangfei.timetable.listener

import com.zhuangfei.timetable.listener.IWeekView.OnWeekItemClickedListener

/**
 * WeekView的Item点击监听默认实现
 */
class OnWeekItemClickedAdapter : OnWeekItemClickedListener {
    override fun onWeekClicked(week: Int) {}
}