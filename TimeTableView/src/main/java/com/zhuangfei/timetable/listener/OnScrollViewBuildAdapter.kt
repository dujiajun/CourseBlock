package com.zhuangfei.timetable.listener

import android.view.LayoutInflater
import android.view.View
import com.zhuangfei.android_timetableview.sample.R
import com.zhuangfei.timetable.listener.ISchedule.OnScrollViewBuildListener

/**
 * 滚动布局构建监听的默认实现
 */
class OnScrollViewBuildAdapter : OnScrollViewBuildListener {
    override fun getScrollView(mInflate: LayoutInflater): View {
        return mInflate.inflate(R.layout.view_simplescrollview, null, false)
    }
}