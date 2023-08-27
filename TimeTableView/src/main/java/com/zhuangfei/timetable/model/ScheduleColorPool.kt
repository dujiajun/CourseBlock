package com.zhuangfei.timetable.model

import android.content.Context
import android.graphics.Color
import com.zhuangfei.android_timetableview.sample.R
import com.zhuangfei.timetable.utils.ColorUtils.alphaColor

/**
 * 颜色池，管理课程项可挑选的颜色
 */
class ScheduleColorPool(var context: Context) {
    /**
     * 获取非本周课程颜色
     *
     * @return
     */
    //课程不在本周时的背景色
    private var uselessColor = 0
    private var colorMap: Map<String, Int> = HashMap()

    /**
     * 获取渲染时是否忽略非本周颜色
     *
     * @return boolean
     */
    //false：非本周课程使用uselessColor渲染
    //true：非本周课程使用colorMap渲染
    var isIgnoreUselessColor = false
        private set

    //使用集合维护颜色池
    private var colorPool: MutableList<Int> = ArrayList()

    init {
        setUselessColor(context.resources.getColor(R.color.useless, null))
        colorMap = HashMap()
        reset()
    }

    /**
     * 获取颜色的映射Map
     *
     * @return Map<String></String>, Integer>
     */
    fun getColorMap(): Map<String, Int> {
        return colorMap
    }

    /**
     * 设置colorMap
     *
     * @param colorMap Map<String></String>, Integer>
     * @return ScheduleColorPool
     */
    fun setColorMap(colorMap: Map<String, Int>): ScheduleColorPool {
        this.colorMap = colorMap
        return this
    }

    /**
     * 设置渲染时是否忽略非本周颜色
     * false：非本周课程使用uselessColor渲染
     * true：非本周课程使用colorMap渲染
     *
     * @return ScheduleColorPool
     */
    fun setIgnoreUselessColor(ignoreUselessColor: Boolean): ScheduleColorPool {
        isIgnoreUselessColor = ignoreUselessColor
        return this
    }

    /**
     * 设置非本周课程颜色
     *
     * @param uselessColor 非本周课程的颜色
     * @return ScheduleColorPool
     */
    fun setUselessColor(uselessColor: Int): ScheduleColorPool {
        this.uselessColor = uselessColor
        return this
    }

    /**
     * 获取非本周课程颜色
     *
     * @return int
     */
    fun getUselessColorWithAlpha(alpha: Float): Int {
        return alphaColor(uselessColor, alpha)
    }

    private val poolInstance: MutableList<Int>
        /**
         * 得到颜色池的实例，即List集合
         *
         * @return List<Integer>
        </Integer> */
        get() {
            return colorPool
        }

    /**
     * 从颜色池中取指定透明度的颜色
     *
     * @param random
     * @param alpha
     * @return int
     */
    fun getColorAutoWithAlpha(random: Int, alpha: Float): Int {
        return if (random < 0) getColorAuto(-random) else alphaColor(getColor(random % size()), alpha)
    }

    /**
     * 根据索引获取颜色，索引越界默认返回 Color.GRAY
     *
     * @param i 索引
     * @return int
     */
    fun getColor(i: Int): Int {
        return if (i < 0 || i >= size()) Color.GRAY else colorPool[i]
    }

    /**
     * 使用模运算根据索引从颜色池中获取颜色,
     * 如果i<0，转换为正数,
     * 否则：重新计算索引j=i mod size
     *
     * @param i 索引
     * @return int颜色
     */
    fun getColorAuto(i: Int): Int {
        return if (i < 0) getColorAuto(-i) else getColor(i % size())
    }

    /**
     * 将指定集合中的颜色加入到颜色池中
     *
     * @param ownColorPool 集合
     * @return ScheduleColorPool
     */
    fun addAll(ownColorPool: Collection<Int>?): ScheduleColorPool {
        poolInstance.addAll(ownColorPool!!)
        return this
    }

    /**
     * 颜色池的大小
     *
     * @return int
     */
    fun size(): Int {
        return poolInstance.size
    }

    /**
     * 清空颜色池，清空默认颜色
     *
     * @return ScheduleColorPool
     */
    fun clear(): ScheduleColorPool {
        poolInstance.clear()
        return this
    }

    /**
     * 在颜色池中添加一些自定义的颜色
     *
     * @param colorIds 多个颜色
     * @return ScheduleColorPool
     */
    fun add(vararg colorIds: Int): ScheduleColorPool {
        for (colorId in colorIds) {
            colorPool.add(colorId)
        }
        return this
    }

    /**
     * 重置，先在池子里添加一些默认的课程项颜色
     *
     * @return ScheduleColorPool
     */
    fun reset(): ScheduleColorPool {
        val colors = intArrayOf(
                R.color.color_1, R.color.color_2, R.color.color_3, R.color.color_4,
                R.color.color_5, R.color.color_6, R.color.color_7, R.color.color_8,
                R.color.color_9, R.color.color_10, R.color.color_11, R.color.color_31,
                R.color.color_32, R.color.color_33, R.color.color_34, R.color.color_35
        )
        clear()
        for (color in colors) {
            add(context.resources.getColor(color, null))
        }
        return this
    }
}