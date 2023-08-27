package com.zhuangfei.timetable.listener

import com.zhuangfei.timetable.TimetableView
import com.zhuangfei.timetable.listener.ISchedule.OnConfigHandleListener

/**
 * Created by Liu ZhuangFei on 2018/12/21.
 */
class OnConfigHandleAdapter : OnConfigHandleListener {
    override fun onParseConfig(key: String, value: String, mView: TimetableView) {}
}