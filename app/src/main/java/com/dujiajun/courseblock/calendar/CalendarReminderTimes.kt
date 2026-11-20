package com.dujiajun.courseblock.calendar

internal object CalendarReminderTimes {
    const val AT_START = 0
    const val MAX_COUNT = 5
    val customRange = 5..60
    val presets = listOf(AT_START, 5, 10, 15, 30, 60)
    val default = setOf(15)

    fun isAllowed(minutes: Int) = minutes == AT_START || minutes in customRange

    fun parse(value: String?): Set<Int> {
        if (value == "") return emptySet()
        val times = value?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull()?.takeIf(::isAllowed) }
            ?.distinct()
            ?.sorted()
            ?.take(MAX_COUNT)
        return times?.takeIf { it.isNotEmpty() }?.toSet() ?: default
    }

    fun encode(times: Set<Int>) = times.sorted().joinToString(",")

    private const val EVENT_SUFFIX = "?reminders="

    fun eventMarker(termMarker: String, times: Set<Int>) = termMarker + EVENT_SUFFIX + encode(times)

    fun termFromEvent(marker: String) = marker.substringBefore(EVENT_SUFFIX)

    fun eventPattern(termMarker: String) = "$termMarker[?]reminders=*"

    fun appliedToEvent(marker: String): Set<Int>? {
        val encoded = marker.substringAfter(EVENT_SUFFIX, missingDelimiterValue = "-")
        val times = parse(encoded)
        return times.takeIf { encode(it) == encoded }
    }
}
