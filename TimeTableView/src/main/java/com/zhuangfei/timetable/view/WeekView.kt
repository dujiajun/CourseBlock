package com.zhuangfei.timetable.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.zhuangfei.android_timetableview.sample.R
import com.zhuangfei.timetable.listener.IWeekView.OnWeekItemClickedListener
import com.zhuangfei.timetable.listener.IWeekView.OnWeekLeftClickedListener
import com.zhuangfei.timetable.listener.OnWeekItemClickedAdapter
import com.zhuangfei.timetable.listener.OnWeekLeftClickedAdapter
import com.zhuangfei.timetable.model.Schedule
import com.zhuangfei.timetable.model.ScheduleEnable
import com.zhuangfei.timetable.model.ScheduleSupport
import com.zhuangfei.timetable.model.WeekViewEnable

/**
 * 周次选择栏自定义View.
 * 每一项均为PerWeekView<br></br>
 */
class WeekView constructor(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs), WeekViewEnable<WeekView?> {
    var mInflate: LayoutInflater

    //周次的容器
    lateinit var container: LinearLayout

    //跟布局
    lateinit var root: LinearLayout

    //左侧按钮
    lateinit var leftlayout: LinearLayout

    //数据
    private var dataSource: List<Schedule> = ArrayList()

    //布局保存
    private var layouts: MutableList<LinearLayout> = ArrayList()
    private var textViews: MutableList<TextView> = ArrayList()

    //当前周
    private var curWeek = 1
    private var preIndex = 1

    //多少项
    private var itemCount = 20
    private var onWeekItemClickedListener: OnWeekItemClickedListener = OnWeekItemClickedAdapter()
    private var onWeekLeftClickedListener: OnWeekLeftClickedListener = OnWeekLeftClickedAdapter()

    init {
        mInflate = LayoutInflater.from(context)
        initView()
    }

    /**
     * 获取Item点击监听
     *
     * @return
     */
    fun onWeekItemClickedListener(): OnWeekItemClickedListener {
        return onWeekItemClickedListener
    }

    /**
     * 设置Item点击监听
     *
     * @param onWeekItemClickedListener
     * @return
     */
    fun callback(onWeekItemClickedListener: OnWeekItemClickedListener): WeekView {
        this.onWeekItemClickedListener = onWeekItemClickedListener
        return this
    }

    /**
     * 获取左侧按钮点击监听
     *
     * @return
     */
    fun onWeekLeftClickedListener(): OnWeekLeftClickedListener {
        return onWeekLeftClickedListener
    }

    /**
     * 设置左侧按钮点击监听
     *
     * @param onWeekLeftClickedListener
     * @return
     */
    fun callback(onWeekLeftClickedListener: OnWeekLeftClickedListener): WeekView {
        this.onWeekLeftClickedListener = onWeekLeftClickedListener
        return this
    }

    /**
     * 设置当前周
     *
     * @param curWeek
     * @return
     */
    override fun curWeek(curWeek: Int): WeekView {
        var curWeek = curWeek
        if (curWeek < 1) curWeek = 1
        this.curWeek = curWeek
        return this
    }

    /**
     * 设置项数
     *
     * @param count
     * @return
     */
    override fun itemCount(count: Int): WeekView {
        if (count <= 0) return this
        itemCount = count
        return this
    }

    override fun itemCount(): Int {
        return itemCount
    }

    /**
     * 设置数据源
     *
     * @param list
     * @return
     */
    override fun source(list: List<ScheduleEnable>): WeekView {
        data(ScheduleSupport.transform(list))
        return this
    }

    /**
     * 设置数据源
     *
     * @param scheduleList
     * @return
     */
    override fun data(scheduleList: List<Schedule>): WeekView {
        dataSource = scheduleList
        return this
    }

    /**
     * 获取数据源
     *
     * @return
     */
    override fun dataSource(): List<Schedule> {
        return dataSource
    }

    private fun initView() {
        mInflate.inflate(R.layout.view_weekview, this)
        container = findViewById(R.id.id_weekview_container)
        root = findViewById(R.id.id_root)
        leftlayout = findViewById(R.id.id_weekview_leftlayout)
    }

    /**
     * 初次构建时调用，显示周次选择布局
     */
    override fun showView(): WeekView {
        if (curWeek < 1) curWeek(1)
        if (curWeek > itemCount()) curWeek = itemCount
        container.removeAllViews()
        layouts = ArrayList()
        textViews = ArrayList()
        leftlayout.setOnClickListener { onWeekLeftClickedListener().onWeekLeftClicked() }
        for (i in 1..itemCount) {
            val view = mInflate.inflate(R.layout.item_weekview, null)
            val perLayout = view.findViewById<LinearLayout>(R.id.id_perweekview_layout)
            val weekText = view.findViewById<TextView>(R.id.id_weektext)
            val bottomText = view.findViewById<TextView>(R.id.id_weektext_bottom)
            weekText.text = "第" + i + "周"
            if (i == curWeek) bottomText.text = "(本周)"
            val perWeekView = view.findViewById<PerWeekView>(R.id.id_perweekview)
            perWeekView.setData(dataSource(), i)
            perLayout.setOnClickListener {
                resetBackground()
                preIndex = i
                perLayout.background = context.resources.getDrawable(
                    R.drawable.weekview_white,
                    null
                )
                onWeekItemClickedListener().onWeekClicked(i)
            }
            layouts.add(perLayout)
            textViews.add(bottomText)
            container.addView(view)
        }
        if (curWeek > 0 && curWeek <= (layouts as ArrayList<LinearLayout>).size) {
            layouts[curWeek - 1].background =
                context.resources.getDrawable(R.drawable.weekview_thisweek, null)
        }
        return this
    }

    /**
     * 当前周被改变后可以调用该方式修正一下底部的文本
     *
     * @return
     */
    override fun updateView(): WeekView {
        if (layouts.size == 0) return this
        if (textViews.size == 0) return this
        for (i in layouts.indices) {
            if (curWeek - 1 == i) {
                textViews[i].text = "(本周)"
            } else {
                textViews[i].text = ""
            }
            layouts[i].setBackgroundColor(
                context.resources.getColor(
                    R.color.app_course_chooseweek_bg,
                    null
                )
            )
        }
        if (curWeek > 0 && curWeek <= layouts.size) {
            layouts[curWeek - 1].background = context.resources.getDrawable(
                R.drawable.weekview_thisweek,
                null
            )
        }
        return this
    }

    /**
     * 重置背景色
     */
    fun resetBackground() {
        layouts[preIndex - 1].setBackgroundColor(
            context.resources.getColor(
                R.color.app_course_chooseweek_bg,
                null
            )
        )
        layouts[curWeek - 1].background =
            context.resources.getDrawable(R.drawable.weekview_thisweek, null)
    }

    /**
     * 隐藏左侧设置当前周的控件
     */
    fun hideLeftLayout(): WeekView {
        leftlayout.visibility = GONE
        return this
    }

    /**
     * 设置控件的可见性
     *
     * @param isShow true:显示，false:隐藏
     */
    override fun isShow(isShow: Boolean): WeekView {
        if (isShow) {
            root.visibility = VISIBLE
        } else {
            root.visibility = GONE
        }
        return this
    }

    /**
     * 判断该控件是否显示
     *
     * @return
     */
    override fun isShowing(): Boolean {
        return root.visibility != GONE
    }

    companion object {
        private const val TAG = "WeekView"
    }
}