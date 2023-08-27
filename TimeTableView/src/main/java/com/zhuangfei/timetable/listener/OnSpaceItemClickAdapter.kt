package com.zhuangfei.timetable.listener

import android.widget.FrameLayout
import android.widget.LinearLayout
import com.zhuangfei.timetable.listener.ISchedule.OnSpaceItemClickListener

/**
 * Created by Liu ZhuangFei on 2018/8/3.
 */
open class OnSpaceItemClickAdapter : OnSpaceItemClickListener {
    private var flagLayout: LinearLayout? = null
    private var itemHeight = 0
    private var itemWidth = 0
    private var monthWidth = 0
    private var marTop = 0
    private var marLeft = 0
    override fun onSpaceItemClick(day: Int, start: Int) {
        //day:从0开始，start：从1开始
        if (flagLayout == null) return
        //itemWidth：是包含了边距的，所以需要减去
        val lp = FrameLayout.LayoutParams(itemWidth - marLeft * 2, itemHeight)
        lp.setMargins(monthWidth + day * itemWidth + marLeft, (start - 1) * (itemHeight + marTop) + marTop, 0, 0)
        flagLayout!!.layoutParams = lp
    }

    override fun onInit(flagLayout: LinearLayout, monthWidth: Int, itemWidth: Int, itemHeight: Int, marTop: Int, marLeft: Int) {
        this.flagLayout = flagLayout
        this.itemHeight = itemHeight
        this.itemWidth = itemWidth
        this.monthWidth = monthWidth
        this.marTop = marTop
        this.marLeft = marLeft
    }

    companion object {
        private const val TAG = "OnSpaceItemClickAdapter"
    }
}