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

    override val schedule: Schedule
        get() {
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
        const val MAX_WEEKS = 22
    }
}