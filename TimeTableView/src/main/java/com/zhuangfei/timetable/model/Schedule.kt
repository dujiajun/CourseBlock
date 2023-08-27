package com.zhuangfei.timetable.model

import java.io.Serializable

/**
 * 课程实体类<br></br>
 * 1.增加了extras,可以保存一些自己需要的东西<br></br>
 *
 * @author Administrator 刘壮飞
 */
class Schedule : Serializable, Comparable<Schedule> {
    /**
     * 课程名
     */
    @JvmField
    var name = ""

    /**
     * 教室
     */
    var room = ""

    /**
     * 教师
     */
    var teacher = ""

    /**
     * 第几周至第几周上
     */
    @JvmField
    var weekList: List<Int> = ArrayList()

    /**
     * 开始上课的节次
     */
    @JvmField
    var start = 0

    /**
     * 上课节数
     */
    @JvmField
    var step = 0

    /**
     * 周几上
     */
    @JvmField
    var day = 0

    /**
     * 一个随机数，用于对应课程的颜色
     */
    @JvmField
    var colorRandom = 0

    /**
     * 额外信息
     */
    var extras: MutableMap<String, Any> = HashMap()

    constructor(
        name: String, room: String, teacher: String,
        weekList: List<Int>, start: Int, step: Int, day: Int,
        colorRandom: Int
    ) : super() {
        this.name = name
        this.room = room
        this.teacher = teacher
        this.weekList = weekList
        this.start = start
        this.step = step
        this.day = day
        this.colorRandom = colorRandom
    }

    constructor() : super()

    fun putExtras(key: String, `val`: Any) {
        extras[key] = `val`
    }

    override fun compareTo(other: Schedule): Int {
        return if (start < other.start) {
            -1
        } else if (start == other.start) {
            0
        } else {
            1
        }
    }
}