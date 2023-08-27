package com.dujiajun.courseblock.model

import com.zhuangfei.timetable.model.Schedule
import com.zhuangfei.timetable.model.ScheduleEnable
import org.litepal.crud.LitePalSupport

class Course : LitePalSupport(), ScheduleEnable {

    val id = 0

    var courseId: String = ""

    var courseName: String = ""

    var classId: String = ""

    var teacher: String = ""

    var note: String = ""

    var location: String = ""

    var day = 1

    var start = 13

    var step = 1

    var weekCode: String = ""

    var isFromServer = false
    override fun getSchedule(): Schedule {
        val s = Schedule()
        s.name = courseName
        s.room = location
        s.day = day
        s.start = start
        s.step = step
        s.teacher = teacher
        val weekList: MutableList<Int> = ArrayList()
        for (j in 0 until MAX_WEEKS) {
            if (weekCode[j] == '1') weekList.add(j + 1)
        }
        s.weekList = weekList
        return s
    }

    companion object {
        const val MAX_STEPS = 14
        @JvmField
        val START_TIMES = arrayOf(
                "8:00", "8:55", "10:00", "10:55",
                "12:00", "12:55", "14:00", "14:55",
                "16:00", "16:55", "18:00", "18:55",
                "20:00", "20:55"
        )
        @JvmField
        val END_TIMES = arrayOf(
                "8:45", "9:40", "10:45", "11:40",
                "12:45", "13:40", "14:45", "15:40",
                "16:45", "17:40", "18:45", "19:40",
                "20:45", "21:40"
        )
        const val MAX_WEEKS = 22
    }
}