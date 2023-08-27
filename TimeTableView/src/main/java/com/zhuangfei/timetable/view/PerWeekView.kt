package com.zhuangfei.timetable.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.zhuangfei.android_timetableview.sample.R
import com.zhuangfei.timetable.model.Schedule
import com.zhuangfei.timetable.model.ScheduleEnable
import com.zhuangfei.timetable.model.ScheduleSupport
import com.zhuangfei.timetable.utils.ScreenUtils.dip2px

/**
 * 周次选择栏的每项自定义View,表示某周的有课情况.
 * 使用周一至周五、第1-10节的数据进行绘制,绘制的结果是一个5x5的点阵：
 *
 *
 * 5列分别表示周一至周五
 * 5行分别表示1-2节、3-4节、5-6节、7-8节、9-10节的有课情况
 *
 *
 * 有课的地方用亮色的圆点，没课的地方用暗色的圆点
 */
class PerWeekView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    View(context, attrs) {
    //控件宽度
    private var width = 0

    //圆点半径
    private var radius = 0

    /**
     * 获取亮色的画笔
     *
     * @return
     */
    //亮色画笔
    val lightPaint: Paint

    /**
     * 获取暗色的画笔
     *
     * @return
     */
    //暗色画笔
    val grayPaint: Paint

    //亮色
    private var lightColor = 0

    //暗色
    private var grayColor = 0

    //数据源
    private var dataSource: MutableList<Schedule>  = ArrayList()

    init {
        initAttr(attrs)
        lightPaint = Paint()
        lightPaint.color = lightColor
        lightPaint.isAntiAlias = true
        lightPaint.style = Paint.Style.FILL
        grayPaint = Paint()
        grayPaint.color = grayColor
        grayPaint.isAntiAlias = true
        grayPaint.style = Paint.Style.FILL
    }

    /**
     * 设置亮色
     *
     * @param lightColor 亮色
     * @return
     */
    fun setLightColor(lightColor: Int): PerWeekView {
        this.lightColor = lightColor
        invalidate()
        return this
    }

    /**
     * 设置暗色
     *
     * @param grayColor 暗色
     * @return
     */
    fun setGrayColor(grayColor: Int): PerWeekView {
        this.grayColor = grayColor
        invalidate()
        return this
    }

    /**
     * 设置数据源
     *
     * @param list
     * @param curWeek
     * @return
     */
    fun setSource(list: List<ScheduleEnable?>?, curWeek: Int): PerWeekView {
        if (list == null) return this
        setData(ScheduleSupport.transform(list), curWeek)
        return this
    }

    /**
     * 设置数据源
     *
     * @param list
     * @param curWeek
     * @return
     */
    fun setData(list: List<Schedule>?, curWeek: Int): PerWeekView {
        if (list == null) return this
        dataSource.clear()
        for (i in list.indices) {
            val schedule = list[i]
            if (ScheduleSupport.isThisWeek(
                    schedule,
                    curWeek
                ) && schedule.start <= 10 && schedule.day <= 5
            ) {
                dataSource.add(schedule)
            }
        }
        invalidate()
        return this
    }

    /**
     * 设置半径Px
     *
     * @param radiusPx 半径
     * @return
     */
    fun setRadiusInPx(radiusPx: Int): PerWeekView {
        radius = radiusPx
        return this
    }

    /**
     * 设置半径Dp
     *
     * @param radiusDp 半径
     * @return
     */
    fun setRadiusInDp(radiusDp: Int): PerWeekView {
        setRadiusInPx(dip2px(context, radiusDp.toFloat()))
        return this
    }

    private fun initAttr(attrs: AttributeSet?) {
        val defRadius = dip2px(context, 2f)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PerWeekView)
        grayColor = ta.getColor(R.styleable.PerWeekView_gray_color, Color.rgb(207, 219, 219))
        lightColor = ta.getColor(R.styleable.PerWeekView_light_color, Color.parseColor("#3FCAB8"))
        radius = ta.getDimension(R.styleable.PerWeekView_radius, defRadius.toFloat()).toInt()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mar = (width - 10 * radius) / 6
        lightPaint.color = lightColor
        grayPaint.color = grayColor
        val tmp = array

        //绘制点
        for (i in 0..4) {
            for (j in 0..4) {
                if (tmp[i][j] == 0) {
                    drawPoint(
                        canvas,
                        mar + radius + (mar + 2 * radius) * j,
                        mar + radius + (mar + 2 * radius) * i,
                        radius,
                        grayPaint
                    )
                } else {
                    drawPoint(
                        canvas,
                        mar + radius + (mar + 2 * radius) * j,
                        mar + radius + (mar + 2 * radius) * i,
                        radius,
                        lightPaint
                    )
                }
            }
        }
    }

    val array: Array<IntArray>
        /**
         * 根据此数据源分析出一个二维数组.
         *
         * @return
         */
        get() {
            val arr = Array(10) { IntArray(5) }
            val tmp = Array(5) { IntArray(5) }

            // 初始化数组
            for (i in 0..9) {
                for (j in 0..4) {
                    arr[i][j] = 0
                }
            }

            // 标记上课的位置
            // 遍历课程集合，将在某课程的上课区间的位置都标记上
            var start: Int
            var end: Int
            for (i in dataSource.indices) {
                val schedule = dataSource[i]
                start = schedule.start
                end = schedule.start + schedule.step - 1
                if (end > 10) end = 10

                //标记区间的所有位置
                for (m in start..end) {
                    arr[m - 1][schedule.day - 1] = 1
                }
            }

            // 合并分组标记
            // 用到了10小节的数据来标记
            // 10小节被分为了5组分别来表示5行的上课状态
            // 每个分组中只要有一个有课，那么该组对外的状态应该为有课
            var t = 0
            var i = 0
            while (i < 10) {
                for (j in 0..4) {
                    if (arr[i][j] == 0 && arr[i + 1][j] == 0) {
                        tmp[t][j] = 0
                    } else {
                        tmp[t][j] = 1
                    }
                }
                t++
                i += 2
            }
            return tmp
        }

    /**
     * 画点
     *
     * @param canvas
     * @param x      圆心x
     * @param y      圆心y
     * @param radius 半径
     * @param p      画笔
     */
    fun drawPoint(canvas: Canvas, x: Int, y: Int, radius: Int, p: Paint) {
        canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), p)
    }

    companion object {
        private const val TAG = "PerWeekView"
    }
}