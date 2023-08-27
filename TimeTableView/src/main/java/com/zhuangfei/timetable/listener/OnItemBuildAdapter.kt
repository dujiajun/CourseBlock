package com.zhuangfei.timetable.listener

import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.widget.FrameLayout
import android.widget.TextView
import com.zhuangfei.timetable.listener.ISchedule.OnItemBuildListener
import com.zhuangfei.timetable.model.Schedule

/**
 * Item构建监听器的默认实现.
 */
class OnItemBuildAdapter : OnItemBuildListener {
    override fun getItemText(schedule: Schedule, isThisWeek: Boolean): String {
        if (TextUtils.isEmpty(schedule.name)) return "未命名"
        if (schedule.room == null) {
            return if (!isThisWeek) "[非本周]" + schedule.name else schedule.name
        }
        var r = schedule.name + "@" + schedule.room
        if (!isThisWeek) {
            r = "[非本周]$r"
        }
        return r
    }

    override fun onItemUpdate(layout: FrameLayout, textView: TextView, countTextView: TextView, schedule: Schedule, gd: GradientDrawable) {}
}