package com.zhuangfei.timetable.listener

/**
 * WeekView的相关接口.
 */
interface IWeekView {
    /**
     * WeekView的Item点击监听器
     */
    interface OnWeekItemClickedListener {
        /**
         * 当Item被点击时回调
         * @param week 选择的周次
         */
        fun onWeekClicked(week: Int)
    }

    /**
     * WeekView的左侧（设置当前周）的点击监听器
     */
    interface OnWeekLeftClickedListener {
        /**
         * 当"设置当前周"按钮被点击时回调
         */
        fun onWeekLeftClicked()
    }
}