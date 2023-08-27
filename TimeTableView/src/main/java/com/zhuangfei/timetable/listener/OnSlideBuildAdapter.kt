package com.zhuangfei.timetable.listener

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.zhuangfei.android_timetableview.sample.R
import com.zhuangfei.timetable.listener.ISchedule.OnSlideBuildListener
import com.zhuangfei.timetable.utils.ColorUtils.alphaColor

/**
 * 控件实现的一个可以显示时间的侧边栏适配器
 * Created by Liu ZhuangFei on 2018/6/18.
 */
open class OnSlideBuildAdapter : OnSlideBuildListener {
    /**
     * 获取时刻数组
     *
     * @return
     */
    //时刻，每个元素保存每节课的开始时间
    var times: Array<String>? = null
        protected set

    //节次文本的颜色、字号
    private var textColor = Color.BLACK
    private var textSize = 14f

    //时刻文本的颜色、字号
    private var timeTextSize = 12f
    private var timeTextColor = Color.GRAY

    //侧边栏背景色
    private var background = Color.WHITE
    private var alpha = 1f
    fun setBackground(backgroundColor: Int): OnSlideBuildAdapter {
        background = backgroundColor
        return this
    }

    /**
     * 设置时刻数组
     *
     * @param times
     * @return
     */
    fun setTimes(times: Array<String>?): OnSlideBuildAdapter {
        this.times = times
        return this
    }

    /**
     * 设置节次文本颜色
     *
     * @param textColor 指定颜色
     * @return
     */
    fun setTextColor(textColor: Int): OnSlideBuildAdapter {
        this.textColor = textColor
        return this
    }

    /**
     * 设置节次文本的大小
     *
     * @param textSize 指定字号
     * @return
     */
    fun setTextSize(textSize: Float): OnSlideBuildAdapter {
        this.textSize = textSize
        return this
    }

    /**
     * 设置节次时间的文本颜色
     *
     * @param timeTextColor 颜色
     * @return
     */
    fun setTimeTextColor(timeTextColor: Int): OnSlideBuildAdapter {
        this.timeTextColor = timeTextColor
        return this
    }

    /**
     * 设置节次时间的文本大小
     *
     * @param timeTextSize 字号
     * @return
     */
    fun setTimeTextSize(timeTextSize: Float): OnSlideBuildAdapter {
        this.timeTextSize = timeTextSize
        return this
    }

    override fun getView(pos: Int, inflater: LayoutInflater, itemHeight: Int, marTop: Int): View {
        val view = inflater.inflate(R.layout.item_slide_time, null, false)
        val numberTextView = view.findViewById<TextView>(R.id.item_slide_number)
        val timeTextView = view.findViewById<TextView>(R.id.item_slide_time)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                itemHeight)
        lp.setMargins(0, marTop, 0, 0)
        view.layoutParams = lp
        numberTextView.text = (pos + 1).toString() + ""
        numberTextView.textSize = textSize
        numberTextView.setTextColor(textColor)
        if (times == null) timeTextView.text = ""
        if (times != null && pos >= 0 && pos < times!!.size) {
            timeTextView.text = times!![pos]
            timeTextView.setTextColor(timeTextColor)
            timeTextView.textSize = timeTextSize
        }
        return view
    }

    override fun onInit(layout: LinearLayout, alpha: Float) {
        this.alpha = alpha
        val alphaColor = alphaColor(background, alpha)
        layout.setBackgroundColor(alphaColor)
    }
}