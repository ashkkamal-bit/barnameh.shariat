package com.example.util

import java.util.Calendar
import java.util.TimeZone

data class JalaliDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..31
    val dayOfWeekPersianIndex: Int = 0 // 0=Saturday .. 6=Friday
) {
    val dateKey: String
        get() = "%04d-%02d-%02d".format(year, month, day)

    val monthName: String
        get() = PersianCalendarUtil.monthName(month)

    val weekDayName: String
        get() = PersianCalendarUtil.weekDayName(dayOfWeekPersianIndex)

    val weekDayShort: String
        get() = PersianCalendarUtil.weekDayShort(dayOfWeekPersianIndex)

    fun formattedFull(usePersianDigits: Boolean = true): String {
        val dayStr = PersianCalendarUtil.formatDigits(day.toString(), usePersianDigits)
        val yearStr = PersianCalendarUtil.formatDigits(year.toString(), usePersianDigits)
        return "$weekDayName $dayStr $monthName $yearStr"
    }

    fun formattedDayAndMonth(usePersianDigits: Boolean = true): String {
        val dayStr = PersianCalendarUtil.formatDigits(day.toString(), usePersianDigits)
        return "$dayStr $monthName"
    }

    fun toIsoGregorian(): String = PersianCalendarUtil.jalaliToIsoGregorian(dateKey)
}

object PersianCalendarUtil {

    private val persianMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val persianWeekDays = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    private val persianWeekDaysShort = listOf(
        "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    fun monthName(month: Int): String =
        persianMonths.getOrElse((month - 1).coerceIn(0, 11)) { "فروردین" }

    fun monthIndexByName(name: String): Int? {
        val normalized = name.trim()
        val idx = persianMonths.indexOfFirst { it == normalized }
        return if (idx >= 0) idx + 1 else null
    }

    fun weekDayName(persianDayIndex: Int): String =
        persianWeekDays.getOrElse(persianDayIndex.coerceIn(0, 6)) { "شنبه" }

    fun weekDayShort(persianDayIndex: Int): String =
        persianWeekDaysShort.getOrElse(persianDayIndex.coerceIn(0, 6)) { "ش" }

    fun allWeekDayShorts(): List<String> = persianWeekDaysShort

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(('۰'.code + (ch.code - '0'.code)).toChar())
                in '٠'..'٩' -> sb.append(('۰'.code + (ch.code - '٠'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatDigits(input: String, usePersianDigits: Boolean): String =
        if (usePersianDigits) toPersianDigits(input) else toEnglishDigits(input)

    fun formatNumber(number: Int, usePersianDigits: Boolean = true): String =
        formatDigits(number.toString(), usePersianDigits)

    fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())

    fun formatTime(hour: Int?, minute: Int?, usePersianDigits: Boolean = true): String {
        if (hour == null || minute == null) return "بدون ساعت"
        val raw = "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        return formatDigits(raw, usePersianDigits)
    }

    fun formatDurationMinutes(minutes: Int, usePersianDigits: Boolean = true): String {
        val clamped = minutes.coerceAtLeast(1)
        return if (clamped >= 60 && clamped % 60 == 0) {
            "${formatNumber(clamped / 60, usePersianDigits)} ساعت"
        } else {
            "${formatNumber(clamped, usePersianDigits)} دقیقه"
        }
    }

    fun today(timeZone: TimeZone = TimeZone.getDefault()): JalaliDate =
        fromCalendar(Calendar.getInstance(timeZone))

    fun offsetFromToday(daysOffset: Int, timeZone: TimeZone = TimeZone.getDefault()): JalaliDate {
        val cal = Calendar.getInstance(timeZone)
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return fromCalendar(cal)
    }

    fun addDays(date: JalaliDate, daysOffset: Int): JalaliDate {
        val cal = jalaliToCalendar(date.year, date.month, date.day)
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return fromCalendar(cal)
    }

    fun weekDatesStartingSaturday(anchor: JalaliDate): List<JalaliDate> {
        val startOffset = -anchor.dayOfWeekPersianIndex
        return (0..6).map { offset -> addDays(anchor, startOffset + offset) }
    }

    fun fromCalendar(cal: Calendar): JalaliDate {
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val persianDow = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
        return gregorianToJalali(gy, gm, gd, persianDow)
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int, persianDow: Int? = null): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        val computedDow = persianDow ?: run {
            val cal = Calendar.getInstance().apply {
                set(gy, gm - 1, gd, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> 0
                Calendar.SUNDAY -> 1
                Calendar.MONDAY -> 2
                Calendar.TUESDAY -> 3
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 5
                Calendar.FRIDAY -> 6
                else -> 0
            }
        }
        return JalaliDate(jy, jm, jd, computedDow)
    }

    fun jalaliToCalendar(
        jy: Int,
        jm: Int,
        jd: Int,
        hour: Int = 12,
        minute: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Calendar {
        var year = jy + 1595
        var days = -355668 + (365 * year) + ((year / 33) * 8) + (((year % 33) + 3) / 4) + jd +
            if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val isLeap = ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))
        val salA = intArrayOf(0, 31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return Calendar.getInstance(timeZone).apply {
            set(gy, gm - 1, gd, hour.coerceIn(0, 23), minute.coerceIn(0, 59), 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun jalaliToIsoGregorian(jalaliDateKey: String): String {
        val date = parseDateKey(jalaliDateKey)
        val cal = jalaliToCalendar(date.year, date.month, date.day)
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(gy, gm, gd)
    }

    fun isoGregorianToJalali(isoDate: String): JalaliDate? {
        val parts = toEnglishDigits(isoDate.trim()).split("-")
        if (parts.size != 3) return null
        val gy = parts[0].toIntOrNull() ?: return null
        val gm = parts[1].toIntOrNull() ?: return null
        val gd = parts[2].toIntOrNull() ?: return null
        if (gy !in 1900..2200 || gm !in 1..12 || gd !in 1..31) return null
        val cal = Calendar.getInstance().apply {
            isLenient = false
            set(gy, gm - 1, gd, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return try {
            cal.timeInMillis // validates calendar date strictly
            fromCalendar(cal)
        } catch (_: Exception) {
            null
        }
    }

    fun isValidJalaliDateKey(key: String): Boolean {
        val parts = toEnglishDigits(key.trim()).split("-")
        if (parts.size != 3) return false
        val y = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        val d = parts[2].toIntOrNull() ?: return false
        if (y !in 1300..1500 || m !in 1..12) return false
        return d in 1..daysInJalaliMonth(y, m)
    }

    fun jalaliToTimestampMillis(
        dateKey: String,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val date = parseDateKey(dateKey)
        return jalaliToCalendar(date.year, date.month, date.day, hour, minute, timeZone).timeInMillis
    }

    fun isJalaliLeapYear(jy: Int): Boolean {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
            1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        val bl = breaks.size
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            jp = jm
        }
        var n = jy - jp
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return leap == 0
    }

    fun daysInJalaliMonth(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isJalaliLeapYear(jy) -> 30
        else -> 29
    }

    fun firstDayOfWeekInMonth(jy: Int, jm: Int): Int {
        val cal = jalaliToCalendar(jy, jm, 1)
        return fromCalendar(cal).dayOfWeekPersianIndex
    }

    fun dateForJalali(jy: Int, jm: Int, jd: Int): JalaliDate {
        val safeMonth = jm.coerceIn(1, 12)
        val maxDay = daysInJalaliMonth(jy, safeMonth)
        val cal = jalaliToCalendar(jy, safeMonth, jd.coerceIn(1, maxDay))
        return fromCalendar(cal)
    }

    fun parseDateKey(key: String): JalaliDate {
        val parts = toEnglishDigits(key.trim()).split("-")
        if (parts.size != 3) return today()
        val y = parts[0].toIntOrNull() ?: return today()
        val m = parts[1].toIntOrNull() ?: return today()
        val d = parts[2].toIntOrNull() ?: return today()
        return dateForJalali(y, m, d)
    }

    fun daysBetween(fromDateKey: String, toDateKey: String): Int {
        val from = parseDateKey(fromDateKey)
        val to = parseDateKey(toDateKey)
        val c1 = jalaliToCalendar(from.year, from.month, from.day, 12, 0)
        val c2 = jalaliToCalendar(to.year, to.month, to.day, 12, 0)
        val diffMillis = c2.timeInMillis - c1.timeInMillis
        return (diffMillis / (24L * 60L * 60L * 1000L)).toInt()
    }

    fun isOverdue(
        dateKey: String,
        timeHour: Int?,
        timeMinute: Int?,
        nowCal: Calendar = Calendar.getInstance()
    ): Boolean {
        val todayDate = fromCalendar(nowCal)
        if (dateKey < todayDate.dateKey) return true
        if (dateKey > todayDate.dateKey) return false
        if (timeHour == null || timeMinute == null) return false
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        return (timeHour * 60 + timeMinute) < nowMinutes
    }

    fun minutesUntil(
        dateKey: String,
        timeHour: Int?,
        timeMinute: Int?,
        nowCal: Calendar = Calendar.getInstance()
    ): Long? {
        if (timeHour == null || timeMinute == null) return null
        val targetMillis = jalaliToTimestampMillis(dateKey, timeHour, timeMinute, nowCal.timeZone)
        val diff = targetMillis - nowCal.timeInMillis
        return diff / 60_000L
    }

    fun formatRemainingPersian(minutes: Long, usePersianDigits: Boolean = true): String {
        if (minutes <= 0L) return "اکنون"
        val hours = minutes / 60L
        val mins = minutes % 60L
        return when {
            hours >= 24L -> {
                val days = hours / 24L
                "${formatDigits(days.toString(), usePersianDigits)} روز مانده"
            }
            hours > 0L && mins > 0L ->
                "${formatDigits(hours.toString(), usePersianDigits)} ساعت و ${formatDigits(mins.toString(), usePersianDigits)} دقیقه"
            hours > 0L ->
                "${formatDigits(hours.toString(), usePersianDigits)} ساعت دیگر"
            else ->
                "${formatDigits(mins.toString(), usePersianDigits)} دقیقه دیگر"
        }
    }
}
