package com.example

import com.example.util.PersianCalendarUtil
import com.example.util.PersianScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PersianScheduleParserTest {

    // 1405-08-25 = Monday 2026-11-16
    private val fixedMonday = PersianCalendarUtil.gregorianToJalali(2026, 11, 16, persianDow = 2)

    private fun calAt(hour: Int, minute: Int): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    @Test
    fun tableCases_tomorrowMorningAndAfternoon() {
        val case1 = PersianScheduleParser.parse("برنامه برای فردا ساعت ۱۰ جلسه", today = fixedMonday)
        assertTrue(case1.dateDetectedExplicitly)
        assertEquals(PersianCalendarUtil.addDays(fixedMonday, 1).dateKey, case1.targetDate.dateKey)
        assertEquals(10, case1.hour)
        assertEquals(0, case1.minute)
        assertFalse(case1.isAmbiguous)

        val case2 = PersianScheduleParser.parse("پس‌فردا ساعت ۱۷:۳۰ ورزش", today = fixedMonday)
        assertTrue(case2.dateDetectedExplicitly)
        assertEquals(PersianCalendarUtil.addDays(fixedMonday, 2).dateKey, case2.targetDate.dateKey)
        assertEquals(17, case2.hour)
        assertEquals(30, case2.minute)
        assertTrue(case2.cleanedTitle.contains("ورزش"))
    }

    @Test
    fun tableCases_twoDaysAndTuesday() {
        val case3 = PersianScheduleParser.parse("برای دو روز دیگه خرید", today = fixedMonday)
        assertTrue(case3.dateDetectedExplicitly)
        assertEquals(PersianCalendarUtil.addDays(fixedMonday, 2).dateKey, case3.targetDate.dateKey)
        // Must NOT invent 16:00 when no time is specified!
        assertNull(case3.hour)
        assertNull(case3.minute)
        assertTrue(case3.isUnscheduled)
        assertEquals("خرید", case3.cleanedTitle)

        val case4 = PersianScheduleParser.parse("سه‌شنبه ساعت ۱۰ ارائه", today = fixedMonday)
        assertTrue(case4.dateDetectedExplicitly)
        assertEquals("سه‌شنبه", case4.targetDate.weekDayName)
        assertEquals(PersianCalendarUtil.addDays(fixedMonday, 1).dateKey, case4.targetDate.dateKey)
        assertEquals(10, case4.hour)
        assertEquals(0, case4.minute)
        assertEquals("ارائه", case4.cleanedTitle)
    }

    @Test
    fun tableCases_nextSaturdayAndHalfPastTwelveNoonMidnight() {
        val case5 = PersianScheduleParser.parse("شنبه آینده ساعت ۸ پیاده‌روی", today = fixedMonday)
        assertTrue(case5.dateDetectedExplicitly)
        assertEquals("شنبه", case5.targetDate.weekDayName)
        // Next Saturday after Monday is +5 days
        assertEquals(PersianCalendarUtil.addDays(fixedMonday, 5).dateKey, case5.targetDate.dateKey)
        assertEquals(8, case5.hour)
        assertEquals(0, case5.minute)

        val case7 = PersianScheduleParser.parse("۱۲:۳۰ ظهر ناهار", today = fixedMonday)
        assertEquals(12, case7.hour)
        assertEquals(30, case7.minute)

        val case8 = PersianScheduleParser.parse("۱۲ شب خواب", today = fixedMonday)
        assertEquals(0, case8.hour)
        assertEquals(0, case8.minute)

        val midnightHalf = PersianScheduleParser.parse("۱۲:۳۰ شب مطالعه", today = fixedMonday)
        assertEquals(0, midnightHalf.hour)
        assertEquals(30, midnightHalf.minute)
    }

    @Test
    fun tableCases_colloquialAndRelativeTimes() {
        val case6 = PersianScheduleParser.parse("فردا صبح ساعت ۹ و نیم دندان‌پزشکی", today = fixedMonday)
        assertEquals(9, case6.hour)
        assertEquals(30, case6.minute)
        assertTrue(case6.cleanedTitle.contains("دندان") && case6.cleanedTitle.contains("پزشکی"))

        val afternoonHalf = PersianScheduleParser.parse("نیم ساعت بعد از ظهر قرار", today = fixedMonday)
        assertEquals(12, afternoonHalf.hour)
        assertEquals(30, afternoonHalf.minute)

        val quarterToNine = PersianScheduleParser.parse("فردا صبح یک ربع به ۹ جلسه", today = fixedMonday)
        assertEquals(8, quarterToNine.hour)
        assertEquals(45, quarterToNine.minute)

        val halfHourFromNow = PersianScheduleParser.parse(
            "نیم ساعت دیگر تماس",
            today = fixedMonday,
            nowCal = calAt(10, 15)
        )
        assertEquals(10, halfHourFromNow.hour)
        assertEquals(45, halfHourFromNow.minute)

        val quarterFromNow = PersianScheduleParser.parse(
            "یک ربع دیگر ارسال ایمیل",
            today = fixedMonday,
            nowCal = calAt(14, 50)
        )
        assertEquals(15, quarterFromNow.hour)
        assertEquals(5, quarterFromNow.minute)
    }

    @Test
    fun tableCases_reminderOffset_ambiguous_andStandaloneWorkPreserved() {
        val case9 = PersianScheduleParser.parse("ده دقیقه قبل از جلسه گزارش رو یادآوری کن", today = fixedMonday)
        assertEquals(10, case9.reminderOffsetMinutes)

        val case10 = PersianScheduleParser.parse("ساعت ۷ زنگ بزن", today = fixedMonday)
        assertEquals(7, case10.hour)
        assertTrue(case10.isAmbiguous)
        assertNotNull(case10.clarificationQuestion)

        val morningNoNumber = PersianScheduleParser.parse("فردا صبح جلسه", today = fixedMonday)
        assertTrue(morningNoNumber.isAmbiguous)
        assertNull(morningNoNumber.hour)

        val case11 = PersianScheduleParser.parse("کار فردا انجام دادن تکلیف", today = fixedMonday)
        assertTrue(case11.cleanedTitle.contains("کار"))
        assertEquals(PersianCalendarUtil.addDays(fixedMonday, 1).dateKey, case11.targetDate.dateKey)
        assertNull(case11.hour)
    }

    @Test
    fun digits_arabicAndPersianAnd24HourAndDuration() {
        val parsed = PersianScheduleParser.parse("ساعت ۱۸:۴۵ به مدت ۹۰ دقیقه تمرین پیانو", today = fixedMonday)
        assertEquals(18, parsed.hour)
        assertEquals(45, parsed.minute)
        assertEquals(90, parsed.durationMinutes)
        assertFalse(parsed.isAmbiguous)
        assertTrue(parsed.cleanedTitle.contains("پیانو"))

        val arabicDigits = PersianScheduleParser.parse("امروز ساعت ٧ عصر جلسه", today = fixedMonday)
        assertEquals(19, arabicDigits.hour)
        assertEquals(0, arabicDigits.minute)
        assertFalse(arabicDigits.isAmbiguous)

        val weekend = PersianScheduleParser.parse("آخر هفته کوهنوردی", today = fixedMonday)
        assertTrue(weekend.dateDetectedExplicitly)
        assertEquals("پنج‌شنبه", weekend.targetDate.weekDayName)

        val nextWeek = PersianScheduleParser.parse("هفته آینده تحویل پروژه", today = fixedMonday)
        assertTrue(nextWeek.dateDetectedExplicitly)
        assertEquals(PersianCalendarUtil.addDays(fixedMonday, 7).dateKey, nextWeek.targetDate.dateKey)
    }

    @Test
    fun jalaliIsoGregorianRoundTrip_andNumeralFormatting() {
        val iso = PersianCalendarUtil.jalaliToIsoGregorian("1405-08-25")
        assertEquals("2026-11-16", iso)

        val back = PersianCalendarUtil.isoGregorianToJalali("2026-11-16")
        assertNotNull(back)
        assertEquals("1405-08-25", back!!.dateKey)

        assertEquals("۱۴:۳۰", PersianCalendarUtil.formatTime(14, 30, usePersianDigits = true))
        assertEquals("14:30", PersianCalendarUtil.formatTime(14, 30, usePersianDigits = false))
    }
}
