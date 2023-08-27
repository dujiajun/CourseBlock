package com.zhuangfei.timetable.operator

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.zhuangfei.timetable.TimetableView

/**
 * 抽象的业务逻辑
 * Created by Liu ZhuangFei on 2018/9/2.
 */
abstract class AbsOperator {
    open fun init(context: Context?, attrs: AttributeSet?, view: TimetableView?) {}
    open fun showView() {}
    open fun updateDateView() {}
    open fun updateSlideView() {}
    open fun changeWeek(week: Int, isCurWeek: Boolean) {}
    open val flagLayout: LinearLayout?
        get() = null
    open val dateLayout: LinearLayout?
        get() = null

    open fun setWeekendsVisible(isShow: Boolean) {}
}