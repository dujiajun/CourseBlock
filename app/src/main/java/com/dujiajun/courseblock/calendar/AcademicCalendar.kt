package com.dujiajun.courseblock.calendar

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal enum class School {
    UNDERGRADUATE, GRADUATE, MEDICINE, PART_TIME_GRADUATE;

    val isGraduate: Boolean
        get() = this == GRADUATE || this == PART_TIME_GRADUATE
}

internal data class AcademicTerm(val year: Int, val number: Int) : Comparable<AcademicTerm> {
    fun key(school: School) = "${school.name}:$year:$number"

    fun next(school: School) = when {
        number == 1 -> AcademicTerm(year, 2)
        number == 2 && !school.isGraduate -> AcademicTerm(year, 3)
        else -> AcademicTerm(year + 1, 1)
    }

    override fun compareTo(other: AcademicTerm) =
        compareValuesBy(this, other, AcademicTerm::year, AcademicTerm::number)
}

internal class AcademicCalendar(
    val firstMonday: LocalDate,
    val weekCount: Int,
    private val holidays: Set<LocalDate> = emptySet(),
    private val makeupDays: Map<LocalDate, LocalDate> = emptyMap(),
) {
    val lastDay: LocalDate = firstMonday.plusWeeks(weekCount.toLong()).minusDays(1)

    fun weekOf(date: LocalDate): Int? =
        if (date in firstMonday..lastDay) 1 + (ChronoUnit.DAYS.between(firstMonday, date) / 7).toInt()
        else null

    fun openingWeek(today: LocalDate): Int = weekOf(today) ?: 1

    fun date(week: Int, day: Int): LocalDate =
        firstMonday.plusWeeks(week - 1L).plusDays(day - 1L)

    fun classDates(week: Int, day: Int): List<LocalDate> {
        if (week !in 1..weekCount || day !in 1..7) return emptyList()
        val nominal = date(week, day)
        return buildList {
            if (nominal !in holidays && nominal !in makeupDays) add(nominal)
            makeupDays.filterValues { it == nominal }.keys.forEach { add(it) }
        }
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
    }
}

internal object OfficialCalendars {
    fun find(term: AcademicTerm, school: School): AcademicCalendar? = forSchool(school)[term]

    fun suggestedTerm(today: LocalDate, school: School): AcademicTerm {
        val published = forSchool(school).entries
            .filterNot { school.isGraduate && it.key.number == 3 }
            .sortedBy { it.value.firstMonday }
        published.firstOrNull { it.value.weekOf(today) != null }?.let { return it.key }
        val previous = published.lastOrNull { it.value.lastDay < today }
        val next = published.firstOrNull { it.value.firstMonday > today }
        val approximate = approximateTerm(today, school)
        val candidate = previous?.key?.next(school)?.let { maxOf(it, approximate) } ?: approximate
        return next?.key?.takeIf { it <= candidate } ?: candidate
    }

    private fun forSchool(school: School) = when (school) {
        School.MEDICINE -> medicine
        School.UNDERGRADUATE, School.GRADUATE -> university
        School.PART_TIME_GRADUATE -> partTimeGraduate
    }

    // Identity fallback when the school has yet to publish exact dates.
    private fun approximateTerm(today: LocalDate, school: School): AcademicTerm = when (today.monthValue) {
        1 -> AcademicTerm(today.year - 1, 1)
        in 2..6 -> AcademicTerm(today.year - 1, 2)
        7 -> if (school.isGraduate) AcademicTerm(today.year, 1) else AcademicTerm(today.year - 1, 3)
        else -> AcademicTerm(today.year, 1)
    }

    // SJTU teaching calendars: https://jwc.sjtu.edu.cn/jxxl/lnxl.htm
    // 2025 holidays: https://www.sjtu.edu.cn/tg/20250325/208440.html
    // 2026 days off and make-up classes: https://dzb.sjtu.edu.cn/Data/View/6741
    private val university = mapOf(
        AcademicTerm(2025, 1) to calendar(
            "2025-09-15", 18,
            holidays = days("2025-10-01", "2025-10-08") + days("2026-01-01", "2026-01-03"),
            makeupDays = mapOf("2025-09-28" to "2025-10-07", "2025-10-11" to "2025-10-08",
                "2026-01-04" to "2026-01-02"),
        ),
        AcademicTerm(2025, 2) to calendar(
            "2026-03-02", 18,
            holidays = days("2026-04-04", "2026-04-06") + days("2026-05-01", "2026-05-05") +
                days("2026-06-19", "2026-06-21"),
            makeupDays = mapOf("2026-05-09" to "2026-05-04"),
        ),
        AcademicTerm(2025, 3) to calendar("2026-07-06", 4),
        AcademicTerm(2026, 1) to calendar(
            "2026-09-14", 18,
            holidays = days("2026-09-25", "2026-09-27") + days("2026-10-01", "2026-10-07") +
                setOf(LocalDate.parse("2027-01-01")),
            makeupDays = mapOf("2026-09-20" to "2026-10-02", "2026-10-10" to "2026-10-06"),
        ),
    )

    // Graduate school adjustments: https://www.gs.sjtu.edu.cn/post/detail/Z3MyOTk0
    // and the graduate school's 2026 holiday notice (issued 2025-12-08).
    private val partTimeGraduate = mapOf(
        AcademicTerm(2025, 1) to calendar(
            "2025-09-15", 18,
            holidays = setOf(LocalDate.parse("2025-10-01"), LocalDate.parse("2025-10-06"),
                LocalDate.parse("2025-09-28"), LocalDate.parse("2025-10-11"),
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-04")),
            makeupDays = mapOf("2025-10-07" to "2025-09-28", "2025-10-08" to "2025-10-11",
                "2026-01-02" to "2026-01-04"),
        ),
        AcademicTerm(2025, 2) to calendar(
            "2026-03-02", 18,
            holidays = setOf(LocalDate.parse("2026-04-05"), LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-05-09"), LocalDate.parse("2026-06-19")),
            makeupDays = mapOf("2026-04-06" to "2026-04-05", "2026-05-04" to "2026-05-09"),
        ),
        AcademicTerm(2026, 1) to calendar(
            "2026-09-14", 18,
            holidays = setOf(LocalDate.parse("2026-09-25"), LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-09-20"), LocalDate.parse("2026-10-10"), LocalDate.parse("2027-01-01")),
            makeupDays = mapOf("2026-10-02" to "2026-09-20", "2026-10-06" to "2026-10-10"),
        ),
    )

    // School of Medicine academic calendars: https://www.shsmu.edu.cn/jwc/xsfw/xnxl.htm
    private val medicine = mapOf(
        AcademicTerm(2025, 1) to calendar("2025-09-15", 19),
        AcademicTerm(2025, 2) to calendar("2026-03-02", 19),
        AcademicTerm(2025, 3) to calendar("2026-07-13", 2),
        AcademicTerm(2026, 1) to calendar("2026-09-07", 19),
        AcademicTerm(2026, 2) to calendar("2027-02-22", 19),
        AcademicTerm(2026, 3) to calendar("2027-07-05", 2),
    )

    private fun calendar(
        firstMonday: String,
        weeks: Int,
        holidays: Set<LocalDate> = emptySet(),
        makeupDays: Map<String, String> = emptyMap(),
    ) = AcademicCalendar(
        LocalDate.parse(firstMonday), weeks, holidays,
        makeupDays.mapKeys { LocalDate.parse(it.key) }.mapValues { LocalDate.parse(it.value) },
    )

    private fun days(first: String, last: String): Set<LocalDate> {
        val start = LocalDate.parse(first)
        return (0..ChronoUnit.DAYS.between(start, LocalDate.parse(last)))
            .mapTo(mutableSetOf()) { start.plusDays(it) }
    }
}
