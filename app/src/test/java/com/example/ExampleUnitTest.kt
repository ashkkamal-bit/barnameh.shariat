package com.example

import com.example.util.PersianCalendarUtil
import com.example.util.PersianScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun gregorianToJalali_convertsKnownDateAccurately() {
        val jalali = PersianCalendarUtil.gregorianToJalali(2026, 11, 16)
        assertEquals(1405, jalali.year)
        assertEquals(8, jalali.month)
        assertEquals(25, jalali.day)
        assertEquals("آبان", jalali.monthName)
    }

    @Test
    fun persianDigitsAndTimeFormatting_isAccurate() {
        assertEquals("۱۴:۳۰", PersianCalendarUtil.formatTime(14, 30, usePersianDigits = true))
        assertEquals("09:05", PersianCalendarUtil.formatTime(9, 5, usePersianDigits = false))
    }

    @Test
    fun parser_detectsTomorrowAndDayAfterTomorrow() {
        val today = PersianCalendarUtil.today()
        val tomorrow = PersianCalendarUtil.offsetFromToday(1)
        val dayAfter = PersianCalendarUtil.offsetFromToday(2)

        val parsedTomorrow = PersianScheduleParser.parse("برنامه برای فردا ساعت ۱۰ جلسه تیم", today = today)
        assertTrue(parsedTomorrow.dateDetectedExplicitly)
        assertEquals(tomorrow.dateKey, parsedTomorrow.targetDate.dateKey)
        assertEquals(10, parsedTomorrow.hour)

        val parsedTwoDays = PersianScheduleParser.parse("برنامه برای دو روز دیگه ساعت ۱۷ ورزش", today = today)
        assertTrue(parsedTwoDays.dateDetectedExplicitly)
        assertEquals(dayAfter.dateKey, parsedTwoDays.targetDate.dateKey)
        assertEquals(17, parsedTwoDays.hour)
    }
}
