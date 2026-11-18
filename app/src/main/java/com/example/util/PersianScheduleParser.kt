package com.example.util

import java.util.Calendar

data class ParsedScheduleCommand(
    val targetDate: JalaliDate,
    val dateDetectedExplicitly: Boolean,
    val relativeDateHint: String?,
    val hour: Int?,
    val minute: Int?,
    val isTimeExplicit: Boolean,
    val isAmbiguous: Boolean,
    val clarificationQuestion: String?,
    val reminderOffsetMinutes: Int?,
    val durationMinutes: Int?,
    val cleanedTitle: String
) {
    val isUnscheduled: Boolean
        get() = hour == null || minute == null
}

object PersianScheduleParser {

    private val numberWords = linkedMapOf(
        "پنجاه و نه" to 59,
        "پنجاه و پنج" to 55,
        "پنجاه" to 50,
        "چهل و پنج" to 45,
        "چهل" to 40,
        "سی و پنج" to 35,
        "سی" to 30,
        "بیست و پنج" to 25,
        "بیست و چهار" to 24,
        "بیست و سه" to 23,
        "بیست و دو" to 22,
        "بیست و یک" to 21,
        "بیست" to 20,
        "نوزده" to 19,
        "هجده" to 18,
        "هفده" to 17,
        "شانزده" to 16,
        "پانزده" to 15,
        "چهارده" to 14,
        "سیزده" to 13,
        "دوازده" to 12,
        "یازده" to 11,
        "ده" to 10,
        "نه" to 9,
        "هشت" to 8,
        "هفت" to 7,
        "شش" to 6,
        "شیش" to 6,
        "پنج" to 5,
        "چهار" to 4,
        "سه" to 3,
        "دو" to 2,
        "یک" to 1,
        "یه" to 1,
        "صفر" to 0
    )

    private val smallHourWords = linkedMapOf(
        "بیست و چهار" to 24,
        "بیست و سه" to 23,
        "بیست و دو" to 22,
        "بیست و یک" to 21,
        "بیست" to 20,
        "نوزده" to 19,
        "هجده" to 18,
        "هفده" to 17,
        "شانزده" to 16,
        "پانزده" to 15,
        "چهارده" to 14,
        "سیزده" to 13,
        "دوازده" to 12,
        "یازده" to 11,
        "ده" to 10,
        "نه" to 9,
        "هشت" to 8,
        "هفت" to 7,
        "شش" to 6,
        "شیش" to 6,
        "پنج" to 5,
        "چهار" to 4,
        "سه" to 3,
        "دو" to 2,
        "یک" to 1,
        "یه" to 1
    )

    // Ordered so compound names match before شنبه
    private val weekdayPatterns = listOf(
        "چهارشنبه" to 4,
        "چهار شنبه" to 4,
        "پنجشنبه" to 5,
        "پنج شنبه" to 5,
        "یکشنبه" to 1,
        "یک شنبه" to 1,
        "دوشنبه" to 2,
        "دو شنبه" to 2,
        "سه شنبه" to 3,
        "سشنبه" to 3,
        "جمعه" to 6,
        "شنبه" to 0
    )

    private val numberWordAlternation = numberWords.keys
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    private val hourWordAlternation = smallHourWords.keys
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    fun parse(
        rawInput: String,
        today: JalaliDate = PersianCalendarUtil.today(),
        fallbackDate: JalaliDate = today,
        nowCal: Calendar = Calendar.getInstance()
    ): ParsedScheduleCommand {
        val normalized = normalizePersian(rawInput)
        val todayDow = today.dayOfWeekPersianIndex

        var detectedOffset: Int? = null
        var explicitDateOverride: JalaliDate? = null
        var relativeHint: String? = null
        var titleWork = " $normalized "

        fun stripTokenSpan(span: String) {
            titleWork = titleWork.replaceFirst(span, " ")
        }

        // --- 0. Duration detection (e.g. "به مدت ۲ ساعت" / "۴۵ دقیقه طول") ---
        var parsedDuration: Int? = null
        val durationRegex = Regex(
            """(?<=^|\s)(?:به\s+مدت\s+)?(\d+|$numberWordAlternation)\s*(ساعت|دقیقه)(?:\s+طول|\s+مدت)?(?=\s|$)"""
        )
        // Only match as duration if NOT part of "ساعت X" or "X دقیقه دیگه" or "X دقیقه قبل از" or "یک ربع به"
        val durationCandidate = Regex(
            """(?<=^|\s)(?:به\s+مدت\s+|مدت\s+)(\d+|$numberWordAlternation)\s*(ساعت|دقیقه)(?=\s|$)"""
        ).find(normalized)
        if (durationCandidate != null) {
            val n = parseNumberToken(durationCandidate.groupValues[1])
            val unit = durationCandidate.groupValues[2]
            if (n != null && n > 0) {
                parsedDuration = if (unit == "ساعت") (n * 60).coerceAtMost(720) else n.coerceAtMost(720)
                stripTokenSpan(durationCandidate.value)
            }
        } else {
            val lengthPhrase = Regex("""(?<=^|\s)(یک\s+ساعت|یه\s+ساعت|نیم\s+ساعت|\d+\s+دقیقه)\s+(?:طول\s+می\s*کشه|طول\s+میکشد|است|باشه)(?=\s|$)""")
                .find(normalized)
            if (lengthPhrase != null) {
                val part = lengthPhrase.groupValues[1]
                parsedDuration = when {
                    part.contains("نیم") -> 30
                    part.contains("ساعت") -> 60
                    else -> Regex("""\d+""").find(part)?.value?.toIntOrNull()?.coerceIn(5, 720)
                }
                stripTokenSpan(lengthPhrase.value)
            }
        }

        // --- 1. Reminder offset detection (e.g. "ده دقیقه قبل از جلسه") ---
        var reminderOffset: Int? = null
        val reminderBeforeRegex = Regex(
            """(?<=^|\s)(?:(\d+|$numberWordAlternation)\s+دقیقه|یک\s+ربع|نیم\s+ساعت)\s+(?:قبل|پیش)\s+(?:از\s+)?(?=\S)"""
        )
        val reminderMatch = reminderBeforeRegex.find(normalized)
        if (reminderMatch != null) {
            reminderOffset = when {
                reminderMatch.value.contains("یک ربع") -> 15
                reminderMatch.value.contains("نیم ساعت") -> 30
                else -> parseNumberToken(reminderMatch.groupValues[1])
            }?.coerceIn(0, 120)
            stripTokenSpan(reminderMatch.value)
        }

        // --- 2. Date Detection ---
        val hasNextWeekModifier = Regex("""(?<=^|\s)هفته\s*(?:بعد|دیگه|دیگر|آینده)(?=\s|$)""").containsMatchIn(normalized)

        // 2a. Explicit Jalali numeric date e.g. 1405/08/25 or 1405-08-25
        val numericDateRegex = Regex("""(?<=^|\s)(1[34]\d{2})[/\-](\d{1,2})[/\-](\d{1,2})(?=\s|$)""")
        val numericDateMatch = numericDateRegex.find(normalized)
        if (numericDateMatch != null) {
            val y = numericDateMatch.groupValues[1].toIntOrNull()
            val m = numericDateMatch.groupValues[2].toIntOrNull()
            val d = numericDateMatch.groupValues[3].toIntOrNull()
            if (y != null && m in 1..12 && d != null && d in 1..PersianCalendarUtil.daysInJalaliMonth(y, m!!)) {
                explicitDateOverride = PersianCalendarUtil.dateForJalali(y, m, d)
                relativeHint = explicitDateOverride.formattedDayAndMonth()
                stripTokenSpan(numericDateMatch.value)
            }
        }

        // 2b. "N روز دیگه / بعد / دیگر / آینده"
        if (explicitDateOverride == null) {
            val nDaysRegex = Regex("""(?<=^|\s)(\d+|$numberWordAlternation)\s+روز\s+(?:دیگه|دیگر|بعد|آینده)(?=\s|$)""")
            val nDaysMatch = nDaysRegex.find(normalized)
            if (nDaysMatch != null) {
                val n = parseNumberToken(nDaysMatch.groupValues[1])
                if (n != null) {
                    detectedOffset = n
                    relativeHint = "${PersianCalendarUtil.toPersianDigits(n)} روز دیگر"
                    stripTokenSpan(nDaysMatch.value)
                }
            }
        }

        // 2c. Explicit relative day words ("پس‌فردا", "فردا", "امروز", "دیروز", "آخر هفته")
        if (explicitDateOverride == null && detectedOffset == null) {
            val pasFardaRegex = Regex("""(?<=^|\s)پس\s*فردا(?=\s|$)""")
            val fardaRegex = Regex("""(?<=^|\s)فردا(?=\s|$)""")
            val emroozRegex = Regex("""(?<=^|\s)امروز(?=\s|$)""")
            val diroozRegex = Regex("""(?<=^|\s)دیروز(?=\s|$)""")
            val akhareHafteRegex = Regex("""(?<=^|\s)آخر\s*هفته(?=\s|$)""")

            when {
                pasFardaRegex.containsMatchIn(normalized) -> {
                    detectedOffset = 2
                    relativeHint = "پس‌فردا"
                    titleWork = titleWork.replace(pasFardaRegex, " ")
                }
                fardaRegex.containsMatchIn(normalized) -> {
                    detectedOffset = 1
                    relativeHint = "فردا"
                    titleWork = titleWork.replace(fardaRegex, " ")
                }
                emroozRegex.containsMatchIn(normalized) -> {
                    detectedOffset = 0
                    relativeHint = "امروز"
                    titleWork = titleWork.replace(emroozRegex, " ")
                }
                diroozRegex.containsMatchIn(normalized) -> {
                    detectedOffset = -1
                    relativeHint = "دیروز"
                    titleWork = titleWork.replace(diroozRegex, " ")
                }
                akhareHafteRegex.containsMatchIn(normalized) -> {
                    // Weekend in Iran starts Thursday (persianDow = 5)
                    val offset = if (todayDow >= 5) 0 else (5 - todayDow)
                    detectedOffset = offset
                    relativeHint = "آخر هفته (${PersianCalendarUtil.weekDayName((todayDow + offset) % 7)})"
                    titleWork = titleWork.replace(akhareHafteRegex, " ")
                }
            }
        }

        // 2d. Persian weekday names with optional "آینده" or "هفته آینده"
        if (explicitDateOverride == null && detectedOffset == null) {
            for ((pattern, targetDow) in weekdayPatterns) {
                val weekdayWithModifierRegex = Regex(
                    """(?<=^|\s)${Regex.escape(pattern)}(?:\s+(?:هفته\s+)?(آینده|بعد|دیگه|دیگر))?(?=\s|$)"""
                )
                val match = weekdayWithModifierRegex.find(normalized)
                if (match != null) {
                    val hasModifier = match.groupValues[1].isNotEmpty() || hasNextWeekModifier
                    val rawDiff = (targetDow - todayDow + 7) % 7
                    val offset = when {
                        hasNextWeekModifier && match.value.contains("هفته") -> {
                            if (rawDiff == 0) 7 else rawDiff + 7
                        }
                        hasModifier -> {
                            if (rawDiff == 0) 7 else rawDiff
                        }
                        else -> rawDiff
                    }
                    detectedOffset = offset
                    relativeHint = when (offset) {
                        0 -> "امروز (${PersianCalendarUtil.weekDayName(targetDow)})"
                        1 -> "فردا (${PersianCalendarUtil.weekDayName(targetDow)})"
                        2 -> "پس‌فردا (${PersianCalendarUtil.weekDayName(targetDow)})"
                        else -> "${PersianCalendarUtil.weekDayName(targetDow)} (${PersianCalendarUtil.toPersianDigits(offset)} روز دیگر)"
                    }
                    stripTokenSpan(match.value)
                    break
                }
            }
        }

        // 2e. Standalone "هفته بعد / دیگه / آینده"
        if (explicitDateOverride == null && detectedOffset == null && hasNextWeekModifier) {
            detectedOffset = 7
            relativeHint = "هفته آینده"
        }
        if (hasNextWeekModifier) {
            titleWork = titleWork.replace(Regex("""(?<=^|\s)هفته\s*(?:بعد|دیگه|دیگر|آینده)(?=\s|$)"""), " ")
        }

        var targetDate = explicitDateOverride ?: if (detectedOffset != null) {
            PersianCalendarUtil.addDays(today, detectedOffset)
        } else {
            fallbackDate
        }

        // --- 3. Time Detection ---
        var parsedHour: Int? = null
        var parsedMinute: Int? = null
        var isTimeExplicit = false
        var isAmbiguous = false
        var clarificationQuestion: String? = null

        // 3a-priority. "نیم ساعت بعد از ظهر" -> 12:30 (must check BEFORE "نیم ساعت بعد")
        run {
            val halfAfterNoonRegex = Regex("""(?<=^|\s)نیم\s+ساعت\s+بعد\s+از\s+ظهر(?=\s|$)""")
            val halfAfterNoonMatch = halfAfterNoonRegex.find(normalized)
            if (halfAfterNoonMatch != null) {
                parsedHour = 12
                parsedMinute = 30
                isTimeExplicit = true
                stripTokenSpan(halfAfterNoonMatch.value)
            }
        }

        // 3a. Relative clock offsets from now: "نیم ساعت دیگر", "یک ربع دیگر", "۲۰ دقیقه دیگه", "۲ ساعت بعد"
        if (!isTimeExplicit) {
            val relHalfHourRegex = Regex("""(?<=^|\s)نیم\s+ساعت\s+(?:دیگه|دیگر|آینده|بعد(?!\s+از\s+ظهر))(?=\s|$)""")
            val relQuarterRegex = Regex("""(?<=^|\s)(?:یک|یه)\s+ربع\s+(?:دیگه|دیگر|آینده|بعد(?!\s+از\s+ظهر))(?=\s|$)""")
            val relMinutesRegex = Regex("""(?<=^|\s)(\d+|$numberWordAlternation)\s+دقیقه\s+(?:دیگه|دیگر|آینده|بعد(?!\s+از\s+ظهر))(?=\s|$)""")
            val relHoursRegex = Regex("""(?<=^|\s)(\d+|$hourWordAlternation)\s+ساعت\s+(?:دیگه|دیگر|آینده|بعد(?!\s+از\s+ظهر))(?=\s|$)""")

            when {
                relHalfHourRegex.containsMatchIn(normalized) -> {
                    val cal = (nowCal.clone() as Calendar).apply { add(Calendar.MINUTE, 30) }
                    parsedHour = cal.get(Calendar.HOUR_OF_DAY)
                    parsedMinute = cal.get(Calendar.MINUTE)
                    if (detectedOffset == null && explicitDateOverride == null) {
                        targetDate = PersianCalendarUtil.fromCalendar(cal)
                        relativeHint = "۳۰ دقیقه دیگر"
                    }
                    isTimeExplicit = true
                    titleWork = titleWork.replace(relHalfHourRegex, " ")
                }
                relQuarterRegex.containsMatchIn(normalized) -> {
                    val cal = (nowCal.clone() as Calendar).apply { add(Calendar.MINUTE, 15) }
                    parsedHour = cal.get(Calendar.HOUR_OF_DAY)
                    parsedMinute = cal.get(Calendar.MINUTE)
                    if (detectedOffset == null && explicitDateOverride == null) {
                        targetDate = PersianCalendarUtil.fromCalendar(cal)
                        relativeHint = "۱۵ دقیقه دیگر"
                    }
                    isTimeExplicit = true
                    titleWork = titleWork.replace(relQuarterRegex, " ")
                }
                relMinutesRegex.containsMatchIn(normalized) -> {
                    val mMatch = relMinutesRegex.find(normalized)!!
                    val mins = parseNumberToken(mMatch.groupValues[1]) ?: 15
                    val cal = (nowCal.clone() as Calendar).apply { add(Calendar.MINUTE, mins) }
                    parsedHour = cal.get(Calendar.HOUR_OF_DAY)
                    parsedMinute = cal.get(Calendar.MINUTE)
                    if (detectedOffset == null && explicitDateOverride == null) {
                        targetDate = PersianCalendarUtil.fromCalendar(cal)
                        relativeHint = "${PersianCalendarUtil.toPersianDigits(mins)} دقیقه دیگر"
                    }
                    isTimeExplicit = true
                    stripTokenSpan(mMatch.value)
                }
                relHoursRegex.containsMatchIn(normalized) -> {
                    val hMatch = relHoursRegex.find(normalized)!!
                    val hrs = parseNumberToken(hMatch.groupValues[1]) ?: 1
                    val cal = (nowCal.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, hrs) }
                    parsedHour = cal.get(Calendar.HOUR_OF_DAY)
                    parsedMinute = cal.get(Calendar.MINUTE)
                    if (detectedOffset == null && explicitDateOverride == null) {
                        targetDate = PersianCalendarUtil.fromCalendar(cal)
                        relativeHint = "${PersianCalendarUtil.toPersianDigits(hrs)} ساعت دیگر"
                    }
                    isTimeExplicit = true
                    stripTokenSpan(hMatch.value)
                }
            }
        }

        // 3c. "یک ربع به X [صبح/ظهر/عصر/شب]" or "ده دقیقه به X"
        if (!isTimeExplicit) {
            val beforeHourRegex = Regex(
                """(?<=^|\s)(?:ساعت\s+)?(?:(یک|یه)\s+ربع|(\d+|$numberWordAlternation)\s+دقیقه)\s+به\s+(\d{1,2}|$hourWordAlternation)(?:\s+(صبح|بامداد|ظهر|بعد\s*از\s*ظهر|بعدازظهر|عصر|شب))?(?=\s|$)"""
            )
            val m = beforeHourRegex.find(normalized)
            if (m != null) {
                val delta = if (m.groupValues[1].isNotEmpty()) 15 else (parseNumberToken(m.groupValues[2]) ?: 15).coerceIn(1, 59)
                val targetH = parseNumberToken(m.groupValues[3])
                val period = normalizePeriod(m.groupValues[4])
                if (targetH != null && targetH in 1..24) {
                    if (period == null && targetH in 1..7) {
                        isAmbiguous = true
                        clarificationQuestion =
                            "منظورتان ${PersianCalendarUtil.formatTime(targetH - 1, 60 - delta)} صبح است یا عصر؟"
                    } else {
                        val resolvedTargetH = adjustHourWithPeriod(if (targetH == 24) 0 else targetH, period)
                        val totalMinutes = ((resolvedTargetH * 60 - delta) + 24 * 60) % (24 * 60)
                        parsedHour = totalMinutes / 60
                        parsedMinute = totalMinutes % 60
                        isTimeExplicit = true
                    }
                    stripTokenSpan(m.value)
                }
            }
        }

        // 3d. HH:MM with optional "ساعت" and period ("ساعت ۷:۳۰ عصر", "۱۲:۳۰ شب", "۱۸:۴۵")
        if (!isTimeExplicit && !isAmbiguous) {
            val hhmmRegex = Regex(
                """(?<=^|\s)(?:ساعت\s+)?([01]?\d|2[0-3]):([0-5]\d)(?:\s*(صبح|بامداد|ظهر|بعد\s*از\s*ظهر|بعدازظهر|عصر|شب|ق\.?ظ|ب\.?ظ))?(?=\s|$)"""
            )
            val match = hhmmRegex.find(normalized)
            if (match != null) {
                val rawH = match.groupValues[1].toIntOrNull()
                val rawM = match.groupValues[2].toIntOrNull() ?: 0
                val period = normalizePeriod(match.groupValues[3])
                if (rawH != null) {
                    if (rawH in 13..23 || rawH == 0) {
                        parsedHour = rawH
                        parsedMinute = rawM
                        isTimeExplicit = true
                    } else if (period != null) {
                        parsedHour = adjustHourWithPeriod(rawH, period).coerceIn(0, 23)
                        parsedMinute = rawM
                        isTimeExplicit = true
                    } else if (rawH in 1..7) {
                        parsedHour = rawH
                        parsedMinute = rawM
                        isAmbiguous = true
                        clarificationQuestion =
                            "منظورتان ساعت ${PersianCalendarUtil.formatTime(rawH, rawM)} صبح است یا ${
                                PersianCalendarUtil.formatTime(rawH + 12, rawM)
                            }؟"
                    } else {
                        // 8..12 without period -> standard daytime 08..12
                        parsedHour = rawH
                        parsedMinute = rawM
                        isTimeExplicit = true
                    }
                    stripTokenSpan(match.value)
                }
            }
        }

        // 3e. "ساعت X [و نیم | و ربع | و N دقیقه] [صبح/ظهر/عصر/شب]"
        if (!isTimeExplicit && !isAmbiguous) {
            val saatRegex = Regex(
                """(?<=^|\s)ساعت\s+(\d{1,2}|$hourWordAlternation)(?:\s*و\s*(نیم|ربع|(\d{1,2}|$numberWordAlternation)(?:\s+دقیقه)?))?(?:\s*(صبح|بامداد|ظهر|بعد\s*از\s*ظهر|بعدازظهر|عصر|شب|ق\.?ظ|ب\.?ظ))?(?=\s|$)"""
            )
            val match = saatRegex.find(normalized)
            if (match != null) {
                val rawH = parseNumberToken(match.groupValues[1])
                val minPart = match.groupValues[2]
                val minNumericPart = match.groupValues[3]
                val period = normalizePeriod(match.groupValues[4])
                if (rawH != null && rawH in 0..24) {
                    val minuteVal = when {
                        minPart == "نیم" -> 30
                        minPart == "ربع" -> 15
                        minNumericPart.isNotEmpty() -> (parseNumberToken(minNumericPart) ?: 0).coerceIn(0, 59)
                        else -> 0
                    }
                    val normalizedH = if (rawH == 24) 0 else rawH
                    if (normalizedH in 13..23 || normalizedH == 0) {
                        parsedHour = normalizedH
                        parsedMinute = minuteVal
                        isTimeExplicit = true
                    } else if (period != null) {
                        parsedHour = adjustHourWithPeriod(normalizedH, period).coerceIn(0, 23)
                        parsedMinute = minuteVal
                        isTimeExplicit = true
                    } else if (normalizedH in 1..7) {
                        parsedHour = normalizedH
                        parsedMinute = minuteVal
                        isAmbiguous = true
                        clarificationQuestion =
                            "منظورتان ساعت ${PersianCalendarUtil.formatTime(normalizedH, minuteVal)} صبح است یا ${
                                PersianCalendarUtil.formatTime(normalizedH + 12, minuteVal)
                            }؟"
                    } else {
                        // 8..12 without period -> 08:xx .. 12:xx
                        parsedHour = normalizedH
                        parsedMinute = minuteVal
                        isTimeExplicit = true
                    }
                    stripTokenSpan(match.value)
                }
            }
        }

        // 3e-bis. Explicit number + period WITHOUT "ساعت" (e.g. "۱۲ شب خواب", "۷ عصر جلسه", "۹ و نیم صبح")
        if (!isTimeExplicit && !isAmbiguous) {
            val bareHourWithPeriodRegex = Regex(
                """(?<=^|\s)(\d{1,2}|$hourWordAlternation)(?:\s*و\s*(نیم|ربع))?\s+(صبح|بامداد|ظهر|بعد\s*از\s*ظهر|بعدازظهر|عصر|شب)(?=\s|$)"""
            )
            val match = bareHourWithPeriodRegex.find(normalized)
            if (match != null) {
                val rawH = parseNumberToken(match.groupValues[1])
                val minPart = match.groupValues[2]
                val period = normalizePeriod(match.groupValues[3])
                if (rawH != null && rawH in 1..24 && period != null) {
                    val minuteVal = when (minPart) {
                        "نیم" -> 30
                        "ربع" -> 15
                        else -> 0
                    }
                    val normalizedH = if (rawH == 24) 0 else rawH
                    parsedHour = adjustHourWithPeriod(normalizedH, period).coerceIn(0, 23)
                    parsedMinute = minuteVal
                    isTimeExplicit = true
                    stripTokenSpan(match.value)
                }
            }
        }

        // 3f. Standalone "نیمه‌شب" -> 00:00
        if (!isTimeExplicit && !isAmbiguous) {
            val midnightRegex = Regex("""(?<=^|\s)نیمه\s*شب(?=\s|$)""")
            val midnightMatch = midnightRegex.find(normalized)
            if (midnightMatch != null) {
                parsedHour = 0
                parsedMinute = 0
                isTimeExplicit = true
                stripTokenSpan(midnightMatch.value)
            }
        }

        // 3g. Period-only without explicit hour (e.g. "فردا صبح جلسه", "عصر ورزش") -> mark ambiguous, DO NOT invent a fake time!
        if (!isTimeExplicit && !isAmbiguous) {
            val barePeriodRegex = Regex("""(?<=^|\s)(صبح|بامداد|ظهر|بعد\s*از\s*ظهر|بعدازظهر|عصر|شب)(?=\s|$)""")
            val periodMatch = barePeriodRegex.find(normalized)
            if (periodMatch != null) {
                val periodLabel = periodMatch.value.trim()
                isAmbiguous = true
                clarificationQuestion = "چه ساعتی در بازه «$periodLabel» مد نظرتان است؟ (یا بدون ساعت ثبت شود؟)"
            }
        }

        // --- 4. Token-aware Title Cleanup (NEVER removes standalone "کار") ---
        val fillerRegex = Regex(
            """(?<=^|\s)(برنامه|تسک|رو|را|برای|واسه|به|در|توی|تو|بذار|بزار|بساز|طراحی کن|طرح کن|تنظیم کن|بچین|باشه|کن|ایجاد کن|ثبت کن|اضافه کن|یادآوری کن|لطفا|لطفاً)(?=\s|$)"""
        )
        var cleaned = titleWork
        repeat(3) {
            cleaned = cleaned.replace(fillerRegex, " ")
        }
        cleaned = cleaned
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('،', ',', '.', '؛', ':', '-')
            .trim()
            .take(80)
            .ifEmpty {
                "برنامه ${targetDate.weekDayName} ${targetDate.formattedDayAndMonth()}"
            }

        return ParsedScheduleCommand(
            targetDate = targetDate,
            dateDetectedExplicitly = detectedOffset != null || explicitDateOverride != null,
            relativeDateHint = relativeHint,
            hour = parsedHour,
            minute = parsedMinute,
            isTimeExplicit = isTimeExplicit,
            isAmbiguous = isAmbiguous,
            clarificationQuestion = clarificationQuestion,
            reminderOffsetMinutes = reminderOffset,
            durationMinutes = parsedDuration,
            cleanedTitle = cleaned
        )
    }

    private fun parseNumberToken(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toIntOrNull() ?: numberWords[trimmed]
    }

    private fun normalizePeriod(raw: String): String? {
        val clean = raw.replace(Regex("""\s+"""), " ").trim()
        if (clean.isEmpty()) return null
        return when {
            clean == "صبح" || clean == "بامداد" || clean.startsWith("ق") -> "صبح"
            clean == "ظهر" -> "ظهر"
            clean.contains("بعد") || clean == "عصر" || clean.startsWith("ب") -> "عصر"
            clean == "شب" -> "شب"
            else -> null
        }
    }

    /**
     * Deterministic 12h -> 24h conversion:
     * - 12 ظهر -> 12
     * - 12 شب -> 0
     * - 12 صبح -> 0
     * - 7 عصر / 7 شب -> 19
     * - 7 صبح -> 7
     * - 1..4 ظهر -> 13..16
     */
    fun adjustHourWithPeriod(rawHour: Int, period: String?): Int {
        return when (period) {
            "عصر" -> when (rawHour) {
                in 1..11 -> rawHour + 12
                12 -> 12
                else -> rawHour
            }
            "شب" -> when (rawHour) {
                12 -> 0
                in 1..11 -> rawHour + 12
                else -> rawHour
            }
            "ظهر" -> when (rawHour) {
                12 -> 12
                in 1..4 -> rawHour + 12
                else -> rawHour
            }
            "صبح" -> when (rawHour) {
                12 -> 0
                else -> rawHour
            }
            else -> rawHour
        }
    }

    fun normalizePersian(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة' -> sb.append('ه')
                '\u200C' -> sb.append(' ')
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
