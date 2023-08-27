package com.zhuangfei.timetable.listener

import android.view.View
import com.zhuangfei.timetable.model.Schedule

/**
 * Item点击的默认实现.
 */
class OnItemClickAdapter : ISchedule.OnItemClickListener {
    override fun onItemClick(v: View, scheduleList: List<Schedule>) {}

    companion object {
        private const val TAG = "OnItemClickAdapter"
    }
}